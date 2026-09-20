import Foundation

enum IframeSecurityPolicy {
    static func isAllowedInitial(_ url: URL) -> Bool {
        url.scheme?.lowercased() == "https" && url.host != nil
    }

    static func isAllowedNavigation(from origin: URL, to candidate: URL) -> Bool {
        guard isAllowedInitial(candidate),
              origin.scheme?.lowercased() == candidate.scheme?.lowercased(),
              origin.host?.lowercased() == candidate.host?.lowercased() else {
            return false
        }
        return effectivePort(origin) == effectivePort(candidate)
    }

    private static func effectivePort(_ url: URL) -> Int {
        url.port ?? 443
    }
}
