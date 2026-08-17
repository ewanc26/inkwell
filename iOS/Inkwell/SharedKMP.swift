//
//  SharedKMP.swift
//  Inkwell
//
//  Swift wrappers around the Kotlin Multiplatform shared core.
//  The InkwellShared.xcframework is built from shared/ and
//  contains pure business logic shared between platforms.
//

import Foundation
import InkwellShared

// MARK: - AT-URI Parser

func parseAtUri(_ uri: String) -> (did: String, collection: String, recordKey: String)? {
    let kotlinUri = AtUri.companion.parse(uri: uri)
    guard let kotlinUri else { return nil }
    return (did: kotlinUri.did, collection: kotlinUri.collection, recordKey: kotlinUri.recordKey)
}

// MARK: - Facet Schema

struct FacetSchema {
    let facet: String
    let byteSlice: String
    let bold: String
    let italic: String
    let code: String
    let strike: String
    let link: String
    let lossy: [String: String]

    static let leaflet: FacetSchema = {
        let d = InkwellShared.FacetSchema.shared.leaflet
        return FacetSchema(
            facet: d.facet as String,
            byteSlice: d.byteSlice as String,
            bold: d.bold as String,
            italic: d.italic as String,
            code: d.code as String,
            strike: d.strike as String,
            link: d.link as String,
            lossy: d.lossy as? [String: String] ?? [:]
        )
    }()
    static let pckt: FacetSchema = {
        let d = InkwellShared.FacetSchema.shared.pckt
        return FacetSchema(
            facet: d.facet as String,
            byteSlice: d.byteSlice as String,
            bold: d.bold as String,
            italic: d.italic as String,
            code: d.code as String,
            strike: d.strike as String,
            link: d.link as String,
            lossy: d.lossy as? [String: String] ?? [:]
        )
    }()
    static let offprint: FacetSchema = {
        let d = InkwellShared.FacetSchema.shared.offprint
        return FacetSchema(
            facet: d.facet as String,
            byteSlice: d.byteSlice as String,
            bold: d.bold as String,
            italic: d.italic as String,
            code: d.code as String,
            strike: d.strike as String,
            link: d.link as String,
            lossy: d.lossy as? [String: String] ?? [:]
        )
    }()
}

// MARK: - Facet Converter

func facetsToMarkdown(_ plaintext: String, facets: [LeafletFacet]?, schema: FacetSchema) -> String {
    var lost = Set<String>()
    return facetsToMarkdown(plaintext, facets: facets, schema: schema, lost: &lost)
}

func facetsToMarkdown(_ plaintext: String, facets: [LeafletFacet]?, schema: FacetSchema, lost: inout Set<String>) -> String {
    let sharedFacets = facets?.map { facetToShared($0) }
    let result = InkwellShared.FacetConverter.shared.facetsToMarkdown(
        plaintext: plaintext,
        facets: sharedFacets,
        boldType: schema.bold,
        italicType: schema.italic,
        codeType: schema.code,
        strikeType: schema.strike,
        linkType: schema.link,
        lossy: schema.lossy,
        lost: nil
    )
    return result
}

func markdownToFacets(_ markdown: String, schema: FacetSchema) -> (plaintext: String, facets: [LeafletFacet]) {
    let pair = InkwellShared.FacetConverter.shared.markdownToFacets(
        markdown: markdown,
        boldType: schema.bold,
        italicType: schema.italic,
        codeType: schema.code,
        strikeType: schema.strike,
        linkType: schema.link
    )
    let sharedFacets = pair.second as? [RichTextFacet] ?? []
    let plaintext = pair.first! as String
    return (plaintext: plaintext, facets: sharedFacets.map { sharedToFacet($0) })
}

// MARK: - Facet Type Bridging

