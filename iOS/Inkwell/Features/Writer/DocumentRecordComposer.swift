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
}

enum DocumentRecordComposer {

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
            coverImage: nil,
            content: content,
            textContent: textContent,
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
        : preservingUnknownFields(from: existingRawRecord, with: composed)
    return try JSONEncoder().encode(submitted).count
}
