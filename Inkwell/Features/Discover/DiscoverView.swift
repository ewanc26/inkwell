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
    @State private var results: [ReaderSearchResult] = []
    @State private var subscriptions: Set<String> = []
    @State private var isSearching = false
    @State private var errorMessage: String?
    @State private var showCredits = false

    private var publications: [ReaderSearchResult] { results.filter(\.isPublication) }
    private var documents: [ReaderSearchResult] { results.filter { !$0.isPublication } }

    var body: some View {
        NavigationStack {
            List {
                Section {
                    HStack {
                        Image(systemName: "magnifyingglass")
                            .foregroundStyle(.secondary)
                        TextField("Search publications and articles", text: $query)
                            .textInputAutocapitalization(.never)
                            .autocorrectionDisabled()
                            .submitLabel(.search)
                            .onSubmit { Task { await search() } }
                    }
                }

                if isSearching {
                    Section {
                        HStack {
                            ProgressView()
                            Text("Searching the Standard.site network...")
                                .foregroundStyle(.secondary)
                                .lineLimit(2)
                        }
                    }
                } else if results.isEmpty && errorMessage == nil {
                    Section {
                        ContentUnavailableView(
                            "Search the Open Web",
                            systemImage: "text.magnifyingglass",
                            description: Text("Find Standard.site writing from Leaflet, pckt, Offprint, and independent publishers.")
                        )
                    }
                }

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
                                canSubscribe: ATURI.parse(publication.uri)?.collection == SiteStandardLexicon.PublicationRecord.type,
                                onSubscribe: { Task { await toggleSubscription(publication) } }
                            )
                        }
                    }
                }

                if let errorMessage {
                    Section {
                        Text(errorMessage)
                            .font(.caption)
                            .foregroundStyle(.red)
                            .lineLimit(3)
                    }
                }
            }
            .listStyle(.insetGrouped)
            .navigationTitle("Discover")
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button { showCredits = true } label: {
                        Image(systemName: "info.circle")
                    }
                }
            }
            .sheet(isPresented: $showCredits) {
                CreditsView()
            }
            .task { await loadSubscriptions() }
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
                ProgressView("Fetching from the author's PDS...")
            }
        }
        .task { await load() }
    }

    private func load() async {
        do {
            let document = try await loginStateManager.fetchDocument(uri: documentURI)
            self.document = document
            if ATURI.parse(document.record.site)?.collection == SiteStandardLexicon.PublicationRecord.type {
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
                        Color.primary.opacity(0.05)
                            .overlay { ProgressView().scaleEffect(0.6) }
                    }
                }
            } else {
                placeholder
            }
        }
        .frame(width: size, height: size)
        .background(Color.primary.opacity(0.05))
        .clipShape(RoundedRectangle(cornerRadius: cornerRadius))
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

    private var subscribeButton: some View {
        Button(action: onSubscribe) {
            Image(systemName: isSubscribed ? "bell.fill" : "bell")
                .font(.subheadline)
                .symbolEffect(.bounce, value: isSubscribed)
                .foregroundStyle(isSubscribed ? Color.white : Color.accentColor)
                .frame(width: 32, height: 32)
                .background(
                    Circle().fill(isSubscribed ? Color.accentColor : Color.accentColor.opacity(0.12))
                )
        }
        .buttonStyle(.borderless)
        .disabled(!canSubscribe)
        .animation(.spring(response: 0.3, dampingFraction: 0.7), value: isSubscribed)
    }
}
