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

import SwiftUI
import UIKit

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
        // `background` will resolve to below — not just the device's
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

        background = rich?.backgroundColor?.color
            ?? Color(hex: palette?.background)
            ?? basic?.background.color
            ?? Color(uiColor: .systemBackground)
        pageBackground = rich?.pageBackground?.color
            ?? Color(hex: palette?.surfaceHover)
            ?? background
        foreground = rich?.primary?.color
            ?? Color(hex: palette?.text)
            ?? basic?.foreground.color
            ?? (isDark ? .white : .black)
        accent = rich?.accentBackground?.color
            ?? Color(hex: palette?.link ?? palette?.accent)
            ?? basic?.accent.color
            ?? .accentColor
        accentForeground = rich?.accentText?.color
            ?? basic?.accentForeground.color
            ?? Color(uiColor: .systemBackground)
        pageWidth = CGFloat(min(max(rich?.pageWidth ?? 680, 320), 1_000))
        showPageBackground = rich?.showPageBackground ?? false

        let sharedFont = rich?.font
        headingFamily = Self.family(for: rich?.headingFont ?? sharedFont)
        bodyFamily = Self.family(for: rich?.bodyFont ?? sharedFont)
    }

    func headingFont(_ style: Font.TextStyle, weight: Font.Weight? = nil) -> Font {
        let font = Font.system(style, design: headingFamily.design)
        return weight.map(font.weight) ?? font
    }

    func bodyFont(_ style: Font.TextStyle, weight: Font.Weight? = nil) -> Font {
        let font = Font.system(style, design: bodyFamily.design)
        return weight.map(font.weight) ?? font
    }

    /// Maps a Leaflet font identifier to a `Font.Design` family. `nil` means
    /// no font was specified anywhere in the resolved theme, so this falls
    /// back to the system font rather than assuming an editorial serif —
    /// theming should be opt-in, driven entirely by what the publication
    /// actually set.
    nonisolated static func family(for identifier: String?) -> FontFamily {
        guard let identifier else { return .sans }
        let value = identifier.lowercased()

        if value.contains("mono") || value.contains("quattro") || value.contains("code") {
            return .monospaced
        }
        if value.contains("lora") || value.contains("newsreader") || value.contains("serif") || value.contains("georgia") {
            return .serif
        }
        if value.contains("atkinson") || value.contains("rounded") {
            return .rounded
        }
        return .sans
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

    init?(hex: String?) {
        guard var value = hex?.trimmingCharacters(in: .whitespacesAndNewlines), !value.isEmpty else {
            return nil
        }
        if value.hasPrefix("#") { value.removeFirst() }
        guard value.count == 6, let rgb = UInt64(value, radix: 16) else { return nil }
        self.init(
            red: Double((rgb >> 16) & 0xff) / 255,
            green: Double((rgb >> 8) & 0xff) / 255,
            blue: Double(rgb & 0xff) / 255
        )
    }
}
