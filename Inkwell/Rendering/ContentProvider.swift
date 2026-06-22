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

// MARK: - Facet Schema

/// Maps a format's facet `$type` strings to the markdown marks we support.
/// Only the `$type` strings differ between formats; the facet structure is
/// identical across Leaflet, Pckt, and Offprint.
struct FacetSchema {
    let facet: String
    let byteSlice: String
    let bold: String
    let italic: String
    let code: String
    let strike: String
    let link: String
    /// Feature `$type` → human label, for features markdown can't represent.
    /// Matching standard.horse's `lossy` pattern.
    let lossy: [String: String]

    static let leaflet = FacetSchema(
        facet: "pub.leaflet.richtext.facet",
        byteSlice: "pub.leaflet.richtext.facet#byteSlice",
        bold: "pub.leaflet.richtext.facet#bold",
        italic: "pub.leaflet.richtext.facet#italic",
        code: "pub.leaflet.richtext.facet#code",
        strike: "pub.leaflet.richtext.facet#strikethrough",
        link: "pub.leaflet.richtext.facet#link",
        lossy: [
            "pub.leaflet.richtext.facet#highlight": "highlight",
            "pub.leaflet.richtext.facet#underline": "underline",
            "pub.leaflet.richtext.facet#atMention": "mentions",
            "pub.leaflet.richtext.facet#didMention": "mentions",
            "pub.leaflet.richtext.facet#footnote": "footnotes",
        ]
    )

    static let pckt = FacetSchema(
        facet: "blog.pckt.richtext.facet",
        byteSlice: "blog.pckt.richtext.facet#byteSlice",
        bold: "blog.pckt.richtext.facet#bold",
        italic: "blog.pckt.richtext.facet#italic",
        code: "blog.pckt.richtext.facet#code",
        strike: "blog.pckt.richtext.facet#strikethrough",
        link: "blog.pckt.richtext.facet#link",
        lossy: [
            "blog.pckt.richtext.facet#highlight": "highlight",
            "blog.pckt.richtext.facet#underline": "underline",
            "blog.pckt.richtext.facet#atMention": "mentions",
            "blog.pckt.richtext.facet#didMention": "mentions",
            "blog.pckt.richtext.facet#id": "anchors",
        ]
    )

    static let offprint = FacetSchema(
        facet: "app.offprint.richtext.facet",
        byteSlice: "app.offprint.richtext.facet#byteSlice",
        bold: "app.offprint.richtext.facet#bold",
        italic: "app.offprint.richtext.facet#italic",
        code: "app.offprint.richtext.facet#code",
        strike: "app.offprint.richtext.facet#strikethrough",
        link: "app.offprint.richtext.facet#link",
        lossy: [
            "app.offprint.richtext.facet#highlight": "highlight",
            "app.offprint.richtext.facet#underline": "underline",
            "app.offprint.richtext.facet#mention": "mentions",
            "app.offprint.richtext.facet#webMention": "mentions",
        ]
    )
}

// MARK: - Facet Converter

/// Converts between AT Protocol facets (plaintext + byte-range features) and
/// markdown inline syntax (**bold**, *italic*, `code`, ~~strike~~, [text](url)).
enum FacetConverter {

