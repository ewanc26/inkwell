import Foundation

enum StandardSiteInputValidation {
    static func firstDocumentError(site: String, title: String, description: String?, path: String?) -> String? {
        if site.isEmpty { return "site: Site is required" }
        if let error = textError(field: "title", value: title, graphemes: 500, bytes: 5_000) { return error }
        if let description, let error = textError(field: "description", value: description, graphemes: 3_000, bytes: 30_000) { return error }
        if let path, !path.isEmpty, (!path.hasPrefix("/") || path.hasSuffix("/")) {
            return "path: Path must start with / and not end with /"
        }
        return nil
    }

    /// Mirrors shared `StandardSiteValidation.validateMetadata`, which the
    /// checked-in `InkwellShared.xcframework` predates. Switch to the shared
    /// call once the framework is rebuilt.
    static func firstMetadataError(tags: [String], contributors: [ContributorDraft], bskyPostURI: String) -> String? {
        for (index, tag) in tags.enumerated() {
            if let error = textError(field: "tags[\(index)]", value: tag, graphemes: 128, bytes: 1_280) { return error }
        }
        for (index, contributor) in contributors.enumerated() {
            let did = contributor.did.trimmingCharacters(in: .whitespacesAndNewlines)
            if did.isEmpty { return "contributors[\(index)].did: Contributor DID is required" }
            if !did.hasPrefix("did:") {
                return "contributors[\(index)].did: Contributor must be a DID (did:plc:… or did:web:…)"
            }
            if let error = textError(field: "contributors[\(index)].role", value: contributor.role, graphemes: 100, bytes: 1_000) { return error }
            if let error = textError(field: "contributors[\(index)].displayName", value: contributor.displayName, graphemes: 100, bytes: 1_000) { return error }
        }
        let uri = bskyPostURI.trimmingCharacters(in: .whitespacesAndNewlines)
        if !uri.isEmpty, !uri.hasPrefix("at://") {
            return "bskyPostRef: Bluesky post reference must be an at:// URI"
        }
        return nil
    }

    static func firstPublicationError(url: String, name: String, description: String?) -> String? {
        guard URL(string: url)?.scheme?.lowercased() == "https" else { return "url: Publication URL must use HTTPS" }
        if let error = textError(field: "name", value: name, graphemes: 500, bytes: 5_000) { return error }
        if let description, let error = textError(field: "description", value: description, graphemes: 3_000, bytes: 30_000) { return error }
        return nil
    }

    private static func textError(field: String, value: String, graphemes: Int, bytes: Int) -> String? {
        if value.count > graphemes { return "\(field): Must be at most \(graphemes) characters" }
        if value.utf8.count > bytes { return "\(field): Must be at most \(bytes) UTF-8 bytes" }
        return nil
    }
}
