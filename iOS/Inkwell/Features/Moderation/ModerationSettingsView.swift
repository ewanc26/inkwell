import SwiftUI

struct ModerationSettingsView: View {
    @State private var settings = ModerationSettings.shared
    @State private var keyword = ""

    private let standardLabels = [
        ("nsfw", "Explicit content"),
        ("sexual", "Sexual content"),
        ("gore", "Graphic content"),
        ("self-harm", "Self-harm"),
    ]

    var body: some View {
        Form {
            Section {
                ForEach(standardLabels, id: \.0) { label, title in
                    Toggle("Hide \(title)", isOn: Binding(
                        get: { settings.hides(label: label) },
                        set: { settings.setHidden($0, for: label) }
                    ))
                }
            } header: {
                Text("Content warnings")
            } footer: {
                Text("Hidden labels are omitted from the reader. Labels come from the publication or document record.")
            }

            Section {
                HStack {
                    TextField("Add a keyword", text: $keyword)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                        .onSubmit(addKeyword)
                    Button("Add", action: addKeyword)
                        .disabled(keyword.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                }
                ForEach(settings.hiddenKeywords.sorted(), id: \.self) { term in
                    Text(term)
                        .swipeActions {
                            Button("Remove", role: .destructive) {
                                settings.hiddenKeywords.remove(term)
                            }
                        }
                }
            } header: {
                Text("Keywords to hide")
            } footer: {
                Text("Keywords are matched case-insensitively against titles, summaries, and cached text.")
            }
        }
        .navigationTitle("Content Filters")
        .navigationBarTitleDisplayMode(.inline)
    }

    private func addKeyword() {
        settings.addKeyword(keyword)
        keyword = ""
    }
}