private func facetToShared(_ facet: LeafletFacet) -> RichTextFacet {
    return RichTextFacet(
        byteStart: Int32(facet.index.byteStart),
        byteEnd: Int32(facet.index.byteEnd),
        features: facet.features.map { RichTextFeature(type: $0.type, uri: $0.uri) }
    )
}

private func sharedToFacet(_ facet: RichTextFacet) -> LeafletFacet {
    return LeafletFacet(
        index: LeafletByteSlice(byteStart: Int(facet.byteStart), byteEnd: Int(facet.byteEnd)),
        features: facet.features.map { LeafletFacetFeature(type: $0.type, uri: $0.uri) }
    )
}

// MARK: - Theme Resolution

func resolveReaderTheme(
    richBackgroundColor: Int? = nil,
    richPageBackgroundColor: Int? = nil,
    richPrimaryColor: Int? = nil,
    richAccentBackgroundColor: Int? = nil,
    richAccentTextColor: Int? = nil,
    richPageWidth: Int? = nil,
    richShowPageBackground: Bool? = nil,
    richHeadingFont: String? = nil,
    richBodyFont: String? = nil,
    richSharedFont: String? = nil,
    paletteBackground: String? = nil,
    paletteText: String? = nil,
    paletteLink: String? = nil,
    paletteAccent: String? = nil,
    paletteSurfaceHover: String? = nil,
    basicBackground: String? = nil,
    basicForeground: String? = nil,
    basicAccent: String? = nil,
    basicAccentForeground: String? = nil,
) -> SharedReaderTheme {
    SharedReaderTheme.Companion.shared.resolve(
        richBackgroundColor: richBackgroundColor.map { KotlinInt(value: Int32($0)) },
        richPageBackgroundColor: richPageBackgroundColor.map { KotlinInt(value: Int32($0)) },
        richPrimaryColor: richPrimaryColor.map { KotlinInt(value: Int32($0)) },
        richAccentBackgroundColor: richAccentBackgroundColor.map { KotlinInt(value: Int32($0)) },
        richAccentTextColor: richAccentTextColor.map { KotlinInt(value: Int32($0)) },
        richPageWidth: richPageWidth.map { KotlinInt(value: Int32($0)) },
        richShowPageBackground: richShowPageBackground.map { KotlinBoolean(value: $0) },
        richHeadingFont: richHeadingFont,
        richBodyFont: richBodyFont,
        richSharedFont: richSharedFont,
        paletteBackground: paletteBackground,
        paletteText: paletteText,
        paletteLink: paletteLink,
        paletteAccent: paletteAccent,
        paletteSurfaceHover: paletteSurfaceHover,
        basicBackground: basicBackground,
        basicForeground: basicForeground,
        basicAccent: basicAccent,
        basicAccentForeground: basicAccentForeground
    )
}

// MARK: - Markdown Parser / Serializer

func parseMarkdown(_ markdown: String) -> [MarkdownBlockNode] {
    let sharedBlocks = MarkdownParser.shared.parse(markdown: markdown)
    return sharedBlocks.map { sharedBlockToLocal($0) }
}

func serializeMarkdown(_ blocks: [MarkdownBlockNode]) -> String {
    let sharedBlocks = blocks.map { localBlockToShared($0) }
    return MarkdownSerializer.shared.serialize(blocks: sharedBlocks)
}

// MARK: - Markdown Type Bridging

