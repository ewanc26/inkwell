//
//  ImageDescriptionSheet.swift
//  Inkwell
//
//  Asks for alt text (or an explicit "decorative" choice) before an inline
//  image is uploaded into the document body.
//

import SwiftUI

struct ImageDescriptionSheet: View {
    let onInsert: (String) -> Void
    let onCancel: () -> Void
    @Environment(\.dismiss) private var dismiss
    @State private var altText = ""
    @State private var decorative = false

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    Text("Add alt text for people using a screen reader, or mark the image as decorative.")
                        .font(.subheadline)
                    TextField("Alt text", text: $altText, axis: .vertical)
                        .disabled(decorative)
                    Toggle("Decorative image", isOn: $decorative)
                } footer: {
                    Text("Describe the image's purpose, not its filename.")
                }
            }
            .navigationTitle("Describe Image")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { onCancel(); dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Insert") {
                        onInsert(decorative ? "" : altText.trimmingCharacters(in: .whitespacesAndNewlines))
                        dismiss()
                    }
                    .disabled(!decorative && altText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                }
            }
        }
    }
}
