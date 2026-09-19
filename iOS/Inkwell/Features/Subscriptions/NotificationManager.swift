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
    private let lastSeenKey = "standardSite.lastSeenDocumentURIs"
    private let initializedPublicationsKey = "standardSite.initializedPublicationURIs"
    private let lastPollKey = "standardSite.lastPollTime"
    private let notificationsKey = "standardSite.notifications"
    private let unreadCountKey = "standardSite.unreadCount"
    private let notificationsEnabledKey = "standardSite.notificationsEnabled"

    /// User-facing on/off switch, surfaced in SettingsView. Distinct from
    /// the OS permission: this gates whether a *banner* is sent, not
    /// whether polling happens -- the in-app notification list and unread
    /// badge keep working either way, since they reflect "new documents
    /// exist", not "you were interrupted about them".
    var notificationsEnabled: Bool {
        get { defaults.object(forKey: notificationsEnabledKey) as? Bool ?? true }
        set { defaults.set(newValue, forKey: notificationsEnabledKey) }
    }

    private init() {
        if let data = defaults.data(forKey: notificationsKey),
           let stored = try? JSONDecoder().decode([StandardSiteNotification].self, from: data) {
            notifications = stored
        }
        unreadCount = defaults.integer(forKey: unreadCountKey)
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
        publication: PublicationEntry?
    ) async {
        var seen = lastSeenURIs
        guard !seen.contains(document.uri) else { return }

        await recordNewDocuments([(doc: document, pub: publication)])
        seen.insert(document.uri)
        saveLastSeenURIs(seen)
        if let publicationURI = publication?.uri {
            saveInitializedPublications(initializedPublications.union([publicationURI]))
        }
        defaults.set(Date(), forKey: lastPollKey)
    }

    /// Polls subscribed publications for new documents and sends local
    /// notifications for any that are new since the last poll.
    ///
    /// - Parameter loginStateManager: The authenticated session manager.
    func pollForNewDocuments(loginStateManager: LoginStateManager) async {
        guard loginStateManager.isAuthenticated else { return }

        // Every fetch below uses try? so a DPoP nonce collision (e.g.
        // BrowseDocumentsView racing the same fetchSubscriptions call)
        // degrades gracefully instead of failing the whole poll — polling
        // is best-effort. The cache in LoginStateManager ensures subsequent
        // polls hit memory, not the network.
        let subs = (try? await loginStateManager.fetchSubscriptions()) ?? []
        var newDocs: [(doc: DocumentEntry, pub: PublicationEntry?)] = []
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
                        newDocs.append((doc, pubEntry))
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
        defaults.set(Date(), forKey: lastPollKey)
    }

    // MARK: - Delivery

    /// Applies the identical first-run, grouping and history policy to
    /// foreground Jetstream events and background polling results.
    private func recordNewDocuments(_ newDocs: [(doc: DocumentEntry, pub: PublicationEntry?)]) async {
        guard !newDocs.isEmpty else { return }

        // Only send notifications if this isn't the first poll (first
        // poll just establishes the baseline of existing documents).
        let lastPoll = defaults.object(forKey: lastPollKey) as? Date
        let isFirstPoll = isFirstPoll(lastPollEpochMillis: Int64(lastPoll?.timeIntervalSince1970 ?? -1))

        if !isFirstPoll {
            // Sort newest first.
            let sortedDocs = newDocs.sorted { $0.doc.record.publishedAt > $1.doc.record.publishedAt }

            switch notificationStyle(newDocCount: Int32(sortedDocs.count)) {
            case .single:
                let doc = sortedDocs[0]
                if notificationsEnabled {
                    await sendNotification(
                        title: doc.pub?.record.name ?? "New Document",
                        body: doc.doc.record.title,
                        documentURI: doc.doc.uri
                    )
                }
            case .summary(let count):
                let newest = sortedDocs[0]
                if notificationsEnabled {
                    await sendNotification(
                        title: "\(count) New Documents",
                        body: "Latest: \(newest.doc.record.title) from \(newest.pub?.record.name ?? "a publication")",
                        documentURI: newest.doc.uri
                    )
                }
            case .none:
                break
            }

            // Update in-app notification list.
            let newNotifications = sortedDocs.map { doc in
                StandardSiteNotification(
                    documentURI: doc.doc.uri,
                    documentTitle: doc.doc.record.title,
                    publicationName: doc.pub?.record.name,
                    publishedAt: doc.doc.record.publishedAt,
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
        Set(defaults.stringArray(forKey: lastSeenKey) ?? [])
    }

    private func saveLastSeenURIs(_ uris: Set<String>) {
        let limited = trimSeenUris(Array(uris))
        defaults.set(limited, forKey: lastSeenKey)
    }

    private func initializedPublications() -> Set<String> {
        Set(defaults.stringArray(forKey: initializedPublicationsKey) ?? [])
    }

    private func saveInitializedPublications(_ uris: Set<String>) {
        defaults.set(Array(uris), forKey: initializedPublicationsKey)
    }

    private func persistNotifications() {
        if let data = try? JSONEncoder().encode(notifications) {
            defaults.set(data, forKey: notificationsKey)
        }
        defaults.set(unreadCount, forKey: unreadCountKey)
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
