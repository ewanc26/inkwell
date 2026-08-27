import Foundation
import Observation

@MainActor
@Observable
final class ModerationSettings {
    static let shared = ModerationSettings()

    private let defaults = UserDefaults.standard
    private let hiddenLabelsKey = "moderation.hiddenLabels"
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

    private init() {
        hiddenLabels = Set(defaults.stringArray(forKey: hiddenLabelsKey) ?? [])
        hiddenKeywords = Set(defaults.stringArray(forKey: hiddenKeywordsKey) ?? [])
    }

    func hides(label: String) -> Bool {
        hiddenLabels.contains(label.lowercased())
    }

    func setHidden(_ hidden: Bool, for label: String) {
        let normalized = label.lowercased()
        if hidden { hiddenLabels.insert(normalized) }
        else { hiddenLabels.remove(normalized) }
    }

    func addKeyword(_ keyword: String) {
        let normalized = keyword.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        guard !normalized.isEmpty else { return }
        hiddenKeywords.insert(normalized)
    }
}

extension Notification.Name {
    static let moderationSettingsChanged = Notification.Name("moderationSettingsChanged")
}
