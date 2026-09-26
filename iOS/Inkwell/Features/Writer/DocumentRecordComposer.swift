//
//  DocumentRecordComposer.swift
//  Inkwell
//
//  One place that turns what the Writer collected into a
//  `site.standard.document` record. Both the create and the edit path go
//  through here so normalization (site/path/plaintext) and field coverage
//  can't drift between them.
//

import Foundation
import ATProtoKit

/// Everything the Writer collects for a single `site.standard.document`.
struct DocumentDraft {
    /// Publication AT-URI or standalone publication URL.
    var site: String
    var title: String
    var description: String?
    var path: String?
    var publishedAt: Date
    /// Set only when saving an edit; nil for a first publish.
    var updatedAt: Date?
    var coverImage: ComAtprotoLexicon.Repository.UploadBlobOutput? = nil
    var bskyPostRef: ComAtprotoLexicon.Repository.StrongReference? = nil
    var tags: [String]? = nil
    var labels: ComAtprotoLexicon.Label.SelfLabelsDefinition? = nil
    var contributors: [SiteStandardLexicon.DocumentRecord.Contributor]? = nil
}

enum DocumentRecordComposer {

    /// Record keys the Writer's metadata sheet owns. On an edit, a key in this
    /// set that the composed record leaves out has been cleared by the author,
    /// so it must be removed from the merged record rather than carried over
    /// from the stored one.
    ///
    /// Mirrors shared `DocumentMetadata.OWNED_KEYS` minus `links`: the Writer
    /// doesn't edit `links`, so it stays on the unknown-field path and is
    /// written back verbatim. Declared locally because the checked-in
    /// `InkwellShared.xcframework` predates `DocumentMetadata`; switch to the
    /// shared constant once the framework is rebuilt.
    static let metadataKeys: Set<String> = [
        "tags", "contributors", "labels", "coverImage", "bskyPostRef",
    ]

    /// Trimmed, de-duplicated tags with any leading `#` removed (Standard.site
    /// asks for no hashtags), or nil when none remain — absent, not `[]`.
    static func normalizedTags(_ tags: [String]) -> [String]? {
        var seen = Set<String>()
        let cleaned = tags
            .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
            .map { String($0.drop(while: { $0 == "#" })) }
            .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
            .filter { !$0.isEmpty && seen.insert($0).inserted }
        return cleaned.isEmpty ? nil : cleaned
    }

    /// Self-label values as the record's `labels`, or nil when there are none.
    static func selfLabels(_ values: [String]) -> ComAtprotoLexicon.Label.SelfLabelsDefinition? {
        var seen = Set<String>()
        let cleaned = values
            .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
            .filter { !$0.isEmpty && seen.insert($0).inserted }
        guard !cleaned.isEmpty else { return nil }
        return ComAtprotoLexicon.Label.SelfLabelsDefinition(
            values: cleaned.map { ComAtprotoLexicon.Label.SelfLabelDefinition(value: $0) }
        )
    }

    /// Contributors with blank optional fields dropped, or nil when none have a DID.
    static func contributors(_ drafts: [ContributorDraft]) -> [SiteStandardLexicon.DocumentRecord.Contributor]? {
        func nonEmpty(_ value: String) -> String? {
            let trimmed = value.trimmingCharacters(in: .whitespacesAndNewlines)
            return trimmed.isEmpty ? nil : trimmed
        }
        let contributors = drafts.compactMap { draft -> SiteStandardLexicon.DocumentRecord.Contributor? in
            guard let did = nonEmpty(draft.did) else { return nil }
            return .init(did: did, role: nonEmpty(draft.role), displayName: nonEmpty(draft.displayName))
        }
        return contributors.isEmpty ? nil : contributors
    }

    /// Standard.site asks for no trailing slash on `site`.
    static func normalizedSite(_ site: String) -> String {
        var normalized = site
        while normalized.hasSuffix("/") {
            normalized.removeLast()
        }
        return normalized
    }

    /// Paths are combined with `site`, so they carry a leading slash.
    static func normalizedPath(_ path: String?) -> String? {
        guard let trimmed = path?.trimmingCharacters(in: .whitespacesAndNewlines),
              !trimmed.isEmpty else { return nil }
        return trimmed.hasPrefix("/") ? trimmed : "/\(trimmed)"
    }

    /// The portable plaintext copy stored in `textContent`, derived from the
    /// editor's markdown. Returns nil when there's nothing worth storing.
    static func plainText(fromMarkdown markdown: String) -> String? {
        (try? AttributedString(markdown: markdown))
            .map { String($0.characters) }
            .flatMap { $0.isEmpty ? nil : $0 }
    }

    /// Builds the record. `content` and `textContent` are passed in already
    /// resolved so the spill step can swap in a blob-backed representation
    /// without this function knowing about blobs.
    static func record(
        draft: DocumentDraft,
        content: UnknownType?,
        textContent: String?
    ) -> SiteStandardLexicon.DocumentRecord {
        SiteStandardLexicon.DocumentRecord(
            site: normalizedSite(draft.site),
            title: draft.title,
            publishedAt: draft.publishedAt,
            path: normalizedPath(draft.path),
            description: draft.description.flatMap { $0.isEmpty ? nil : $0 },
            coverImage: draft.coverImage,
            content: content,
            textContent: textContent,
            bskyPostRef: draft.bskyPostRef,
            tags: draft.tags,
            labels: draft.labels,
            contributors: draft.contributors,
            updatedAt: draft.updatedAt
        )
    }
}

/// Encoded size of the record exactly as it will be submitted over XRPC.
///
/// On an edit the write merges the composed record over the record already in
/// the repo (`preservingUnknownFields`), so fields Inkwell doesn't model come
/// back and count against the limit. Pass `mergedWith:` on that path or the
/// spill decision would be made against a smaller record than the one the PDS
/// actually receives.
func encodedDocumentRecordSize(
    _ record: SiteStandardLexicon.DocumentRecord,
    mergedWith existingRawRecord: UnknownType? = nil
) throws -> Int {
    let composed = UnknownType.record(record)
    let submitted = existingRawRecord == nil
        ? composed
        : preservingUnknownFields(
            from: existingRawRecord,
            with: composed,
            clearingAbsent: DocumentRecordComposer.metadataKeys
        )
    return try JSONEncoder().encode(submitted).count
}
