//
//  WriterViewModel+Draft.swift
//  Inkwell
//
//  Draft autosave and recovery: projects the editor into a `WriterDraft`,
//  debounces writes to `WriterDraftStore`, restores the signed-in account's
//  draft on launch, and holds a draft back when the document it edits has
//  changed on the PDS since. Mirrors Android's `WriterDraftExtensions.kt`.
//

import Foundation
import ATProtoKit

/// Contributor wire shape inside `WriterDraft.metadataContributors`.
private struct DraftContributor: Codable {
    let did: String
    let role: String?
    let displayName: String?
}

extension WriterViewModel {

    // MARK: - Projection

    /// The autosaveable projection of the editor, with an empty `savedAt` so
    /// two projections compare equal whenever the user-visible content does.
    func draftSnapshot(accountDid: String) -> WriterDraft {
        let contributorRows = contributors.map {
            DraftContributor(
                did: $0.did,
                role: $0.role.isEmpty ? nil : $0.role,
                displayName: $0.displayName.isEmpty ? nil : $0.displayName
            )
        }
        let contributorsJSON = (try? JSONEncoder().encode(contributorRows))
            .flatMap { String(data: $0, encoding: .utf8) } ?? "[]"
        let coverJSON = coverImage
            .flatMap { try? JSONEncoder().encode($0) }
            .flatMap { String(data: $0, encoding: .utf8) }
        let bskyURI = bskyPostURI.trimmingCharacters(in: .whitespacesAndNewlines)

        return WriterDraft(
            accountDid: accountDid,
            publicationUri: selectedPublication?.uri ?? pendingDraftPublicationURI ?? "",
            editingDocumentUri: editingDocumentURI,
            editingDocumentRevision: editingDocumentURI == nil ? nil : editingDocumentRecordCID,
            title: title,
            description: description,
            path: path,
            format: selectedProviderId,
            markdown: markdown,
            uploadedBlobKeys: Set(uploadedBlobs.keys).union(restoredBlobKeys).sorted(),
            metadataTags: tags,
            metadataContributors: contributorsJSON,
            bskyPostRefUri: bskyURI.isEmpty ? nil : bskyPostURI,
            selfLabelValues: selfLabels,
            coverImageJson: coverJSON,
            savedAt: ""
        )
    }

    /// What `WriteView` watches to drive autosave; nil until this account's
    /// stored draft has been read, so the empty initial editor can never
    /// overwrite a draft that hasn't been restored yet.
    var autosaveSnapshot: WriterDraft? {
        guard draftAutosaveReady, let did = draftAccountDID else { return nil }
        return draftSnapshot(accountDid: did)
    }

    // MARK: - Restore

    /// Loads `accountDid`'s draft (if any) once per account, then enables autosave.
    func restoreDraft(accountDid did: String?) async {
        if let did, did == draftAccountDID, draftAutosaveReady { return }
        draftAutosaveTask?.cancel()
        draftAutosaveReady = false
        // Keep the last account while signed out, so the next sign-in can tell a switch.
        guard let did else { return }
        let previous = draftAccountDID
        draftAccountDID = did

        // Another account's text must never be autosaved as this account's draft.
        if let previous, previous != did {
            cancelEditing()
            uploadedBlobs = [:]
            clearDraftFlags()
        }
        draftBaseline = nil
        draftWritten = false

        if let draft = await draftStore.load(accountDid: did) {
            if let uri = draft.editingDocumentUri {
                // An edit needs the live record (its CID and unmodelled fields);
                // loading it compares revisions and applies or holds the draft.
                await loadDocumentForEditing(uri: uri)
            } else {
                applyDraft(draft)
            }
        }
        guard !Task.isCancelled, draftAccountDID == did else { return }
        draftAutosaveReady = true
    }

    /// Reads the stored draft for `uri` before the editor starts changing, so
    /// autosave cannot overwrite it mid-load.
    func storedDraft(forDocument uri: String) async -> WriterDraft? {
        guard let did = draftAccountDID else { return nil }
        let draft = await draftStore.load(accountDid: did)
        return draft?.editingDocumentUri == uri ? draft : nil
    }

    /// Called once `uri` is loaded: a draft based on the current revision is
    /// applied on top, while one based on an older revision is held for the
    /// user to choose rather than silently winning or losing.
    func reconcileDraft(_ stored: WriterDraft?, loadedDocument uri: String) {
        clearDraftFlags()
        markDraftSettled()
        guard let stored else { return }
        if let revision = editingDocumentRecordCID,
           WriterDraftPolicy.hasRevisionConflict(stored, documentUri: uri, remoteRevision: revision) {
            heldConflictDraft = stored
            draftConflict = true
        } else {
            applyDraft(stored)
        }
    }

