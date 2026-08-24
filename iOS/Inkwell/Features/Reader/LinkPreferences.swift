//
//  LinkPreferences.swift
//  Inkwell
//
//  Governs how content links (article body links, post/website preview
//  cards, discover results) open -- in-app via SFSafariViewController, or
//  handed off to the system browser. Deliberately separate from Settings'
//  own legal/notification links and the OAuth login flow, which always use
//  the system browser / CustomTabs regardless of this preference.
//

import SwiftUI
import Observation

@MainActor
@Observable
final class LinkPreferences {
    static let shared = LinkPreferences()

    private let defaults = UserDefaults.standard
    private let openLinksInAppKey = "links.openInApp"

    var openLinksInApp: Bool {
        didSet { defaults.set(openLinksInApp, forKey: openLinksInAppKey) }
    }

    private init() {
        openLinksInApp = defaults.object(forKey: openLinksInAppKey) as? Bool ?? true
    }
}
