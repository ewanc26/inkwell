import Foundation

enum OAuthIssuerPolicy {
    nonisolated static func sameHTTPSOrigin(_ lhs: String, _ rhs: String) -> Bool {
        guard let left = normalizedOrigin(lhs), let right = normalizedOrigin(rhs) else { return false }
        return left == right
    }

    private nonisolated static func normalizedOrigin(_ raw: String) -> String? {
        guard var components = URLComponents(string: raw),
              components.scheme?.lowercased() == "https",
              let host = components.host?.lowercased(),
              !host.isEmpty,
              components.user == nil,
              components.password == nil,
              components.query == nil,
              components.fragment == nil,
              components.path.isEmpty || components.path == "/" else { return nil }

        components.scheme = "https"
        components.host = host
        components.path = ""
        if components.port == 443 { components.port = nil }
        return components.string?.lowercased()
    }
}
