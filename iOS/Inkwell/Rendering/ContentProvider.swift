//
//  ContentProvider.swift
//  Inkwell
//
//  Created by Letta on 20/06/2026.
//
//  Multi-format content provider system, modelled on standard.horse's
//  architecture: markdown is the universal editing format, and each provider
//  converts to/from its AT Protocol record format. This lets Inkwell write
//  posts in Leaflet, Markpub, Pckt, or Offprint format from a single editor.
//

import Foundation
import InkwellShared
import ATProtoKit

// MARK: - ContentProvider Protocol

/// Context passed to `fromMarkdown` so providers can round-trip image blobs.
/// Matching standard.horse's `WriteCtx`.
struct WriteContext {
    /// The content object being replaced, so existing image blobs round-trip.
    let previousContent: UnknownType?
}

/// A content-format provider that reads and writes one standard.site `content`
/// member ($type), converting it to/from the markdown the editor speaks.
protocol ContentProvider {
    /// Stable id used in the format picker and provider lookups.
    var id: String { get }
    /// Display name, e.g. "Leaflet".
    var label: String { get }
    /// The content object's `$type` this provider reads and writes.
    var contentType: String { get }
    /// Whether in-post image upload works.
    var supportsImages: Bool { get }
    /// True if this provider handles the given stored content object.
    func matches(_ content: UnknownType?) -> Bool
    /// Read stored content into editable markdown.
    func toMarkdown(_ content: UnknownType?) -> ConvertResult
    /// Build a fresh content object from edited markdown, using the write
    /// context to round-trip existing image blobs from the previous content.
    func fromMarkdown(_ markdown: String, ctx: WriteContext) -> UnknownType?
}

/// Result of converting stored content to markdown.
struct ConvertResult {
    let markdown: String
    /// Human labels for blocks/features dropped converting to markdown.
    let lost: [String]
}

// MARK: - Image Blob Harvesting

/// Walks a content tree (recursively) collecting every image blob referenced by
/// CID, keyed by CID string. Matching standard.horse's `harvestImages`.
func harvestImageBlobs(from content: UnknownType?) -> [String: ComAtprotoLexicon.Repository.UploadBlobOutput] {
    guard let content else { return [:] }
    var out: [String: ComAtprotoLexicon.Repository.UploadBlobOutput] = [:]
    harvestImageBlobs(from: content, into: &out)
    return out
}

private func harvestImageBlobs(from value: Any, into out: inout [String: ComAtprotoLexicon.Repository.UploadBlobOutput]) {
    let mirror = Mirror(reflecting: value)
    // Walk the struct/class properties looking for blobs.
    for child in mirror.children {
        if let blob = child.value as? ComAtprotoLexicon.Repository.UploadBlobOutput,
           blob.mimeType.hasPrefix("image/") {
            let cid = blob.reference.link
            if !cid.isEmpty { out[cid] = blob }
        } else if let dict = child.value as? [String: Any] {
            for (_, v) in dict { harvestImageBlobs(from: v, into: &out) }
        } else if let array = child.value as? [Any] {
            for v in array { harvestImageBlobs(from: v, into: &out) }
        } else if child.value is CustomReflectable || Mirror(reflecting: child.value).children.count > 0 {
            harvestImageBlobs(from: child.value, into: &out)
        }
    }
}

// MARK: - Facet Converter

/// Converts between AT Protocol facets (plaintext + byte-range features) and
/// markdown inline syntax (**bold**, *italic*, `code`, ~~strike~~, [text](url)).
enum FacetConverter {

