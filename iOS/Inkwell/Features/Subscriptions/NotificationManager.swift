//
//  NotificationManager.swift
//  Inkwell
//
//  Created by Letta on 20/06/2026.
//
//  Manages local notifications for new documents from subscribed
//  publications. A Jetstream event while the Reader is open, or a poll
//  triggered by the app's background fetch/manual refresh, reaches the same
//  delivery path:
//
//  1. Fetches the user's subscriptions (site.standard.graph.subscription
//     records).
//  2. For each subscription, fetches documents from the publication
//     author's repo.
//  3. Compares document URIs against the last-seen set stored in
//     UserDefaults.
//  4. For any new documents, schedules a local notification and updates
//     the last-seen set.
//
//  This is still a local-notification model — there is no remote push
//  provider. Jetstream gives an open Reader immediate updates; iOS
//  background refresh polls when the app is not active.
//

import Foundation
import ATProtoKit
import InkwellShared
import OSLog
import UserNotifications
import Observation

@MainActor
@Observable
final class NotificationManager {
    private let logger = Logger(subsystem: "uk.ewancroft.Inkwell", category: "Notifications")
    static let shared = NotificationManager()

    // MARK: - State

    /// Number of unread notifications (new documents since last viewed).
    private(set) var unreadCount = 0

    /// The most recent notifications, newest first.
    private(set) var notifications: [StandardSiteNotification] = []

    // MARK: - Storage

    private let defaults = UserDefaults.standard
    private let storagePrefix = "standardSite.account."
    private var activeAccountDID: String?

    private func key(_ name: String, for did: String) -> String {
        "\(storagePrefix)\(did).\(name)"
    }

    /// User-facing on/off switch, surfaced in SettingsView. Distinct from
    /// the OS permission: this gates whether a *banner* is sent, not
    /// whether polling happens -- the in-app notification list and unread
    /// badge keep working either way, since they reflect "new documents
    /// exist", not "you were interrupted about them".
    var notificationsEnabled: Bool {
        get { activeAccountDID.flatMap { defaults.object(forKey: key("notificationsEnabled", for: $0)) as? Bool } ?? true }
        set { if let did = activeAccountDID { defaults.set(newValue, forKey: key("notificationsEnabled", for: did)) } }
    }

    private init() {}

    func activate(accountDID did: String) {
        guard activeAccountDID != did else { return }
        activeAccountDID = did
        if let data = defaults.data(forKey: key("notifications", for: did)),
           let stored = try? JSONDecoder().decode([StandardSiteNotification].self, from: data) {
            notifications = stored
        } else {
            notifications = []
        }
        unreadCount = defaults.integer(forKey: key("unreadCount", for: did))
    }

    func deactivate() {
        activeAccountDID = nil
        notifications = []
        unreadCount = 0
    }

    // MARK: - Permission

    /// Requests permission to send local notifications. Call this on
    /// first launch or when the user first subscribes.
    func requestPermission() async {
        if TestingMode.suppressesInterruptions { return }
        let center = UNUserNotificationCenter.current()
        do {
            try await center.requestAuthorization(options: [.alert, .badge, .sound])
        } catch {
            // Permission denied — notifications just won't fire, but the
            // in-app notification list still works.
            logger.error("[NotificationManager] authorization request failed: \(error.localizedDescription)")
        }
    }

    // MARK: - Polling

    /// Records a just-created document received through the Reader's live
    /// Jetstream connection. The caller must already have established that
    /// the document belongs to a subscription.
    ///
    /// Background refresh still uses polling, but both paths share the
    /// seen-URI store and delivery rules so a foreground event never results
    /// in a second banner when the next refresh runs.
    func recordLiveDocument(
        _ document: DocumentEntry,
        publication: PublicationEntry?,
        accountDID: String
    ) async {
        activate(accountDID: accountDID)
        var seen = lastSeenURIs
        guard !seen.contains(document.uri) else { return }

        let labels = (document.record.labels?.values.map { ModerationLabel(value: $0.value, source: nil) } ?? [])
            + (publication?.record.labels?.values.map { ModerationLabel(value: $0.value, source: nil) } ?? [])
        let sensitive = shouldRedactNotification(
            title: document.record.title,
            description: document.record.description,
            textContent: document.record.textContent,
            labels: labels
        )
        await recordNewDocuments([(doc: document, pub: publication, sensitive: sensitive)])
        seen.insert(document.uri)
        saveLastSeenURIs(seen)
        if let publicationURI = publication?.uri {
            saveInitializedPublications(self.initializedPublications().union([publicationURI]))
        }
        if let did = activeAccountDID { defaults.set(Date(), forKey: key("lastPollTime", for: did)) }
    }

