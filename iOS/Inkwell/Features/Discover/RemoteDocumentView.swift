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
            // Parse the AT-URI once to determine the collection type
            // before making any network requests.
            guard let parsed = parseAtUri(documentURI) else {
                throw LoginError.invalidURI
            }

            // If the document points at a publication record, fetch both
            // the document and publication concurrently instead of
            // sequentially — two PDS round-trips in parallel rather than
            // one after the other.
            let isPublication = parsed.collection == SiteStandardLexicon.PublicationRecord.type

            if isPublication {
                async let docTask = loginStateManager.fetchDocument(uri: documentURI)
                async let pubTask = loginStateManager.fetchPublication(uri: documentURI)
                let fetchedDocument = try await docTask
                let fetchedPublication = try? await pubTask
                let labels = (fetchedDocument.record.labels?.values.map(\.value) ?? []) +
                    (fetchedPublication?.record.labels?.values.map(\.value) ?? [])
                if shouldHideContent(
                    title: fetchedDocument.record.title,
                    description: fetchedDocument.record.description,
                    textContent: fetchedDocument.record.textContent,
                    labels: labels
                ) {
                    errorMessage = "This article is hidden by your content filters."
                } else {
                    self.document = fetchedDocument
                    self.publication = fetchedPublication
                }
            } else {
                let fetchedDocument = try await loginStateManager.fetchDocument(uri: documentURI)
                if shouldHideContent(
                    title: fetchedDocument.record.title,
                    description: fetchedDocument.record.description,
                    textContent: fetchedDocument.record.textContent,
                    labels: fetchedDocument.record.labels?.values.map(\.value) ?? []
                ) {
                    errorMessage = "This article is hidden by your content filters."
                } else {
                    self.document = fetchedDocument
                }
            }
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}
