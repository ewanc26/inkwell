//
//  DiscoverView.swift
//  Inkwell
//
//  Cross-platform Standard.site discovery backed by pub search. Search
//  results provide discovery metadata; Inkwell reads the authoritative record
//  directly from the author's PDS before rendering or subscribing.
//
//  RemoteDocumentView lives in RemoteDocumentView.swift; the search result
//  row views live in DiscoverSearchRows.swift.
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
            .searchable(text: $query, prompt: "Publications and articles")
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
            .task {
                guard !CommandLine.arguments.contains("-screenshot") else { return }
                await loadSubscriptions()
            }
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
