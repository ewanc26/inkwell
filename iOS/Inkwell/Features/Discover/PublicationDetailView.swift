//
//  PublicationDetailView.swift
//  Inkwell
//
//  In-app publication page. Tapping a publication in Discover opens its
//  documents natively rather than sending the reader to the site in a browser.
//  Documents are fetched from the author's own PDS and rendered with the same
//  ReadView used everywhere else in the app.
//

import SwiftUI
import ATProtoKit

struct PublicationDetailView: View {
    @Environment(LoginStateManager.self) private var loginStateManager

    let publication: PublicationResult
    @Binding var path: NavigationPath

    @State private var documents: [DocumentEntry] = []
    @State private var publications: [PublicationEntry] = []
    @State private var isLoading = false
    @State private var errorMessage: String?

    var body: some View {
        List(documents) { document in
            Button {
                path.append(document.uri)
            } label: {
                PublicationDocumentRow(document: document)
            }
            .buttonStyle(.plain)
        }
        .listStyle(.insetGrouped)
        .navigationTitle(publication.name)
        .overlay {
            if isLoading && documents.isEmpty {
                ProgressView("Fetching documents…")
                    .controlSize(.large)
            } else if documents.isEmpty && !isLoading {
                ContentUnavailableView(
                    "No Documents",
                    systemImage: "doc.text",
                    description: Text("This publication hasn't published any documents yet.")
                )
            }
        }
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
        .task { await load() }
    }

    private func load() async {
        isLoading = true
        defer { isLoading = false }

        do {
            async let publicationsTask = loginStateManager.fetchPublications(fromDID: publication.did)
            async let documentsTask = loginStateManager.fetchDocuments(fromDID: publication.did)

            self.publications = try await publicationsTask
            let allDocuments = try await documentsTask

            let publicationURLMap = Dictionary(
                uniqueKeysWithValues: publications.map { ($0.uri, $0.record.url) }
            )

            documents = allDocuments.compactMap { entry in
                documentBelongsToPublication(entry.record, publicationURLMap: publicationURLMap)
                    ? entry
                    : nil
            }
            .sorted { $0.record.publishedAt > $1.record.publishedAt }
        } catch {
            errorMessage = "Couldn't load documents: \(error.localizedDescription)"
        }
    }

    /// A document belongs to this publication when its `site` field matches the
    /// publication's domain directly, or when it points to a publication record
    /// whose URL resolves to the same domain.
    private func documentBelongsToPublication(
        _ document: SiteStandardLexicon.DocumentRecord,
        publicationURLMap: [String: String]
    ) -> Bool {
        let site = document.site.lowercased()
        let targetHost = normalizedHost(for: publication.domain)

        // Direct URL match.
        if normalizedHost(for: site) == targetHost {
            return true
        }

        // AT-URI reference to a publication record: resolve it and compare URLs.
        if let parsed = parseAtUri(site),
           parsed.collection == SiteStandardLexicon.PublicationRecord.type,
           let publicationURL = publicationURLMap[site]?.lowercased(),
           normalizedHost(for: publicationURL) == targetHost {
            return true
        }

        return false
    }

    /// Extracts a normalized host from a URL or bare domain string, stripping
    /// scheme, path, query, and trailing slashes.
    private func normalizedHost(for string: String) -> String? {
        var trimmed = string.trimmingCharacters(in: .whitespacesAndNewlines)
            .lowercased()
        while trimmed.hasSuffix("/") {
            trimmed.removeLast()
        }

        if trimmed.isEmpty {
            return nil
        }

        // If there's no scheme, treat it as a host already.
        if trimmed.contains("://") {
            guard let url = URL(string: trimmed), let host = url.host else { return nil }
            return host
        }

        return trimmed
    }
}

struct PublicationDocumentRow: View {
    let document: DocumentEntry

    private var coverURL: URL? {
        guard let cover = document.record.coverImage else { return nil }
        return URL(string: "https://cdn.bsky.app/img/feed_thumbnail/plain/\(document.authorDID)/\(cover.reference.link)")
    }

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            SearchResultThumbnail(
                urlString: coverURL?.absoluteString,
                placeholderSystemImage: "doc.text.image"
            )

            VStack(alignment: .leading, spacing: 5) {
                Text(document.record.title)
                    .font(.system(.body, design: .serif, weight: .semibold))
                    .lineLimit(2)
                if let description = document.record.description, !description.isEmpty {
                    Text(description)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .lineLimit(2)
                }
                Text(document.record.publishedAt, format: .dateTime.month().day().year())
                    .font(.caption2)
                    .foregroundStyle(.secondary)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(.vertical, 4)
    }
}
