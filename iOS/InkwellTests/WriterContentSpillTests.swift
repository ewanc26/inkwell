import XCTest
import ATProtoKit
@testable import Inkwell

/// Covers the decision `DocumentContentSpill.fit` makes just below and just
/// above the record-size threshold, for each format's defined blob-backed
/// representation and for the formats that have none.
///
/// The thresholds are measured rather than hardcoded: each test encodes the
/// candidate record, then sets `limit` relative to that measurement, so the
/// boundary is exact instead of approximate.
final class WriterContentSpillTests: XCTestCase {

    // MARK: - Fixtures

    private let draft = DocumentDraft(
        site: "at://did:plc:example/site.standard.publication/self",
        title: "Spill threshold",
        description: "boundary fixture",
        path: "/spill",
        publishedAt: Date(timeIntervalSince1970: 1_750_000_000),
        updatedAt: nil
    )

    /// A body large enough that spilling it into a blob is a real saving
    /// relative to the ~150-byte blob reference that replaces it.
    private func body(characters: Int) -> String {
        String(repeating: "abcdefghij", count: characters / 10)
    }

    private func blobStub(
        _ data: Data,
        _ mimeType: String
    ) -> ComAtprotoLexicon.Repository.UploadBlobOutput {
        .init(
            type: "blob",
            reference: .init(link: "bafyreispillfixture000000000000000000000000000000000000000"),
            mimeType: mimeType,
            size: data.count
        )
    }

    private func recordSize(
        _ content: UnknownType,
        _ textContent: String?,
        mergedWith existingRawRecord: UnknownType? = nil
    ) throws -> Int {
        try encodedDocumentRecordSize(
            DocumentRecordComposer.record(draft: draft, content: content, textContent: textContent),
            mergedWith: existingRawRecord
        )
    }

    /// Runs `fit`, recording what (if anything) was uploaded.
    private func fit(
        provider: ContentProvider,
        markdown: String,
        limit: Int,
        preferBlobBacked: Bool = false,
        existingRawRecord: UnknownType? = nil
    ) async throws -> (result: DocumentContentSpill.Result, uploads: [(Data, String)]) {
        guard let inline = provider.fromMarkdown(markdown, ctx: WriteContext(previousContent: nil)) else {
            throw XCTSkip("\(provider.label) could not convert the fixture markdown")
        }
        let uploads = UploadRecorder()
        let result = try await DocumentContentSpill.fit(
            draft: draft,
            provider: provider,
            inlineContent: inline,
            markdown: markdown,
            textContent: DocumentRecordComposer.plainText(fromMarkdown: markdown),
            preferBlobBacked: preferBlobBacked,
            existingRawRecord: existingRawRecord,
            limit: limit,
            uploadBlob: { data, mimeType in
                uploads.record(data, mimeType)
                return self.blobStub(data, mimeType)
            }
        )
        return (result, uploads.uploads)
    }

    /// Collects the blob uploads `fit` performs so tests can assert the
    /// blob was written before the record was composed.
    private final class UploadRecorder {
        private(set) var uploads: [(Data, String)] = []
        func record(_ data: Data, _ mimeType: String) { uploads.append((data, mimeType)) }
    }

    // MARK: - Threshold: markpub

    func testMarkpubContentJustUnderThresholdStaysInline() async throws {
        let provider = MarkpubProvider()
        let markdown = body(characters: 4_000)
        let inline = provider.fromMarkdown(markdown, ctx: WriteContext(previousContent: nil))!
        let textContent = DocumentRecordComposer.plainText(fromMarkdown: markdown)
        let inlineSize = try recordSize(inline, textContent)

        // Exactly at the limit: the record fits, so nothing spills.
        let (result, uploads) = try await fit(
            provider: provider,
            markdown: markdown,
            limit: inlineSize
        )

        XCTAssertFalse(result.isBlobBacked)
        XCTAssertEqual(result.textContent, textContent)
        XCTAssertTrue(uploads.isEmpty, "an inline record must not upload a content blob")
        XCTAssertNil(result.content.getRecord(ofType: MarkpubContent.self)?.text.textBlob)
        XCTAssertEqual(result.content.getRecord(ofType: MarkpubContent.self)?.text.markdown, markdown)
    }

