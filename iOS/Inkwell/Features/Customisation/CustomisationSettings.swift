//
//  CustomisationSettings.swift
//  Inkwell
//
//  User-chosen appearance overrides, unlocked by a paid license key (see
//  LicenseVerifier.swift). Deliberately takes priority over whatever a
//  publication's own theme sets -- the point is reading everything the
//  way *you* want, not just a fallback for publications that set nothing.
//

import SwiftUI
import Observation

@MainActor
@Observable
final class CustomisationSettings {
    static let shared = CustomisationSettings()

    private let defaults = UserDefaults.standard
    private let unlockedKey = "customisation.unlocked"
    private let accentColorHexKey = "customisation.accentColorHex"
    private let fontFamilyKey = "customisation.fontFamily"
    private let appearanceOverrideKey = "customisation.appearanceOverride"

    private(set) var isUnlocked: Bool

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

    private init() {
        isUnlocked = defaults.bool(forKey: unlockedKey)
        accentColorHex = defaults.string(forKey: accentColorHexKey)
        fontFamilyOverride = (defaults.string(forKey: fontFamilyKey)).flatMap(ReaderTheme.FontFamily.init(rawValue:))
        appearanceOverride = switch defaults.string(forKey: appearanceOverrideKey) {
        case "light": .light
        case "dark": .dark
        default: nil
        }
    }

    /// Returns true and persists the unlock if the key verifies.
    @discardableResult
    func unlock(withKey key: String) -> Bool {
        guard LicenseVerifier.isValid(licenseKey: key) else { return false }
        isUnlocked = true
        defaults.set(true, forKey: unlockedKey)
        return true
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