    /// Puts `draft`'s content into the editor on top of whatever document is loaded.
    func applyDraft(_ draft: WriterDraft, announce: Bool = true) {
        title = draft.title
        description = draft.description
        path = draft.path
        markdown = draft.markdown
        if !isEditing, !draft.format.isEmpty, ProviderRegistry.providerById(draft.format) != nil {
            selectedProviderId = draft.format
        }
        tags = draft.metadataTags
        let rows = (try? JSONDecoder().decode([DraftContributor].self, from: Data(draft.metadataContributors.utf8))) ?? []
        contributors = rows.map {
            ContributorDraft(did: $0.did, role: $0.role ?? "", displayName: $0.displayName ?? "")
        }
        selfLabels = draft.selfLabelValues
        // The CID is re-resolved at publish unless this still names the loaded reference.
        bskyPostURI = draft.bskyPostRefUri ?? ""
        coverImage = draft.coverImageJson.flatMap {
            try? JSONDecoder().decode(ComAtprotoLexicon.Repository.UploadBlobOutput.self, from: Data($0.utf8))
        }
        coverImagePreview = nil
        restoredBlobKeys = Set(draft.uploadedBlobKeys)

        if let publication = publications.first(where: { $0.uri == draft.publicationUri }) {
            if selectedPublication?.uri != publication.uri { selectedPublication = publication }
        } else if !draft.publicationUri.isEmpty {
            pendingDraftPublicationURI = draft.publicationUri
        }

        draftRestored = announce
        showDraftBanner = announce
        draftConflict = false
        heldConflictDraft = nil
    }

    // MARK: - Autosave

    /// Debounces a write of `snapshot`; each call cancels the previous one.
    func scheduleDraftAutosave(_ snapshot: WriterDraft?) {
        draftAutosaveTask?.cancel()
        // Never write while a document is loading or the user is choosing between two versions.
        guard let snapshot, !draftAutosaveSuspended, !draftConflict else { return }
        draftAutosaveTask = Task { [weak self] in
            try? await Task.sleep(for: .milliseconds(WriterDraftPolicy.maxDebounceMs))
            guard !Task.isCancelled, let self else { return }
            await self.persistDraft(snapshot)
        }
    }

    private func persistDraft(_ snapshot: WriterDraft) async {
        if snapshot == draftBaseline {
            await draftStore.clear(accountDid: snapshot.accountDid)
        } else if WriterDraftPolicy.isEmpty(snapshot) {
            // Empty only clears once this session has written: an editor that
            // never loaded (an offline edit restore) must not erase the draft.
            if draftWritten { await draftStore.clear(accountDid: snapshot.accountDid) }
        } else {
            var stamped = snapshot
            stamped.savedAt = WriterDraftPolicy.timestamp(Date())
            do {
                try await draftStore.save(stamped)
                draftWritten = true
            } catch {
                print("[WriterDraft] Could not autosave draft: \(type(of: error))")
            }
        }
    }

    /// Sets the content that counts as "nothing to save", e.g. a freshly
    /// loaded or just-published document.
    func markDraftSettled() {
        guard let did = draftAccountDID else { return }
        draftBaseline = draftSnapshot(accountDid: did)
    }

    // MARK: - Publish / discard

    /// Clears the stored draft after a successful publish, update, or delete.
    func onDraftPublished() {
        draftAutosaveTask?.cancel()
        restoredBlobKeys = []
        markDraftSettled()
        clearDraftFlags()
        guard let did = draftAccountDID else { return }
        Task { await draftStore.clear(accountDid: did) }
    }

    /// Throws the restored draft away: an edit reverts to the published
    /// document, a new document goes back to an empty editor.
    func discardDraft() {
        if isEditing, let baseline = draftBaseline, baseline.editingDocumentUri == editingDocumentURI {
            applyDraft(baseline, announce: false)
        } else {
            resetAfterPublish()
            uploadedBlobs = [:]
        }
        onDraftPublished()
    }

    func dismissDraftBanner() {
        showDraftBanner = false
    }

    /// Keeping the draft rebases it onto the current revision, so publishing
    /// deliberately replaces the remote edit (still guarded by `swapRecord`).
    /// Reloading drops the draft in favour of the published version.
    func resolveDraftConflict(keepDraft: Bool) {
        guard let held = heldConflictDraft else {
            draftConflict = false
            return
        }
        if keepDraft {
            applyDraft(held)
        } else {
            discardDraft()
        }
    }

    private func clearDraftFlags() {
        draftRestored = false
        showDraftBanner = false
        draftConflict = false
        heldConflictDraft = nil
    }
}
