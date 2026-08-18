//
//  DiscoverView.swift
//  Inkwell
//
//  Cross-platform Standard.site discovery backed by pub search. Search
//  results provide discovery metadata; Inkwell reads the authoritative record
//  directly from the author's PDS before rendering or subscribing.
//

import SwiftUI

struct DiscoverView: View {
    @Environment(LoginStateManager.self) private var loginStateManager

    @State private var query = ""
    /// The query the currently-displayed results came back for, so the
    /// empty state can quote what was actually searched rather than
    /// whatever's been typed since.
    @State private var searchedQuery = ""
    @State private var results: [ReaderSearchResult] = []
    @State private var subscriptions: Set<String> = []
    @State private var isSearching = false
    @State private var errorMessage: String?
    @State private var showAbout = false

    private var publications: [ReaderSearchResult] { results.filter(\.isPublication) }
    private var documents: [ReaderSearchResult] { results.filter { !$0.isPublication } }

    var body: some View {
        NavigationStack {
            List {
                if !documents.isEmpty {
                    Section("Documents") {
                        ForEach(documents) { result in
                            if result.isStandardSiteDocument {
                                NavigationLink {
                                    RemoteDocumentView(documentURI: result.uri)
                                } label: {
                                    DocumentSearchRow(result: result)
                                }
                            } else if let url = result.webURL {
                                Link(destination: url) {
                                    DocumentSearchRow(result: result)
                                }
                            }
                        }
                    }
                }

                if !publications.isEmpty {
                    Section("Publications") {
                        ForEach(publications) { publication in
                            PublicationSearchRow(
                                publication: publication,
                                isSubscribed: subscriptions.contains(publication.uri),
                                canSubscribe: parseAtUri(publication.uri)?.collection == SiteStandardLexicon.PublicationRecord.type,
                                onSubscribe: { Task { await toggleSubscription(publication) } }
                            )
                        }
                    }
                }
            }
            .listStyle(.insetGrouped)
            // Placeholder states belong over the list, not stuffed into a
            // row of it — a ContentUnavailableView inside a Section gets
            // the list's row insets and separators, which is not what it's
            // designed to sit in.
            .overlay {
                placeholder
            }
            .navigationTitle("Discover")
            // The system search field, rather than a TextField dressed up
            // with a magnifying glass in the first row of the list: it gets
            // the cancel button, scroll-to-reveal, dictation, and the
            // search-tab integration for free.
            .searchable(
                text: $query,
                prompt: "Publications and articles"
            )
            .textInputAutocapitalization(.never)
            .autocorrectionDisabled()
            .onSubmit(of: .search) {
                Task { await search() }
            }
            .accountToolbar(showAbout: $showAbout)
            .alert(
                "Something Went Wrong",
                isPresented: Binding(
                    get: { errorMessage != nil },
                    set: { if !$0 { errorMessage = nil } }
                )
            ) {
                Button("OK", role: .cancel) { errorMessage = nil }
            } message: {
                Text(errorMessage ?? "")
            }
            .task {
                if CommandLine.arguments.contains("-screenshot") {
                    query = "Standard.site"
                    searchedQuery = query
                    results = [
                        ReaderSearchResult(
                            type: "publication",
                            uri: "at://did:plc:ewan/site.standard.publication/1",
                            did: "did:plc:ewan",
                            title: "Ewan's Corner",
                            snippet: "Essays on open protocols, software, and digital garden notes.",
                            createdAt: "2026-06-20T12:00:00Z",
                            rkey: "1",
                            basePath: "ewancroft.uk",
                            platform: "leaflet",
                            path: nil,
                            coverImage: nil,
                            handle: "ewancroft.uk"
                        ),
                        ReaderSearchResult(
                            type: "publication",
                            uri: "at://did:plc:atproto/site.standard.publication/2",
                            did: "did:plc:atproto",
                            title: "AT Protocol Weekly",
                            snippet: "Weekly round-up of lexicons and PDS updates across the network.",
                            createdAt: "2026-06-18T10:00:00Z",
                            rkey: "2",
                            basePath: "atproto.news",
                            platform: "leaflet",
                            path: nil,
                            coverImage: nil,
                            handle: "atproto.news"
                        ),
                        ReaderSearchResult(
                            type: "publication",
                            uri: "at://did:plc:leaflet/site.standard.publication/3",
                            did: "did:plc:leaflet",
                            title: "Leaflet Lab",
                            snippet: "Deep dives into block-based document design and web publishing.",
                            createdAt: "2026-06-15T08:00:00Z",
                            rkey: "3",
                            basePath: "leaflet.pub",
                            platform: "leaflet",
                            path: nil,
                            coverImage: nil,
                            handle: "leaflet.pub"
                        )
                    ]
                    subscriptions = ["at://did:plc:ewan/site.standard.publication/1"]
                }
            }
            .task { await loadSubscriptions() }
        }
    }

