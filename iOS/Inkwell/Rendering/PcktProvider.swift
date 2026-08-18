//
//  PcktProvider.swift
//  Inkwell
//

import Foundation
import InkwellShared
import ATProtoKit

// MARK: - Pckt Provider

/// Pckt provider (`blog.pckt.content`). Pckt stores an `items` array of blocks.
/// Blocks map closely to markdown; inline formatting uses pckt's richtext facets.
struct PcktProvider: ContentProvider {
    let id = "pckt"
    let label = "pckt"
    let contentType = "blog.pckt.content"
    let supportsImages = true

    private let schema = FacetSchema.pckt
    private let b: (String) -> String = { "blog.pckt.block.\($0)" }

    /// Blocks that can't be represented as markdown, matching
    /// standard.horse's LOSS_LABELS.
    private let lossLabels: [String: String] = [
        "blog.pckt.block.table": "tables",
        "blog.pckt.block.mention": "mention blocks",
        "blog.pckt.block.gallery": "galleries",
        "blog.pckt.block.iframe": "embeds",
        "blog.pckt.block.website": "website cards",
        "blog.pckt.block.blueskyEmbed": "Bluesky posts",
        "blog.pckt.block.noteEmbed": "note embeds",
        "blog.pckt.block.hardBreak": "hard breaks",
    ]
    // NOTE: Loss labels are also defined in shared KMP BlockLossLabels.pckt.

    func matches(_ content: UnknownType?) -> Bool {
        content?.getRecord(ofType: PcktContent.self) != nil
    }

    func toMarkdown(_ content: UnknownType?) -> ConvertResult {
        guard let pckt = content?.getRecord(ofType: PcktContent.self) else {
            return ConvertResult(markdown: "", lost: [])
        }

        var lost = Set<String>()
        var blocks: [MarkdownBlockNode] = []

        for block in pckt.items ?? [] {
            if let mdBlock = pcktBlockToMarkdown(block, lost: &lost) {
                blocks.append(mdBlock)
            }
        }

        return ConvertResult(markdown: MarkdownSerializerEngine.serialize(blocks), lost: Array(lost))
    }

