//
//  SharedKMP.swift
//  SharedKMP
//
//  Swift wrappers around the Kotlin Multiplatform shared core.
//  The InkwellShared.xcframework is built from Android/shared/ and
//  contains pure business logic shared between platforms.
//

import Foundation
import SharedKMP

// MARK: - AT-URI Parser

/// Parses an AT-URI string into its components.
/// Returns nil if the URI is malformed.
func parseAtUri(_ uri: String) -> (did: String, collection: String, recordKey: String)? {
    let kotlinUri = InkwellSharedKt.AtUri.parse(uri)
    guard let kotlinUri else { return nil }
    return (did: kotlinUri.did, collection: kotlinUri.collection, recordKey: kotlinUri.recordKey)
}

// MARK: - Markdown Parser

/// Parses markdown text into a list of MarkdownBlock values.
func parseMarkdown(_ markdown: String) -> [MarkdownBlock] {
    InkwellSharedKt.MarkdownParser.parse(markdown)
}

/// Serializes MarkdownBlock values back to a markdown string.
func serializeMarkdown(_ blocks: [MarkdownBlock]) -> String {
    InkwellSharedKt.MarkdownSerializer.serialize(blocks)
}

// MARK: - Facet Schema

/// Facet NSID constants for Leaflet rich-text formatting.
struct FacetSchema {
    static let leaflet = InkwellSharedKt.FacetSchema.leaflet
    static let pckt = InkwellSharedKt.FacetSchema.pckt
    static let offprint = InkwellSharedKt.FacetSchema.offprint
}

// MARK: - Facet Converter

/// Converts rich-text facets (plaintext + byte-range features) to
/// markdown inline syntax.
func facetsToMarkdown(
    _ plaintext: String,
    facets: [RichTextFacet]?,
    schema: FacetDefinition,
    lost: inout Set<String>
) -> String {
    let sharedFacets = facets?.map { facet in
        let features = facet.features.map { feature in
            return RichTextFeature(type: feature.type, uri: feature.uri)
        }
        return RichTextFacet(byteStart: Int32(facet.byteStart), byteEnd: Int32(facet.byteEnd), features: features)
    } ?? []

    return InkwellSharedKt.FacetsToMarkdown(
        plaintext,
        sharedFacets: sharedFacets,
        boldType: schema.bold,
        italicType: schema.italic,
        codeType: schema.code,
        strikeType: schema.strike,
        linkType: schema.link,
        lossy: schema.lossy,
        lost: &lost
    )
}

/// Minimal facet representation for the shared converter.
struct RichTextFacet {
    let byteStart: Int
    let byteEnd: Int
    let features: [RichTextFeature]
}

struct RichTextFeature {
    let type: String
    let uri: String?
}

// MARK: - Theme Resolution

/// Resolves a publication/document's visual theme from the richest
/// available source down to system defaults.
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
    SharedReaderTheme.resolve(
        richBackgroundColor: richBackgroundColor.map { Int32($0) },
        richPageBackgroundColor: richPageBackgroundColor.map { Int32($0) },
        richPrimaryColor: richPrimaryColor.map { Int32($0) },
        richAccentBackgroundColor: richAccentBackgroundColor.map { Int32($0) },
        richAccentTextColor: richAccentTextColor.map { Int32($0) },
        richPageWidth: richPageWidth.map { Int32($0) },
        richShowPageBackground: richShowPageBackground ?? false,
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

// MARK: - Tip Prompt Policy

/// Determines whether the tip prompt should be shown.
func shouldShowTip(
    launchCount: Int,
    lastShownEpochMillis: Int64 = -1,
    nowEpochMillis: Int64
) -> Bool {
    TipPromptPolicyKt.shouldShowTip(
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
    let style = NotificationPolicyKt.notificationStyle(newDocCount)
    switch style {
    case .none: return .none
    case .single: return .single
    case .summary(let count): return .summary(count: Int32(clamping: count))
    }
}

func isFirstPoll(lastPollEpochMillis: Int64 = -1) -> Bool {
    NotificationPolicyKt.isFirstPoll(lastPollEpochMillis)
}

func trimSeenUris(_ uris: [String]) -> [String] {
    NotificationPolicyKt.trimSeenUris(uris)
}
