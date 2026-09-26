//
//  WriterViewModel+Publish.swift
//  Inkwell
//
//  The publish/update path: validate, compose the record, spill oversized
//  content into the format's blob-backed representation, then write.
//

import SwiftUI
import InkwellShared
import ATProtoKit

extension WriterViewModel {

    // MARK: - Publishing

    func publish() {
        guard let publication = selectedPublication,
              let provider = ProviderRegistry.providerById(selectedProviderId) else {
            publishError = "Select a publication and format."
            return
        }

        guard !title.isEmpty else {
            publishError = "Title is required."
            return
        }

        if let validationError = StandardSiteInputValidation.firstDocumentError(
            site: publication.uri,
            title: title,
            description: description.isEmpty ? nil : description,
            path: path.isEmpty ? nil : path
        ) {
            publishError = validationError
            return
        }

        guard verifiedPublicationURI == publication.uri else {
            publishError = "Verify the publication domain before publishing."
            return
        }

        isPublishing = true
        publishError = nil
        publishSuccess = nil

        Task {
            do {
                if isEditing {
                    try await performUpdate(publication: publication, provider: provider)
                } else {
                    try await performCreate(publication: publication, provider: provider)
                }
            } catch {
                publishError = publishFailureMessage(error)
            }
            isPublishing = false
        }
    }

    /// A spill failure already says exactly what the author has to change, so
    /// it is surfaced verbatim rather than buried under a generic prefix.
    private func publishFailureMessage(_ error: Error) -> String {
        if let spill = error as? DocumentContentSpillError,
           let description = spill.errorDescription {
            return description
        }
        return isEditing
            ? writerEditErrorMessage(error)
            : "Failed to publish: \(error.localizedDescription)"
    }

    // MARK: - Record composition

    private func draft(for publication: PublicationEntry, isEdit: Bool) -> DocumentDraft {
        DocumentDraft(
            site: publication.uri,
            title: title,
            description: description.isEmpty ? nil : description,
            path: path.isEmpty ? nil : path,
            publishedAt: Date(),
            updatedAt: isEdit ? Date() : nil
        )
    }

    /// Builds the record that will be written, spilling content into the
    /// format's blob-backed representation when the inline record would blow
    /// past the AT Protocol record limit. The blob is uploaded first, per the
    /// normal blob lifecycle, so the record only ever references a blob the
    /// PDS already holds.
    ///
    /// Returns the inline content alongside the record: when the body spilled
    /// into a blob the record's own content is just a blob reference, which is
    /// useless as the next save's `WriteContext.previousContent`. The inline
    /// form is what image blobs are recovered from.
    private func composeRecord(
        publication: PublicationEntry,
        provider: ContentProvider,
        isEdit: Bool
    ) async throws -> (record: SiteStandardLexicon.DocumentRecord, inlineContent: UnknownType) {
        let writeContext = WriteContext(previousContent: isEdit ? editingDocumentContent : nil)
        guard let inlineContent = provider.fromMarkdown(markdown, ctx: writeContext) else {
            throw LoginError.contentConversionFailed
        }

        let draft = draft(for: publication, isEdit: isEdit)
        let fitted = try await DocumentContentSpill.fit(
            draft: draft,
            provider: provider,
            inlineContent: inlineContent,
            markdown: markdown,
            textContent: DocumentRecordComposer.plainText(fromMarkdown: markdown),
            preferBlobBacked: isEdit && editingDocumentIsBlobBacked,
            existingRawRecord: isEdit ? editingDocumentRawRecord : nil,
            uploadBlob: { data, mimeType in
                try await self.loginStateManager.uploadBlob(data, mimeType: mimeType)
            }
        )

        let record = DocumentRecordComposer.record(
            draft: draft,
            content: fitted.content,
            textContent: fitted.textContent
        )
        return (record, inlineContent)
    }

    // MARK: - Writes

    private func performCreate(publication: PublicationEntry, provider: ContentProvider) async throws {
        let (document, _) = try await composeRecord(
            publication: publication,
            provider: provider,
            isEdit: false
        )
        let reference = try await loginStateManager.createDocument(document)
        publishSuccess = SiteStandardLexicon.Verification.discoveryLinkTag(
            forRecordURI: reference.recordURI,
            relation: SiteStandardLexicon.DocumentRecord.type
        )
        InkwellHaptics.success()
        resetAfterPublish()
    }

    private func performUpdate(publication: PublicationEntry, provider: ContentProvider) async throws {
        guard let editURI = editingDocumentURI,
              let revision = editingDocumentRecordCID,
              let parsed = parseAtUri(editURI) else {
            publishError = "Invalid document URI."
            return
        }

        let (document, inlineContent) = try await composeRecord(
            publication: publication,
            provider: provider,
            isEdit: true
        )
        let written = try await loginStateManager.updateDocument(
            document,
            recordKey: parsed.recordKey,
            recordCID: revision,
            existingRawRecord: editingDocumentRawRecord
        )

        // The editor stays open on this document, so its edit baseline has to
        // advance to what was just written: the new CID is the revision a
        // second save must swap against, and the merged record carries the
        // fields the next save has to keep preserving. Without this, saving
        // twice in a row failed as a stale-revision conflict.
        editingDocumentRecordCID = written.reference.recordCID
        editingDocumentRawRecord = written.record
        editingDocumentContent = inlineContent
        editingDocumentIsBlobBacked = contentIsBlobBacked(document.content)
        publishSuccess = "Document updated."
        InkwellHaptics.success()
    }
}
