//
//  WriterViewModel.swift
//  Inkwell
//
//  ViewModel for the Write screen, extracted from WriteView to enable
//  proper state management, document editing, image upload, and loss
//  reporting — matching the architecture standard.horse uses.
//

import SwiftUI
import InkwellShared
import ATProtoKit

/// ViewModel for the writing screen. Owns all editor state and actions.
@Observable
@MainActor
final class WriterViewModel {
    let loginStateManager: LoginStateManager

    // MARK: - Publications

    var publications: [PublicationEntry] = []
    var selectedPublication: PublicationEntry?
    var isLoadingPublications = true

    // MARK: - Verification

    var verifiedPublicationURI: String?
    var verificationMessage: String?
    var isVerifyingPublication = false

    // MARK: - Document metadata

    var title = ""
    var description = ""
    var path = ""
    var selectedProviderId: String = ProviderRegistry.defaultProvider.id
    var markdown = ""

    // MARK: - Editor state

    var showPreview = true
    var uploadedBlobs: [String: ComAtprotoLexicon.Repository.UploadBlobOutput] = [:]
    var lostFeatures: [String] = []

    // MARK: - Document editing

    var editingDocumentURI: String?
    var editingDocumentRevision: String?
    var isEditing: Bool { editingDocumentURI != nil }

    // MARK: - Publishing

    var isPublishing = false
    var publishError: String?
    var publishSuccess: String?

    // MARK: - UI state

    var showCreatePublication = false
    var showAbout = false

    // MARK: - Computed

    var canPublish: Bool {
        !isPublishing && !publications.isEmpty && !title.isEmpty
            && verifiedPublicationURI == selectedPublication?.uri
    }

    var isVerified: Bool {
        selectedPublication.map { verifiedPublicationURI == $0.uri } ?? false
    }

    var activeProvider: ContentProvider? {
        ProviderRegistry.providerById(selectedProviderId)
    }

    var canUploadImages: Bool {
        activeProvider?.supportsImages ?? false
    }

    // MARK: - Init

    init(loginStateManager: LoginStateManager) {
        self.loginStateManager = loginStateManager
    }

    // MARK: - Publication Loading

