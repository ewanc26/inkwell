//
//  RemoteDocumentView.swift
//  Inkwell
//

import SwiftUI

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
