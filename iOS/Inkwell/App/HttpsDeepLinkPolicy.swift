import Foundation

/// Recognizes Inkwell's HTTPS universal-link hand-off
/// (`https://inkwell.ewancroft.uk/open?uri=<AT-URI>`) — separate from both the trusted
/// `inkwell://document` custom scheme (``ContentDeepLinkPolicy``) and the OAuth callback.
///
/// A URL arriving here is only an *unverified claim* about a document AT-URI. Unlike the
/// custom scheme — used for internal routing and notifications, where Inkwell itself is
/// the source of the AT-URI — an `https://` universal link can be authored and shared by
/// anyone, including a link claiming an AT-URI the domain never actually published.
/// ``VerifiedDeepLinkResolver`` must confirm the claim against the author's own PDS record
/// and published discovery link before it's safe to route into the Reader.
enum HttpsDeepLinkPolicy {
    static let host = "inkwell.ewancroft.uk"
    static let openPath = "/open"

    /// Extracts the unverified candidate document AT-URI from a universal-link URL, or
    /// nil if the URL doesn't match Inkwell's hand-off host/path/shape at all.
    static func candidateDocumentURI(from url: URL) -> String? {
        guard url.scheme?.lowercased() == "https",
              url.host?.lowercased() == host,
              isOpenPath(url.path),
              let value = URLComponents(url: url, resolvingAgainstBaseURL: false)?
                  .queryItems?.first(where: { $0.name == "uri" })?.value,
              let parsed = parseAtUri(value),
              parsed.collection == SiteStandardLexicon.DocumentRecord.type else {
            return nil
        }
        return value
    }

    private static func isOpenPath(_ path: String) -> Bool {
        path == openPath || path.hasPrefix(openPath + "/")
    }
}
