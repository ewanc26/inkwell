//
//  ReaderTheme.swift
//  Inkwell
//
//  Resolves a publication's visual identity — colours, fonts, and page
//  dimensions — from the richest available source down to system defaults.
//  The resolution chain runs: Leaflet's rich theme → standard.site's
//  BasicDefinition → system appearance, so a publication that sets nothing
//  still renders legibly without ever looking broken.
//
//  The cascade itself lives in shared KMP (SharedReaderTheme.resolve,
//  called via resolveReaderTheme in SharedKMP.swift) and is identical to
//  Android's ui/reader/ReaderTheme.kt -- this file only converts iOS's
//  native Leaflet/theme models to the shared function's inputs and its
//  Int/enum outputs back to SwiftUI Color/Font, the same adapter role
//  Android's wrapper plays for Compose Color.
//

import SwiftUI
import UIKit
import InkwellShared

struct ReaderTheme {
    enum FontFamily: String, Equatable {
        case sans
        case serif
        case rounded
        case monospaced

        var design: Font.Design {
            switch self {
            case .sans: return .default
            case .serif: return .serif
            case .rounded: return .rounded
            case .monospaced: return .monospaced
            }
        }
    }

    let background: Color
    let pageBackground: Color
    let foreground: Color
    let accent: Color
    let accentForeground: Color
    let pageWidth: CGFloat
    let showPageBackground: Bool
    let headingFamily: FontFamily
    let bodyFamily: FontFamily

    init(
        document: SiteStandardLexicon.DocumentRecord? = nil,
        publication: SiteStandardLexicon.PublicationRecord? = nil,
        colorScheme: ColorScheme
    ) {
        let rich = document?.theme ?? publication?.theme
        let basic = publication?.basicTheme

        // Which of the theme's dark/light palettes actually matches what
        // the background will resolve to — not just the device's
        // appearance. A publication can set a fixed `backgroundColor`
        // independent of its dark/light palettes; picking foreground/accent
        // from the palette keyed to the device's colorScheme in that case
        // can pick the wrong one entirely (e.g. a fixed dark background
        // paired with the `.light` palette's near-black text, or the
        // `.label` fallback resolving black because the device itself is in
        // light mode) — illegible text baked onto a background it was never
        // designed to sit on.
        let isDark = rich?.backgroundColor?.color.isPerceptuallyDark ?? (colorScheme == .dark)
        let palette = isDark ? rich?.dark : rich?.light

        let customisation = CustomisationSettings.shared

        let shared = resolveReaderTheme(
            richBackgroundColor: rich?.backgroundColor?.rgbInt,
            richPageBackgroundColor: rich?.pageBackground?.rgbInt,
            richPrimaryColor: rich?.primary?.rgbInt,
            richAccentBackgroundColor: rich?.accentBackground?.rgbInt,
            richAccentTextColor: rich?.accentText?.rgbInt,
            richPageWidth: rich?.pageWidth,
            richShowPageBackground: rich?.showPageBackground,
            richHeadingFont: rich?.headingFont,
            richBodyFont: rich?.bodyFont,
            richSharedFont: rich?.font,
            paletteBackground: palette?.background,
            paletteText: palette?.text,
            paletteLink: palette?.link,
            paletteAccent: palette?.accent,
            paletteSurfaceHover: palette?.surfaceHover,
            basicBackground: basic?.background.hexString,
            basicForeground: basic?.foreground.hexString,
            basicAccent: basic?.accent.hexString,
            basicAccentForeground: basic?.accentForeground.hexString,
            overrideAccentRgb: customisation.accentColorRgbInt,
            overrideFontFamily: customisation.fontFamilyOverride?.toShared()
        )

        background = Color(rgbInt: Int(shared.backgroundRgb))
        pageBackground = Color(rgbInt: Int(shared.pageBackgroundRgb))
        foreground = Color(rgbInt: Int(shared.foregroundRgb))
        accent = Color(rgbInt: Int(shared.accentRgb))
        accentForeground = Color(rgbInt: Int(shared.accentForegroundRgb))
        pageWidth = CGFloat(shared.pageWidthDp)
        showPageBackground = shared.showPageBackground
        headingFamily = shared.headingFontFamily.toLocal()
        bodyFamily = shared.bodyFontFamily.toLocal()
    }

    func headingFont(_ style: Font.TextStyle, weight: Font.Weight? = nil) -> Font {
        let font = Font.system(style, design: headingFamily.design)
        return weight.map(font.weight) ?? font
    }

    func bodyFont(_ style: Font.TextStyle, weight: Font.Weight? = nil) -> Font {
        let font = Font.system(style, design: bodyFamily.design)
        return weight.map(font.weight) ?? font
    }
}

// MARK: - Shared KMP Conversion

private extension SiteStandardLexicon.Theme.ColorValue {
    /// 0xRRGGBB, matching SharedReaderTheme's Int colour convention.
    var rgbInt: Int { (r << 16) | (g << 8) | b }
}

private extension SiteStandardLexicon.Theme.RGBColor {
    var hexString: String { String(format: "#%02X%02X%02X", r, g, b) }
}

private extension ReaderTheme.FontFamily {
    func toShared() -> InkwellShared.SharedReaderTheme.FontFamily {
        switch self {
        case .sans: return .sans
        case .serif: return .serif
        case .rounded: return .rounded
        case .monospaced: return .monospaced
        }
    }
}

private extension InkwellShared.SharedReaderTheme.FontFamily {
    func toLocal() -> ReaderTheme.FontFamily {
        switch self {
        case .sans: return .sans
        case .serif: return .serif
        case .rounded: return .rounded
        case .monospaced: return .monospaced
        default: return .sans
        }
    }
}

private extension Color {
    /// Standard perceived-luminance formula (Rec. 601): true below the
    /// midpoint, i.e. a color dark enough to need light text on top of it.
    var isPerceptuallyDark: Bool {
        var r: CGFloat = 0, g: CGFloat = 0, b: CGFloat = 0, a: CGFloat = 0
        UIColor(self).getRed(&r, green: &g, blue: &b, alpha: &a)
        return (0.299 * r + 0.587 * g + 0.114 * b) < 0.5
    }

    /// Reads only the RGB bytes, ignoring any alpha byte -- SharedReaderTheme's
    /// Int outputs aren't guaranteed to carry a meaningful top byte.
    init(rgbInt: Int) {
        self.init(
            red: Double((rgbInt >> 16) & 0xff) / 255,
            green: Double((rgbInt >> 8) & 0xff) / 255,
            blue: Double(rgbInt & 0xff) / 255
        )
    }
}
