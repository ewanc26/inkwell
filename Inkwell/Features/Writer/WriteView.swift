//
//  WriteView.swift
//  Inkwell
//
//  Created by Letta on 20/06/2026.
//
//  Universal markdown editor for writing standard.site documents in any
//  supported format (Leaflet, Markpub, Pckt, Offprint). The editor works
//  in markdown; the selected provider converts to/from the format-specific
//  AT Protocol record on publish.
//

import SwiftUI
import ATProtoKit

struct WriteView: View {
    @Environment(LoginStateManager.self) private var loginStateManager

    @State private var publications: [PublicationEntry] = []
    @State private var selectedPublication: PublicationEntry?
    @State private var selectedProviderId: String = ProviderRegistry.defaultProvider.id
    @State private var title: String = ""
    @State private var description: String = ""
    @State private var markdown: String = ""
    @State private var path: String = ""
    @State private var isPublishing = false
    @State private var publishError: String?
    @State private var publishSuccess: String?
    @State private var isLoadingPublications = true
    @State private var showCreatePublication = false
    @State private var verifiedPublicationURI: String?
    @State private var verificationMessage: String?
    @State private var isVerifyingPublication = false
    @State private var showCredits = false

    var body: some View {
        NavigationStack {
            Form {
                // MARK: - Publication
                if isLoadingPublications {
                    HStack {
                        ProgressView()
                        Text("Loading publications...")
                    }
                } else if publications.isEmpty {
                    VStack(alignment: .leading, spacing: 8) {
                        Text("No publications found.")
                            .font(.callout)
                            .foregroundStyle(.secondary)
                        if let error = publishError {
                            Text(error)
                                .font(.caption)
                                .foregroundStyle(.red)
                                .lineLimit(4)
                        }
                        Button {
                            showCreatePublication = true
                        } label: {
                            Label("Create a Publication", systemImage: "plus.circle")
                        }
                    }
                } else {
                    Picker("Publication", selection: $selectedPublication) {
                        ForEach(publications) { publication in
                            Text(publication.record.name).tag(Optional(publication))
                        }
                    }
                    Button {
                        showCreatePublication = true
                    } label: {
                        Label("New Publication", systemImage: "plus")
                    }
                }

                if let publication = selectedPublication {
                    Section("Verification") {
                        Label(
                            verifiedPublicationURI == publication.uri ? "Verified publication" : "Verification required",
                            systemImage: verifiedPublicationURI == publication.uri ? "checkmark.seal.fill" : "exclamationmark.triangle"
                        )
                        .foregroundStyle(verifiedPublicationURI == publication.uri ? Color.green : Color.orange)

                        if let verificationMessage {
                            Text(verificationMessage)
                                .font(.caption)
                                .foregroundStyle(.secondary)
                                .textSelection(.enabled)
                        }

                        Button("Verify Again") {
                            Task { await verifySelectedPublication() }
                        }
                        .disabled(isVerifyingPublication)
                    }
                }

                // MARK: - Format Provider
                Picker("Format", selection: $selectedProviderId) {
                    ForEach(ProviderRegistry.providers, id: \.id) { provider in
                        Text(provider.label).tag(provider.id)
                    }
                }

                // MARK: - Document metadata
                TextField("Title", text: $title)
                TextField("Description", text: $description, axis: .vertical)
                    .lineLimit(2...4)
                TextField("Path (optional)", text: $path)
                    .autocorrectionDisabled()
                    .textInputAutocapitalization(.never)

                // MARK: - Markdown editor
                Section("Content") {
                    TextEditor(text: $markdown)
                        .frame(minHeight: 300)
                        .font(.body.monospaced())
                        .textInputAutocapitalization(.sentences)
                        .autocorrectionDisabled(false)
                }

                // MARK: - Publish
                if let error = publishError {
                    Text(error)
                        .foregroundStyle(.red)
                        .font(.callout)
                        .lineLimit(4)
                }
                if let success = publishSuccess {
                    Text(success)
                        .foregroundStyle(.green)
                        .font(.callout)
                        .lineLimit(6)
                }
                Button {
                    publish()
                } label: {
                    if isPublishing {
                        HStack {
                            ProgressView()
                            Text("Publishing...")
                        }
                    } else {
                        Label("Publish", systemImage: "paperplane.fill")
                    }
                }
                .disabled(
                    isPublishing || publications.isEmpty || title.isEmpty ||
                    verifiedPublicationURI != selectedPublication?.uri
                )
            }
            .navigationTitle("Write")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(role: .destructive, action: loginStateManager.signOut) {
                        Label("Sign Out", systemImage: "rectangle.portrait.and.arrow.right")
                    }
                }
                ToolbarItem(placement: .principal) {
                    HStack(spacing: 8) {
                        avatarView
                        Text(timeZoneAwareGreeting)
                            .font(.headline)
                            .lineLimit(1)
                            .minimumScaleFactor(0.8)
                    }
                }
                ToolbarItem(placement: .topBarTrailing) {
                    Button { showCredits = true } label: {
                        Image(systemName: "info.circle")
                    }
                }
            }
            .sheet(isPresented: $showCredits) {
                CreditsView()
            }
            .task {
                await loadPublications()
            }
            .task(id: selectedPublication?.uri) {
                await verifySelectedPublication()
            }
            .sheet(isPresented: $showCreatePublication) {
                CreatePublicationView { url, name, desc in
                    Task {
                        do {
                            let reference = try await loginStateManager.createPublication(
                                url: url,
                                name: name,
                                description: desc
                            )
                            await loadPublications(selecting: reference.recordURI)
                            publishSuccess = "Publication record created. Configure its verification endpoint before publishing."
                        } catch {
                            publishError = "Failed to create publication: \(error.localizedDescription)"
                        }
                    }
                }
            }
        }
    }

    // MARK: - Actions

    private func loadPublications(selecting uri: String? = nil) async {
        isLoadingPublications = true
        do {
            publications = try await loginStateManager.fetchPublicationsWithURIs()
            selectedPublication = uri.flatMap { selectedURI in
                publications.first(where: { $0.uri == selectedURI })
            } ?? publications.first
        } catch {
            publishError = "Failed to load publications: \(error.localizedDescription)"
        }
        isLoadingPublications = false
    }

    private func publish() {
        guard let pub = selectedPublication,
              let provider = ProviderRegistry.providerById(selectedProviderId) else {
            publishError = "Select a publication and format."
            return
        }

        guard !title.isEmpty else {
            publishError = "Title is required."
            return
        }

        guard verifiedPublicationURI == pub.uri else {
            publishError = "Verify the publication domain before publishing."
            return
        }

        isPublishing = true
        publishError = nil
        publishSuccess = nil

        Task {
            do {
                let reference = try await loginStateManager.createDocument(
                    title: title,
                    description: description.isEmpty ? nil : description,
                    path: path.isEmpty ? nil : path,
                    site: pub.uri,
                    markdown: markdown,
                    provider: provider
                )
                let linkTag = SiteStandardLexicon.Verification.discoveryLinkTag(
                    forRecordURI: reference.recordURI,
                    relation: SiteStandardLexicon.DocumentRecord.type
                )
                publishSuccess = "Published \(reference.recordURI)\nAdd this to the document page's <head>:\n\(linkTag)"
                // Reset form
                title = ""
                description = ""
                path = ""
                markdown = ""
            } catch {
                publishError = "Failed to publish: \(error.localizedDescription)"
            }
            isPublishing = false
        }
    }

    private func verifySelectedPublication() async {
        guard let publication = selectedPublication else {
            verifiedPublicationURI = nil
            verificationMessage = nil
            return
        }

        isVerifyingPublication = true
        defer { isVerifyingPublication = false }
        do {
            try await SiteStandardLexicon.Verification.verify(
                publicationURI: publication.uri,
                publication: publication.record
            )
            verifiedPublicationURI = publication.uri
            verificationMessage = "The publication domain points back to this record."
        } catch {
            verifiedPublicationURI = nil
            let endpoint = SiteStandardLexicon.Verification.publicationVerificationURL(
                for: publication.record.url
            )?.absoluteString ?? publication.record.url
            verificationMessage = "Serve \(publication.uri) as plain text from \(endpoint), then verify again."
        }
    }

    // MARK: - Header views (shared with BrowseDocumentsView)

    private var avatarView: some View {
        AsyncImage(url: loginStateManager.avatarURL) { phase in
            if let image = phase.image {
                image
                    .resizable()
                    .scaledToFill()
            } else {
                Image(systemName: "person.crop.circle.fill")
                    .resizable()
                    .foregroundStyle(.secondary)
            }
        }
        .frame(width: 24, height: 24)
        .clipShape(Circle())
    }

    private var timeZoneAwareGreeting: String {
        let hour = Calendar.current.component(.hour, from: Date())
        let period: String
        switch hour {
        case 5..<12: period = "morning"
        case 12..<17: period = "afternoon"
        case 17..<22: period = "evening"
        default: period = "night"
        }
        if let name = loginStateManager.displayName {
            return "Good \(period), \(name)"
        } else {
            return "Good \(period)"
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
                    TextField("URL (e.g. https://mysite.com)", text: $url)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                    TextField("Publication Name", text: $name)
                    TextField("Description (optional)", text: $description, axis: .vertical)
                        .lineLimit(2...4)
                }
                Section {
                    Button {
                        create()
                    } label: {
                        if isCreating {
                            HStack {
                                ProgressView()
                                Text("Creating...")
                            }
                        } else {
                            Label("Create Publication", systemImage: "plus.circle")
                        }
                    }
                    .disabled(url.isEmpty || name.isEmpty || isCreating)
                }
            }
            .navigationTitle("New Publication")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancel") { dismiss() }
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