    /// Polls subscribed publications for new documents and sends local
    /// notifications for any that are new since the last poll.
    ///
    /// - Parameter loginStateManager: The authenticated session manager.
    func pollForNewDocuments(loginStateManager: LoginStateManager) async {
        guard loginStateManager.isAuthenticated, let accountDID = loginStateManager.currentDID else {
            deactivate()
            return
        }
        activate(accountDID: accountDID)

        // Every fetch below uses try? so a DPoP nonce collision (e.g.
        // BrowseDocumentsView racing the same fetchSubscriptions call)
        // degrades gracefully instead of failing the whole poll — polling
        // is best-effort. The cache in LoginStateManager ensures subsequent
        // polls hit memory, not the network.
        let subs = (try? await loginStateManager.fetchSubscriptions()) ?? []
        var newDocs: [(doc: DocumentEntry, pub: PublicationEntry?, sensitive: Bool)] = []
        var allSeenURIs = Set<String>(lastSeenURIs)
        var initializedPublications = initializedPublications()

        for sub in subs {
            guard let pubURI = sub.publicationURI else { continue }

            // Fetch the publication record for metadata.
            let pubs: [PublicationEntry] = (try? await loginStateManager.fetchPublications(fromDID: pubURI.did)) ?? []
            let pubEntry = pubs.first(where: { $0.uri == sub.record.publication })

            // Fetch documents from the publication author's repo.
            guard let docs = try? await loginStateManager.fetchDocuments(fromDID: pubURI.did) else {
                // A failed scan is not an empty repository. Leave this
                // publication's checkpoint untouched for a later retry.
                continue
            }
            let publicationURI = "at://\(pubURI.did)/\(pubURI.collection)/\(pubURI.recordKey)"
            let wasInitialized = initializedPublications.contains(publicationURI)

            // Filter documents that belong to this publication.
            let pubDocs: [DocumentEntry]
            if let pubEntry {
                pubDocs = docs.filter {
                    sharedDocumentBelongsToPublication(
                        documentSite: $0.record.site,
                        publicationUri: pubEntry.uri,
                        publicationUrl: pubEntry.record.url
                    )
                }
            } else {
                pubDocs = docs.filter { $0.record.site == sub.record.publication }
            }

            // Find documents we haven't seen before.
            for doc in pubDocs {
                if !allSeenURIs.contains(doc.uri) {
                    if wasInitialized {
                        let labels = (doc.record.labels?.values.map { ModerationLabel(value: $0.value, source: nil) } ?? [])
                            + (pubEntry?.record.labels?.values.map { ModerationLabel(value: $0.value, source: nil) } ?? [])
                        let sensitive = shouldRedactNotification(
                            title: doc.record.title,
                            description: doc.record.description,
                            textContent: doc.record.textContent,
                            labels: labels
                        )
                        newDocs.append((doc, pubEntry, sensitive))
                    }
                    allSeenURIs.insert(doc.uri)
                }
            }
            // A successful empty scan is still authoritative and establishes
            // a baseline for newly added subscriptions.
            initializedPublications.insert(publicationURI)
        }

        await recordNewDocuments(newDocs)

        // Update last-seen URIs and poll time.
        saveLastSeenURIs(allSeenURIs)
        saveInitializedPublications(initializedPublications)
        if let did = activeAccountDID { defaults.set(Date(), forKey: key("lastPollTime", for: did)) }
    }

    // MARK: - Delivery

