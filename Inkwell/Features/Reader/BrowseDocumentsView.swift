import SwiftUI
import ATProtoKit

struct BrowseDocumentsView: View {
    @Environment(LoginStateManager.self) private var loginStateManager
    @State private var notificationManager = NotificationManager.shared

    @State private var selectedFeed = ReaderFeed.following
    @State private var following: [ReaderFeedItem] = []
    @State private var yours: [ReaderFeedItem] = []
    @State private var isLoading = true
    @State private var errorMessage: String?
    @State private var showCredits = false

    private enum ReaderFeed: String, CaseIterable, Identifiable {
        case following = "Following"
        case yours = "Yours"

        var id: Self { self }
    }

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                Picker("Reader feed", selection: $selectedFeed) {
                    ForEach(ReaderFeed.allCases) { feed in
                        Text(feed.rawValue).tag(feed)
                    }
                }
                .pickerStyle(.segmented)
                .padding(.horizontal, 16)
                .padding(.vertical, 10)

                content
            }
            .navigationTitle("Reader")
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button(role: .destructive, action: loginStateManager.signOut) {
                        Label("Sign Out", systemImage: "rectangle.portrait.and.arrow.right")
                    }
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Button { showCredits = true } label: {
                        Image(systemName: "info.circle")
                    }
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Button { Task { await loadData() } } label: {
                        Image(systemName: "arrow.clockwise")
                    }
                    .disabled(isLoading)
                }
            }
            .sheet(isPresented: $showCredits) {
                CreditsView()
            }
            .task {
                await loadData()
                notificationManager.markAllAsRead()
            }
        }
    }

    @ViewBuilder
    private var content: some View {
        if isLoading && activeItems.isEmpty {
            ProgressView("Loading your reader...")
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        } else if let errorMessage, activeItems.isEmpty {
            ContentUnavailableView(
                "Reader Unavailable",
                systemImage: "exclamationmark.triangle",
                description: Text(errorMessage)
            )
        } else if activeItems.isEmpty {
            ContentUnavailableView {
                Label(emptyTitle, systemImage: selectedFeed == .following ? "books.vertical" : "doc.text")
            } description: {
                Text(emptyDescription)
            }
        } else {
            ScrollView {
                LazyVStack(spacing: 18) {
                    ForEach(activeItems) { item in
                        NavigationLink {
                            ReadView(
                                document: item.document.record,
                                publication: item.publication?.record,
                                documentURI: item.document.uri,
                                authorDID: item.document.authorDID
                            )
                        } label: {
                            ReaderPostCard(item: item)
                        }
                        .buttonStyle(.plain)
                    }
                }
                .padding(.horizontal, 14)
                .padding(.vertical, 12)
            }
            .refreshable { await loadData() }
        }
    }

    private var activeItems: [ReaderFeedItem] {
        selectedFeed == .following ? following : yours
    }

    private var emptyTitle: String {
        selectedFeed == .following ? "Nothing to read yet" : "No published posts"
    }

    private var emptyDescription: String {
        if selectedFeed == .following {
            return "Subscribe to publications in Discover and their latest posts will appear here."
        }
        return "Posts you publish from Inkwell or another standard.site app will appear here."
    }

    private func loadData() async {
        isLoading = true
        errorMessage = nil
        defer { isLoading = false }

        do {
            async let ownPublications = loginStateManager.fetchPublicationsWithURIs()
            async let ownDocuments = loginStateManager.fetchDocumentsWithURIs()
            async let subscriptions = loginStateManager.fetchSubscriptions()

            let (publications, documents, followedPublications) = try await (
                ownPublications,
                ownDocuments,
                subscriptions
            )

            yours = documents.map { document in
                ReaderFeedItem(
                    document: document,
                    publication: publications.first(where: { $0.contains(document.record) })
                )
            }

            var followedItems: [ReaderFeedItem] = []
            for subscription in followedPublications {
                guard let publicationURI = subscription.publicationURI else { continue }
                let publication = try? await loginStateManager.fetchPublication(uri: subscription.record.publication)
                guard let remoteDocuments = try? await loginStateManager.fetchDocuments(fromDID: publicationURI.did) else {
                    continue
                }

                for document in remoteDocuments where (publication?.contains(document.record) ?? false) || document.record.site == subscription.record.publication {
                    followedItems.append(ReaderFeedItem(document: document, publication: publication))
                }
            }

            following = deduplicated(followedItems)
            yours.sort(by: ReaderFeedItem.newestFirst)
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    private func deduplicated(_ items: [ReaderFeedItem]) -> [ReaderFeedItem] {
        var seen = Set<String>()
        return items
            .sorted(by: ReaderFeedItem.newestFirst)
            .filter { seen.insert($0.id).inserted }
    }
}

struct ReaderFeedItem: Identifiable {
    let document: DocumentEntry
    let publication: PublicationEntry?

    var id: String { document.uri }

    nonisolated static func newestFirst(_ lhs: Self, _ rhs: Self) -> Bool {
        lhs.document.record.publishedAt > rhs.document.record.publishedAt
    }
}

private struct ReaderPostCard: View {
    let item: ReaderFeedItem
    @Environment(\.colorScheme) private var colorScheme

    private var document: SiteStandardLexicon.DocumentRecord { item.document.record }
    private var publication: SiteStandardLexicon.PublicationRecord? { item.publication?.record }

    // Same theme resolution as ReadView (Leaflet's rich theme falling back
    // to basicTheme, then system defaults) so a card in the feed matches
    // what the publication actually looks like once opened. Untethemed
    // documents fall back to a grouped-background card instead of
    // ReaderTheme's plain systemBackground default, so cards stay visually
    // distinct from the surrounding scroll view when no theme is set.
    private var theme: ReaderTheme {
        ReaderTheme(document: document, publication: publication, colorScheme: colorScheme)
    }

    private var hasExplicitTheme: Bool {
        document.theme != nil || publication?.theme != nil || publication?.basicTheme != nil
    }

    private var background: Color {
        hasExplicitTheme ? theme.background : Color(uiColor: .secondarySystemGroupedBackground)
    }
    private var foreground: Color { theme.foreground }
    private var accent: Color { theme.accent }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            if let coverURL {
                AsyncImage(url: coverURL) { phase in
                    switch phase {
                    case .success(let image):
                        image.resizable().scaledToFill()
                            .frame(maxWidth: .infinity)
                            .aspectRatio(16 / 9, contentMode: .fit)
                            .clipped()
                    case .failure, .empty:
                        EmptyView()
                    @unknown default:
                        EmptyView()
                    }
                }
            }

            VStack(alignment: .leading, spacing: 9) {
                Text(document.title)
                    .font(theme.headingFont(.title3, weight: .bold))
                    .foregroundStyle(foreground)
                    .lineLimit(2)

                if let description = document.description, !description.isEmpty {
                    Text(description)
                        .font(theme.bodyFont(.subheadline))
                        .foregroundStyle(foreground.opacity(0.7))
                        .lineLimit(3)
                }

                HStack(spacing: 6) {
                    Text(formattedDate)
                    if let publicationName = publication?.name {
                        Text("·")
                        Text(publicationName)
                            .fontWeight(.semibold)
                            .foregroundStyle(accent)
                            .lineLimit(1)
                    }
                    Spacer(minLength: 8)
                    Image(systemName: "arrow.up.right")
                }
                .font(.caption)
                .foregroundStyle(foreground.opacity(0.55))
            }
            .padding(16)
        }
        .background(background)
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .stroke(foreground.opacity(0.1), lineWidth: 1)
        }
    }

    private var coverURL: URL? {
        guard let cover = document.coverImage else { return nil }
        return URL(string: "https://cdn.bsky.app/img/feed_thumbnail/plain/\(item.document.authorDID)/\(cover.reference.link)")
    }

    private var formattedDate: String {
        document.publishedAt.formatted(date: .abbreviated, time: .omitted)
    }
}
