//
//  DocumentContentSpill.swift
//  Inkwell
//
//  Keeps a published `site.standard.document` under AT Protocol's practical
//  record size by moving authored content into the content format's own
//  blob-backed representation (markpub `textBlob`, Leaflet `blobPages`)
//  rather than failing the write.
//
//  Formats that define no blob-backed representation are not given an
//  invented one: they surface an explicit Writer error instead.
//

import Foundation
import ATProtoKit

/// A content format's defined blob-backed representation of its content.
///
/// `data` is uploaded with `com.atproto.repo.uploadBlob` *before* the record
/// is written, and `build` then produces the content object that references
/// the returned blob.
struct BlobBackedContent {
    let data: Data
    let mimeType: String
    let build: (ComAtprotoLexicon.Repository.UploadBlobOutput) -> UnknownType?
}

enum DocumentContentSpillError: LocalizedError {
    /// The chosen format has no interoperable way to represent this content
    /// within the record limit.
    case formatCannotFit(format: String, bytes: Int, limit: Int)
    /// The blob-backed record is still oversized — the metadata alone is too big.
    case stillTooLargeAfterSpill(format: String, bytes: Int, limit: Int)
    case blobContentEncodingFailed(format: String)

    var errorDescription: String? {
        switch self {
        case .formatCannotFit(let format, let bytes, let limit):
            return "\(format) has no blob-backed way to store a document this large "
                + "(\(bytes) bytes; the limit is \(limit)). Switch the format to "
                + "\(Self.blobBackedFormatList), or shorten the document."
        case .stillTooLargeAfterSpill(let format, let bytes, let limit):
            return "Even with the content stored as a blob, this \(format) record is "
                + "\(bytes) bytes, over the \(limit)-byte limit. Shorten the title, "
                + "description, or path."
        case .blobContentEncodingFailed(let format):
            return "Couldn't prepare this \(format) document's content for upload."
        }
    }

    /// The formats the author can actually switch to, read off the registry so
    /// this copy can't name a format that lost (or gained) blob support.
    private static var blobBackedFormatList: String {
        let labels = ProviderRegistry.providers
            .filter(\.supportsBlobBackedContent)
            .map(\.label)
        guard let last = labels.last else { return "a format with blob-backed content" }
        guard labels.count > 1 else { return last }
        return labels.dropLast().joined(separator: ", ") + " or " + last
    }
}

/// Budget for the portable `textContent` copy.
///
/// Mirrors shared `RecordSizePolicy.MAX_INLINE_TEXT_CONTENT_BYTES`; declared
/// locally for the same reason as `maxDocumentRecordBytes` in
/// `LoginStateManager+Records.swift` — the checked-in
/// `InkwellShared.xcframework` predates the constant. Switch to the shared
/// constant once the framework consuming this change is rebuilt.
let maxInlineTextContentBytes = 64 * 1024
private let textContentTruncationSuffix = "…"

/// Truncates `text` to at most `limit` UTF-8 bytes without splitting a
/// Unicode scalar. Mirrors shared `RecordSizePolicy.truncateTextContent`.
func truncateTextContent(_ text: String?, limit: Int = maxInlineTextContentBytes) -> String? {
    guard let text else { return nil }
    guard limit > 0 else { return nil }
    guard text.utf8.count > limit else { return text }

    let budget = limit - textContentTruncationSuffix.utf8.count
    guard budget > 0 else { return nil }

    var used = 0
    var truncated = ""
    // Unicode scalars, not characters: a grapheme cluster can be arbitrarily
    // long, and cutting between scalars is still valid UTF-8.
    for scalar in text.unicodeScalars {
        let width = String(scalar).utf8.count
        if used + width > budget { break }
        used += width
        truncated.unicodeScalars.append(scalar)
    }
    guard !truncated.isEmpty else { return nil }
    return truncated + textContentTruncationSuffix
}

enum DocumentContentSpill {

    struct Result {
        let content: UnknownType
        let textContent: String?
        /// True when the content was written through the format's blob-backed
        /// representation rather than inline.
        let isBlobBacked: Bool
    }