    /// Applies the identical first-run, grouping and history policy to
    /// foreground Jetstream events and background polling results.
    private func recordNewDocuments(_ newDocs: [(doc: DocumentEntry, pub: PublicationEntry?, sensitive: Bool)]) async {
        guard !newDocs.isEmpty else { return }

        // Only send notifications if this isn't the first poll (first
        // poll just establishes the baseline of existing documents).
        guard let did = activeAccountDID else { return }
        let lastPoll = defaults.object(forKey: key("lastPollTime", for: did)) as? Date
        let isFirstPoll = isFirstPoll(lastPollEpochMillis: Int64(lastPoll?.timeIntervalSince1970 ?? -1))

        if !isFirstPoll {
            // Sort newest first.
            let sortedDocs = newDocs.sorted { $0.doc.record.publishedAt > $1.doc.record.publishedAt }

            switch notificationStyle(newDocCount: Int32(sortedDocs.count)) {
            case .single:
                let doc = sortedDocs[0]
                if notificationsEnabled {
                    await sendNotification(
                        title: doc.sensitive ? "New document from a subscribed publication" : (doc.pub?.record.name ?? "New Document"),
                        body: doc.sensitive ? "Open Inkwell to view this document" : doc.doc.record.title,
                        documentURI: doc.doc.uri
                    )
                }
            case .summary(let count):
                let newest = sortedDocs[0]
                if notificationsEnabled {
                    await sendNotification(
                        title: String(localized: "^[\(count) new document](inflect: true)"),
                        body: newest.sensitive ? "Open Inkwell to view your new documents" : "Latest: \(newest.doc.record.title) from \(newest.pub?.record.name ?? "a publication")",
                        documentURI: newest.doc.uri
                    )
                }
            case .none:
                break
            }

            // Update in-app notification list.
            let newNotifications = sortedDocs.map { doc in
                notificationMetadata(
                    documentURI: doc.doc.uri,
                    title: doc.doc.record.title,
                    publicationName: doc.pub?.record.name,
                    publishedAt: doc.doc.record.publishedAt,
                    sensitive: doc.sensitive,
                    date: Date()
                )
            }
            notifications.insert(contentsOf: newNotifications, at: 0)

            // Keep only the most recent notifications.
            notifications = trimNotifications(notifications) as? [StandardSiteNotification] ?? notifications

            unreadCount += sortedDocs.count
            persistNotifications()
        }
    }

    /// Marks all notifications as read.
    func markAllAsRead() {
        unreadCount = 0
        persistNotifications()
    }

    /// Clears all notifications.
    func clearAll() {
        notifications = []
        unreadCount = 0
        persistNotifications()
    }

    // MARK: - Private

    private func sendNotification(title: String, body: String, documentURI: String) async {
        let content = UNMutableNotificationContent()
        content.title = title
        content.body = body
        content.sound = .default
        content.userInfo = ["documentURI": documentURI]

        let trigger = UNTimeIntervalNotificationTrigger(timeInterval: 1, repeats: false)
        let request = UNNotificationRequest(
            identifier: UUID().uuidString,
            content: content,
            trigger: trigger
        )

        do {
            try await UNUserNotificationCenter.current().add(request)
        } catch {
            // Notification delivery failed — not critical.
            logger.error("[NotificationManager] failed to schedule notification: \(error.localizedDescription)")
        }
    }

    private var lastSeenURIs: Set<String> {
        guard let did = activeAccountDID else { return [] }
        return Set(defaults.stringArray(forKey: key("lastSeenDocumentURIs", for: did)) ?? [])
    }

    private func saveLastSeenURIs(_ uris: Set<String>) {
        let limited = trimSeenUris(Array(uris))
        if let did = activeAccountDID { defaults.set(limited, forKey: key("lastSeenDocumentURIs", for: did)) }
    }

    private func initializedPublications() -> Set<String> {
        guard let did = activeAccountDID else { return [] }
        return Set(defaults.stringArray(forKey: key("initializedPublicationURIs", for: did)) ?? [])
    }

    private func saveInitializedPublications(_ uris: Set<String>) {
        if let did = activeAccountDID { defaults.set(Array(uris), forKey: key("initializedPublicationURIs", for: did)) }
    }

    private func persistNotifications() {
        if let data = try? JSONEncoder().encode(notifications) {
            if let did = activeAccountDID { defaults.set(data, forKey: key("notifications", for: did)) }
        }
        if let did = activeAccountDID { defaults.set(unreadCount, forKey: key("unreadCount", for: did)) }
    }
}

// MARK: - Notification Model

/// A single notification representing a new document from a subscribed
/// publication.
struct StandardSiteNotification: Identifiable, Codable, Equatable {
    let id: UUID
    let documentURI: String
    let documentTitle: String
    let publicationName: String?
    let publishedAt: Date
    let date: Date  // when the notification was created

    init(
        id: UUID = UUID(),
        documentURI: String,
        documentTitle: String,
        publicationName: String?,
        publishedAt: Date,
        date: Date
    ) {
        self.id = id
        self.documentURI = documentURI
        self.documentTitle = documentTitle
        self.publicationName = publicationName
        self.publishedAt = publishedAt
        self.date = date
    }
}

/// Projects fetched document metadata into the safe notification/history form.
/// Sensitive content must not leak its title or publication name outside the
/// reader's moderation gate.
func notificationMetadata(
    documentURI: String,
    title: String,
    publicationName: String?,
    publishedAt: Date,
    sensitive: Bool,
    date: Date
) -> StandardSiteNotification {
    StandardSiteNotification(
        documentURI: documentURI,
        documentTitle: sensitive ? "Hidden document" : title,
        publicationName: sensitive ? nil : publicationName,
        publishedAt: publishedAt,
        date: date
    )
}
