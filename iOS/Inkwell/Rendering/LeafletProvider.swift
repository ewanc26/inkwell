//
//  LeafletProvider.swift
//  Inkwell
//

import Foundation
import InkwellShared
import ATProtoKit

// MARK: - Leaflet Provider

/// Leaflet provider (`pub.leaflet.content`). Leaflet documents are a list of
/// pages; we read and write a single `linearDocument` page whose `blocks` map
/// closely to markdown. Inline formatting uses leaflet's richtext facets.
struct LeafletProvider: ContentProvider {
    let id = "leaflet"
    let label = "Leaflet"
    let contentType = LeafletTypes.shared.CONTENT
    let supportsImages = true

    private let schema = FacetSchema.leaflet

    private let lossLabels: [String: String] = [
        LeafletTypes.shared.BLOCKS_IFRAME: "embeds",
        LeafletTypes.shared.BLOCKS_WEBSITE: "website cards",
        LeafletTypes.shared.BLOCKS_BSKY_POST: "Bluesky posts",
        LeafletTypes.shared.BLOCKS_STANDARD_SITE_POST: "linked posts",
        LeafletTypes.shared.BLOCKS_PAGE: "sub-pages",
        LeafletTypes.shared.BLOCKS_POLL: "polls",
        LeafletTypes.shared.BLOCKS_BUTTON: "buttons",
        LeafletTypes.shared.BLOCKS_POSTS_LIST: "post lists",
        LeafletTypes.shared.BLOCKS_SIGNUP: "signup forms",
    ]
    // NOTE: Loss labels are also defined in shared KMP BlockLossLabels.leaflet.
    // The iOS providers keep their own copy for now because they work with
    // typed Swift structs; the shared copy is used by Android and new code.

    func matches(_ content: UnknownType?) -> Bool {
        content?.getRecord(ofType: LeafletContent.self) != nil
    }

    /// When `blobPages` are present (large leaflet documents), the caller
    /// should fetch and decode the blob before building the LeafletContent.
    /// This helper unpacks pages from either source.
    private static func unpackPages(_ content: LeafletContent) -> [LeafletPage] {
        content.pages ?? []
    }

    func toMarkdown(_ content: UnknownType?) -> ConvertResult {
        guard let leaflet = content?.getRecord(ofType: LeafletContent.self) else {
            return ConvertResult(markdown: "", lost: [])
        }

        var lost = Set<String>()
        var blocks: [MarkdownBlockNode] = []

        // When blobPages are present, the inline pages array may be empty —
        // the caller is expected to have already fetched the blob and
        // reconstructed LeafletContent with the decoded pages. We just
        // read whatever pages are available.
        let pages = LeafletProvider.unpackPages(leaflet)
        // Prefer the linearDocument page, but accept any page.
        let page = pages.first(where: { $0.type == "pub.leaflet.pages.linearDocument" }) ?? pages.first
        let blockContainers = page?.blocks ?? []

        for container in blockContainers {
            let inner = container.block
            if let mdBlock = leafletBlockToMarkdown(inner, alignment: container.alignment, lost: &lost) {
                blocks.append(mdBlock)
            }
        }

        return ConvertResult(markdown: MarkdownSerializerEngine.serialize(blocks), lost: Array(lost))
    }