    func testMarkpubContentJustOverThresholdSpillsToTextBlob() async throws {
        let provider = MarkpubProvider()
        let markdown = body(characters: 4_000)
        let inline = provider.fromMarkdown(markdown, ctx: WriteContext(previousContent: nil))!
        let textContent = DocumentRecordComposer.plainText(fromMarkdown: markdown)
        let inlineSize = try recordSize(inline, textContent)

        // One byte under the inline size: the record no longer fits.
        let (result, uploads) = try await fit(
            provider: provider,
            markdown: markdown,
            limit: inlineSize - 1
        )

        XCTAssertTrue(result.isBlobBacked)
        let markpub = try XCTUnwrap(result.content.getRecord(ofType: MarkpubContent.self))
        XCTAssertNotNil(markpub.text.textBlob, "markpub must spill into its defined textBlob")
        XCTAssertNil(markpub.text.markdown, "the body must not be duplicated inline")
        XCTAssertEqual(uploads.count, 1)
        XCTAssertEqual(uploads.first?.1, "text/markdown")
        XCTAssertEqual(uploads.first?.0, markdown.data(using: .utf8))
        // The blob-backed record must actually fit.
        XCTAssertLessThanOrEqual(try recordSize(result.content, result.textContent), inlineSize - 1)
    }

    // MARK: - Threshold: Leaflet

    func testLeafletContentJustUnderThresholdStaysInline() async throws {
        let provider = LeafletProvider()
        let markdown = body(characters: 4_000)
        let inline = provider.fromMarkdown(markdown, ctx: WriteContext(previousContent: nil))!
        let textContent = DocumentRecordComposer.plainText(fromMarkdown: markdown)
        let inlineSize = try recordSize(inline, textContent)

        let (result, uploads) = try await fit(
            provider: provider,
            markdown: markdown,
            limit: inlineSize
        )

        XCTAssertFalse(result.isBlobBacked)
        XCTAssertTrue(uploads.isEmpty)
        let leaflet = try XCTUnwrap(result.content.getRecord(ofType: LeafletContent.self))
        XCTAssertNil(leaflet.blobPages)
        XCTAssertFalse(leaflet.pages?.isEmpty ?? true)
    }

    func testLeafletContentJustOverThresholdSpillsToBlobPages() async throws {
        let provider = LeafletProvider()
        let markdown = body(characters: 4_000)
        let inline = provider.fromMarkdown(markdown, ctx: WriteContext(previousContent: nil))!
        let textContent = DocumentRecordComposer.plainText(fromMarkdown: markdown)
        let inlineSize = try recordSize(inline, textContent)

        let (result, uploads) = try await fit(
            provider: provider,
            markdown: markdown,
            limit: inlineSize - 1
        )

        XCTAssertTrue(result.isBlobBacked)
        let leaflet = try XCTUnwrap(result.content.getRecord(ofType: LeafletContent.self))
        XCTAssertNotNil(leaflet.blobPages, "Leaflet must spill into its defined blobPages")
        XCTAssertNil(leaflet.pages, "the pages must not be duplicated inline")
        XCTAssertEqual(uploads.count, 1)
        XCTAssertEqual(uploads.first?.1, "application/json")

        // The uploaded payload must decode exactly the way the Reader decodes
        // it — a plain `[LeafletPage]` array, not a wrapper object.
        let uploaded = try XCTUnwrap(uploads.first?.0)
        let decoded = try JSONDecoder().decode([LeafletPage].self, from: uploaded)
        let originalPages = try XCTUnwrap(inline.getRecord(ofType: LeafletContent.self)?.pages)
        XCTAssertEqual(decoded, originalPages)
    }

    // MARK: - A realistic oversized document against the shipped ceiling

    func testDocumentOverTheShippedCeilingSpillsWithoutAnExplicitLimit() async throws {
        let provider = MarkpubProvider()
        // Well past 900 KiB once the record carries both the markdown and the
        // plaintext copy.
        let markdown = body(characters: 1_000_000)
        let (result, uploads) = try await fit(
            provider: provider,
            markdown: markdown,
            limit: maxDocumentRecordBytes
        )

        XCTAssertTrue(result.isBlobBacked)
        XCTAssertEqual(uploads.count, 1)
        let finalSize = try recordSize(result.content, result.textContent)
        XCTAssertLessThanOrEqual(finalSize, maxDocumentRecordBytes)
        // textContent had to be capped too, or the record would still be over.
        let text = try XCTUnwrap(result.textContent)
        XCTAssertLessThanOrEqual(text.utf8.count, maxInlineTextContentBytes)
        XCTAssertTrue(text.hasSuffix("…"))
    }

    func testShippedCeilingMatchesTheSharedPolicy() {
        // Mirrors shared RecordSizePolicy.MAX_DOCUMENT_RECORD_BYTES.
        XCTAssertEqual(maxDocumentRecordBytes, 900 * 1024)
        XCTAssertEqual(maxInlineTextContentBytes, 64 * 1024)
    }

    // MARK: - textContent is trimmed before a blob is paid for

