//
//  WriterDraftSchema.swift
//  Inkwell
//
//  The on-disk shape of an autosaved Writer draft and the rules about it.
//
//  Mirrors shared `WriterDraftSchema`/`WriterDraftPolicy`
//  (`shared/.../draft/WriterDraftSchema.kt`), which the checked-in
//  `InkwellShared.xcframework` predates. Field names, defaults, and every
//  rule below must match the Kotlin source exactly; once the framework is
//  rebuilt, replace this file with thin wrappers over the shared types.
//

import Foundation

/// A Writer draft as autosaved to app-private storage. Never leaves the
/// device and holds no credentials — only what the user typed and the
/// non-secret identifiers needed to put it back in context.
nonisolated struct WriterDraft: Codable, Equatable, Sendable {
    /// The signed-in DID the draft belongs to; a draft never restores into another account.
    var accountDid: String
    /// The selected publication's AT-URI, or empty when none was selected.
    var publicationUri: String
    /// The document being edited, or nil for a new document.
    var editingDocumentUri: String?
    /// CID of `editingDocumentUri` when it was loaded, used to detect remote edits since.
    var editingDocumentRevision: String?
    var title = ""
    var description = ""
    var path = ""
    var format = ""
    var markdown = ""
    /// Blob CIDs inserted into `markdown` as images.
    var uploadedBlobKeys: [String] = []
    var metadataTags: [String] = []
    /// JSON array of `{did, role?, displayName?}` objects.
    var metadataContributors = "[]"
    var bskyPostRefUri: String?
    var selfLabelValues: [String] = []
    /// Cover image blob ref as JSON, or nil.
    var coverImageJson: String?
    /// ISO-8601 instant the draft was written.
    var savedAt: String
    var schemaVersion = WriterDraftPolicy.schemaVersion

    init(
        accountDid: String,
        publicationUri: String,
        editingDocumentUri: String? = nil,
        editingDocumentRevision: String? = nil,
        title: String = "",
        description: String = "",
        path: String = "",
        format: String = "",
        markdown: String = "",
        uploadedBlobKeys: [String] = [],
        metadataTags: [String] = [],
        metadataContributors: String = "[]",
        bskyPostRefUri: String? = nil,
        selfLabelValues: [String] = [],
        coverImageJson: String? = nil,
        savedAt: String
    ) {
        self.accountDid = accountDid
        self.publicationUri = publicationUri
        self.editingDocumentUri = editingDocumentUri
        self.editingDocumentRevision = editingDocumentRevision
        self.title = title
        self.description = description
        self.path = path
        self.format = format
        self.markdown = markdown
        self.uploadedBlobKeys = uploadedBlobKeys
        self.metadataTags = metadataTags
        self.metadataContributors = metadataContributors
        self.bskyPostRefUri = bskyPostRefUri
        self.selfLabelValues = selfLabelValues
        self.coverImageJson = coverImageJson
        self.savedAt = savedAt
    }

    /// Matches the Kotlin codec: defaulted fields may be absent, unknown keys are ignored.
    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        accountDid = try c.decode(String.self, forKey: .accountDid)
        publicationUri = try c.decode(String.self, forKey: .publicationUri)
        editingDocumentUri = try c.decodeIfPresent(String.self, forKey: .editingDocumentUri)
        editingDocumentRevision = try c.decodeIfPresent(String.self, forKey: .editingDocumentRevision)
        title = try c.decodeIfPresent(String.self, forKey: .title) ?? ""
        description = try c.decodeIfPresent(String.self, forKey: .description) ?? ""
        path = try c.decodeIfPresent(String.self, forKey: .path) ?? ""
        format = try c.decodeIfPresent(String.self, forKey: .format) ?? ""
        markdown = try c.decodeIfPresent(String.self, forKey: .markdown) ?? ""
        uploadedBlobKeys = try c.decodeIfPresent([String].self, forKey: .uploadedBlobKeys) ?? []
        metadataTags = try c.decodeIfPresent([String].self, forKey: .metadataTags) ?? []
        metadataContributors = try c.decodeIfPresent(String.self, forKey: .metadataContributors) ?? "[]"
        bskyPostRefUri = try c.decodeIfPresent(String.self, forKey: .bskyPostRefUri)
        selfLabelValues = try c.decodeIfPresent([String].self, forKey: .selfLabelValues) ?? []
        coverImageJson = try c.decodeIfPresent(String.self, forKey: .coverImageJson)
        savedAt = try c.decode(String.self, forKey: .savedAt)
        schemaVersion = try c.decodeIfPresent(Int.self, forKey: .schemaVersion) ?? WriterDraftPolicy.schemaVersion
    }
}

/// Autosave timing, expiry, and conflict rules. Mirrors shared `WriterDraftPolicy`.
nonisolated enum WriterDraftPolicy {
    /// Autosave waits this long after the last edit before writing.
    static let maxDebounceMs: UInt64 = 2000
    static let schemaVersion = 1
    /// Drafts older than this are discarded rather than offered for recovery.
    static let maxAgeMs: Int64 = 30 * 24 * 60 * 60 * 1000

    /// True when `savedAt` is more than `maxAgeMs` before `now`. An
    /// unparseable timestamp counts as stale.
    static func isDraftStale(savedAt: String, now: Date) -> Bool {
        guard let saved = parseInstant(savedAt) else { return true }
        return Int64((now.timeIntervalSince(saved) * 1000).rounded(.down)) > maxAgeMs
    }

    /// True when the draft carries nothing the user typed or chose.
    static func isEmpty(_ draft: WriterDraft) -> Bool {
        let contributors = draft.metadataContributors.trimmingCharacters(in: .whitespacesAndNewlines)
        return draft.title.isBlank
            && draft.description.isBlank
            && draft.path.isBlank
            && draft.markdown.isBlank
            && draft.metadataTags.isEmpty
            && (contributors.isEmpty || contributors == "[]")
            && (draft.bskyPostRefUri ?? "").isBlank
            && draft.selfLabelValues.isEmpty
            && draft.coverImageJson == nil
    }

    /// True when `draft` may be offered to `accountDid` at `now`.
    static func isRestorable(_ draft: WriterDraft, accountDid: String, now: Date) -> Bool {
        draft.accountDid == accountDid
            && draft.schemaVersion == schemaVersion
            && !isDraftStale(savedAt: draft.savedAt, now: now)
    }

    /// True when `draft` edits `documentUri` but was based on a different
    /// revision than the `remoteRevision` now on the PDS.
    static func hasRevisionConflict(_ draft: WriterDraft, documentUri: String, remoteRevision: String) -> Bool {
        draft.editingDocumentUri == documentUri
            && draft.editingDocumentRevision != nil
            && draft.editingDocumentRevision != remoteRevision
    }

    static func timestamp(_ date: Date) -> String {
        fractionalFormatter.string(from: date)
    }

    private static func parseInstant(_ value: String) -> Date? {
        fractionalFormatter.date(from: value) ?? wholeSecondFormatter.date(from: value)
    }

    // ISO8601DateFormatter is documented thread-safe; it just isn't marked Sendable.
    nonisolated(unsafe) private static let fractionalFormatter: ISO8601DateFormatter = {
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        return formatter
    }()

    nonisolated(unsafe) private static let wholeSecondFormatter = ISO8601DateFormatter()
}

nonisolated private extension String {
    /// Kotlin `isBlank`: empty or whitespace only.
    var isBlank: Bool { allSatisfy(\.isWhitespace) }
}
