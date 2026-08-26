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
    @State private var results: [ReaderSearchResult] = []
    @State private var isSearching = false
    @State private var errorMessage: String?
    @State private var showAbout = false

    var body: some View {
        NavigationStack {
            List {
                if !results.isEmpty {
                    Section("Documents") {
                        ForEach(results) { result in
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
            .task { }
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
            results = try await StandardReaderAPI.shared.search(query: trimmed).results
        } catch {
            errorMessage = "Search is unavailable: \(error.localizedDescription)"
        }
    }
}