    private func leafletBlockToMarkdown(_ block: LeafletBlock, alignment: String?, lost: inout Set<String>) -> MarkdownBlockNode? {
        if let alignment = alignment, !alignment.hasSuffix("textAlignLeft") {
            lost.insert("text alignment")
        }

        switch block.type {
        case LeafletTypes.shared.BLOCKS_TEXT:
            let text = FacetConverter.facetsToMarkdown(
                block.plaintext ?? "", facets: block.facets, schema: schema
            )
            // Empty text blocks (leaflet spacers) have no markdown equivalent.
            return text.isEmpty ? nil : .paragraph(text: text)

        case LeafletTypes.shared.BLOCKS_HEADER:
            let level = max(1, min(6, block.level ?? 1))
            let text = FacetConverter.facetsToMarkdown(
                block.plaintext ?? "", facets: block.facets, schema: schema
            )
            return .heading(level: level, text: text)

        case LeafletTypes.shared.BLOCKS_BLOCKQUOTE:
            let text = FacetConverter.facetsToMarkdown(
                block.plaintext ?? "", facets: block.facets, schema: schema
            )
            return .blockquote(text: text)

        case LeafletTypes.shared.BLOCKS_CODE:
            return .code(language: block.language, content: block.plaintext ?? "")

        case LeafletTypes.shared.BLOCKS_MATH:
            return .math(tex: block.tex ?? "")

        case LeafletTypes.shared.BLOCKS_HORIZONTAL_RULE:
            return .horizontalRule

        case LeafletTypes.shared.BLOCKS_IMAGE:
            // Image blobs are stored as PDS blobs; we reference by CID.
            let cid = block.image?.reference.link ?? ""
            return cid.isEmpty ? nil : .image(alt: block.alt ?? "", url: cid)

        case LeafletTypes.shared.BLOCKS_UNORDERED_LIST:
            let items = (block.children ?? []).map { leafletListItemToMarkdown($0) }
            return .unorderedList(items: items)

        case LeafletTypes.shared.BLOCKS_ORDERED_LIST:
            let start = block.startIndex ?? 1
            let items = (block.children ?? []).map { leafletListItemToMarkdown($0) }
            return .orderedList(start: start, items: items)

        default:
            if let label = lossLabels[block.type] {
                lost.insert(label)
            } else {
                lost.insert("an unsupported block")
            }
            return nil
        }
    }

    private func leafletListItemToMarkdown(_ item: LeafletListItem) -> MarkdownListItemNode {
        var text = ""
        if let content = item.content {
            switch content.type {
            case LeafletTypes.shared.BLOCKS_TEXT:
                text = FacetConverter.facetsToMarkdown(
                    content.plaintext ?? "", facets: content.facets, schema: schema
                )
            case LeafletTypes.shared.BLOCKS_IMAGE:
                let cid = content.image?.reference.link ?? ""
                text = "![\(content.alt ?? "")](\(cid))"
            default:
                text = content.plaintext ?? ""
            }
        }

        // standard.horse handles both `children` (unordered nested) and
        // `orderedListChildren` (ordered nested). Inkwell's LeafletListItem
        // only has `children`, but when a stored record has both we prefer
        // `orderedListChildren` for ordered nesting.
        var mdChildren: [MarkdownListItemNode]? = nil
        if let kids = item.children, !kids.isEmpty {
            mdChildren = kids.map { leafletListItemToMarkdown($0) }
        }

        return MarkdownListItemNode(text: text, checked: item.checked, children: mdChildren)
    }

    func fromMarkdown(_ markdown: String, ctx: WriteContext) -> UnknownType? {
        // Harvest existing image blobs from the previous content so CIDs in
        // markdown (e.g. ![](bafy...)) can be matched and reattached verbatim
        // without re-uploading — matching standard.horse's round-trip pattern.
        let previousBlobs = harvestImageBlobs(from: ctx.previousContent)

        let blocks = MarkdownParserEngine.parse(markdown)
        var leafletBlocks: [LeafletBlockContainer] = []

        for block in blocks {
            if let lb = markdownToLeafletBlock(block, previousBlobs: previousBlobs) {
                leafletBlocks.append(LeafletBlockContainer(block: lb))
            }
        }

        let page = LeafletPage(type: "pub.leaflet.pages.linearDocument", blocks: leafletBlocks)
        let content = LeafletContent(pages: [page], blobPages: nil)
        return UnknownType.record(content)
    }

