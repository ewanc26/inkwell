//
//  WriteView.swift
//  Inkwell
//
//  Universal markdown editor for writing standard.site documents in any
//  supported format (Leaflet, Markpub, Pckt, Offprint). The editor works
//  in markdown; the selected provider converts to/from the format-specific
//  AT Protocol record on publish.
//
//  Redesigned to match standard.horse's writing experience: split-pane
//  editor with live preview, formatting toolbar, image upload, loss
//  reporting, and document editing support.
//

import SwiftUI
import PhotosUI
import ATProtoKit

struct WriteView: View {
    @Environment(LoginStateManager.self) private var loginStateManager
    @Environment(\.colorScheme) private var colorScheme
    @State private var viewModel: WriterViewModel
    @State private var showSignIn = false
    @State private var showDocumentPicker = false
    @State private var selectedPhoto: PhotosPickerItem?

    init() {
        let lsm = LoginStateManager()
        _viewModel = State(initialValue: WriterViewModel(loginStateManager: lsm))
    }

    var body: some View {
        NavigationStack {
            if !loginStateManager.isAuthenticated {
                ContentUnavailableView {
                    Label("Sign In to Write", systemImage: "square.and.pencil")
                } description: {
                    Text("You need an AT Protocol account to publish documents.")
                } actions: {
                    Button("Sign In") { showSignIn = true }
                        .buttonStyle(.borderedProminent)
                }
                .sheet(isPresented: $showSignIn) {
                    LoginView()
                }
            } else {
                Form {
                    // MARK: - Publication
                    Section {
                        if viewModel.isLoadingPublications {
                            HStack(spacing: 10) {
                                ProgressView()
                                Text("Loading publications…")
                                    .foregroundStyle(.secondary)
                            }
                        } else if viewModel.publications.isEmpty {
                            Button {
                                viewModel.showCreatePublication = true
                            } label: {
                                Label("Create a Publication", systemImage: "plus.circle")
                            }
                        } else {
                            Picker("Publication", selection: $viewModel.selectedPublication) {
                                ForEach(viewModel.publications) { publication in
                                    Text(publication.record.name).tag(Optional(publication))
                                }
                            }
                            Button {
                                viewModel.showCreatePublication = true
                            } label: {
                                Label("New Publication", systemImage: "plus")
                            }
                        }
                    } header: {
                        Text("Publication")
                    } footer: {
                        if viewModel.publications.isEmpty && !viewModel.isLoadingPublications {
                            Text("You need a publication before you can publish a document.")
                        }
                    }

                    if let publication = viewModel.selectedPublication {
                        Section {
                            Label(
                                viewModel.isVerified ? "Verified publication" : "Verification required",
                                systemImage: viewModel.isVerified ? "checkmark.seal.fill" : "exclamationmark.triangle.fill"
                            )
                            .foregroundStyle(viewModel.isVerified ? Color.green : Color.orange)
                            .accessibilityLabel(viewModel.isVerified ? "Publication verified" : "Publication not verified")

                            Button("Verify Again") {
                                Task { await viewModel.verifySelectedPublication() }
                            }
                            .disabled(viewModel.isVerifyingPublication)
                        } header: {
                            Text("Verification")
                        } footer: {
                            if let verificationMessage = viewModel.verificationMessage {
                                Text(verificationMessage)
                                    .textSelection(.enabled)
                            } else {
                                Text("Inkwell checks that \(publication.record.url) points back at this record before letting you publish to it.")
                            }
                        }
                    }

                    // MARK: - Document metadata
                    Section("Details") {
                        TextField("Title", text: $viewModel.title)
                        TextField("Description", text: $viewModel.description, axis: .vertical)
                            .lineLimit(2...4)
                        TextField("Path (optional)", text: $viewModel.path)
                            .autocorrectionDisabled()
                            .textInputAutocapitalization(.never)
                        Picker("Format", selection: $viewModel.selectedProviderId) {
                            ForEach(ProviderRegistry.providers, id: \.id) { provider in
                                Text(provider.label).tag(provider.id)
                            }
                        }
                        .disabled(viewModel.isEditing)
                    }

                    // MARK: - Editing indicator
                    if viewModel.isEditing {
                        Section {
                            HStack(spacing: 8) {
                                Image(systemName: "edit")
                                    .foregroundStyle(.blue)
                                Text("Editing existing document")
                                    .foregroundStyle(.blue)
                                Spacer()
                                Button("Cancel") {
                                    viewModel.cancelEditing()
                                }
                                .buttonStyle(.plain)
                                .foregroundStyle(.secondary)
                            }
                        }
                    }

                    // MARK: - Document picker
                    Section {
                        Button {
                            showDocumentPicker = true
                        } label: {
                            Label(
                                viewModel.isEditing ? "Change Document" : "Edit Existing Document",
                                systemImage: "list.bullet"
                            )
                        }
                    }

                    // MARK: - Loss reporting
                    if !viewModel.lostFeatures.isEmpty {
                        Section {
                            HStack(spacing: 8) {
                                Image(systemName: "exclamationmark.triangle.fill")
                                    .foregroundStyle(.orange)
                                Text("This post contains **\(viewModel.lostFeatures.joined(separator: ", "))** that markdown can't represent. Saving will drop those.")
                                    .font(.subheadline)
                            }
                        }
                    }

                    // MARK: - Formatting toolbar
                    Section {
                        FormattingToolbar(
                            markdown: $viewModel.markdown,
                            canUploadImages: viewModel.canUploadImages,
                            onImagePicker: {}
                        )
                        .listRowInsets(EdgeInsets(top: 4, leading: 0, bottom: 4, trailing: 0))
                    }

                    // MARK: - Markdown editor + preview
                    Section {
                        VStack(spacing: 0) {
                            // Editor
                            TextEditor(text: $viewModel.markdown)
                                .frame(minHeight: 200)
                                .font(.body.monospaced())
                                .textInputAutocapitalization(.sentences)
                                .scrollContentBackground(.visible)

                            if viewModel.showPreview {
                                Divider()

                                // Live preview
                                ScrollView {
                                    MarkdownRendererView(
                                        markdown: viewModel.markdown,
                                        theme: ReaderTheme(colorScheme: colorScheme)
                                    )
                                    .padding(12)
                                }
                                .frame(minHeight: 200, maxHeight: 400)
                                .background(Color(.tertiarySystemBackground))
                            }
                        }
                        .overlay(alignment: .topTrailing) {
                            Button {
                                viewModel.showPreview.toggle()
                            } label: {
                                Image(systemName: viewModel.showPreview ? "eye.slash" : "eye")
                                    .font(.caption)
                                    .padding(6)
                                    .background(.ultraThinMaterial)
                                    .clipShape(Circle())
                            }
                            .padding(4)
                        }
                    } header: {
                        HStack {
                            Text("Content")
                            Spacer()
                            if viewModel.canUploadImages {
                                PhotosPicker(
                                    selection: $selectedPhoto,
                                    matching: .images,
                                    photoLibrary: .shared()
                                ) {
                                    Label("Image", systemImage: "photo.badge.plus")
                                        .font(.caption)
                                }
                            }
                        }
                    } footer: {
                        Text("Markdown. Inkwell converts it to the selected format on publish.")
                    }

                    // MARK: - Publish result
                    if let linkTag = viewModel.publishSuccess {
                        Section {
                            Text(linkTag)
                                .font(.footnote.monospaced())
                                .textSelection(.enabled)
                            Button("Copy Link Tag", systemImage: "doc.on.doc") {
                                UIPasteboard.general.string = linkTag
                                InkwellHaptics.success()
                            }
                            Button("Dismiss", role: .cancel) {
                                viewModel.publishSuccess = nil
                            }
                        } header: {
                            Label("Published", systemImage: "checkmark.circle.fill")
                                .foregroundStyle(.green)
                        } footer: {
                            Text("Add this tag to the document page's <head> so readers can discover the record.")
                        }
                    }
                }
                .navigationTitle("Write")
                .toolbar {
                    ToolbarItem(placement: .confirmationAction) {
                        Button {
                            viewModel.publish()
                        } label: {
                            if viewModel.isPublishing {
                                ProgressView()
                            } else {
                                Text(viewModel.isEditing ? "Update" : "Publish")
                            }
                        }
                        .disabled(!viewModel.canPublish)
                    }
                }
                .accountToolbar(showAbout: $viewModel.showAbout)
                .alert(
                    "Couldn't Publish",
                    isPresented: Binding(
                        get: { viewModel.publishError != nil },
                        set: { if !$0 { viewModel.publishError = nil } }
                    )
                ) {
                    Button("OK", role: .cancel) { viewModel.publishError = nil }
                } message: {
                    Text(viewModel.publishError ?? "")
                }
                .onChange(of: selectedPhoto) { _, newItem in
                    guard let newItem else { return }
                    Task {
                        if let data = try? await newItem.loadTransferable(type: Data.self) {
                            viewModel.uploadImage(data, mimeType: "image/jpeg")
                        }
                        selectedPhoto = nil
                    }
                }
                .task {
                    if CommandLine.arguments.contains("-screenshot") {
                        let mockPub = SiteStandardLexicon.PublicationRecord(
                            url: "https://ewancroft.uk",
                            name: "Ewan's Corner",
                            description: "Essays on open protocols, software, and digital garden notes."
                        )
                        viewModel.isLoadingPublications = false
                        viewModel.publications = [PublicationEntry(uri: "at://did:plc:ewan/site.standard.publication/1", authorDID: "did:plc:ewan", record: mockPub)]
                        viewModel.selectedPublication = viewModel.publications.first
                        viewModel.title = "Building Decentralized Sites with AT Protocol"
                        viewModel.path = "building-decentralized-sites"
                        viewModel.markdown = "# Building Decentralized Sites\n\nPublishing directly to your Personal Data Server ensures full ownership of your content.\n\n## Why Metadata Matters\n- Full portability across PDS hosts\n- Cryptographic verification"
                    }
                }
                .task {
                    await viewModel.loadPublications()
                }
                .task(id: viewModel.selectedPublication?.uri) {
                    await viewModel.verifySelectedPublication()
                }
                .sheet(isPresented: $viewModel.showCreatePublication) {
                    CreatePublicationView { url, name, desc in
                        Task {
                            do {
                                let reference = try await loginStateManager.createPublication(
                                    url: url,
                                    name: name,
                                    description: desc
                                )
                                await viewModel.loadPublications(selecting: reference.recordURI)
                            } catch {
                                viewModel.publishError = "Failed to create publication: \(error.localizedDescription)"
                            }
                        }
                    }
                }
                .sheet(isPresented: $showDocumentPicker) {
                    DocumentPickerSheet(
                        loginStateManager: loginStateManager,
                        publications: viewModel.publications,
                        selectedPublication: viewModel.selectedPublication,
                        onSelectDocument: { uri in
                            Task { await viewModel.loadDocumentForEditing(uri: uri) }
                        }
                    )
                }
            }
        }
    }
}

// MARK: - Document Picker Sheet

private struct DocumentPickerSheet: View {
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
                guard let uri = record.uri,
                      let value = record.value,
                      let doc = try? value.getRecord(ofType: SiteStandardLexicon.DocumentRecord.self) else {
                    return nil
                }
                return (uri, doc.title)
            }
        } catch {
            self.error = error.localizedDescription
        }
    }
}

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

#Preview {
    WriteView()
        .environment(LoginStateManager())
}
