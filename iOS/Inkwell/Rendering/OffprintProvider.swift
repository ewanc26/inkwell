//
//  OffprintProvider.swift
//  Inkwell
//

import Foundation
import InkwellShared
import ATProtoKit

// MARK: - Offprint Provider

/// Offprint provider (`app.offprint.content`). Offprint stores an `items`
/// array of blocks. Blocks map closely to markdown; inline formatting uses
/// offprint's richtext facets.
struct OffprintProvider: ContentProvider {
    let id = "offprint"
    let label = "Offprint"
    let contentType = "app.offprint.content"
    let supportsImages = true

    private let schema = FacetSchema.offprint
    private let b: (String) -> String = { "app.offprint.block.\($0)" }

    /// Blocks that can't be represented as markdown, matching
    /// standard.horse's LOSS_LABELS.
    private let lossLabels: [String: String] = [
        "app.offprint.block.callout": "callouts",
        "app.offprint.block.button": "buttons",
        "app.offprint.block.webBookmark": "bookmarks",
        "app.offprint.block.webEmbed": "embeds",
        "app.offprint.block.blueskyPost": "Bluesky posts",
        "app.offprint.block.imageGrid": "image grids",
        "app.offprint.block.imageCarousel": "image carousels",
        "app.offprint.block.imageDiff": "image comparisons",
    ]
    // NOTE: Loss labels are also defined in shared KMP BlockLossLabels.offprint.

    func matches(_ content: UnknownType?) -> Bool {
        content?.getRecord(ofType: OffprintContent.self) != nil
    }

    func toMarkdown(_ content: UnknownType?) -> ConvertResult {
        guard let offprint = content?.getRecord(ofType: OffprintContent.self) else {
            return ConvertResult(markdown: "", lost: [])
        }

        var lost = Set<String>()
        var blocks: [MarkdownBlockNode] = []

        for block in offprint.items ?? [] {
            if let mdBlock = offprintBlockToMarkdown(block, lost: &lost) {
                blocks.append(mdBlock)
            }
        }

        return ConvertResult(markdown: MarkdownSerializerEngine.serialize(blocks), lost: Array(lost))
    }

    private func offprintBlockToMarkdown(_ block: OffprintBlock, lost: inout Set<String>) -> MarkdownBlockNode? {
        switch block.type {
        case LeafletTypes.shared.BLOCKS_TEXT:
            let text = FacetConverter.facetsToMarkdown(
                block.plaintext ?? "", facets: block.facets, schema: schema
            )
            return text.isEmpty ? nil : .paragraph(text: text)

        case b("heading"):
            let level = max(1, min(3, block.level ?? 1))
            let text = FacetConverter.facetsToMarkdown(
                block.plaintext ?? "", facets: block.facets, schema: schema
            )
            return .heading(level: level, text: text)

        case LeafletTypes.shared.BLOCKS_BLOCKQUOTE:
            let inner = block.content ?? []
            let text = inner.map { block in
                FacetConverter.facetsToMarkdown(
                    block.plaintext ?? "", facets: block.facets, schema: schema
                )
            }.joined(separator: "\n")
            return .blockquote(text: text)

        case b("codeBlock"):
            return .code(language: block.language, content: block.code ?? "")

        case b("mathBlock"):
            return .math(tex: block.plaintext ?? "")

        case LeafletTypes.shared.BLOCKS_HORIZONTAL_RULE:
            return .horizontalRule

        case LeafletTypes.shared.BLOCKS_IMAGE:
            let cid = block.image?.reference.link ?? ""
            return .image(alt: block.alt ?? "", url: cid)

        case b("bulletList"):
            let items = (block.children ?? []).map { offprintListItemToMarkdown($0) }
            return .unorderedList(items: items)

        case LeafletTypes.shared.BLOCKS_ORDERED_LIST:
            let start = block.start ?? 1
            let items = (block.children ?? []).map { offprintListItemToMarkdown($0) }
            return .orderedList(start: start, items: items)

        case b("taskList"):
            let items = (block.children ?? []).map { offprintListItemToMarkdown($0) }
            return .taskList(items: items)

        default:
            if let label = lossLabels[block.type] {
                lost.insert(label)
            } else {
                lost.insert("an unsupported block")
            }
            return nil
        }
    }

