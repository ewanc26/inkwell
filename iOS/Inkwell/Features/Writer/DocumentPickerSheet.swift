//
//  DocumentPickerSheet.swift
//  Inkwell
//

import SwiftUI
import ATProtoKit

// MARK: - Document Picker Sheet

struct DocumentPickerSheet: View {
    let loginStateManager: LoginStateManager
    let publications: [PublicationEntry]
    let selectedPublication: PublicationEntry?
    let onSelectDocument: (String) -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var documents: [(uri: String, title: String)] = []
    @State private var isLoading = false
    @State private var error: String?

    var body: some View {
        NavigationStack {
            List {
                if let error {
                    Text(error)
                        .foregroundStyle(.red)
                }
                if isLoading {
                    ProgressView()
                } else if documents.isEmpty {
                    Text("No documents found.")
                        .foregroundStyle(.secondary)
                } else {
                    ForEach(documents, id: \.uri) { doc in
                        Button(doc.title) {
                            onSelectDocument(doc.uri)
                            dismiss()
                        }
                    }
                }
            }
            .navigationTitle("Select Document")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
            }
            .task {
                await loadDocuments()
            }
        }
    }

    private func loadDocuments() async {
        guard let pub = selectedPublication else { return }
        isLoading = true
        defer { isLoading = false }

        do {
            let parsed = parseAtUri(pub.uri)
            guard let did = parsed?.did else { return }

            let records = try await loginStateManager.listRecordsPage(
                from: did,
                collection: SiteStandardLexicon.DocumentRecord.type,
                limit: 25
            )

            documents = records.records.compactMap { record in
                guard let value = record.value,
                      let doc = try? value.getRecord(ofType: SiteStandardLexicon.DocumentRecord.self) else {
                    return nil
                }
                return (record.uri, doc.title)
            }
        } catch {
            self.error = error.localizedDescription
        }
    }
}