    func loadPublications(selecting uri: String? = nil) async {
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

    func selectPublication(_ publication: PublicationEntry) {
        selectedPublication = publication
        verifiedPublicationURI = nil
        verificationMessage = nil
        publishSuccess = nil
        publishError = nil
    }

    // MARK: - Verification

    func verifySelectedPublication() async {
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
            ) ?? publication.record.url
            verificationMessage = "Serve \(publication.uri) as plain text from \(endpoint), then verify again."
        }
    }

    // MARK: - Image Upload

    func uploadImage(_ image_data: Data, mimeType: String) {
        guard canUploadImages else { return }
        Task {
            do {
                let blob = try await loginStateManager.uploadBlob(image_data, mimeType: mimeType)
                let cid = blob.reference.link
                uploadedBlobs[cid] = blob
                let markdownImage = "![Image](\(cid))"
                insertTextAtEnd(markdownImage)
            } catch {
                publishError = "Failed to upload image: \(error.localizedDescription)"
            }
        }
    }

    // MARK: - Text Insertion

    func insertTextAtEnd(_ text: String) {
        if markdown.isEmpty {
            markdown = text
        } else {
            markdown += "\n\n\(text)"
        }
    }

    func insertMarkdown(_ syntax: String, placeholder: String = "text") {
        markdown += "\n\(syntax.replacingOccurrences(of: "text", with: placeholder))"
    }

    // MARK: - Document Editing

    func loadDocumentForEditing(uri: String) async {
        do {
            let entry = try await loginStateManager.fetchDocument(uri: uri)

            editingDocumentURI = uri
            // We need the CID for swapCommit. Re-fetch to get it.
            let parsed = parseAtUri(uri)
            guard let parsed else {
                publishError = "Invalid document URI."
                return
            }
            let (_, cid, _) = try await loginStateManager.getRepositoryRecord(
                from: parsed.did,
                collection: parsed.collection,
                recordKey: parsed.recordKey
            )
            editingDocumentRevision = cid

            let document = entry.record
            title = document.title
            description = document.description ?? ""
            path = document.path ?? ""

            if let siteURI = document.site as? String {
                if let matchingPub = publications.first(where: { $0.uri == siteURI }) {
                    selectedPublication = matchingPub
                }
            }

            if let content = document.content {
                let contentDict = unknownTypeToDict(content)
                let contentType = contentDict["$type"] as? String
                if let formatName = ContentFormatDispatcher.shared.formatForContentType(type: contentType) {
                    selectedProviderId = formatName.lowercased() == "markpub" ? "markpub" : formatName
                }

                let convertResult = sharedContentToMarkdown(contentDict)
                markdown = convertResult.markdown
                lostFeatures = convertResult.lost
            }
        } catch {
            publishError = "Failed to load document: \(error.localizedDescription)"
        }
    }

    func cancelEditing() {
        editingDocumentURI = nil
        editingDocumentRevision = nil
        title = ""
        description = ""
        path = ""
        markdown = ""
        lostFeatures = []
        selectedProviderId = ProviderRegistry.defaultProvider.id
    }

    // MARK: - Publishing

    func publish() {
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
                if let editURI = editingDocumentURI, let revision = editingDocumentRevision {
                    let parsed = parseAtUri(editURI)
                    guard let parsed else {
                        publishError = "Invalid document URI."
                        isPublishing = false
                        return
                    }

                    let writeCtx = WriteContext(previousContent: nil)
                    guard let contentRecord = provider.fromMarkdown(markdown, ctx: writeCtx) else {
                        throw LoginError.contentConversionFailed
                    }

                    var normalizedSite = pub.uri
                    while normalizedSite.hasSuffix("/") {
                        normalizedSite.removeLast()
                    }
                    let normalizedPath = path.isEmpty ? nil : (path.hasPrefix("/") ? path : "/\(path)")
                    let plainText = (try? AttributedString(markdown: markdown))
                        .map { String($0.characters) }
                        .flatMap { $0.isEmpty ? nil : $0 }

                    let document = SiteStandardLexicon.DocumentRecord(
                        site: normalizedSite,
                        title: title,
                        publishedAt: Date(),
                        path: normalizedPath,
                        description: description.isEmpty ? nil : description,
                        coverImage: nil,
                        content: contentRecord,
                        textContent: plainText
                    )

                    try await loginStateManager.updateRecord(
                        collection: SiteStandardLexicon.DocumentRecord.type,
                        recordKey: parsed.recordKey,
                        record: UnknownType.record(document),
                        revision: revision
                    )
                    publishSuccess = "Document updated."
                    InkwellHaptics.success()
                } else {
                    let reference = try await loginStateManager.createDocument(
                        title: title,
                        description: description.isEmpty ? nil : description,
                        path: path.isEmpty ? nil : path,
                        site: pub.uri,
                        markdown: markdown,
                        provider: provider,
                        previousContent: nil
                    )
                    let linkTag = SiteStandardLexicon.Verification.discoveryLinkTag(
                        forRecordURI: reference.recordURI,
                        relation: SiteStandardLexicon.DocumentRecord.type
                    )
                    publishSuccess = linkTag
                    InkwellHaptics.success()

                    title = ""
                    description = ""
                    path = ""
                    markdown = ""
                    lostFeatures = []
                }
            } catch {
                publishError = "Failed to publish: \(error.localizedDescription)"
            }
            isPublishing = false
        }
    }
}

// MARK: - Helpers

/// Converts an `UnknownType` to a dictionary for shared KMP consumption.
private func unknownTypeToDict(_ value: UnknownType) -> [String: Any] {
    guard let data = try? JSONEncoder().encode(value),
          let dict = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else {
        return [:]
    }
    return dict
}
