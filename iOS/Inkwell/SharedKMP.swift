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
