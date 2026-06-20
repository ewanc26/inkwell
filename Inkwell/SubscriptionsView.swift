//
//  SubscriptionsView.swift
//  Inkwell
//
//  Created by Letta on 20/06/2026.
//
//  Shows the user's subscribed publications and a unified feed of documents
//  from all of them. Subscriptions are `site.standard.graph.subscription`
//  records in the user's repo; each one points at a publication record's
//  AT-URI. To build the feed, we parse the subscription's publication URI
//  to get the author's DID, then fetch documents from that author's repo.
//
//  This is the "reading from other people" half of the reader:
//  BrowseDocumentsView shows the user's own documents, while this view
//  shows documents from publications the user has chosen to follow.
//

import SwiftUI

struct SubscriptionsView: View {
    @Environment(LoginStateManager.self) private var loginStateManager
    @State private var notificationManager = NotificationManager.shared

    @State private var subscriptions: [SubscriptionEntry] = []
    @State private var feed: [FeedItem] = []
    @State private var isLoading = true
    @State private var errorMessage: String?

    /// A document from a subscribed publication, with the publication context.
    struct FeedItem: Identifiable {
        let document: DocumentEntry
        let publication: PublicationEntry?
        var id: String { document.uri }
    }

    var body: some View {
        NavigationStack {
            Group {
                if isLoading {
                    ProgressView("Loading subscriptions...")
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else if let error = errorMessage {
                    ContentUnavailableView(
                        "Failed to Load",
                        systemImage: "exclamationmark.triangle",
                        description: Text(error)
                    )
                } else if subscriptions.isEmpty {
                    ContentUnavailableView(
                        "No Subscriptions",
                        systemImage: "bell.slash",
                        description: Text("Discover publications in the Discover tab and subscribe to see their latest documents here.")
                    )
                } else {
                    List {
                        if !notificationManager.notifications.isEmpty {
                            Section("Updates") {
                                ForEach(notificationManager.notifications) { notification in
                                    NavigationLink {
                                        RemoteDocumentView(documentURI: notification.documentURI)
                                    } label: {
                                        VStack(alignment: .leading, spacing: 4) {
                                            Text(notification.documentTitle)
                                                .font(.headline)
                                            if let publicationName = notification.publicationName {
                                                Text(publicationName)
                                                    .font(.caption)
                                                    .foregroundStyle(.secondary)
                                            }
                                        }
                                    }
                                }
                                Button("Clear Updates", role: .destructive) {
                                    notificationManager.clearAll()
                                }
                            }
                        }

                        // MARK: - Subscriptions list
                        Section("Following") {
                            ForEach(subscriptions) { sub in
                                SubscriptionRow(subscription: sub)
                            }
                        }

                        // MARK: - Feed
                        if !feed.isEmpty {
                            Section("Latest from Subscriptions") {
                                ForEach(feed) { item in
                                    NavigationLink {
                                        ReadView(
                                            document: item.document.record,
                                            publication: item.publication?.record,
                                            documentURI: item.document.uri,
                                            authorDID: item.document.authorDID
                                        )
                                    } label: {
                                        FeedDocumentRow(item: item)
                                    }
                                }
                            }
                        }
                    }
                    .listStyle(.insetGrouped)
                    .refreshable {
                        await loadData()
                    }
                }
            }
            .navigationTitle("Subscriptions")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button {
                        Task { await loadData() }
                    } label: {
                        Image(systemName: "arrow.clockwise")
                    }
                }
            }
            .task {
                await loadData()
                notificationManager.markAllAsRead()
            }
        }
    }

    // MARK: - Data Loading

    private func loadData() async {
        isLoading = true
        errorMessage = nil

        do {
            let subs = try await loginStateManager.fetchSubscriptions()
            await MainActor.run {
                self.subscriptions = subs
            }

            // For each subscription, fetch documents from the publication author's repo.
            var items: [FeedItem] = []
            for sub in subs {
                guard let pubURI = sub.publicationURI else { continue }

                // Fetch the publication record to get its metadata.
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

                for doc in pubDocs {
                    items.append(FeedItem(document: doc, publication: pubEntry))
                }
            }

            // Sort by published date descending.
            items.sort { $0.document.record.publishedAt > $1.document.record.publishedAt }

            await MainActor.run {
                self.feed = items
                self.isLoading = false
            }
        } catch {
            await MainActor.run {
                self.errorMessage = error.localizedDescription
                self.isLoading = false
            }
        }
    }
}

// MARK: - Subviews

struct SubscriptionRow: View {
    let subscription: SubscriptionEntry

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: "bell.fill")
                .foregroundStyle(Color.accentColor)
                .font(.title3)

            VStack(alignment: .leading, spacing: 4) {
                if let pubURI = subscription.publicationURI {
                    Text(pubURI.did)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .lineLimit(1)
                }
                Text(subscription.record.publication)
                    .font(.caption2)
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
            }
        }
        .padding(.vertical, 4)
    }
}

struct FeedDocumentRow: View {
    let item: SubscriptionsView.FeedItem

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            VStack(alignment: .leading, spacing: 6) {
                if let pubName = item.publication?.record.name {
                    Text(pubName.uppercased())
                        .font(.caption2)
                        .fontWeight(.bold)
                        .foregroundStyle(.secondary)
                        .tracking(1)
                }

                Text(item.document.record.title)
                    .font(.system(.body, design: .serif))
                    .fontWeight(.semibold)
                    .lineLimit(2)

                if let desc = item.document.record.description, !desc.isEmpty {
                    Text(desc)
                        .font(.system(.caption, design: .serif))
                        .foregroundStyle(.secondary)
                        .lineLimit(2)
                }

                Text(formatDate(item.document.record.publishedAt))
                    .font(.caption2)
                    .foregroundStyle(.secondary)
            }
            Spacer()
        }
        .padding(.vertical, 4)
    }

    private func formatDate(_ date: Date) -> String {
        let formatter = DateFormatter()
        formatter.dateStyle = .medium
        formatter.timeStyle = .none
        return formatter.string(from: date)
    }
}
