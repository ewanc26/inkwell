import Foundation
import Observation

enum ModerationLabelMode: String, CaseIterable, Identifiable {
    case show
    case warn
    case hide

    var id: Self { self }

    var title: String {
        switch self {
        case .show: "Show"
        case .warn: "Warn"
        case .hide: "Hide"
        }
    }
}

@MainActor
@Observable
final class ModerationSettings {
    static let shared = ModerationSettings()

    private let defaults = UserDefaults.standard
    private let hiddenLabelsKey = "moderation.hiddenLabels"
    private let warningLabelsKey = "moderation.warningLabels"
    private let customLabelsKey = "moderation.customLabels"
    private let knownLabelersKey = "moderation.knownLabelers"
    private let disabledLabelersKey = "moderation.disabledLabelers"
    private let hiddenKeywordsKey = "moderation.hiddenKeywords"

    var hiddenLabels: Set<String> {
        didSet {
            defaults.set(Array(hiddenLabels), forKey: hiddenLabelsKey)
            NotificationCenter.default.post(name: .moderationSettingsChanged, object: nil)
        }
    }

    var hiddenKeywords: Set<String> {
        didSet {
            defaults.set(Array(hiddenKeywords), forKey: hiddenKeywordsKey)
            NotificationCenter.default.post(name: .moderationSettingsChanged, object: nil)
        }
    }

    var warningLabels: Set<String> {
        didSet {
            defaults.set(Array(warningLabels), forKey: warningLabelsKey)
            announceChange()
        }
    }

    var customLabels: Set<String> {
        didSet {
            defaults.set(Array(customLabels), forKey: customLabelsKey)
            announceChange()
        }
    }

    var knownLabelers: Set<String> {
        didSet {
            defaults.set(Array(knownLabelers), forKey: knownLabelersKey)
            announceChange()
        }
    }

    var disabledLabelers: Set<String> {
        didSet {
            defaults.set(Array(disabledLabelers), forKey: disabledLabelersKey)
            announceChange()
        }
    }

    private init() {
        hiddenLabels = Set(defaults.stringArray(forKey: hiddenLabelsKey) ?? [])
        hiddenKeywords = Set(defaults.stringArray(forKey: hiddenKeywordsKey) ?? [])
        warningLabels = Set(defaults.stringArray(forKey: warningLabelsKey) ?? [])
        customLabels = Set(defaults.stringArray(forKey: customLabelsKey) ?? [])
        knownLabelers = Set(defaults.stringArray(forKey: knownLabelersKey) ?? [])
        disabledLabelers = Set(defaults.stringArray(forKey: disabledLabelersKey) ?? [])
    }

    func hides(label: String) -> Bool {
        hiddenLabels.contains(label.lowercased())
    }

    func setHidden(_ hidden: Bool, for label: String) {
        let normalized = normalized(label)
        if hidden { hiddenLabels.insert(normalized) }
        else { hiddenLabels.remove(normalized) }
    }

    func labelMode(for label: String) -> ModerationLabelMode {
        let normalized = normalized(label)
        if hiddenLabels.contains(normalized) { return .hide }
        if warningLabels.contains(normalized) { return .warn }
        return .show
    }

    func setLabelMode(_ mode: ModerationLabelMode, for label: String) {
        let normalized = normalized(label)
        hiddenLabels.remove(normalized)
        warningLabels.remove(normalized)
        switch mode {
        case .show:
            break
        case .warn:
            warningLabels.insert(normalized)
        case .hide:
            hiddenLabels.insert(normalized)
        }
    }

    func addCustomLabel(_ label: String) {
        guard let normalized = normalizedOrNil(label) else { return }
        customLabels.insert(normalized)
    }

    func removeCustomLabel(_ label: String) {
        guard let normalized = normalizedOrNil(label) else { return }
        customLabels.remove(normalized)
    }

    func isLabelerEnabled(_ labeler: String) -> Bool {
        !disabledLabelers.contains(normalized(labeler))
    }

    func setLabelerEnabled(_ enabled: Bool, for labeler: String) {
        guard let normalized = normalizedOrNil(labeler) else { return }
        knownLabelers.insert(normalized)
        if enabled { disabledLabelers.remove(normalized) }
        else { disabledLabelers.insert(normalized) }
    }

    func removeLabeler(_ labeler: String) {
        guard let normalized = normalizedOrNil(labeler) else { return }
        knownLabelers.remove(normalized)
        disabledLabelers.remove(normalized)
    }

    func addKeyword(_ keyword: String) {
        guard let normalized = normalizedOrNil(keyword) else { return }
        hiddenKeywords.insert(normalized)
    }

    private func normalized(_ value: String) -> String {
        value.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
    }

    private func normalizedOrNil(_ value: String) -> String? {
        let normalized = normalized(value)
        return normalized.isEmpty ? nil : normalized
    }

    private func announceChange() {
        NotificationCenter.default.post(name: .moderationSettingsChanged, object: nil)
    }
}

extension Notification.Name {
    static let moderationSettingsChanged = Notification.Name("moderationSettingsChanged")
}
