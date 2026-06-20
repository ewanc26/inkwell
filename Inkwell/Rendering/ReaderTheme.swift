import SwiftUI

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
        let palette = colorScheme == .dark ? rich?.dark : rich?.light
        let basic = publication?.basicTheme

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
            ?? Color(uiColor: .label)
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