    func testOversizedTextContentIsTrimmedRatherThanSpillingContent() async throws {
        let provider = MarkpubProvider()
        // Long enough that the plaintext copy alone blows the 64 KiB budget.
        let markdown = body(characters: 200_000)
        let inline = provider.fromMarkdown(markdown, ctx: WriteContext(previousContent: nil))!
        let full = DocumentRecordComposer.plainText(fromMarkdown: markdown)
        let capped = truncateTextContent(full)
        XCTAssertNotEqual(full, capped, "fixture must exceed the textContent budget")

        let cappedSize = try recordSize(inline, capped)
        let (result, uploads) = try await fit(
            provider: provider,
            markdown: markdown,
            limit: cappedSize
        )

        XCTAssertFalse(result.isBlobBacked, "trimming textContent should avoid the upload")
        XCTAssertTrue(uploads.isEmpty)
        XCTAssertEqual(result.textContent, capped)
    }

    // MARK: - Formats with no blob-backed representation

    func testPcktReportsThatItCannotFitRatherThanInventingABlobField() async {
        let provider = PcktProvider()
        XCTAssertFalse(provider.supportsBlobBackedContent)

        do {
            _ = try await fit(provider: provider, markdown: body(characters: 4_000), limit: 512)
            XCTFail("pckt has no blob-backed representation; it must not publish")
        } catch let error as DocumentContentSpillError {
            guard case .formatCannotFit(let format, _, let limit) = error else {
                return XCTFail("expected formatCannotFit, got \(error)")
            }
            XCTAssertEqual(format, provider.label)
            XCTAssertEqual(limit, 512)
            let message = try? XCTUnwrap(error.errorDescription)
            XCTAssertTrue(message?.contains("Markdown (markpub)") ?? false)
            XCTAssertTrue(message?.contains("Leaflet") ?? false)
        } catch {
            XCTFail("expected DocumentContentSpillError, got \(error)")
        }
    }

    func testOffprintReportsThatItCannotFit() async {
        let provider = OffprintProvider()
        XCTAssertFalse(provider.supportsBlobBackedContent)

        do {
            _ = try await fit(provider: provider, markdown: body(characters: 4_000), limit: 512)
            XCTFail("Offprint has no blob-backed representation; it must not publish")
        } catch let error as DocumentContentSpillError {
            guard case .formatCannotFit = error else {
                return XCTFail("expected formatCannotFit, got \(error)")
            }
        } catch {
            XCTFail("expected DocumentContentSpillError, got \(error)")
        }
    }

    // MARK: - Round-trip preservation

    func testEditingABlobBackedDocumentKeepsItBlobBackedEvenWhenItWouldFitInline() async throws {
        let provider = MarkpubProvider()
        let markdown = "A short edit that would comfortably fit inline."

        let (result, uploads) = try await fit(
            provider: provider,
            markdown: markdown,
            limit: maxDocumentRecordBytes,
            preferBlobBacked: true
        )

        XCTAssertTrue(result.isBlobBacked, "a blob-backed document must not be forced back inline")
        XCTAssertEqual(uploads.count, 1)
        XCTAssertNil(result.content.getRecord(ofType: MarkpubContent.self)?.text.markdown)
    }

    func testEditingABlobBackedLeafletKeepsBlobPages() async throws {
        let (result, _) = try await fit(
            provider: LeafletProvider(),
            markdown: "# Short\n\nStill blob-backed.",
            limit: maxDocumentRecordBytes,
            preferBlobBacked: true
        )

        XCTAssertTrue(result.isBlobBacked)
        let leaflet = try XCTUnwrap(result.content.getRecord(ofType: LeafletContent.self))
        XCTAssertNotNil(leaflet.blobPages)
        XCTAssertNil(leaflet.pages)
    }

    func testAFormatWithNoBlobFieldStillPublishesAnEditThatFitsInline() async throws {
        // A pckt document that was (somehow) flagged blob-backed shouldn't fail
        // an edit that fits perfectly well inline: there is nothing to preserve.
        let (result, uploads) = try await fit(
            provider: PcktProvider(),
            markdown: "Short pckt body.",
            limit: maxDocumentRecordBytes,
            preferBlobBacked: true
        )

        XCTAssertFalse(result.isBlobBacked)
        XCTAssertTrue(uploads.isEmpty)
    }

    // MARK: - The edit path measures the record it actually submits

