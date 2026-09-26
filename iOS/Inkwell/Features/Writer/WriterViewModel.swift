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
    var editingDocumentRecordCID: String?
    var editingDocumentRawRecord: UnknownType?
    /// The loaded document's content with any blob-backed body resolved
    /// inline. Feeds `WriteContext.previousContent` so image blobs reattach.
    var editingDocumentContent: UnknownType?
    /// True when the loaded document stored its body in the format's
    /// blob-backed representation. Re-saving must not force it back inline.
    var editingDocumentIsBlobBacked = false
    var isEditing: Bool { editingDocumentURI != nil }
    var showDeleteConfirmation = false

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

    func uploadImage(_ image_data: Data, mimeType: String, altText: String = "") {
        guard canUploadImages else { return }
        Task {
            do {
                let blob = try await loginStateManager.uploadBlob(image_data, mimeType: mimeType)
                let cid = blob.reference.link
                uploadedBlobs[cid] = blob
                let markdownImage = "![\(altText.trimmingCharacters(in: .whitespacesAndNewlines))](\(cid))"
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
            // We need the record CID for swapRecord. Re-fetch to get it.
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
            editingDocumentRecordCID = cid

            let document = entry.record
            let rawRecord = try? await loginStateManager.getRepositoryRecord(
                from: parsed.did, collection: parsed.collection, recordKey: parsed.recordKey
            )
            editingDocumentRawRecord = rawRecord?.value
            title = document.title
            description = document.description ?? ""
            path = document.path ?? ""

            if let siteURI = document.site as? String {
                if let matchingPub = publications.first(where: { $0.uri == siteURI }) {
                    selectedPublication = matchingPub
                }
            }

            if let content = document.content {
                editingDocumentIsBlobBacked = contentIsBlobBacked(content)
                // A blob-backed body has to come back off the PDS before the
                // shared converter can turn it into editable markdown.
                var resolved = content
                if contentBodyNeedsBlobDownload(content) {
                    let fetched = await loginStateManager.resolveBlobBackedContent(
                        content,
                        authorDID: parsed.did
                    )
                    guard let fetched, !contentBodyNeedsBlobDownload(fetched) else {
                        cancelEditing()
                        publishError = "Couldn't download this document's stored content. "
                            + "Editing it now would publish an empty document."
                        return
                    }
                    resolved = fetched
                }
                editingDocumentContent = resolved

                let contentDict = unknownTypeToDict(resolved)
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
        editingDocumentRecordCID = nil
        editingDocumentRawRecord = nil
        editingDocumentContent = nil
        editingDocumentIsBlobBacked = false
        title = ""
        description = ""
        path = ""
        markdown = ""
        lostFeatures = []
        selectedProviderId = ProviderRegistry.defaultProvider.id
    }

    func deleteDocument() {
        guard let documentURI = editingDocumentURI,
              let revision = editingDocumentRecordCID,
              let parsed = parseAtUri(documentURI) else {
            publishError = "This document is no longer available to delete."
            return
        }

        isPublishing = true
        publishError = nil
        Task {
            do {
                try await loginStateManager.deleteRecord(
                    collection: SiteStandardLexicon.DocumentRecord.type,
                    recordKey: parsed.recordKey,
                    swapRecord: revision
                )
                await OfflineContentStore.shared.remove(uri: documentURI)
                cancelEditing()
                publishSuccess = "Document deleted."
                InkwellHaptics.success()
            } catch {
                publishError = writerDeleteErrorMessage(error)
            }
            isPublishing = false
        }
    }

    /// Resets the editor after a successful first publish.
    func resetAfterPublish() {
        title = ""
        description = ""
        path = ""
        markdown = ""
        lostFeatures = []
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

func preservingUnknownFields(from existing: UnknownType?, with updated: UnknownType) -> UnknownType {
    guard let existing,
          let existingFields = try? existing.asCodableValue(),
          let updatedFields = try? updated.asCodableValue() else {
        return updated
    }
    var merged = existingFields
    updatedFields.forEach { merged[$0.key] = $0.value }
    if case let .string(publishedAt)? = existingFields["publishedAt"],
       !publishedAt.isEmpty {
        merged["publishedAt"] = .string(publishedAt)
    }
    return .unknown(merged)
}
