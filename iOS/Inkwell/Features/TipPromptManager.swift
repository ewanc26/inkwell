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
        // TipPromptPolicy.shouldShowTip expects millisecond epoch values (it
        // divides the difference by 1000*60*60*24 to get days) — passing
        // timeIntervalSince1970 (seconds) directly here made the computed
        // day-diff 1000x too small, so the 7-day cooldown effectively never
        // re-triggered once it had fired once.
        let lastShown = lastShownDate.map { $0.timeIntervalSince1970 * 1000 } ?? -1
        let now = Date().timeIntervalSince1970 * 1000

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
