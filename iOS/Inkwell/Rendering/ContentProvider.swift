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
//  Individual providers live in their own files in this directory:
//  LeafletProvider.swift, MarkpubProvider.swift, PcktProvider.swift,
//  OffprintProvider.swift. The registry lives in ProviderRegistry.swift.
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
/// Delegates to shared KMP for canonical conversion.
enum FacetConverter {

    // These call the global bridge functions of the same name declared in
    // SharedKMP.swift. Unqualified calls here would instead resolve to the
    // sibling static method on this same enum (Swift prefers member lookup
    // over the enclosing/global scope), causing infinite self-recursion —
    // qualify with the module name to reach the intended global function.
    static func facetsToMarkdown(_ plaintext: String, facets: [LeafletFacet]?, schema: FacetSchema, lost: inout Set<String>) -> String {
        Inkwell.facetsToMarkdown(plaintext, facets: facets, schema: schema, lost: &lost)
    }

    static func facetsToMarkdown(_ plaintext: String, facets: [LeafletFacet]?, schema: FacetSchema) -> String {
        Inkwell.facetsToMarkdown(plaintext, facets: facets, schema: schema)
    }

    static func markdownToFacets(_ markdown: String, schema: FacetSchema) -> (plaintext: String, facets: [LeafletFacet]) {
        Inkwell.markdownToFacets(markdown, schema: schema)
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
