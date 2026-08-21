//
//  AccessibilitySettings.swift
//  Inkwell
//
//  Accessibility overrides -- always free, unlike the aesthetic overrides
//  in CustomisationSettings.swift. Gating accessibility behind a paywall
//  is bad practice regardless of Inkwell's own honour-system pricing
//  model, so these live in their own persisted store.
//

import SwiftUI
import Observation

@MainActor
@Observable
final class AccessibilitySettings {
    static let shared = AccessibilitySettings()

    private let defaults = UserDefaults.standard
    private let fontSizeScaleKey = "accessibility.fontSizeScale"
    private let boldTextKey = "accessibility.boldText"
    private let increaseContrastKey = "accessibility.increaseContrast"
    private let underlineLinksKey = "accessibility.underlineLinks"

    /// 1.0 is the system default size; the allowed range mirrors iOS's own
    /// larger-text accessibility slider (roughly 0.8x-1.5x).
    var fontSizeScale: Double {
        didSet { defaults.set(fontSizeScale, forKey: fontSizeScaleKey) }
    }

    var boldText: Bool {
        didSet { defaults.set(boldText, forKey: boldTextKey) }
    }

    var increaseContrast: Bool {
        didSet { defaults.set(increaseContrast, forKey: increaseContrastKey) }
    }

    var underlineLinks: Bool {
        didSet { defaults.set(underlineLinks, forKey: underlineLinksKey) }
    }

    private init() {
        let storedScale = defaults.object(forKey: fontSizeScaleKey) as? Double
        fontSizeScale = storedScale ?? 1.0
        boldText = defaults.bool(forKey: boldTextKey)
        increaseContrast = defaults.bool(forKey: increaseContrastKey)
        underlineLinks = defaults.bool(forKey: underlineLinksKey)
    }

    func resetToDefaults() {
        fontSizeScale = 1.0
        boldText = false
        increaseContrast = false
        underlineLinks = false
    }
}
