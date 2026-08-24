//
//  ReaderSortSettings.swift
//  Inkwell
//
//  Persisted preference for the order reader feed items are shown in.
//  Mirrors AccessibilitySettings.swift / CustomisationSettings.swift's
//  @Observable-singleton-backed-by-UserDefaults pattern.
//

import SwiftUI
import Observation

enum ReaderSortOrder: String {
    case newestFirst
    case oldestFirst
}

@MainActor
@Observable
final class ReaderSortSettings {
    static let shared = ReaderSortSettings()

    private let defaults = UserDefaults.standard
    private let sortOrderKey = "reader.sortOrder"

    var sortOrder: ReaderSortOrder {
        didSet { defaults.set(sortOrder.rawValue, forKey: sortOrderKey) }
    }

    private init() {
        sortOrder = (defaults.string(forKey: sortOrderKey)).flatMap(ReaderSortOrder.init(rawValue:)) ?? .newestFirst
    }
}
