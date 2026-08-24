//
//  HapticsSettings.swift
//  Inkwell
//
//  Global on/off for InkwellHaptics (UI/InkwellTheme.swift). Some users
//  find haptics distracting or drain-inducing; this lets them opt out
//  without losing the rest of the app's feel.
//

import SwiftUI
import Observation

@MainActor
@Observable
final class HapticsSettings {
    static let shared = HapticsSettings()

    private let defaults = UserDefaults.standard
    private let enabledKey = "haptics.enabled"

    var enabled: Bool {
        didSet { defaults.set(enabled, forKey: enabledKey) }
    }

    private init() {
        enabled = defaults.object(forKey: enabledKey) as? Bool ?? true
    }
}
