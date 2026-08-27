//
//  RemoteDocumentView.swift
//  Inkwell
//

import SwiftUI
import ATProtoKit

struct RemoteDocumentView: View {
    @Environment(LoginStateManager.self) private var loginStateManager
    let documentURI: String

    @State private var document: DocumentEntry?
    @State private var publication: PublicationEntry?
    @State private var pendingDocument: DocumentEntry?
    @State private var pendingPublication: PublicationEntry?
    @State private var moderationPresentation: ContentModerationPresentation?
    @State private var errorMessage: String?
    @State private var isShowingCachedCopy = false

    var body: some View {
        Group {
            if let document {
                ReadView(
                    document: document.record,
                    publication: publication?.record,
                    documentURI: document.uri,
                    documentCID: document.cid,
                    authorDID: document.authorDID
                )
                .safeAreaInset(edge: .top, spacing: 0) {
                    if isShowingCachedCopy {
                        CachedDocumentBanner()
                    }
                }
            } else if let moderationPresentation {
                ContentUnavailableView {
                    Label(
                        moderationPresentation == .warning ? "Content Warning" : "Content Hidden",
                        systemImage: "eye.slash"
                    )
                } description: {
                    Text(
                        moderationPresentation == .warning
                            ? "This article has a content label you chose to see behind a warning."
                            : "This article matches one of your content filters."
                    )
                } actions: {
                    Button("Reveal Article", action: revealArticle)
                        .buttonStyle(.borderedProminent)
                }
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
        errorMessage = nil
        moderationPresentation = nil
        pendingDocument = nil
        pendingPublication = nil
        isShowingCachedCopy = false

        do {
            guard let parsed = parseAtUri(documentURI),
                  parsed.collection == SiteStandardLexicon.DocumentRecord.type else {
                throw LoginError.invalidURI
            }

            let fetchedDocument = try await loginStateManager.fetchDocument(uri: documentURI)
            var fetchedPublication: PublicationEntry?
            if let site = publicationURI(for: fetchedDocument) {
                fetchedPublication = try? await loginStateManager.fetchPublication(uri: site)
                if fetchedPublication == nil {
                    fetchedPublication = await OfflineContentStore.shared.publication(uri: site)
                }
            } else {
                fetchedPublication = nil
            }

            await OfflineContentStore.shared.cache(document: fetchedDocument)
            if let fetchedPublication {
                await OfflineContentStore.shared.cache(publication: fetchedPublication)
            }
            present(fetchedDocument, publication: fetchedPublication, isCached: false)
        } catch {
            guard let cachedDocument = await OfflineContentStore.shared.document(uri: documentURI) else {
                errorMessage = error.localizedDescription
                return
            }
            let cachedPublication: PublicationEntry?
            if let site = publicationURI(for: cachedDocument) {
                cachedPublication = await OfflineContentStore.shared.publication(uri: site)
            } else {
                cachedPublication = nil
            }
            present(cachedDocument, publication: cachedPublication, isCached: true)
        }
    }

    private func revealArticle() {
        guard let pendingDocument else { return }
        document = pendingDocument
        publication = pendingPublication
        moderationPresentation = nil
    }

    private func present(
        _ fetchedDocument: DocumentEntry,
        publication fetchedPublication: PublicationEntry?,
        isCached: Bool
    ) {
        isShowingCachedCopy = isCached
        let decision = contentModerationPresentation(
            title: fetchedDocument.record.title,
            description: fetchedDocument.record.description,
            textContent: fetchedDocument.record.textContent,
            labels: (fetchedDocument.record.labels?.values.map(\.value) ?? []) +
                (fetchedPublication?.record.labels?.values.map(\.value) ?? [])
        )

        switch decision {
        case .visible:
            document = fetchedDocument
            publication = fetchedPublication
        case .warning, .hidden:
            pendingDocument = fetchedDocument
            pendingPublication = fetchedPublication
            moderationPresentation = decision
        }
    }

    private func publicationURI(for document: DocumentEntry) -> String? {
        guard let parsed = parseAtUri(document.record.site),
              parsed.collection == SiteStandardLexicon.PublicationRecord.type else {
            return nil
        }
        return document.record.site
    }
}

private struct CachedDocumentBanner: View {
    var body: some View {
        Label("Showing a saved copy", systemImage: "archivebox")
            .font(.footnote.weight(.medium))
            .frame(maxWidth: .infinity)
            .padding(.vertical, 8)
            .background(.thinMaterial)
            .accessibilityLabel("Showing a saved copy of this article")
    }
}
