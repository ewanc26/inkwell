//
//  WriterViewModel+Metadata.swift
//  Inkwell
//
//  The optional `site.standard.document` metadata layer: tags, contributors,
//  self-labels, the Bluesky discussion post, and the cover image. Loaded from
//  the record on edit, edited in `WriterMetadataSheet`, and folded into the
//  `DocumentDraft` on publish.
//

import SwiftUI
import InkwellShared
import ATProtoKit

/// One editable contributor row. Fields stay plain strings while editing;
/// `DocumentRecordComposer.contributors` trims them into the wire shape.
struct ContributorDraft: Identifiable, Equatable {
    let id = UUID()
    var did = ""
    var role = ""
    var displayName = ""
}

enum WriterMetadataError: LocalizedError {
    case invalidBskyPostURI
    case bskyPostUnresolved(String)
    case coverImageTooLarge(bytes: Int)

    var errorDescription: String? {
        switch self {
        case .invalidBskyPostURI:
            return "The Bluesky post must be an at://did:…/app.bsky.feed.post/… URI."
        case .bskyPostUnresolved(let reason):
            return "Couldn't find that Bluesky post to reference it: \(reason)"
        case .coverImageTooLarge(let bytes):
            return "Cover images must be at most 1 MB; this one is "
                + ByteCountFormatter.string(fromByteCount: Int64(bytes), countStyle: .file)
                + " after preparation. Choose a smaller image."
        }
    }
}

/// Author-applicable self-label values, in display order. Mirrors shared
/// `DocumentMetadata.SELF_LABEL_VALUES`, which the checked-in
/// `InkwellShared.xcframework` predates.
let writerSelfLabelValues = ["sexual", "nudity", "porn", "graphic-media"]

/// `site.standard.document#coverImage` `maxSize`.
private let maxCoverImageBytes = 1_000_000
private let bskyPostCollection = "app.bsky.feed.post"

extension WriterViewModel {

    // MARK: - Load / reset

    func loadMetadata(from document: SiteStandardLexicon.DocumentRecord) {
        tags = document.tags ?? []
        contributors = (document.contributors ?? []).map {
            ContributorDraft(did: $0.did, role: $0.role ?? "", displayName: $0.displayName ?? "")
        }
        selfLabels = document.labels?.values.map(\.value) ?? []
        resolvedBskyPostRef = document.bskyPostRef
        bskyPostURI = document.bskyPostRef?.recordURI ?? ""
        coverImage = document.coverImage
        coverImagePreview = nil
    }

    func resetMetadata() {
        tags = []
        contributors = []
        selfLabels = []
        bskyPostURI = ""
        resolvedBskyPostRef = nil
        coverImage = nil
        coverImagePreview = nil
        metadataError = nil
    }

    // MARK: - Tags

    /// Adds each comma-separated tag in `input` that isn't already present.
    func addTags(from input: String) {
        let candidates = input.split(separator: ",").map(String.init)
        for tag in DocumentRecordComposer.normalizedTags(candidates) ?? [] where !tags.contains(tag) {
            tags.append(tag)
        }
    }

    func removeTag(_ tag: String) {
        tags.removeAll { $0 == tag }
    }

    // MARK: - Labels

    /// Offered values first, then any other values the loaded record carried.
    var displayedSelfLabels: [String] {
        writerSelfLabelValues + selfLabels.filter { !writerSelfLabelValues.contains($0) }
    }

    func isSelfLabelApplied(_ value: String) -> Bool {
        selfLabels.contains(value)
    }

    func setSelfLabel(_ value: String, applied: Bool) {
        if applied {
            if !selfLabels.contains(value) { selfLabels.append(value) }
        } else {
            selfLabels.removeAll { $0 == value }
        }
    }

    // MARK: - Cover image

    func uploadCoverImage(_ output: ImageUploadSanitizer.Output) async {
        guard output.data.count <= maxCoverImageBytes else {
            metadataError = WriterMetadataError.coverImageTooLarge(bytes: output.data.count).errorDescription
            return
        }
        isUploadingCoverImage = true
        defer { isUploadingCoverImage = false }
        do {
            coverImage = try await loginStateManager.uploadBlob(output.data, mimeType: output.mimeType)
            coverImagePreview = output.data
        } catch {
            metadataError = "Failed to upload cover image: \(error.localizedDescription)"
        }
    }

    func removeCoverImage() {
        coverImage = nil
        coverImagePreview = nil
    }

    // MARK: - Publish

    /// Resolves `bskyPostURI` to a strong reference. An unchanged reference is
    /// reused as loaded; a new one has its current CID fetched from the
    /// author's PDS, since `com.atproto.repo.strongRef` requires it.
    func resolveBskyPostRef() async throws -> ComAtprotoLexicon.Repository.StrongReference? {
        let uri = bskyPostURI.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !uri.isEmpty else { return nil }
        if let resolved = resolvedBskyPostRef, resolved.recordURI == uri {
            return resolved
        }
        guard let parsed = parseAtUri(uri),
              parsed.did.hasPrefix("did:"),
              parsed.collection == bskyPostCollection else {
            throw WriterMetadataError.invalidBskyPostURI
        }
        let record: (uri: String, cid: String?, value: UnknownType?)
        do {
            record = try await loginStateManager.getRepositoryRecord(
                from: parsed.did,
                collection: parsed.collection,
                recordKey: parsed.recordKey
            )
        } catch {
            throw WriterMetadataError.bskyPostUnresolved(error.localizedDescription)
        }
        guard let cid = record.cid, !cid.isEmpty else {
            throw WriterMetadataError.bskyPostUnresolved("the PDS returned no record CID.")
        }
        let reference = ComAtprotoLexicon.Repository.StrongReference(recordURI: uri, cidHash: cid)
        resolvedBskyPostRef = reference
        return reference
    }

    /// Copies the metadata state onto `draft`, resolving the Bluesky reference.
    func applyMetadata(to draft: inout DocumentDraft) async throws {
        draft.tags = DocumentRecordComposer.normalizedTags(tags)
        draft.contributors = DocumentRecordComposer.contributors(contributors)
        draft.labels = DocumentRecordComposer.selfLabels(selfLabels)
        draft.coverImage = coverImage
        draft.bskyPostRef = try await resolveBskyPostRef()
    }
}
