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

        do {
            guard let parsed = parseAtUri(documentURI),
                  parsed.collection == SiteStandardLexicon.DocumentRecord.type else {
                throw LoginError.invalidURI
            }

            let fetchedDocument = try await loginStateManager.fetchDocument(uri: documentURI)
            let fetchedPublication: PublicationEntry?
            if let site = parseAtUri(fetchedDocument.record.site),
               site.collection == SiteStandardLexicon.PublicationRecord.type {
                fetchedPublication = try? await loginStateManager.fetchPublication(uri: fetchedDocument.record.site)
            } else {
                fetchedPublication = nil
            }

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
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    private func revealArticle() {
        guard let pendingDocument else { return }
        document = pendingDocument
        publication = pendingPublication
        moderationPresentation = nil
    }
}