    /// Chooses inline or blob-backed content so the final record fits.
    ///
    /// - Parameters:
    ///   - preferBlobBacked: set when the document being edited was already
    ///     blob-backed. Re-saving must not silently force it back inline.
    ///   - existingRawRecord: the record already in the repo, on an edit. The
    ///     write merges the composed record over it, so the size decision has
    ///     to be made against that merge, not the composed record alone.
    ///   - uploadBlob: the blob upload, injected so the decision logic is
    ///     testable without a PDS.
    static func fit(
        draft: DocumentDraft,
        provider: ContentProvider,
        inlineContent: UnknownType,
        markdown: String,
        textContent: String?,
        preferBlobBacked: Bool,
        existingRawRecord: UnknownType? = nil,
        limit: Int = maxDocumentRecordBytes,
        uploadBlob: (Data, String) async throws -> ComAtprotoLexicon.Repository.UploadBlobOutput
    ) async throws -> Result {

        func size(_ content: UnknownType, _ text: String?) throws -> Int {
            try encodedDocumentRecordSize(
                DocumentRecordComposer.record(draft: draft, content: content, textContent: text),
                mergedWith: existingRawRecord
            )
        }

        let cappedText = truncateTextContent(textContent)

        if !preferBlobBacked {
            if try size(inlineContent, textContent) <= limit {
                return Result(content: inlineContent, textContent: textContent, isBlobBacked: false)
            }
            // The plaintext copy is a search convenience — trim it before
            // paying for a blob upload.
            if try size(inlineContent, cappedText) <= limit {
                return Result(content: inlineContent, textContent: cappedText, isBlobBacked: false)
            }
        }

        guard let payload = provider.blobBackedContent(for: inlineContent, markdown: markdown) else {
            if preferBlobBacked, try size(inlineContent, cappedText) <= limit {
                // Nothing to spill into, but it fits: publish inline rather
                // than fail an edit that was already viable.
                return Result(content: inlineContent, textContent: cappedText, isBlobBacked: false)
            }
            throw DocumentContentSpillError.formatCannotFit(
                format: provider.label,
                bytes: try size(inlineContent, cappedText),
                limit: limit
            )
        }

        let blob = try await uploadBlob(payload.data, payload.mimeType)
        guard let spilled = payload.build(blob) else {
            throw DocumentContentSpillError.blobContentEncodingFailed(format: provider.label)
        }

        if try size(spilled, textContent) <= limit {
            return Result(content: spilled, textContent: textContent, isBlobBacked: true)
        }
        let spilledSize = try size(spilled, cappedText)
        guard spilledSize <= limit else {
            throw DocumentContentSpillError.stillTooLargeAfterSpill(
                format: provider.label,
                bytes: spilledSize,
                limit: limit
            )
        }
        return Result(content: spilled, textContent: cappedText, isBlobBacked: true)
    }
}

// MARK: - Blob-backed detection

/// True when stored content references its format's blob-backed
/// representation at all, so an edit must keep it that way.
///
/// Deliberately distinct from `contentBodyNeedsBlobDownload`: a record may
/// carry *both* the blob reference and an inline copy, which is readable as-is
/// but still wants to stay blob-backed when re-saved.
func contentIsBlobBacked(_ content: UnknownType?) -> Bool {
    guard let content else { return false }
    if let leaflet = content.getRecord(ofType: LeafletContent.self) {
        return leaflet.blobPages != nil
    }
    if let markpub = content.getRecord(ofType: MarkpubContent.self) {
        return markpub.text.textBlob != nil
    }
    return false
}

/// True when the blob is the only copy of the body, so it has to come off the
/// PDS before the content can be rendered or edited. A record carrying both a
/// blob reference and usable inline content needs no download.
func contentBodyNeedsBlobDownload(_ content: UnknownType?) -> Bool {
    guard let content else { return false }
    if let leaflet = content.getRecord(ofType: LeafletContent.self) {
        return leaflet.blobPages != nil && (leaflet.pages?.isEmpty ?? true)
    }
    if let markpub = content.getRecord(ofType: MarkpubContent.self) {
        return markpub.text.textBlob != nil && (markpub.text.markdown?.isEmpty ?? true)
    }
    return false
}