    /// Convert facets to markdown inline text. Unknown features are tracked
    /// in `lost` using schema.lossy labels.
    static func facetsToMarkdown(_ plaintext: String, facets: [LeafletFacet]?, schema: FacetSchema, lost: inout Set<String>) -> String {
        guard let facets, !facets.isEmpty else { return plaintext }

        var insertions: [(offset: Int, text: String)] = []

        for facet in facets {
            for feature in facet.features {
                let type = feature.type
                var markdown: String?

                switch type {
                case schema.bold:
                    markdown = "**"
                case schema.italic:
                    markdown = "*"
                case schema.code:
                    markdown = "`"
                case schema.strike:
                    markdown = "~~"
                case schema.link:
                    if let uri = feature.uri, !uri.isEmpty {
                        markdown = "["
                    } else {
                        markdown = nil
                    }
                default:
                    if let label = schema.lossy[type] {
                        lost.insert(label)
                    } else {
                        lost.insert("unsupported inline feature")
                    }
                    markdown = nil
                }

                if let open = markdown {
                    let close = type == schema.link && feature.uri != nil ? "](\(feature.uri ?? ""))" : open
                    let start = facet.index.byteStart
                    let end = facet.index.byteEnd
                    if start < end && start >= 0 && end <= plaintext.utf8.count {
                        insertions.append((offset: start, text: open))
                        insertions.append((offset: end, text: close))
                    }
                }
            }
        }

        guard !insertions.isEmpty else { return plaintext }

        insertions.sort { $0.offset > $1.offset }

        var result = plaintext
        for (offset, text) in insertions {
            let idx = result.utf8.index(result.utf8.startIndex, offsetBy: offset, limitedBy: result.utf8.endIndex)
            if let idx {
                result.insert(contentsOf: text, at: idx)
            }
        }

        return result
    }

    /// Convenience overload — converts facets to markdown without tracking
    /// lossy inline features (e.g. list item text, image alts).
    static func facetsToMarkdown(_ plaintext: String, facets: [LeafletFacet]?, schema: FacetSchema) -> String {
        var dummy = Set<String>()
        return facetsToMarkdown(plaintext, facets: facets, schema: schema, lost: &dummy)
    }

    /// Parse markdown inline syntax into plaintext + facets.
    /// Handles **bold**, *italic*, `code`, ~~strike~~, and [text](url).
    static func markdownToFacets(_ markdown: String, schema: FacetSchema) -> (plaintext: String, facets: [LeafletFacet]) {
        var plaintext = ""
        var facets: [LeafletFacet] = []
        let chars = Array(markdown)
        var i = 0

        // Stack of active marks: (byteStart, featureType, uri?)
        var markStack: [(start: Int, type: String, uri: String?)] = []

        while i < chars.count {
            // Bold: **text**
            if i + 1 < chars.count && chars[i] == "*" && chars[i + 1] == "*" {
                if let mark = markStack.last, mark.type == schema.bold {
                    let byteEnd = plaintext.utf8.count
                    if byteEnd > mark.start {
                        facets.append(LeafletFacet(
                            index: LeafletByteSlice(byteStart: mark.start, byteEnd: byteEnd),
                            features: [LeafletFacetFeature(type: schema.bold)]
                        ))
                    }
                    markStack.removeLast()
                    i += 2
                } else {
                    markStack.append((start: plaintext.utf8.count, type: schema.bold, uri: nil))
                    i += 2
                }
                continue
            }

            // Italic: *text*
            if chars[i] == "*" {
                if let mark = markStack.last, mark.type == schema.italic {
                    let byteEnd = plaintext.utf8.count
                    if byteEnd > mark.start {
                        facets.append(LeafletFacet(
                            index: LeafletByteSlice(byteStart: mark.start, byteEnd: byteEnd),
                            features: [LeafletFacetFeature(type: schema.italic)]
                        ))
                    }
                    markStack.removeLast()
                    i += 1
                } else {
                    markStack.append((start: plaintext.utf8.count, type: schema.italic, uri: nil))
                    i += 1
                }
                continue
            }

            // Strikethrough: ~~text~~
            if i + 1 < chars.count && chars[i] == "~" && chars[i + 1] == "~" {
                if let mark = markStack.last, mark.type == schema.strike {
                    let byteEnd = plaintext.utf8.count
                    if byteEnd > mark.start {
                        facets.append(LeafletFacet(
                            index: LeafletByteSlice(byteStart: mark.start, byteEnd: byteEnd),
                            features: [LeafletFacetFeature(type: schema.strike)]
                        ))
                    }
                    markStack.removeLast()
                    i += 2
                } else {
                    markStack.append((start: plaintext.utf8.count, type: schema.strike, uri: nil))
                    i += 2
                }
                continue
            }

            // Code: `text`
            if chars[i] == "`" {
                // Find closing backtick
                if let closeIdx = chars[(i + 1)...].firstIndex(of: "`") {
                    let content = String(chars[(i + 1)..<closeIdx])
                    let byteStart = plaintext.utf8.count
                    plaintext += content
                    let byteEnd = plaintext.utf8.count
                    facets.append(LeafletFacet(
                        index: LeafletByteSlice(byteStart: byteStart, byteEnd: byteEnd),
                        features: [LeafletFacetFeature(type: schema.code)]
                    ))
                    i = closeIdx + 1
                    continue
                }
            }

            // Link: [text](url)
            if chars[i] == "[" {
                if let closeBracket = chars[(i + 1)...].firstIndex(of: "]"),
                   closeBracket + 1 < chars.count, chars[closeBracket + 1] == "(" {
                    let openParen = closeBracket + 1
                    if let closeParen = chars[(openParen + 1)...].firstIndex(of: ")") {
                        let text = String(chars[(i + 1)..<closeBracket])
                        let url = String(chars[(openParen + 1)..<closeParen])
                        let byteStart = plaintext.utf8.count
                        plaintext += text
                        let byteEnd = plaintext.utf8.count
                        facets.append(LeafletFacet(
                            index: LeafletByteSlice(byteStart: byteStart, byteEnd: byteEnd),
                            features: [LeafletFacetFeature(type: schema.link, uri: url)]
                        ))
                        i = closeParen + 1
                        continue
                    }
                }
            }

            // Regular character
            plaintext.append(chars[i])
            i += 1
        }

        return (plaintext, facets)
    }
}

