import Foundation

enum ContentDeepLinkPolicy {
    static func documentURI(from url: URL) -> String? {
        guard url.scheme?.lowercased() == "inkwell",
              url.host?.lowercased() == "document",
              let value = URLComponents(url: url, resolvingAgainstBaseURL: false)?
                .queryItems?.first(where: { $0.name == "uri" })?.value,
              let parsed = parseAtUri(value),
              parsed.collection == SiteStandardLexicon.DocumentRecord.type else {
            return nil
        }
        return value
    }
}