    /// The prompt, spinner, or "no results" state shown in place of the
    /// list. Nothing is drawn once there are results to show.
    @ViewBuilder
    private var placeholder: some View {
        if isSearching && results.isEmpty {
            ProgressView("Searching the Standard.site network…")
                .controlSize(.large)
        } else if results.isEmpty {
            if searchedQuery.isEmpty {
                ContentUnavailableView(
                    "Search the Open Web",
                    systemImage: "text.magnifyingglass",
                    description: Text("Find Standard.site writing from Leaflet, pckt, Offprint, and independent publishers.")
                )
            } else {
                ContentUnavailableView.search(text: searchedQuery)
            }
        }
    }

    private func search() async {
        let trimmed = query.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        isSearching = true
        errorMessage = nil
        defer { isSearching = false }

        do {
            results = try await StandardReaderAPI.shared.search(query: trimmed).results
            searchedQuery = trimmed
        } catch {
            errorMessage = "Search is unavailable: \(error.localizedDescription)"
        }
    }

    private func loadSubscriptions() async {
        let records = (try? await loginStateManager.fetchSubscriptions()) ?? []
        subscriptions = Set(records.map { $0.record.publication })
    }

    private func toggleSubscription(_ publication: ReaderSearchResult) async {
        do {
            if subscriptions.contains(publication.uri) {
                let records = try await loginStateManager.fetchSubscriptions()
                if let record = records.first(where: { $0.record.publication == publication.uri }) {
                    try await loginStateManager.deleteSubscription(recordKey: record.recordKey)
                    subscriptions.remove(publication.uri)
                }
            } else {
                let record = try await loginStateManager.fetchPublication(uri: publication.uri)
                _ = try await SiteStandardLexicon.Verification.verify(
                    publicationURI: record.uri,
                    publication: record.record
                )
                _ = try await loginStateManager.createSubscription(publicationURI: publication.uri)
                subscriptions.insert(publication.uri)
                await NotificationManager.shared.requestPermission()
            }
        } catch {
            errorMessage = "Could not update subscription: \(error.localizedDescription)"
        }
    }
}

struct RemoteDocumentView: View {
    @Environment(LoginStateManager.self) private var loginStateManager
    let documentURI: String

    @State private var document: DocumentEntry?
    @State private var publication: PublicationEntry?
    @State private var errorMessage: String?

