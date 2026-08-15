//
//  NotificationManager.swift
//  Inkwell
//
//  Created by Letta on 20/06/2026.
//
//  Manages local notifications for new documents from subscribed
//  publications. On each poll (triggered by the app's background fetch
//  or a manual refresh), the manager:
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
//  This is a "pull" notification model — there's no push server. The
//  app polls on launch and via iOS background fetch. This is the
//  standard approach for AT Protocol apps that don't have their own
//  backend to relay firehose events as push notifications.
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
    private let lastPollKey = "standardSite.lastPollTime"
    private let notificationsKey = "standardSite.notifications"
    private let unreadCountKey = "standardSite.unreadCount"

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
        let center = UNUserNotificationCenter.current()
        do {
            try await center.requestAuthorization(options: [.alert, .badge, .sound])
        } catch {
            // Permission denied — notifications just won't fire, but the
            // in-app notification list still works.
        }
    }

    // MARK: - Polling

    /// Polls subscribed publications for new documents and sends local
    /// notifications for any that are new since the last poll.
    ///
    /// - Parameter loginStateManager: The authenticated session manager.
    func pollForNewDocuments(loginStateManager: LoginStateManager) async {
        guard loginStateManager.isAuthenticated else { return }

        do {
            // Use try? so a DPoP nonce collision (e.g. BrowseDocumentsView
            // racing the same fetchSubscriptions call) degrades gracefully
            // instead of logging an error. The cache in LoginStateManager
            // ensures subsequent polls hit memory, not the network.
            let subs = (try? await loginStateManager.fetchSubscriptions()) ?? []
            var newDocs: [(doc: DocumentEntry, pub: PublicationEntry?)] = []
            var allSeenURIs = Set<String>(lastSeenURIs)

            for sub in subs {
                guard let pubURI = sub.publicationURI else { continue }

                // Fetch the publication record for metadata.
                let pubs: [PublicationEntry] = (try? await loginStateManager.fetchPublications(fromDID: pubURI.did)) ?? []
                let pubEntry = pubs.first(where: { $0.uri == sub.record.publication })

                // Fetch documents from the publication author's repo.
                let docs: [DocumentEntry] = (try? await loginStateManager.fetchDocuments(fromDID: pubURI.did)) ?? []

                // Filter documents that belong to this publication.
                let pubDocs: [DocumentEntry]
                if let pubEntry {
                    pubDocs = docs.filter { pubEntry.contains($0.record) }
                } else {
                    pubDocs = docs.filter { $0.record.site == sub.record.publication }
                }

                // Find documents we haven't seen before.
                for doc in pubDocs {
                    if !allSeenURIs.contains(doc.uri) {
                        newDocs.append((doc, pubEntry))
                        allSeenURIs.insert(doc.uri)
                    }
                }
            }

            // Only send notifications if this isn't the first poll (first
            // poll just establishes the baseline of existing documents).
            let isFirstPoll = defaults.object(forKey: lastPollKey) == nil

            if !isFirstPoll && !newDocs.isEmpty {
                // Sort newest first.
                newDocs.sort { $0.doc.record.publishedAt > $1.doc.record.publishedAt }

                // Send a summary notification if there are multiple, or
                // a single notification for the newest document.
                if newDocs.count == 1 {
                    let doc = newDocs[0]
                    await sendNotification(
                        title: doc.pub?.record.name ?? "New Document",
                        body: doc.doc.record.title,
                        documentURI: doc.doc.uri
                    )
                } else {
                    let newest = newDocs[0]
                    await sendNotification(
                        title: "\(newDocs.count) New Documents",
                        body: "Latest: \(newest.doc.record.title) from \(newest.pub?.record.name ?? "a publication")",
                        documentURI: newest.doc.uri
                    )
                }

                // Update in-app notification list.
                let newNotifications = newDocs.map { doc in
                    StandardSiteNotification(
                        documentURI: doc.doc.uri,
                        documentTitle: doc.doc.record.title,
                        publicationName: doc.pub?.record.name,
                        publishedAt: doc.doc.record.publishedAt,
                        date: Date()
                    )
                }
                notifications.insert(contentsOf: newNotifications, at: 0)

                // Keep only the last 50 notifications.
                if notifications.count > 50 {
                    notifications = Array(notifications.prefix(50))
                }

                unreadCount += newDocs.count
                persistNotifications()
            }

            // Update last-seen URIs and poll time.
            saveLastSeenURIs(allSeenURIs)
            defaults.set(Date(), forKey: lastPollKey)

        } catch {
            // Silent failure — polling is best-effort.
            logger.error("[NotificationManager] poll failed: \(error.localizedDescription)")
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
        }
    }

    private var lastSeenURIs: Set<String> {
        Set(defaults.stringArray(forKey: lastSeenKey) ?? [])
    }

    private func saveLastSeenURIs(_ uris: Set<String>) {
        // Keep only the last 500 URIs to avoid unbounded growth.
        let limited = Array(uris.suffix(500))
        defaults.set(limited, forKey: lastSeenKey)
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