    private func offprintListItemToMarkdown(_ item: OffprintListItem) -> MarkdownListItemNode {
        var text = ""
        if let content = item.content, content.type == LeafletTypes.shared.BLOCKS_TEXT {
            text = FacetConverter.facetsToMarkdown(
                content.plaintext ?? "", facets: content.facets, schema: schema
            )
        }

        var children: [MarkdownListItemNode]? = nil
        if let kids = item.children, !kids.isEmpty {
            children = kids.map { offprintListItemToMarkdown($0) }
        }

        return MarkdownListItemNode(text: text, checked: item.checked, children: children)
    }

    func fromMarkdown(_ markdown: String, ctx: WriteContext) -> UnknownType? {
        let blocks = MarkdownParserEngine.parse(markdown)
        var items: [OffprintBlock] = []

        for block in blocks {
            if let ob = markdownToOffprintBlock(block) {
                items.append(ob)
            }
        }

        let content = OffprintContent(items: items)
        return UnknownType.record(content)
    }

    private func markdownToOffprintBlock(_ block: MarkdownBlockNode) -> OffprintBlock? {
        switch block {
        case .heading(let level, let text):
            let (plaintext, facets) = FacetConverter.markdownToFacets(text, schema: schema)
            // Offprint headings are levels 1-3
            return OffprintBlock(
                type: b("heading"), plaintext: plaintext, level: min(level, 3),
                facets: facets.isEmpty ? nil : facets
            )

        case .paragraph(let text):
            let (plaintext, facets) = FacetConverter.markdownToFacets(text, schema: schema)
            return OffprintBlock(
                type: LeafletTypes.shared.BLOCKS_TEXT, plaintext: plaintext,
                facets: facets.isEmpty ? nil : facets
            )

        case .blockquote(let text):
            let (plaintext, facets) = FacetConverter.markdownToFacets(text, schema: schema)
            return OffprintBlock(
                type: LeafletTypes.shared.BLOCKS_BLOCKQUOTE,
                content: [OffprintBlock(type: LeafletTypes.shared.BLOCKS_TEXT, plaintext: plaintext,
                                        facets: facets.isEmpty ? nil : facets)]
            )

        case .code(let language, let content):
            return OffprintBlock(
                type: b("codeBlock"), code: content, language: language
            )

        case .math(let tex):
            return OffprintBlock(type: b("mathBlock"), plaintext: tex)

        case .horizontalRule:
            return OffprintBlock(type: LeafletTypes.shared.BLOCKS_HORIZONTAL_RULE)

        case .image:
            // Offprint images are blob-only; external URLs can't be stored
            return nil

        case .unorderedList(let items):
            let listItems = items.map { markdownToOffprintListItem($0, ordered: false) }
            return OffprintBlock(type: b("bulletList"), children: listItems)

        case .orderedList(let start, let items):
            let listItems = items.map { markdownToOffprintListItem($0, ordered: true) }
            return OffprintBlock(type: LeafletTypes.shared.BLOCKS_ORDERED_LIST, children: listItems, start: start)

        case .taskList(let items):
            let listItems = items.map { markdownToOffprintListItem($0, ordered: false) }
            return OffprintBlock(type: b("taskList"), children: listItems)
        }
    }

    private func markdownToOffprintListItem(_ item: MarkdownListItemNode, ordered: Bool) -> OffprintListItem {
        let (plaintext, facets) = FacetConverter.markdownToFacets(item.text, schema: schema)
        let textBlock = OffprintBlock(
            type: LeafletTypes.shared.BLOCKS_TEXT, plaintext: plaintext,
            facets: facets.isEmpty ? nil : facets
        )
        let children = item.children?.map { markdownToOffprintListItem($0, ordered: ordered) }
        return OffprintListItem(
            content: textBlock, checked: item.checked, children: children
        )
    }
}