private func sharedBlockToLocal(_ block: MarkdownBlock) -> MarkdownBlockNode {
    switch block {
    case let heading as MarkdownBlock.Heading:
        return .heading(level: Int(heading.level), text: heading.text as String)
    case let paragraph as MarkdownBlock.Paragraph:
        return .paragraph(text: paragraph.text as String)
    case let code as MarkdownBlock.Code:
        return .code(language: code.language as String?, content: code.content as String)
    case let math as MarkdownBlock.Math:
        return .math(tex: math.tex as String)
    case let blockquote as MarkdownBlock.Blockquote:
        return .blockquote(text: blockquote.text as String)
    case let image as MarkdownBlock.Image:
        return .image(alt: image.alt as String, url: image.url as String)
    case _ as MarkdownBlock.HorizontalRule:
        return .horizontalRule
    case let list as MarkdownBlock.UnorderedList:
        return .unorderedList(items: list.items.map { sharedListItemToLocal($0) })
    case let list as MarkdownBlock.OrderedList:
        return .orderedList(start: Int(list.start), items: list.items.map { sharedListItemToLocal($0) })
    case let list as MarkdownBlock.TaskList:
        return .taskList(items: list.items.map { sharedListItemToLocal($0) })
    default:
        return .paragraph(text: "")
    }
}

private func localBlockToShared(_ block: MarkdownBlockNode) -> MarkdownBlock {
    switch block {
    case .heading(let level, let text):
        return MarkdownBlock.Heading(level: Int32(level), text: text)
    case .paragraph(let text):
        return MarkdownBlock.Paragraph(text: text)
    case .code(let language, let content):
        return MarkdownBlock.Code(language: language, content: content)
    case .math(let tex):
        return MarkdownBlock.Math(tex: tex)
    case .blockquote(let text):
        return MarkdownBlock.Blockquote(text: text)
    case .image(let alt, let url):
        return MarkdownBlock.Image(alt: alt, url: url)
    case .horizontalRule:
        return MarkdownBlock.HorizontalRule.shared
    case .unorderedList(let items):
        return MarkdownBlock.UnorderedList(items: items.map { localListItemToShared($0) })
    case .orderedList(let start, let items):
        return MarkdownBlock.OrderedList(start: Int32(start), items: items.map { localListItemToShared($0) })
    case .taskList(let items):
        return MarkdownBlock.TaskList(items: items.map { localListItemToShared($0) })
    }
}

private func sharedListItemToLocal(_ item: MarkdownListItem) -> MarkdownListItemNode {
    return MarkdownListItemNode(
        text: item.text as String,
        checked: item.checked?.boolValue,
        children: item.children?.map { sharedListItemToLocal($0) }
    )
}

private func localListItemToShared(_ item: MarkdownListItemNode) -> MarkdownListItem {
    let sharedChecked: KotlinBoolean? = item.checked.map { KotlinBoolean(value: $0) }
    return MarkdownListItem(
        text: item.text as String,
        checked: sharedChecked,
        children: item.children?.map { localListItemToShared($0) }
    )
}

// MARK: - Tip Prompt Policy

func shouldShowTip(
    launchCount: Int,
    lastShownEpochMillis: Int64 = -1,
    nowEpochMillis: Int64
) -> Bool {
    TipPromptPolicy.shared.shouldShowTip(
        launchCount: Int32(launchCount),
        lastShownEpochMillis: lastShownEpochMillis,
        nowEpochMillis: nowEpochMillis
    )
}

// MARK: - Notification Policy

enum NotificationStyle {
    case none
    case single
    case summary(count: Int32)
}

func notificationStyle(newDocCount: Int32) -> NotificationStyle {
    let style = NotificationPolicy.shared.notificationStyle(newDocCount: newDocCount)
    if style is NotificationStyleNone {
        return .none
    } else if style is NotificationStyleSingle {
        return .single
    } else if let summary = style as? NotificationStyleSummary {
        return .summary(count: Int32(clamping: summary.count))
    }
    return .none
}

func isFirstPoll(lastPollEpochMillis: Int64 = -1) -> Bool {
    NotificationPolicy.shared.isFirstPoll(lastPollEpochMillis: lastPollEpochMillis)
}

func trimSeenUris(_ uris: [String]) -> [String] {
    NotificationPolicy.shared.trimSeenUris(seenUris: uris)
}

func trimNotifications(_ notifications: [Any]) -> [Any] {
    NotificationPolicy.shared.trimNotifications(notifications: notifications)
}
