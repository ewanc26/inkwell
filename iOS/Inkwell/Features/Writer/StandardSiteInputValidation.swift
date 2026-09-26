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
