import SwiftUI

struct ModerationSettingsView: View {
    @State private var settings = ModerationSettings.shared
    @State private var customLabel = ""
    @State private var labeler = ""
    @State private var keyword = ""

    private let standardLabels = [
        ("nsfw", "Explicit content"),
        ("sexual", "Sexual content"),
        ("gore", "Graphic content"),
        ("self-harm", "Self-harm"),
        ("impersonation", "Impersonation"),
    ]

    var body: some View {
        Form {
            Section {
                Text("Choose whether content with each label is shown, shown behind a warning, or hidden until you reveal it.")
                    .font(.footnote)
                    .foregroundStyle(.secondary)

                ForEach(standardLabels, id: \.0) { label, title in
                    labelModePicker(title: title, label: label)
                }
            } header: {
                Text("Content warnings")
            } footer: {
                Text("Labels come from the publication or document record. The choice only affects how Inkwell presents them.")
            }

            Section {
                addValueField(
                    title: "Custom label",
                    value: $customLabel,
                    buttonTitle: "Add label",
                    action: {
                        settings.addCustomLabel(customLabel)
                        customLabel = ""
                    }
                )

                ForEach(settings.customLabels.sorted(), id: \.self) { label in
                    VStack(alignment: .leading, spacing: 8) {
                        HStack {
                            Text(label)
                            Spacer()
                            Button("Remove", role: .destructive) {
                                settings.removeCustomLabel(label)
                            }
                        }
                        labelModePicker(title: "Display", label: label, compact: true)
                    }
                    .padding(.vertical, 2)
                }
            } header: {
                Text("Custom labels")
            } footer: {
                Text("Add labels used by a publication or labeler service, then choose how they should appear.")
            }

            Section {
                addValueField(
                    title: "Labeler DID or service",
                    value: $labeler,
                    buttonTitle: "Add labeler",
                    action: {
                        settings.setLabelerEnabled(true, for: labeler)
                        labeler = ""
                    }
                )

                ForEach(settings.knownLabelers.sorted(), id: \.self) { source in
                    HStack(alignment: .top, spacing: 12) {
                        Toggle(source, isOn: Binding(
                            get: { settings.isLabelerEnabled(source) },
                            set: { settings.setLabelerEnabled($0, for: source) }
                        ))
                        .accessibilityHint("Controls whether Inkwell uses labels from this source")

                        Button(role: .destructive) {
                            settings.removeLabeler(source)
                        } label: {
                            Image(systemName: "trash")
                        }
                        .accessibilityLabel("Remove \(source)")
                    }
                }
            } header: {
                Text("Labeler services")
            } footer: {
                Text("Turn a source off to ignore labels from that service when the PDS supplies source attribution.")
            }

            Section {
                addValueField(
                    title: "Keyword to hide",
                    value: $keyword,
                    buttonTitle: "Add keyword",
                    action: {
                        settings.addKeyword(keyword)
                        keyword = ""
                    }
                )

                ForEach(settings.hiddenKeywords.sorted(), id: \.self) { term in
                    HStack {
                        Text(term)
                        Spacer()
                        Button("Remove", role: .destructive) {
                            settings.hiddenKeywords.remove(term)
                        }
                    }
                }
            } header: {
                Text("Keywords to hide")
            } footer: {
                Text("Keywords are matched case-insensitively against article titles, summaries, and available text.")
            }
        }
        .navigationTitle("Content Filters")
        .navigationBarTitleDisplayMode(.inline)
    }

    @ViewBuilder
    private func labelModePicker(title: String, label: String, compact: Bool = false) -> some View {
        let selection = Binding(
            get: { settings.labelMode(for: label) },
            set: { settings.setLabelMode($0, for: label) }
        )
        if compact {
            Picker("", selection: selection) {
                ForEach(ModerationLabelMode.allCases) { mode in
                    Text(mode.title).tag(mode)
                }
            }
            .pickerStyle(.menu)
            .labelsHidden()
            .accessibilityLabel("\(title) display")
        } else {
            Picker(title, selection: selection) {
                ForEach(ModerationLabelMode.allCases) { mode in
                    Text(mode.title).tag(mode)
                }
            }
            .pickerStyle(.menu)
            .accessibilityLabel("\(title) display")
        }
    }

    @ViewBuilder
    private func addValueField(
        title: String,
        value: Binding<String>,
        buttonTitle: String,
        action: @escaping () -> Void
    ) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            TextField(title, text: value)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .onSubmit(action)
            Button(buttonTitle, action: action)
                .disabled(value.wrappedValue.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
        }
    }
}
