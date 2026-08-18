//
//  CreatePublicationView.swift
//  Inkwell
//

import SwiftUI

// MARK: - Create Publication View

struct CreatePublicationView: View {
    @Environment(\.dismiss) private var dismiss

    let onCreate: (String, String, String?) -> Void

    @State private var url = ""
    @State private var name = ""
    @State private var description = ""
    @State private var isCreating = false

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    TextField("URL", text: $url, prompt: Text("https://mysite.com"))
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                        .keyboardType(.URL)
                        .textContentType(.URL)
                    TextField("Name", text: $name, prompt: Text("My Publication"))
                    TextField("Description", text: $description, axis: .vertical)
                        .lineLimit(2...4)
                } footer: {
                    Text("The site this publication lives at. You'll verify ownership of the domain before publishing to it.")
                }
            }
            .navigationTitle("New Publication")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Create", action: create)
                        .disabled(url.isEmpty || name.isEmpty || isCreating)
                }
            }
        }
    }

    private func create() {
        isCreating = true
        var trimmedURL = url.trimmingCharacters(in: .whitespacesAndNewlines)
        while trimmedURL.hasSuffix("/") {
            trimmedURL.removeLast()
        }
        let trimmedName = name.trimmingCharacters(in: .whitespacesAndNewlines)
        let trimmedDesc = description.trimmingCharacters(in: .whitespacesAndNewlines)

        onCreate(trimmedURL, trimmedName, trimmedDesc.isEmpty ? nil : trimmedDesc)
        isCreating = false
        dismiss()
    }
}