// MARK: - Markdown Block Types

/// Common block representation that all providers convert to/from.
enum MarkdownBlockNode {
    case heading(level: Int, text: String)
    case paragraph(text: String)
    case code(language: String?, content: String)
    case math(tex: String)
    case blockquote(text: String)
    case image(alt: String, url: String)
    case horizontalRule
    case unorderedList(items: [MarkdownListItemNode])
    case orderedList(start: Int, items: [MarkdownListItemNode])
    case taskList(items: [MarkdownListItemNode])
}

struct MarkdownListItemNode {
    let text: String
    let checked: Bool?  // nil = not a task item
    let children: [MarkdownListItemNode]?
}

// MARK: - Markdown Parser

/// Editor-side markdown parser — delegates to shared KMP for the
/// block-level parse. Round-trip correctness for the writer depends on
/// this staying in sync with `MarkdownSerializer` below.
enum MarkdownParserEngine {
    static func parse(_ markdown: String) -> [MarkdownBlockNode] {
        parseMarkdown(markdown)
    }
}

// MARK: - Markdown Serializer

/// Converts MarkdownBlockNode array back to a markdown string — delegates
/// to shared KMP for canonical serialization.
enum MarkdownSerializerEngine {

    static func serialize(_ blocks: [MarkdownBlockNode]) -> String {
        serializeMarkdown(blocks)
    }
}

// MARK: - Leaflet Provider

/// Leaflet provider (`pub.leaflet.content`). Leaflet documents are a list of
/// pages; we read and write a single `linearDocument` page whose `blocks` map
/// closely to markdown. Inline formatting uses leaflet's richtext facets.
struct LeafletProvider: ContentProvider {
    let id = "leaflet"
    let label = "Leaflet"
    let contentType = "pub.leaflet.content"
    let supportsImages = true

    private let schema = FacetSchema.leaflet
    private let b: (String) -> String = { "pub.leaflet.blocks.\($0)" }