    func testEditPreflightCountsFieldsPreservedFromTheExistingRevision() async throws {
        let provider = MarkpubProvider()
        let markdown = body(characters: 4_000)
        let inline = provider.fromMarkdown(markdown, ctx: WriteContext(previousContent: nil))!
        let textContent = DocumentRecordComposer.plainText(fromMarkdown: markdown)

        // A previous revision carrying a field Inkwell doesn't model. An edit
        // merges it back in, so what reaches the PDS is larger than the record
        // the Writer composed.
        let existing = UnknownType.unknown([
            "$type": .string("site.standard.document"),
            "someOtherClientsField": .string(String(repeating: "x", count: 4_000)),
        ])

        // Measure all three candidates rather than guessing at encoding
        // overhead: the composed record on its own, the same record merged
        // with the existing revision, and the blob-backed record merged with
        // it. The blob reference and the preserved field both cost bytes, so
        // the ordering has to be observed, not assumed.
        let composedSize = try recordSize(inline, textContent)
        let inlineMergedSize = try recordSize(inline, textContent, mergedWith: existing)
        XCTAssertGreaterThan(inlineMergedSize, composedSize, "fixture must make the merge bigger")

        let payload = try XCTUnwrap(provider.blobBackedContent(for: inline, markdown: markdown))
        let spilled = try XCTUnwrap(payload.build(blobStub(payload.data, payload.mimeType)))
        let spilledMergedSize = try recordSize(spilled, textContent, mergedWith: existing)

        // A ceiling the composed record clears and the merged record doesn't,
        // and that the blob-backed merge does clear.
        let limit = max(composedSize, spilledMergedSize)
        XCTAssertLessThan(
            limit,
            inlineMergedSize,
            "fixture must leave a ceiling only the blob-backed merge fits under"
        )

        let (result, uploads) = try await fit(
            provider: provider,
            markdown: markdown,
            limit: limit,
            existingRawRecord: existing
        )

        XCTAssertTrue(result.isBlobBacked, "the merged record is the one that has to fit")
        XCTAssertEqual(uploads.count, 1)
        XCTAssertLessThanOrEqual(
            try recordSize(result.content, result.textContent, mergedWith: existing),
            limit
        )
    }

    // MARK: - Blob-backed detection

    func testBlobBackedDetectionDistinguishesPresenceFromNeedingADownload() throws {
        let blob = blobStub(Data("x".utf8), "text/markdown")

        let blobOnly = UnknownType.record(
            MarkpubContent(text: MarkpubText(markdown: nil, textBlob: blob))
        )
        XCTAssertTrue(contentIsBlobBacked(blobOnly))
        XCTAssertTrue(contentBodyNeedsBlobDownload(blobOnly))

        // Both present: readable as-is, but still wants to stay blob-backed.
        let both = UnknownType.record(
            MarkpubContent(text: MarkpubText(markdown: "inline copy", textBlob: blob))
        )
        XCTAssertTrue(contentIsBlobBacked(both))
        XCTAssertFalse(contentBodyNeedsBlobDownload(both))

        let inlineOnly = UnknownType.record(
            MarkpubContent(text: MarkpubText(markdown: "inline copy"))
        )
        XCTAssertFalse(contentIsBlobBacked(inlineOnly))
        XCTAssertFalse(contentBodyNeedsBlobDownload(inlineOnly))

        let leafletBlobOnly = UnknownType.record(LeafletContent(pages: nil, blobPages: blob))
        XCTAssertTrue(contentIsBlobBacked(leafletBlobOnly))
        XCTAssertTrue(contentBodyNeedsBlobDownload(leafletBlobOnly))

        let leafletBoth = UnknownType.record(
            LeafletContent(
                pages: [LeafletPage(type: "pub.leaflet.pages.linearDocument", blocks: [])],
                blobPages: blob
            )
        )
        XCTAssertTrue(contentIsBlobBacked(leafletBoth))
        XCTAssertFalse(contentBodyNeedsBlobDownload(leafletBoth))

        XCTAssertFalse(contentIsBlobBacked(nil))
        XCTAssertFalse(contentBodyNeedsBlobDownload(nil))
    }

    // MARK: - textContent truncation policy (mirrors shared RecordSizePolicy)

    func testTruncationLeavesTextUnderBudgetUntouched() {
        let text = String(repeating: "a", count: maxInlineTextContentBytes)
        XCTAssertEqual(truncateTextContent(text), text)
        XCTAssertNil(truncateTextContent(nil))
    }

    func testTruncationStaysWithinTheByteBudget() throws {
        let text = String(repeating: "a", count: maxInlineTextContentBytes + 1_000)
        let truncated = try XCTUnwrap(truncateTextContent(text))
        XCTAssertLessThanOrEqual(truncated.utf8.count, maxInlineTextContentBytes)
        XCTAssertTrue(truncated.hasSuffix("…"))
    }

    func testTruncationNeverSplitsAMultiByteScalar() throws {
        let text = String(repeating: "😀", count: 64)
        let truncated = try XCTUnwrap(truncateTextContent(text, limit: 40))
        XCTAssertLessThanOrEqual(truncated.utf8.count, 40)
        // Every scalar bar the suffix must still be the whole emoji.
        XCTAssertTrue(truncated.dropLast().allSatisfy { $0 == "😀" })
    }

    func testTruncationReturnsNilWhenNothingFits() {
        XCTAssertNil(truncateTextContent("hello", limit: 1))
        XCTAssertNil(truncateTextContent("hello", limit: 0))
    }
}
