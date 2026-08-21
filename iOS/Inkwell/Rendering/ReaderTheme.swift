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
        let accessibility = AccessibilitySettings.shared

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
            overrideFontFamily: customisation.fontFamilyOverride?.toShared(),
            increaseContrast: accessibility.increaseContrast
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
        Self.scaledFont(style, family: headingFamily, weight: weight)
    }

    func bodyFont(_ style: Font.TextStyle, weight: Font.Weight? = nil) -> Font {
        Self.scaledFont(style, family: bodyFamily, weight: weight)
    }

    /// Applies AccessibilitySettings' font size scale and bold-text
    /// override on top of the system's own Dynamic Type size for [style]
    /// -- the two compose rather than compete, same as iOS's own
    /// accessibility text size and Bold Text settings do for system UI.
    private static func scaledFont(_ style: Font.TextStyle, family: FontFamily, weight: Font.Weight?) -> Font {
        let accessibility = AccessibilitySettings.shared
        let baseSize = UIFont.preferredFont(forTextStyle: style.uiKit).pointSize
        let scaledSize = baseSize * accessibility.fontSizeScale
        let resolvedWeight = accessibility.boldText ? .bold : (weight ?? .regular)
        return .system(size: scaledSize, weight: resolvedWeight, design: family.design)
    }
}

private extension Font.TextStyle {
    var uiKit: UIFont.TextStyle {
        switch self {
        case .largeTitle: return .largeTitle
        case .title: return .title1
        case .title2: return .title2
        case .title3: return .title3
        case .headline: return .headline
        case .subheadline: return .subheadline
        case .body: return .body
        case .callout: return .callout
        case .footnote: return .footnote
        case .caption: return .caption1
        case .caption2: return .caption2
        default: return .body
        }
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