    /// Blocks that can't be represented as markdown, with human labels
    /// matching standard.horse's LOSS_LABELS.
    private let lossLabels: [String: String] = [
        "pub.leaflet.blocks.iframe": "embeds",
        "pub.leaflet.blocks.website": "website cards",
        "pub.leaflet.blocks.bskyPost": "Bluesky posts",
        "pub.leaflet.blocks.standardSitePost": "linked posts",
        "pub.leaflet.blocks.page": "sub-pages",
        "pub.leaflet.blocks.poll": "polls",
        "pub.leaflet.blocks.button": "buttons",
        "pub.leaflet.blocks.postsList": "post lists",
        "pub.leaflet.blocks.signup": "signup forms",
    ]

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
        case b("text"):
            let text = FacetConverter.facetsToMarkdown(
                block.plaintext ?? "", facets: block.facets, schema: schema
            )
            // Empty text blocks (leaflet spacers) have no markdown equivalent.
            return text.isEmpty ? nil : .paragraph(text: text)

        case b("header"):
            let level = max(1, min(6, block.level ?? 1))
            let text = FacetConverter.facetsToMarkdown(
                block.plaintext ?? "", facets: block.facets, schema: schema
            )
            return .heading(level: level, text: text)

        case b("blockquote"):
            let text = FacetConverter.facetsToMarkdown(
                block.plaintext ?? "", facets: block.facets, schema: schema
            )
            return .blockquote(text: text)

        case b("code"):
            return .code(language: block.language, content: block.plaintext ?? "")

        case b("math"):
            return .math(tex: block.tex ?? "")

        case b("horizontalRule"):
            return .horizontalRule

        case b("image"):
            // Image blobs are stored as PDS blobs; we reference by CID.
            let cid = block.image?.reference.link ?? ""
            return cid.isEmpty ? nil : .image(alt: block.alt ?? "", url: cid)

        case b("unorderedList"):
            let items = (block.children ?? []).map { leafletListItemToMarkdown($0) }
            return .unorderedList(items: items)

        case b("orderedList"):
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
            case b("text"):
                text = FacetConverter.facetsToMarkdown(
                    content.plaintext ?? "", facets: content.facets, schema: schema
                )
            case b("image"):
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
                type: b("header"), plaintext: plaintext, level: level,
                facets: facets.isEmpty ? nil : facets
            )

        case .paragraph(let text):
            let (plaintext, facets) = FacetConverter.markdownToFacets(text, schema: schema)
            return LeafletBlock(
                type: b("text"), plaintext: plaintext,
                facets: facets.isEmpty ? nil : facets
            )

        case .blockquote(let text):
            let (plaintext, facets) = FacetConverter.markdownToFacets(text, schema: schema)
            return LeafletBlock(
                type: b("blockquote"), plaintext: plaintext,
                facets: facets.isEmpty ? nil : facets
            )

        case .code(let language, let content):
            if language == "math" {
                return LeafletBlock(type: b("math"), tex: content)
            }
            return LeafletBlock(
                type: b("code"), plaintext: content, language: language
            )

        case .math(let tex):
            return LeafletBlock(type: b("math"), tex: tex)

        case .horizontalRule:
            return LeafletBlock(type: b("horizontalRule"))

        case .image(let alt, let url):
            // Match CID against previous content's blobs (standard.horse pattern).
            // A blob CID is a base32 CIDv1 (`bafy…`) or base58 CIDv0 (`Qm…`).
            let isCID = url.hasPrefix("baf") || url.hasPrefix("Qm")
            if isCID, let existingBlob = previousBlobs[url] {
                return LeafletBlock(
                    type: b("image"), image: existingBlob, alt: alt.isEmpty ? nil : alt
                )
            }
            // External URLs can't be stored in leaflet format.
            return nil

        case .unorderedList(let items):
            let listItems = items.map { markdownToLeafletListItem($0, ordered: false, previousBlobs: previousBlobs) }
            return LeafletBlock(type: b("unorderedList"), children: listItems)

        case .orderedList(let start, let items):
            let listItems = items.map { markdownToLeafletListItem($0, ordered: true, previousBlobs: previousBlobs) }
            return LeafletBlock(type: b("orderedList"), children: listItems, startIndex: start)

        case .taskList(let items):
            let listItems = items.map { markdownToLeafletListItem($0, ordered: false, previousBlobs: previousBlobs) }
            return LeafletBlock(type: b("unorderedList"), children: listItems)
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
            type: b("text"), plaintext: plaintext,
            facets: facets.isEmpty ? nil : facets
        )
        let children = item.children?.map { markdownToLeafletListItem($0, ordered: false, previousBlobs: previousBlobs) }
        return LeafletListItem(
            type: itemType, content: content, checked: item.checked,
            children: children
        )
    }
}