    private func markdownToLeafletBlock(_ block: MarkdownBlockNode, previousBlobs: [String: ComAtprotoLexicon.Repository.UploadBlobOutput]) -> LeafletBlock? {
        switch block {
        case .heading(let level, let text):
            let (plaintext, facets) = FacetConverter.markdownToFacets(text, schema: schema)
            return LeafletBlock(
                type: LeafletTypes.shared.BLOCKS_HEADER, plaintext: plaintext, level: level,
                facets: facets.isEmpty ? nil : facets
            )

        case .paragraph(let text):
            let (plaintext, facets) = FacetConverter.markdownToFacets(text, schema: schema)
            return LeafletBlock(
                type: LeafletTypes.shared.BLOCKS_TEXT, plaintext: plaintext,
                facets: facets.isEmpty ? nil : facets
            )

        case .blockquote(let text):
            let (plaintext, facets) = FacetConverter.markdownToFacets(text, schema: schema)
            return LeafletBlock(
                type: LeafletTypes.shared.BLOCKS_BLOCKQUOTE, plaintext: plaintext,
                facets: facets.isEmpty ? nil : facets
            )

        case .code(let language, let content):
            if language == "math" {
                return LeafletBlock(type: LeafletTypes.shared.BLOCKS_MATH, tex: content)
            }
            return LeafletBlock(
                type: LeafletTypes.shared.BLOCKS_CODE, plaintext: content, language: language
            )

        case .math(let tex):
            return LeafletBlock(type: LeafletTypes.shared.BLOCKS_MATH, tex: tex)

        case .horizontalRule:
            return LeafletBlock(type: LeafletTypes.shared.BLOCKS_HORIZONTAL_RULE)

        case .image(let alt, let url):
            // Match CID against previous content's blobs (standard.horse pattern).
            // A blob CID is a base32 CIDv1 (`bafy…`) or base58 CIDv0 (`Qm…`).
            let isCID = url.hasPrefix("baf") || url.hasPrefix("Qm")
            if isCID, let existingBlob = previousBlobs[url] {
                return LeafletBlock(
                    type: LeafletTypes.shared.BLOCKS_IMAGE, image: existingBlob, alt: alt.isEmpty ? nil : alt
                )
            }
            // External URLs can't be stored in leaflet format.
            return nil

        case .unorderedList(let items):
            let listItems = items.map { markdownToLeafletListItem($0, ordered: false, previousBlobs: previousBlobs) }
            return LeafletBlock(type: LeafletTypes.shared.BLOCKS_UNORDERED_LIST, children: listItems)

        case .orderedList(let start, let items):
            let listItems = items.map { markdownToLeafletListItem($0, ordered: true, previousBlobs: previousBlobs) }
            return LeafletBlock(type: LeafletTypes.shared.BLOCKS_ORDERED_LIST, children: listItems, startIndex: start)

        case .taskList(let items):
            let listItems = items.map { markdownToLeafletListItem($0, ordered: false, previousBlobs: previousBlobs) }
            return LeafletBlock(type: LeafletTypes.shared.BLOCKS_UNORDERED_LIST, children: listItems)
        }
    }

    /// Builds a leaflet list item, matching standard.horse's `listItemBlock`.
    /// Nested ordered lists become `orderedListChildren`, unordered become
    /// `children`, and task items carry their `checked` flag. The item content
    /// defaults to an empty text block when no text is provided.
    private func markdownToLeafletListItem(_ item: MarkdownListItemNode, ordered: Bool, previousBlobs: [String: ComAtprotoLexicon.Repository.UploadBlobOutput]) -> LeafletListItem {
        let itemType = ordered
            ? "pub.leaflet.blocks.orderedList#listItem"
            : "pub.leaflet.blocks.unorderedList#listItem"
        let (plaintext, facets) = FacetConverter.markdownToFacets(item.text, schema: schema)
        let content = LeafletBlock(
            type: LeafletTypes.shared.BLOCKS_TEXT, plaintext: plaintext,
            facets: facets.isEmpty ? nil : facets
        )
        let children = item.children?.map { markdownToLeafletListItem($0, ordered: false, previousBlobs: previousBlobs) }
        return LeafletListItem(
            type: itemType, content: content, checked: item.checked,
            children: children
        )
    }
}