    var body: some View {
        Group {
            if let document {
                ReadView(
                    document: document.record,
                    publication: publication?.record,
                    documentURI: document.uri,
                    authorDID: document.authorDID
                )
            } else if let errorMessage {
                ContentUnavailableView(
                    "Document Unavailable",
                    systemImage: "exclamationmark.triangle",
                    description: Text(errorMessage)
                )
            } else {
                ProgressView("Fetching from the author's PDS…")
                    .controlSize(.large)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
        }
        .task { await load() }
    }

    private func load() async {
        do {
            let document = try await loginStateManager.fetchDocument(uri: documentURI)
            self.document = document
            if parseAtUri(document.record.site)?.collection == SiteStandardLexicon.PublicationRecord.type {
                publication = try? await loginStateManager.fetchPublication(uri: document.record.site)
            }
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}

/// A small bounded thumbnail shared by both search row types — fixed-size
/// regardless of the AsyncImage's loading phase, so a slow or missing image
/// never grows the row or distorts the list. Falls back to an SF Symbol on
/// a faint tinted square when there's nothing to show.
private struct SearchResultThumbnail: View {
    let urlString: String?
    let placeholderSystemImage: String
    var size: CGFloat = 52
    var cornerRadius: CGFloat = 10

    var body: some View {
        Group {
            if let urlString, let url = URL(string: urlString) {
                AsyncImage(url: url) { phase in
                    switch phase {
                    case .success(let image):
                        image.resizable().scaledToFill()
                    case .failure:
                        placeholder
                    default:
                        ProgressView().controlSize(.small)
                    }
                }
            } else {
                placeholder
            }
        }
        .frame(width: size, height: size)
        // The system's grouped-content fill, so the thumbnail well tracks
        // light/dark and increased-contrast the way every other inset
        // grouped row does — an opaque 5% black never did.
        .background(Color(uiColor: .tertiarySystemFill))
        .clipShape(RoundedRectangle(cornerRadius: cornerRadius, style: .continuous))
    }

    private var placeholder: some View {
        Image(systemName: placeholderSystemImage)
            .foregroundStyle(.secondary)
    }
}

private struct DocumentSearchRow: View {
    let result: ReaderSearchResult

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            SearchResultThumbnail(
                urlString: result.coverImage,
                placeholderSystemImage: "doc.text.image"
            )

            VStack(alignment: .leading, spacing: 5) {
                Text(result.title)
                    .font(.system(.body, design: .serif, weight: .semibold))
                    .lineLimit(2)
                if let snippet = result.snippet, !snippet.isEmpty {
                    Text(snippet)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .lineLimit(2)
                }
                HStack(spacing: 4) {
                    Text(result.platform ?? "standard.site")
                        .lineLimit(1)
                    if let handle = result.handle {
                        Text("·")
                        Text(handle)
                            .lineLimit(1)
                            .truncationMode(.middle)
                    }
                }
                .font(.caption2)
                .foregroundStyle(.secondary)
            }
            // Lets the text column truncate instead of pushing the row wider
            // than the list, however long the title/handle turn out to be.
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(.vertical, 4)
    }
}

private struct PublicationSearchRow: View {
    let publication: ReaderSearchResult
    let isSubscribed: Bool
    let canSubscribe: Bool
    let onSubscribe: () -> Void

    var body: some View {
        HStack(spacing: 12) {
            SearchResultThumbnail(
                urlString: publication.coverImage,
                placeholderSystemImage: "building.2.crop.left.right.fill",
                size: 44
            )

            VStack(alignment: .leading, spacing: 4) {
                Text(publication.title)
                    .font(.headline)
                    .lineLimit(2)
                if let basePath = publication.basePath {
                    Text(basePath)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .lineLimit(1)
                        .truncationMode(.middle)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            subscribeButton
        }
        .padding(.vertical, 4)
    }

    /// A real system button rather than a symbol on a hand-drawn tinted
    /// circle: `.borderedProminent` when subscribed and `.bordered` when
    /// not gives the same filled/outlined read, but with the system's own
    /// contrast handling, disabled appearance, and press feedback.
    private var subscribeButton: some View {
        Button(action: onSubscribe) {
            Image(systemName: isSubscribed ? "bell.fill" : "bell")
                .symbolEffect(.bounce, value: isSubscribed)
                .accessibilityHidden(true)
        }
        .modifier(ProminentBorderedWhen(isProminent: isSubscribed))
        .buttonBorderShape(.circle)
        .disabled(!canSubscribe)
        .accessibilityLabel(isSubscribed ? "Unsubscribe" : "Subscribe")
        .accessibilityAddTraits(isSubscribed ? [.isSelected] : [])
        .animation(InkwellMotion.micro, value: isSubscribed)
    }
}

/// Switches a button between the bordered and prominent-bordered system
/// styles. The two are distinct types, so the choice can't be made with a
/// ternary inside `.buttonStyle(_:)` — it has to branch at the view level.
private struct ProminentBorderedWhen: ViewModifier {
    let isProminent: Bool

    @ViewBuilder
    func body(content: Content) -> some View {
        if isProminent {
            content.buttonStyle(.borderedProminent)
        } else {
            content.buttonStyle(.bordered)
        }
    }
}