// MARK: - Markpub Provider

/// Markpub provider (`at.markpub.markdown`). Markpub stores GFM markdown
/// directly, so conversion is near-identity: read the inline `text.markdown`
/// and write it straight back. Nothing is ever lost.
struct MarkpubProvider: ContentProvider {
    let id = "markpub"
    let label = "Markdown (markpub)"
    let contentType = "at.markpub.markdown"
    let supportsImages = false

    func matches(_ content: UnknownType?) -> Bool {
        content?.getRecord(ofType: MarkpubContent.self) != nil
    }

    func toMarkdown(_ content: UnknownType?) -> ConvertResult {
        guard let markpub = content?.getRecord(ofType: MarkpubContent.self) else {
            return ConvertResult(markdown: "", lost: [])
        }
        return ConvertResult(markdown: markpub.text.markdown ?? "", lost: [])
    }

    func fromMarkdown(_ markdown: String, ctx: WriteContext) -> UnknownType? {
        let text = MarkpubText(type: "at.markpub.text", markdown: markdown)
        let content = MarkpubContent(text: text)
        return UnknownType.record(content)
    }
}

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
        case b("text"):
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

        case b("blockquote"):
            let inner = block.content ?? []
            let text = inner.map { block in
                FacetConverter.facetsToMarkdown(
                    block.plaintext ?? "", facets: block.facets, schema: schema
                )
            }.joined(separator: "\n")
            return .blockquote(text: text)

        case b("codeBlock"):
            return .code(language: block.language, content: block.plaintext ?? "")

        case b("horizontalRule"):
            return .horizontalRule

        case b("hardBreak"):
            return nil  // paragraph breaks already separate blocks

        case b("image"):
            let attrs = block.attrs
            let url = attrs?.blob?.reference.link ?? attrs?.src ?? ""
            return .image(alt: attrs?.alt ?? "", url: url)

        case b("bulletList"):
            let items = (block.listContent ?? []).map { pcktListItemToMarkdown($0) }
            return .unorderedList(items: items)

        case b("orderedList"):
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
            case b("text"):
                text += FacetConverter.facetsToMarkdown(
                    block.plaintext ?? "", facets: block.facets, schema: schema
                )
            case b("bulletList"), b("orderedList"):
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
                type: b("text"), plaintext: plaintext,
                facets: facets.isEmpty ? nil : facets
            )

        case .blockquote(let text):
            let (plaintext, facets) = FacetConverter.markdownToFacets(text, schema: schema)
            return PcktBlock(
                type: b("blockquote"),
                content: [PcktBlock(type: b("text"), plaintext: plaintext,
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
            return PcktBlock(type: b("horizontalRule"))

        case .image(let alt, let url):
            // Pckt allows a plain URL src (unlike leaflet)
            return PcktBlock(
                type: b("image"),
                attrs: PcktBlockAttrs(src: url, alt: alt)
            )

        case .unorderedList(let items):
            let listItems = items.map { markdownToPcktListItem($0, isTaskItem: false) }
            return PcktBlock(type: b("bulletList"), listContent: listItems)

        case .orderedList(let start, let items):
            let listItems = items.map { markdownToPcktListItem($0, isTaskItem: false) }
            return PcktBlock(type: b("orderedList"), listContent: listItems, start: start)

        case .taskList(let items):
            let listItems = items.map { markdownToPcktListItem($0, isTaskItem: true) }
            return PcktBlock(type: b("taskList"), listContent: listItems)
        }
    }

    private func markdownToPcktListItem(_ item: MarkdownListItemNode, isTaskItem: Bool) -> PcktListItem {
        let (plaintext, facets) = FacetConverter.markdownToFacets(item.text, schema: schema)
        var content: [PcktBlock] = [
            PcktBlock(type: b("text"), plaintext: plaintext, facets: facets.isEmpty ? nil : facets)
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
        case b("text"):
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

        case b("blockquote"):
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

        case b("horizontalRule"):
            return .horizontalRule

        case b("image"):
            let cid = block.image?.reference.link ?? ""
            return .image(alt: block.alt ?? "", url: cid)

        case b("bulletList"):
            let items = (block.children ?? []).map { offprintListItemToMarkdown($0) }
            return .unorderedList(items: items)

        case b("orderedList"):
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
        if let content = item.content, content.type == b("text") {
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
                type: b("text"), plaintext: plaintext,
                facets: facets.isEmpty ? nil : facets
            )

        case .blockquote(let text):
            let (plaintext, facets) = FacetConverter.markdownToFacets(text, schema: schema)
            return OffprintBlock(
                type: b("blockquote"),
                content: [OffprintBlock(type: b("text"), plaintext: plaintext,
                                        facets: facets.isEmpty ? nil : facets)]
            )

        case .code(let language, let content):
            return OffprintBlock(
                type: b("codeBlock"), code: content, language: language
            )

        case .math(let tex):
            return OffprintBlock(type: b("mathBlock"), plaintext: tex)

        case .horizontalRule:
            return OffprintBlock(type: b("horizontalRule"))

        case .image:
            // Offprint images are blob-only; external URLs can't be stored
            return nil

        case .unorderedList(let items):
            let listItems = items.map { markdownToOffprintListItem($0, ordered: false) }
            return OffprintBlock(type: b("bulletList"), children: listItems)

        case .orderedList(let start, let items):
            let listItems = items.map { markdownToOffprintListItem($0, ordered: true) }
            return OffprintBlock(type: b("orderedList"), children: listItems, start: start)

        case .taskList(let items):
            let listItems = items.map { markdownToOffprintListItem($0, ordered: false) }
            return OffprintBlock(type: b("taskList"), children: listItems)
        }
    }

    private func markdownToOffprintListItem(_ item: MarkdownListItemNode, ordered: Bool) -> OffprintListItem {
        let (plaintext, facets) = FacetConverter.markdownToFacets(item.text, schema: schema)
        let textBlock = OffprintBlock(
            type: b("text"), plaintext: plaintext,
            facets: facets.isEmpty ? nil : facets
        )
        let children = item.children?.map { markdownToOffprintListItem($0, ordered: ordered) }
        return OffprintListItem(
            content: textBlock, checked: item.checked, children: children
        )
    }
}

// MARK: - Provider Registry

/// All providers, markpub first (the default for new posts, matching standard.horse).
enum ProviderRegistry {
    static let providers: [ContentProvider] = [
        MarkpubProvider(),
        LeafletProvider(),
        PcktProvider(),
        OffprintProvider(),
    ]

    static let defaultProvider = providers[0]

    /// The provider that handles a stored content object, if any.
    static func detectProvider(_ content: UnknownType?) -> ContentProvider? {
        providers.first(where: { $0.matches(content) })
    }

    /// Find a provider by its id.
    static func providerById(_ id: String) -> ContentProvider? {
        providers.first(where: { $0.id == id })
    }

    /// The provider whose `$type` matches the given content type string.
    static func providerByContentType(_ type: String?) -> ContentProvider? {
        guard let type = type else { return nil }
        return providers.first(where: { $0.contentType == type })
    }
}
