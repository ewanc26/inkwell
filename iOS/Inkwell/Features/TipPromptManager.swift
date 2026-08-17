//
//  TipPromptManager.swift
//  Inkwell
//
//  Manages an occasional in-app tip prompt asking the user to support
//  development via Ko-fi. The prompt appears after a few app launches
//  and is suppressed for a cooldown period after dismissal.
//

import Foundation
import SwiftUI
import Observation

@MainActor
@Observable
final class TipPromptManager {
    static let shared = TipPromptManager()

    private let defaults = UserDefaults.standard
    private let launchCountKey = "tipPrompt.launchCount"
    private let lastShownKey = "tipPrompt.lastShownDate"

    private init() {}

    var shouldShowTip: Bool {
        let count = launchCount
        let lastShown = lastShownDate?.timeIntervalSince1970 ?? -1
        let now = Date().timeIntervalSince1970

        return Inkwell.shouldShowTip(
            launchCount: count,
            lastShownEpochMillis: Int64(lastShown),
            nowEpochMillis: Int64(now)
        )
    }

    func recordLaunch() {
        let current = launchCount
        defaults.set(current + 1, forKey: launchCountKey)
    }

    func markShown() {
        defaults.set(Date(), forKey: lastShownKey)
    }

    // MARK: - Private

    private var launchCount: Int {
        defaults.integer(forKey: launchCountKey)
    }

    private var lastShownDate: Date? {
        defaults.object(forKey: lastShownKey) as? Date
    }
}
