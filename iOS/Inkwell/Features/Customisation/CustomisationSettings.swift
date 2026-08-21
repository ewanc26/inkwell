//
//  CustomisationSettings.swift
//  Inkwell
//
//  User-chosen appearance overrides -- free for everyone, not gated.
//  Deliberately takes priority over whatever a publication's own theme
//  sets -- the point is reading everything the way *you* want, not just
//  a fallback for publications that set nothing.
//
//  The first time someone actually changes one of these, SettingsView
//  shows a one-off tip nudge (Ko-fi/GitHub Sponsors) rather than gating
//  the feature behind payment -- a soft ask, not a paywall.
//

import SwiftUI
import Observation

@MainActor
@Observable
final class CustomisationSettings {
    static let shared = CustomisationSettings()

    private let defaults = UserDefaults.standard
    private let accentColorHexKey = "customisation.accentColorHex"
    private let fontFamilyKey = "customisation.fontFamily"
    private let appearanceOverrideKey = "customisation.appearanceOverride"
    private let hasShownTipPromptKey = "customisation.hasShownTipPrompt"

    var accentColorHex: String? {
        didSet { defaults.set(accentColorHex, forKey: accentColorHexKey) }
    }

    var fontFamilyOverride: ReaderTheme.FontFamily? {
        didSet { defaults.set(fontFamilyOverride?.rawValue, forKey: fontFamilyKey) }
    }

    /// nil means "follow the system".
    var appearanceOverride: ColorScheme? {
        didSet {
            let value: String? = switch appearanceOverride {
            case .light: "light"
            case .dark: "dark"
            default: nil
            }
            defaults.set(value, forKey: appearanceOverrideKey)
        }
    }

    private(set) var hasShownTipPrompt: Bool

    private init() {
        accentColorHex = defaults.string(forKey: accentColorHexKey)
        fontFamilyOverride = (defaults.string(forKey: fontFamilyKey)).flatMap(ReaderTheme.FontFamily.init(rawValue:))
        appearanceOverride = switch defaults.string(forKey: appearanceOverrideKey) {
        case "light": .light
        case "dark": .dark
        default: nil
        }
        hasShownTipPrompt = defaults.bool(forKey: hasShownTipPromptKey)
    }

    func markTipPromptShown() {
        hasShownTipPrompt = true
        defaults.set(true, forKey: hasShownTipPromptKey)
    }

    /// 0xRRGGBB, matching SharedReaderTheme's Int colour convention -- fed
    /// into resolveReaderTheme's overrideAccentRgb, not converted to a
    /// native Color here, since the shared resolver is the single place
    /// that decides the final accent (see ReaderTheme.swift).
    var accentColorRgbInt: Int? {
        guard var hex = accentColorHex?.trimmingCharacters(in: .whitespacesAndNewlines), !hex.isEmpty else {
            return nil
        }
        if hex.hasPrefix("#") { hex.removeFirst() }
        guard hex.count == 6 else { return nil }
        return Int(hex, radix: 16)
    }
}