    private func pcktBlockToMarkdown(_ block: PcktBlock, lost: inout Set<String>) -> MarkdownBlockNode? {
        switch block.type {
        case LeafletTypes.shared.BLOCKS_TEXT:
            let text = FacetConverter.facetsToMarkdown(
                block.plaintext ?? "", facets: block.facets, schema: schema
            )
            return text.isEmpty ? nil : .paragraph(text: text)

        case b("heading"):
            let level = max(1, min(6, block.level ?? 1))
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
            return .code(language: block.language, content: block.plaintext ?? "")

        case LeafletTypes.shared.BLOCKS_HORIZONTAL_RULE:
            return .horizontalRule

        case b("hardBreak"):
            return nil  // paragraph breaks already separate blocks

        case LeafletTypes.shared.BLOCKS_IMAGE:
            let attrs = block.attrs
            let url = attrs?.blob?.reference.link ?? attrs?.src ?? ""
            return .image(alt: attrs?.alt ?? "", url: url)

        case b("bulletList"):
            let items = (block.listContent ?? []).map { pcktListItemToMarkdown($0) }
            return .unorderedList(items: items)

        case LeafletTypes.shared.BLOCKS_ORDERED_LIST:
            let start = block.start ?? 1
            let items = (block.listContent ?? []).map { pcktListItemToMarkdown($0) }
            return .orderedList(start: start, items: items)

        case b("taskList"):
            let items = (block.listContent ?? []).map { pcktListItemToMarkdown($0) }
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

    private func pcktListItemToMarkdown(_ item: PcktListItem) -> MarkdownListItemNode {
        var text = ""
        var children: [MarkdownListItemNode]? = nil
        for block in item.content ?? [] {
            switch block.type {
            case LeafletTypes.shared.BLOCKS_TEXT:
                text += FacetConverter.facetsToMarkdown(
                    block.plaintext ?? "", facets: block.facets, schema: schema
                )
            case b("bulletList"), LeafletTypes.shared.BLOCKS_ORDERED_LIST:
                // A nested sub-list lives as another entry in this item's
                // `content` array, alongside its text block — see
                // standard.horse's `pckt.ts` `listItemToMdast`.
                children = (block.listContent ?? []).map { pcktListItemToMarkdown($0) }
            default:
                break
            }
        }

        return MarkdownListItemNode(text: text, checked: item.checked, children: children)
    }

    func fromMarkdown(_ markdown: String, ctx: WriteContext) -> UnknownType? {
        let blocks = MarkdownParserEngine.parse(markdown)
        var items: [PcktBlock] = []

        for block in blocks {
            if let pb = markdownToPcktBlock(block) {
                items.append(pb)
            }
        }

        let content = PcktContent(items: items)
        return UnknownType.record(content)
    }

    private func markdownToPcktBlock(_ block: MarkdownBlockNode) -> PcktBlock? {
        switch block {
        case .heading(let level, let text):
            let (plaintext, facets) = FacetConverter.markdownToFacets(text, schema: schema)
            return PcktBlock(
                type: b("heading"), plaintext: plaintext, level: level,
                facets: facets.isEmpty ? nil : facets
            )

        case .paragraph(let text):
            let (plaintext, facets) = FacetConverter.markdownToFacets(text, schema: schema)
            return PcktBlock(
                type: LeafletTypes.shared.BLOCKS_TEXT, plaintext: plaintext,
                facets: facets.isEmpty ? nil : facets
            )

        case .blockquote(let text):
            let (plaintext, facets) = FacetConverter.markdownToFacets(text, schema: schema)
            return PcktBlock(
                type: LeafletTypes.shared.BLOCKS_BLOCKQUOTE,
                content: [PcktBlock(type: LeafletTypes.shared.BLOCKS_TEXT, plaintext: plaintext,
                                     facets: facets.isEmpty ? nil : facets)]
            )

        case .code(let language, let content):
            return PcktBlock(
                type: b("codeBlock"), plaintext: content, language: language
            )

        case .math:
            // Pckt doesn't have a math block; store as code
            return nil

        case .horizontalRule:
            return PcktBlock(type: LeafletTypes.shared.BLOCKS_HORIZONTAL_RULE)

        case .image(let alt, let url):
            // Pckt allows a plain URL src (unlike leaflet)
            return PcktBlock(
                type: LeafletTypes.shared.BLOCKS_IMAGE,
                attrs: PcktBlockAttrs(src: url, alt: alt)
            )

        case .unorderedList(let items):
            let listItems = items.map { markdownToPcktListItem($0, isTaskItem: false) }
            return PcktBlock(type: b("bulletList"), listContent: listItems)

        case .orderedList(let start, let items):
            let listItems = items.map { markdownToPcktListItem($0, isTaskItem: false) }
            return PcktBlock(type: LeafletTypes.shared.BLOCKS_ORDERED_LIST, listContent: listItems, start: start)

        case .taskList(let items):
            let listItems = items.map { markdownToPcktListItem($0, isTaskItem: true) }
            return PcktBlock(type: b("taskList"), listContent: listItems)
        }
    }

    private func markdownToPcktListItem(_ item: MarkdownListItemNode, isTaskItem: Bool) -> PcktListItem {
        let (plaintext, facets) = FacetConverter.markdownToFacets(item.text, schema: schema)
        var content: [PcktBlock] = [
            PcktBlock(type: LeafletTypes.shared.BLOCKS_TEXT, plaintext: plaintext, facets: facets.isEmpty ? nil : facets)
        ]
        if let kids = item.children, !kids.isEmpty {
            // Nested sub-list: another entry in this item's `content` array,
            // alongside its text block — see standard.horse's `pckt.ts`
            // `itemBlock`.
            let nested = kids.map { markdownToPcktListItem($0, isTaskItem: false) }
            content.append(PcktBlock(type: b("bulletList"), listContent: nested))
        }
        let itemType = isTaskItem ? b("taskItem") : b("listItem")
        return PcktListItem(type: itemType, content: content, checked: isTaskItem ? (item.checked ?? false) : nil)
    }
}
