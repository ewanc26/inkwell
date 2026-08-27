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

/// Which facet of the Standard.site network a Discover search targets.
enum DiscoverSearchScope: String, CaseIterable, Identifiable {
    case documents
    case publications

    var id: String { rawValue }

    var title: String {
        switch self {
        case .documents: "Documents"
        case .publications: "Publications"
        }
    }
}

struct DiscoverView: View {
    @Environment(LoginStateManager.self) private var loginStateManager

    @Binding var path: NavigationPath
    @State private var query = ""
    @State private var scope: DiscoverSearchScope = .documents
    @State private var results: [ReaderSearchResult] = []
    @State private var actors: [ReaderSearchActorResult] = []
    @State private var publications: [PublicationResult] = []
    @State private var isSearching = false
    @State private var errorMessage: String?
    @State private var showAbout = false

    var body: some View {
        VStack(spacing: 0) {
            Picker("Search scope", selection: $scope) {
                ForEach(DiscoverSearchScope.allCases) { scope in
                    Text(scope.title).tag(scope)
                }
            }
            .pickerStyle(.segmented)
            .padding(.horizontal, 16)
            .padding(.vertical, 8)

            List {
                if scope == .publications {
                    if !publications.isEmpty {
                        Section("Publications") {
                            ForEach(publications) { publication in
                                Button {
                                    path.append(publication)
                                } label: {
                                    PublicationDiscoveryRow(publication: publication)
                                }
                                .buttonStyle(.plain)
                            }
                        }
                    }
                } else {
                    if !actors.isEmpty {
                        Section("Users") {
                            ForEach(actors) { actor in
                                ActorSearchRow(actor: actor)
                            }
                        }
                    }

                    if !results.isEmpty {
                        Section("Documents") {
                            ForEach(results) { result in
                                if result.isStandardSiteDocument {
                                    Button {
                                        path.append(result.uri)
                                    } label: {
                                        DocumentSearchRow(result: result)
                                    }
                                    .buttonStyle(.plain)
                                } else if let url = result.webURL {
                                    Link(destination: url) {
                                        DocumentSearchRow(result: result)
                                    }
                                }
                            }
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
        }
        .navigationTitle("Discover")
        .inAppLinkHandling()
        .searchable(text: $query, prompt: "Articles and documents")
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
    }

    /// The prompt, spinner, or "no results" state shown in place of the
    /// list. Nothing is drawn once there are results to show.
    @ViewBuilder
    private var placeholder: some View {
        if isSearching && results.isEmpty && actors.isEmpty && publications.isEmpty {
            ProgressView("Searching the Standard.site network…")
                .controlSize(.large)
        } else if results.isEmpty && actors.isEmpty && publications.isEmpty {
            ContentUnavailableView(
                "Search the Open Web",
                systemImage: "text.magnifyingglass",
                description: Text("Find Standard.site writing from Leaflet, pckt, Offprint, and independent publishers.")
            )
        }
    }

    private func search() async {
        let trimmed = query.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        isSearching = true
        errorMessage = nil
        defer { isSearching = false }

        do {
            if scope == .publications {
                let response = try await StandardReaderAPI.shared.search(
                    query: trimmed,
                    mode: "publications"
                )
                results = response.results
                actors = []
                publications = PublicationResult.aggregate(response.results)
            } else {
                async let documents = StandardReaderAPI.shared.search(query: trimmed).results
                async let actors = StandardReaderAPI.shared.searchActors(query: trimmed)
                results = try await documents
                self.actors = (try await actors).actors
                publications = []
            }
        } catch {
            errorMessage = "Search is unavailable: \(error.localizedDescription)"
        }
    }
}
