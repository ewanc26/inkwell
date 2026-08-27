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
    @State private var resolvedPublication: PublicationEntry?
    @State private var isLoading = false
    @State private var errorMessage: String?
    @State private var isShowingCachedCopy = false

    private var publicationTitle: String {
        resolvedPublication?.record.name ?? publication.name
    }

    var body: some View {
        List {
            if let resolvedPublication {
                Section {
                    PublicationDetailHeader(publication: resolvedPublication)
                        .listRowInsets(EdgeInsets(top: 12, leading: 20, bottom: 12, trailing: 20))
                }
            }

            Section("Latest Posts") {
                ForEach(documents) { document in
                    switch moderationPresentation(for: document) {
                    case .visible:
                        Button {
                            path.append(document.uri)
                        } label: {
                            PublicationDocumentRow(document: document)
                        }
                        .buttonStyle(.plain)
                    case .warning, .hidden:
                        Button {
                            path.append(document.uri)
                        } label: {
                            PublicationModeratedDocumentRow(
                                presentation: moderationPresentation(for: document)
                            )
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
        }
        .listStyle(.insetGrouped)
        .safeAreaInset(edge: .top, spacing: 0) {
            if isShowingCachedCopy {
                CachedPublicationBanner()
            }
        }
        .navigationTitle(publicationTitle)
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
        isShowingCachedCopy = false
        defer { isLoading = false }

        do {
            async let publicationsTask = loginStateManager.fetchPublications(fromDID: publication.did)
            async let documentsTask = loginStateManager.fetchDocuments(fromDID: publication.did)

            let fetchedPublications = try await publicationsTask
            let fetchedDocuments = try await documentsTask
            for entry in fetchedPublications {
                await OfflineContentStore.shared.cache(publication: entry)
            }
            for entry in fetchedDocuments {
                await OfflineContentStore.shared.cache(document: entry)
            }
            present(publications: fetchedPublications, documents: fetchedDocuments)
        } catch {
            let cachedPublications = await OfflineContentStore.shared.publications(authorDID: publication.did)
            let cachedDocuments = await OfflineContentStore.shared.documents(authorDID: publication.did)
            guard !cachedPublications.isEmpty || !cachedDocuments.isEmpty else {
                errorMessage = "Couldn't load documents: \(error.localizedDescription)"
                return
            }
            present(publications: cachedPublications, documents: cachedDocuments)
            isShowingCachedCopy = true
        }
    }

    private func present(publications: [PublicationEntry], documents: [DocumentEntry]) {
        self.publications = publications

        // Search results only identify a publication by domain. Prefer the
        // authoritative `site.standard.publication` record for its display
        // name once the author's PDS records have been loaded.
        resolvedPublication = publications.first { entry in
            normalizedHost(for: entry.record.url) == normalizedHost(for: publication.domain)
        }

        let publicationURLMap = Dictionary(
            uniqueKeysWithValues: publications.map { ($0.uri, $0.record.url) }
        )

        self.documents = documents.compactMap { entry in
            documentBelongsToPublication(entry.record, publicationURLMap: publicationURLMap)
                ? entry
                : nil
        }
        .sorted { $0.record.publishedAt > $1.record.publishedAt }
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

    private func moderationPresentation(for document: DocumentEntry) -> ContentModerationPresentation {
        contentModerationPresentation(
            title: document.record.title,
            description: document.record.description,
            textContent: document.record.textContent,
            labels: (document.record.labels?.values.map(\.value) ?? []) +
                (resolvedPublication?.record.labels?.values.map(\.value) ?? [])
        )
    }
}

private struct PublicationDetailHeader: View {
    let publication: PublicationEntry

    private var iconURL: String? {
        guard let icon = publication.record.icon else { return nil }
        return "https://cdn.bsky.app/img/feed_thumbnail/plain/\(publication.authorDID)/\(icon.reference.link)"
    }

    private var domain: String {
        URL(string: publication.record.url)?.host ?? publication.record.url
    }

    var body: some View {
        HStack(alignment: .top, spacing: 14) {
            SearchResultThumbnail(
                urlString: iconURL,
                placeholderSystemImage: "building.2.crop.left.right.fill",
                size: 64,
                cornerRadius: 16
            )

            VStack(alignment: .leading, spacing: 5) {
                Text(publication.record.name)
                    .font(.title3.weight(.semibold))
                    .foregroundStyle(.primary)
                    .fixedSize(horizontal: false, vertical: true)

                Text(domain)
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
                    .truncationMode(.middle)

                if let description = publication.record.description,
                   !description.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                    Text(description)
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                        .fixedSize(horizontal: false, vertical: true)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .accessibilityElement(children: .combine)
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

private struct PublicationModeratedDocumentRow: View {
    let presentation: ContentModerationPresentation

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: "eye.slash")
                .font(.title3)
                .foregroundStyle(.secondary)
                .accessibilityHidden(true)
            VStack(alignment: .leading, spacing: 4) {
                Text(presentation == .warning ? "Content Warning" : "Content Hidden")
                    .font(.headline)
                Text("Open to review or reveal this article.")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
        }
        .padding(.vertical, 6)
        .accessibilityElement(children: .combine)
    }
}

private struct CachedPublicationBanner: View {
    var body: some View {
        Label("Showing saved publication data", systemImage: "archivebox")
            .font(.footnote.weight(.medium))
            .frame(maxWidth: .infinity)
            .padding(.vertical, 8)
            .background(.thinMaterial)
            .accessibilityLabel("Showing saved publication data")
    }
}