    /// Convert facets to markdown inline text. Unknown features are tracked
    /// in `lost` using schema.lossy labels.
    static func facetsToMarkdown(_ plaintext: String, facets: [LeafletFacet]?, schema: FacetSchema, lost: inout Set<String>) -> String {

        guard let facets = facets, !facets.isEmpty else {
            return plaintext
        }

        // Convert to UTF-8 bytes once for efficient byte-range extraction.
        let utf8Bytes = Array(plaintext.utf8)
        let totalBytes = utf8Bytes.count

        // Collect all byte boundaries where the active mark-set may change.
        var boundaries = Set<Int>([0, totalBytes])
        for facet in facets {
            boundaries.insert(facet.index.byteStart)
            boundaries.insert(facet.index.byteEnd)
        }
        let sortedBounds = boundaries.sorted()

        // For each segment, determine which marks are active.
        struct Segment {
            let text: String
            let bold: Bool
            let italic: Bool
            let code: Bool
            let strike: Bool
            let link: String?
        }

        var segments: [Segment] = []
        for idx in 0..<(sortedBounds.count - 1) {
            let start = sortedBounds[idx]
            let end = sortedBounds[idx + 1]
            if start >= end || start >= totalBytes { continue }
            let clampedEnd = min(end, totalBytes)

            // Extract the text for this byte range by decoding UTF-8 bytes.
            let byteSlice = Array(utf8Bytes[start..<clampedEnd])
            let text = String(bytes: byteSlice, encoding: .utf8) ?? ""
            if text.isEmpty { continue }

            // Determine active marks at this byte position.
            var bold = false, italic = false, code = false, strike = false
            var link: String? = nil
            for facet in facets {
                if start >= facet.index.byteStart && start < facet.index.byteEnd {
                    for feature in facet.features {
                        switch feature.type {
                        case schema.bold: bold = true
                        case schema.italic: italic = true
                        case schema.code: code = true
                        case schema.strike: strike = true
                        case schema.link: link = feature.uri
                        default:
                            if let label = schema.lossy[feature.type] {
                                lost.insert(label)
                            }
                        }
                    }
                }
            }

            let seg = Segment(text: text, bold: bold, italic: italic, code: code, strike: strike, link: link)

            // Merge with previous segment if marks are identical.
            if let last = segments.last,
               last.bold == seg.bold, last.italic == seg.italic,
               last.code == seg.code, last.strike == seg.strike,
               last.link == seg.link {
                segments[segments.count - 1] = Segment(
                    text: last.text + seg.text, bold: seg.bold, italic: seg.italic,
                    code: seg.code, strike: seg.strike, link: seg.link
                )
            } else {
                segments.append(seg)
            }
        }

        // Build markdown from segments.
        var result = ""
        for seg in segments {
            var wrapped = seg.text
            if seg.code {
                wrapped = "`\(wrapped)`"
            } else {
                if seg.strike { wrapped = "~~\(wrapped)~~" }
                if seg.italic { wrapped = "*\(wrapped)*" }
                if seg.bold { wrapped = "**\(wrapped)**" }
            }
            if let link = seg.link {
                wrapped = "[\(wrapped)](\(link))"
            }
            result += wrapped
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
enum MarkdownBlock {
    case heading(level: Int, text: String)
    case paragraph(text: String)
    case code(language: String?, content: String)
    case math(tex: String)
    case blockquote(text: String)
    case image(alt: String, url: String)
    case horizontalRule
    case unorderedList(items: [MarkdownListItem])
    case orderedList(start: Int, items: [MarkdownListItem])
    case taskList(items: [MarkdownListItem])
}

struct MarkdownListItem {
    let text: String
    let checked: Bool?  // nil = not a task item
    let children: [MarkdownListItem]?
}

// MARK: - Markdown Parser

/// A simple line-by-line markdown parser that handles the block types
/// common to all standard.site providers. Not a full CommonMark parser,
/// but sufficient for the editor's round-trip needs.
enum MarkdownParser {

    static func parse(_ markdown: String) -> [MarkdownBlock] {
        var blocks: [MarkdownBlock] = []
        let lines = markdown.components(separatedBy: "\n")
        var i = 0

        while i < lines.count {
            let trimmed = lines[i].trimmingCharacters(in: .whitespaces)

            // Skip empty lines
            if trimmed.isEmpty {
                i += 1
                continue
            }

            // Code block / math block
            if trimmed.hasPrefix("```") {
                let lang = String(trimmed.dropFirst(3))
                var codeLines: [String] = []
                i += 1
                while i < lines.count && !lines[i].trimmingCharacters(in: .whitespaces).hasPrefix("```") {
                    codeLines.append(lines[i])
                    i += 1
                }
                i += 1  // skip closing ```
                let content = codeLines.joined(separator: "\n")
                if lang == "math" {
                    blocks.append(.math(tex: content))
                } else {
                    blocks.append(.code(language: lang.isEmpty ? nil : lang, content: content))
                }
                continue
            }

            // Heading
            if trimmed.hasPrefix("#") {
                let level = trimmed.prefix(while: { $0 == "#" }).count
                if level >= 1 && level <= 6 {
                    let text = trimmed.dropFirst(level).trimmingCharacters(in: .whitespaces)
                    blocks.append(.heading(level: level, text: text))
                    i += 1
                    continue
                }
            }

            // Horizontal rule
            if trimmed == "---" || trimmed == "***" || trimmed == "___" {
                blocks.append(.horizontalRule)
                i += 1
                continue
            }

            // Blockquote
            if trimmed.hasPrefix(">") {
                var quoteLines: [String] = []
                while i < lines.count {
                    let l = lines[i].trimmingCharacters(in: .whitespaces)
                    if l.hasPrefix(">") {
                        quoteLines.append(l.dropFirst().trimmingCharacters(in: .whitespaces))
                        i += 1
                    } else if !l.isEmpty {
                        break
                    } else {
                        i += 1
                        break
                    }
                }
                blocks.append(.blockquote(text: quoteLines.joined(separator: "\n")))
                continue
            }

            // Image (on its own line)
            if trimmed.hasPrefix("![") {
                if let closeBracket = trimmed.firstIndex(of: "]"),
                   trimmed.index(after: closeBracket) < trimmed.endIndex,
                   trimmed[trimmed.index(after: closeBracket)] == "(",
                   let closeParen = trimmed[trimmed.index(after: trimmed.index(after: closeBracket))...].firstIndex(of: ")") {
                    let alt = String(trimmed[trimmed.index(after: trimmed.startIndex)..<closeBracket])
                    let urlStart = trimmed.index(closeBracket, offsetBy: 2)
                    let url = String(trimmed[urlStart..<closeParen])
                    blocks.append(.image(alt: alt, url: url))
                    i += 1
                    continue
                }
            }

            // Unordered list / Task list
            if trimmed.hasPrefix("- ") || trimmed.hasPrefix("* ") {
                let (items, nextI) = parseList(lines, from: i, ordered: false)
                if items.allSatisfy({ $0.checked != nil }) && !items.isEmpty {
                    blocks.append(.taskList(items: items))
                } else {
                    blocks.append(.unorderedList(items: items))
                }
                i = nextI
                continue
            }

            // Ordered list: "1. Item" or "1) Item" — match leading digits
            // followed by a period or parenthesis
            var numberStart = trimmed.startIndex
            var numberStr = ""
            while numberStart < trimmed.endIndex, trimmed[numberStart].isNumber {
                numberStr.append(trimmed[numberStart])
                numberStart = trimmed.index(after: numberStart)
            }
            if !numberStr.isEmpty, numberStart < trimmed.endIndex,
               (trimmed[numberStart] == "." || trimmed[numberStart] == ")"),
               trimmed.index(after: numberStart) < trimmed.endIndex,
               trimmed[trimmed.index(after: numberStart)].isWhitespace,
               let number = Int(numberStr) {
                let (items, nextI) = parseList(lines, from: i, ordered: true)
                blocks.append(.orderedList(start: number, items: items))
                i = nextI
                continue
            }

            // Paragraph (collect consecutive non-special lines)
            var paraLines: [String] = []
            while i < lines.count {
                let l = lines[i].trimmingCharacters(in: .whitespaces)
                if l.isEmpty || l.hasPrefix("#") || l.hasPrefix(">") || l.hasPrefix("```") ||
                   l.hasPrefix("- ") || l.hasPrefix("* ") || l == "---" || l == "***" ||
                   l.hasPrefix("![") {
                    break
                }
                paraLines.append(l)
                i += 1
            }
            if !paraLines.isEmpty {
                blocks.append(.paragraph(text: paraLines.joined(separator: " ")))
            }
        }

        return blocks
    }

    private static func parseList(_ lines: [String], from start: Int, ordered: Bool) -> ([MarkdownListItem], Int) {
        var items: [MarkdownListItem] = []
        var i = start
        let baseIndent = lines[start].prefix(while: { $0 == " " }).count

        while i < lines.count {
            let line = lines[i]
            let trimmed = line.trimmingCharacters(in: .whitespaces)

            if trimmed.isEmpty {
                i += 1
                continue
            }

            // Check if this line is a list item at the base indent level
            let indent = line.prefix(while: { $0 == " " }).count
            if indent < baseIndent {
                break
            }

            // Check for list markers
            let isUnordered = trimmed.hasPrefix("- ") || trimmed.hasPrefix("* ")
            let isOrdered = ordered && trimmed.range(of: #"^\d+[.)]\s"#, options: .regularExpression) != nil

            if !isUnordered && !isOrdered {
                break
            }

            // Extract item text (strip the marker)
            var itemText: String
            if isUnordered {
                itemText = String(trimmed.dropFirst(2))
            } else {
                // Find the first space after the ordered list marker (e.g. "1. ")
                if let markerEnd = trimmed.firstIndex(of: " ") {
                    itemText = String(trimmed[trimmed.index(after: markerEnd)...])
                } else {
                    itemText = ""
                }
            }

            // Check for task list checkbox
            var checked: Bool? = nil
            if itemText.hasPrefix("[x] ") || itemText.hasPrefix("[X] ") {
                checked = true
                itemText = String(itemText.dropFirst(4))
            } else if itemText.hasPrefix("[ ] ") {
                checked = false
                itemText = String(itemText.dropFirst(4))
            }

            // Check for nested list items (more indented)
            var children: [MarkdownListItem] = []
            if i + 1 < lines.count {
                let nextIndent = lines[i + 1].prefix(while: { $0 == " " }).count
                if nextIndent > baseIndent {
                    let (nested, nextI) = parseList(lines, from: i + 1, ordered: false)
                    children = nested
                    i = nextI - 1
                }
            }

            items.append(MarkdownListItem(text: itemText, checked: checked, children: children.isEmpty ? nil : children))
            i += 1
        }

        return (items, i)
    }
}

// MARK: - Markdown Serializer

/// Converts MarkdownBlock array back to a markdown string.
enum MarkdownSerializer {

    static func serialize(_ blocks: [MarkdownBlock]) -> String {
        blocks.map { blockToString($0) }.joined(separator: "\n\n")
    }

    private static func blockToString(_ block: MarkdownBlock) -> String {
        switch block {
        case .heading(let level, let text):
            return String(repeating: "#", count: level) + " " + text

        case .paragraph(let text):
            return text

        case .code(let language, let content):
            let lang = language ?? ""
            return "```\(lang)\n\(content)\n```"

        case .math(let tex):
            return "```math\n\(tex)\n```"

        case .blockquote(let text):
            return text.components(separatedBy: "\n").map { "> " + $0 }.joined(separator: "\n")

        case .image(let alt, let url):
            return "![\(alt)](\(url))"

        case .horizontalRule:
            return "---"

        case .unorderedList(let items):
            return items.map { listItemToString($0, prefix: "- ") }.joined(separator: "\n")

        case .orderedList(let start, let items):
            return items.enumerated().map { (idx, item) in
                listItemToString(item, prefix: "\(start + idx). ")
            }.joined(separator: "\n")

        case .taskList(let items):
            return items.map { item in
                let checkbox = item.checked == true ? "[x] " : item.checked == false ? "[ ] " : ""
                return "- " + checkbox + item.text
            }.joined(separator: "\n")
        }
    }

    private static func listItemToString(_ item: MarkdownListItem, prefix: String) -> String {
        var result = prefix + item.text
        if let children = item.children {
            let childLines = children.map { listItemToString($0, prefix: "  - ") }
            result += "\n" + childLines.joined(separator: "\n")
        }
        return result
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

    func toMarkdown(_ content: UnknownType?) -> ConvertResult {
        guard let leaflet = content?.getRecord(ofType: LeafletContent.self) else {
            return ConvertResult(markdown: "", lost: [])
        }

        var lost = Set<String>()
        var blocks: [MarkdownBlock] = []

        let pages = leaflet.pages ?? []
        // Prefer the linearDocument page, but accept any page (including blob-loaded pages).
        let page = pages.first(where: { $0.type == "pub.leaflet.pages.linearDocument" }) ?? pages.first
        let blockContainers = page?.blocks ?? []

        for container in blockContainers {
            let inner = container.block
            if let mdBlock = leafletBlockToMarkdown(inner, alignment: container.alignment, lost: &lost) {
                blocks.append(mdBlock)
            }
        }

        return ConvertResult(markdown: MarkdownSerializer.serialize(blocks), lost: Array(lost))
    }

    private func leafletBlockToMarkdown(_ block: LeafletBlock, alignment: String?, lost: inout Set<String>) -> MarkdownBlock? {
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

    private func leafletListItemToMarkdown(_ item: LeafletListItem) -> MarkdownListItem {
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
        var mdChildren: [MarkdownListItem]? = nil
        if let kids = item.children, !kids.isEmpty {
            mdChildren = kids.map { leafletListItemToMarkdown($0) }
        }

        return MarkdownListItem(text: text, checked: item.checked, children: mdChildren)
    }

    func fromMarkdown(_ markdown: String, ctx: WriteContext) -> UnknownType? {
        // Harvest existing image blobs from the previous content so CIDs in
        // markdown (e.g. ![](bafy...)) can be matched and reattached verbatim
        // without re-uploading — matching standard.horse's round-trip pattern.
        let previousBlobs = harvestImageBlobs(from: ctx.previousContent)

        let blocks = MarkdownParser.parse(markdown)
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

    private func markdownToLeafletBlock(_ block: MarkdownBlock, previousBlobs: [String: ComAtprotoLexicon.Repository.UploadBlobOutput]) -> LeafletBlock? {
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
    private func markdownToLeafletListItem(_ item: MarkdownListItem, ordered: Bool, previousBlobs: [String: ComAtprotoLexicon.Repository.UploadBlobOutput]) -> LeafletListItem {
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
    ]

    func matches(_ content: UnknownType?) -> Bool {
        content?.getRecord(ofType: PcktContent.self) != nil
    }

    func toMarkdown(_ content: UnknownType?) -> ConvertResult {
        guard let pckt = content?.getRecord(ofType: PcktContent.self) else {
            return ConvertResult(markdown: "", lost: [])
        }

        var lost = Set<String>()
        var blocks: [MarkdownBlock] = []

        for block in pckt.items ?? [] {
            if let mdBlock = pcktBlockToMarkdown(block, lost: &lost) {
                blocks.append(mdBlock)
            }
        }

        return ConvertResult(markdown: MarkdownSerializer.serialize(blocks), lost: Array(lost))
    }

    private func pcktBlockToMarkdown(_ block: PcktBlock, lost: inout Set<String>) -> MarkdownBlock? {
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

    private func pcktListItemToMarkdown(_ item: PcktListItem) -> MarkdownListItem {
        var text = ""
        var children: [MarkdownListItem]? = nil
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

        return MarkdownListItem(text: text, checked: item.checked, children: children)
    }

    func fromMarkdown(_ markdown: String, ctx: WriteContext) -> UnknownType? {
        let blocks = MarkdownParser.parse(markdown)
        var items: [PcktBlock] = []

        for block in blocks {
            if let pb = markdownToPcktBlock(block) {
                items.append(pb)
            }
        }

        let content = PcktContent(items: items)
        return UnknownType.record(content)
    }

    private func markdownToPcktBlock(_ block: MarkdownBlock) -> PcktBlock? {
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

    private func markdownToPcktListItem(_ item: MarkdownListItem, isTaskItem: Bool) -> PcktListItem {
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
        var blocks: [MarkdownBlock] = []

        for block in offprint.items ?? [] {
            if let mdBlock = offprintBlockToMarkdown(block, lost: &lost) {
                blocks.append(mdBlock)
            }
        }

        return ConvertResult(markdown: MarkdownSerializer.serialize(blocks), lost: Array(lost))
    }

    private func offprintBlockToMarkdown(_ block: OffprintBlock, lost: inout Set<String>) -> MarkdownBlock? {
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
            return .code(language: block.language, content: block.plaintext ?? "")

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

    private func offprintListItemToMarkdown(_ item: OffprintListItem) -> MarkdownListItem {
        var text = ""
        if let content = item.content, content.type == b("text") {
            text = FacetConverter.facetsToMarkdown(
                content.plaintext ?? "", facets: content.facets, schema: schema
            )
        }

        var children: [MarkdownListItem]? = nil
        if let kids = item.children, !kids.isEmpty {
            children = kids.map { offprintListItemToMarkdown($0) }
        }

        return MarkdownListItem(text: text, checked: item.checked, children: children)
    }

    func fromMarkdown(_ markdown: String, ctx: WriteContext) -> UnknownType? {
        let blocks = MarkdownParser.parse(markdown)
        var items: [OffprintBlock] = []

        for block in blocks {
            if let ob = markdownToOffprintBlock(block) {
                items.append(ob)
            }
        }

        let content = OffprintContent(items: items)
        return UnknownType.record(content)
    }

    private func markdownToOffprintBlock(_ block: MarkdownBlock) -> OffprintBlock? {
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
                type: b("codeBlock"), plaintext: content, language: language
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

    private func markdownToOffprintListItem(_ item: MarkdownListItem, ordered: Bool) -> OffprintListItem {
        let (plaintext, facets) = FacetConverter.markdownToFacets(item.text, schema: schema)
        let textBlock = OffprintBlock(
            type: b("text"), plaintext: plaintext,
            facets: facets.isEmpty ? nil : facets
        )
        let itemType: String
        if item.checked != nil {
            itemType = "app.offprint.block.taskList#taskItem"
        } else {
            itemType = ordered ? "app.offprint.block.orderedList#listItem" : "app.offprint.block.bulletList#listItem"
        }
        let children = item.children?.map { markdownToOffprintListItem($0, ordered: ordered) }
        return OffprintListItem(
            type: itemType, content: textBlock, checked: item.checked, children: children
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
