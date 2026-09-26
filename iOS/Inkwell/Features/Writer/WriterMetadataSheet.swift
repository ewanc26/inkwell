//
//  WriterMetadataSheet.swift
//  Inkwell
//
//  Edits the optional `site.standard.document` metadata layer: cover image,
//  tags, content warnings (self-labels), contributors, and the Bluesky post
//  carrying the document's discussion. State lives on `WriterViewModel`, so
//  dismissing the sheet never discards edits.
//

import SwiftUI
import PhotosUI

struct WriterMetadataSheet: View {
    @Bindable var viewModel: WriterViewModel
    @Environment(\.dismiss) private var dismiss
    @State private var coverPhoto: PhotosPickerItem?

    var body: some View {
        NavigationStack {
            Form {
                coverImageSection
                tagsSection
                contentWarningSection
                contributorsSection
                blueskySection
            }
            .navigationTitle("Metadata")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") { dismiss() }
                }
            }
            .alert(
                "Metadata",
                isPresented: Binding(
                    get: { viewModel.metadataError != nil },
                    set: { if !$0 { viewModel.metadataError = nil } }
                )
            ) {
                Button("OK", role: .cancel) { viewModel.metadataError = nil }
            } message: {
                Text(viewModel.metadataError ?? "")
            }
            .onChange(of: coverPhoto) { _, item in
                guard let item else { return }
                coverPhoto = nil
                Task {
                    guard let data = try? await item.loadTransferable(type: Data.self) else {
                        viewModel.metadataError = "Couldn't read the selected image."
                        return
                    }
                    do {
                        let output = try await ImageUploadSanitizer.sanitizeAsync(data)
                        await viewModel.uploadCoverImage(output)
                    } catch {
                        viewModel.metadataError = error.localizedDescription
                    }
                }
            }
        }
    }

    // MARK: - Cover image

    private var coverImageSection: some View {
        Section {
            if viewModel.isUploadingCoverImage {
                HStack(spacing: 10) {
                    ProgressView()
                    Text("Uploading cover image…")
                        .foregroundStyle(.secondary)
                }
                .accessibilityElement(children: .combine)
            } else if viewModel.coverImage != nil {
                coverPreview
                    .accessibilityLabel("Cover image")
                Button("Remove Cover Image", role: .destructive) {
                    viewModel.removeCoverImage()
                }
            }
            PhotosPicker(selection: $coverPhoto, matching: .images, photoLibrary: .shared()) {
                Label(
                    viewModel.coverImage == nil ? "Choose Cover Image" : "Replace Cover Image",
                    systemImage: "photo"
                )
            }
            .disabled(viewModel.isUploadingCoverImage)
        } header: {
            Text("Cover Image")
        } footer: {
            Text("Shown as the document's thumbnail. Up to 1 MB; it uploads to your PDS as soon as you choose it.")
        }
    }

    @ViewBuilder
    private var coverPreview: some View {
        if let data = viewModel.coverImagePreview, let image = UIImage(data: data) {
            Image(uiImage: image)
                .resizable()
                .scaledToFill()
                .frame(maxWidth: .infinity, maxHeight: 180)
                .clipShape(RoundedRectangle(cornerRadius: 8))
        } else if let cover = viewModel.coverImage, let did = viewModel.loginStateManager.currentDID {
            PDSBlobImage(
                did: did,
                cid: cover.reference.link,
                loginStateManager: viewModel.loginStateManager,
                height: 180
            )
            .clipShape(RoundedRectangle(cornerRadius: 8))
        }
    }

    // MARK: - Tags

    private var tagsSection: some View {
        Section {
            TagChipsEditor(
                tags: viewModel.tags,
                onAdd: { viewModel.addTags(from: $0) },
                onRemove: { viewModel.removeTag($0) }
            )
        } header: {
            Text("Tags")
        } footer: {
            Text("Used to categorize the document. No need for a leading #.")
        }
    }

    // MARK: - Content warnings

    private var contentWarningSection: some View {
        Section {
            ForEach(viewModel.displayedSelfLabels, id: \.self) { value in
                Toggle(
                    Self.labelTitle(value),
                    isOn: Binding(
                        get: { viewModel.isSelfLabelApplied(value) },
                        set: { viewModel.setSelfLabel(value, applied: $0) }
                    )
                )
            }
        } header: {
            Text("Content Warnings")
        } footer: {
            Text("Self-labels readers' apps use to blur or hide the document.")
        }
    }

    private static func labelTitle(_ value: String) -> String {
        switch value {
        case "sexual": return "Sexually Suggestive"
        case "nudity": return "Non-sexual Nudity"
        case "porn": return "Adult Content"
        case "graphic-media": return "Graphic Media"
        default: return value
        }
    }

    // MARK: - Contributors

    private var contributorsSection: some View {
        Section {
            ForEach($viewModel.contributors) { $contributor in
                VStack(alignment: .leading, spacing: 6) {
                    TextField("DID (did:plc:…)", text: $contributor.did)
                        .autocorrectionDisabled()
                        .textInputAutocapitalization(.never)
                        .font(.body.monospaced())
                        .accessibilityLabel("Contributor DID")
                    TextField("Role (optional)", text: $contributor.role)
                        .accessibilityLabel("Contributor role")
                    TextField("Display name (optional)", text: $contributor.displayName)
                        .accessibilityLabel("Contributor display name")
                }
                .padding(.vertical, 2)
            }
            .onDelete { viewModel.contributors.remove(atOffsets: $0) }

            Button {
                viewModel.contributors.append(ContributorDraft())
            } label: {
                Label("Add Contributor", systemImage: "person.badge.plus")
            }
        } header: {
            Text("Contributors")
        } footer: {
            Text("People who worked on this document besides you, such as editors or translators. Swipe to remove.")
        }
    }

    // MARK: - Bluesky discussion

    private var blueskySection: some View {
        Section {
            TextField("at://did:…/app.bsky.feed.post/…", text: $viewModel.bskyPostURI, axis: .vertical)
                .autocorrectionDisabled()
                .textInputAutocapitalization(.never)
                .keyboardType(.URL)
                .font(.body.monospaced())
                .accessibilityLabel("Bluesky post AT-URI")
        } header: {
            Text("Bluesky Discussion")
        } footer: {
            Text("Links the Bluesky post where this document is discussed. Inkwell looks up the post when you publish.")
        }
    }
}
