//
//  SiteStandardVerification.swift
//  Inkwell
//
//  Created by Ewan Croft on 20/06/2026.
//

import Foundation

extension SiteStandardLexicon {

    /// Verifies that a Standard.site record actually belongs to the domain it claims to.
    ///
    /// Since publication/document records reference web pages, anyone could in principle
    /// publish a record claiming someone else's domain. Standard.site's answer is a
    /// `.well-known` endpoint (publications) and an HTML `<link>` tag (documents) that the
    /// domain itself serves, pointing back at the AT-URI. A record should only be treated as
    /// trustworthy once that round-trip checks out.
    ///
    /// - SeeAlso: [standard.site/docs/verification](https://standard.site/docs/verification/)
    public enum Verification {

        /// The reason a publication failed verification.
        public enum Failure: Error, LocalizedError {

            /// The publication's `url` couldn't be turned into a request URL.
            case invalidPublicationURL(String)

            /// The `.well-known` endpoint didn't return a successful response.
            case endpointUnreachable(statusCode: Int?)

            /// The endpoint responded, but the body wasn't a usable AT-URI.
            case malformedResponse

            /// The endpoint's AT-URI didn't match the publication record being verified.
            case mismatchedURI(expected: String, found: String)

            /// The canonical document page did not contain the required link relation.
            case documentLinkMissing(expected: String)

            public var errorDescription: String? {
                switch self {
                    case .invalidPublicationURL(let url):
                        return "\"\(url)\" isn't a valid publication URL."
                    case .endpointUnreachable(let statusCode):
                        if let statusCode {
                            return "The .well-known endpoint returned status \(statusCode)."
                        }
                        return "The .well-known endpoint couldn't be reached."
                    case .malformedResponse:
                        return "The .well-known endpoint's response wasn't a usable AT-URI."
                    case .mismatchedURI(let expected, let found):
                        return "Expected \(expected) but the domain's .well-known endpoint points to \(found)."
                    case .documentLinkMissing(let expected):
                        return "The document page doesn't link back to \(expected)."
                }
            }
        }

        /// Verifies a publication by fetching its `.well-known` endpoint and confirming it
        /// points back at the given AT-URI.
        ///
        /// - Parameters:
        ///   - publicationURI: The publication record's own AT-URI
        ///   (ex: `at://did:plc:abc123/site.standard.publication/rkey`), as claimed by
        ///   whoever's surfacing it (a firehose consumer, a reader feature, etc.) — *not*
        ///   something read out of the publication record itself, since that would just be
        ///   trusting the thing being verified.
        ///   - publication: The publication record being verified, used for its `url`.
        ///
        /// - Throws: ``Failure`` if the domain doesn't confirm ownership of `publicationURI`,
        /// or a networking error if the endpoint can't be reached at all.
        public static func verify(
            publicationURI: String,
            publication: SiteStandardLexicon.PublicationRecord
        ) async throws {
            guard let endpoint = publicationVerificationURL(for: publication.url) else {
                throw Failure.invalidPublicationURL(publication.url)
            }

            let (data, response) = try await URLSession.shared.data(from: endpoint)

            guard let httpResponse = response as? HTTPURLResponse, (200...299).contains(httpResponse.statusCode) else {
                throw Failure.endpointUnreachable(statusCode: (response as? HTTPURLResponse)?.statusCode)
            }

            guard let foundURI = String(data: data, encoding: .utf8)?.trimmingCharacters(in: .whitespacesAndNewlines),
                  foundURI.hasPrefix("at://") else {
                throw Failure.malformedResponse
            }

            guard foundURI == publicationURI else {
                throw Failure.mismatchedURI(expected: publicationURI, found: foundURI)
            }
        }

        /// Builds the verification endpoint, including the publication path for
        /// non-root publications as required by the Standard.site specification.
        static func publicationVerificationURL(for publicationURL: String) -> URL? {
            guard var components = URLComponents(string: publicationURL),
                  components.scheme?.lowercased() == "https",
                  components.host != nil else {
                return nil
            }

            let publicationPath = components.path
                .split(separator: "/")
                .map(String.init)
                .joined(separator: "/")
            components.path = "/.well-known/site.standard.publication"
            if !publicationPath.isEmpty {
                components.path += "/\(publicationPath)"
            }
            components.query = nil
            components.fragment = nil
            return components.url
        }

        /// Verifies a document page by checking for its required
        /// `<link rel="site.standard.document" href="at://...">` element.
        public static func verify(
            documentURI: String,
            document: SiteStandardLexicon.DocumentRecord,
            publication: SiteStandardLexicon.PublicationRecord? = nil
        ) async throws {
            guard let url = document.canonicalURL(publication: publication) else {
                throw Failure.invalidPublicationURL(document.site)
            }

            let (data, response) = try await URLSession.shared.data(from: url)
            guard let httpResponse = response as? HTTPURLResponse,
                  (200...299).contains(httpResponse.statusCode) else {
                throw Failure.endpointUnreachable(statusCode: (response as? HTTPURLResponse)?.statusCode)
            }
            guard let html = String(data: data, encoding: .utf8) else {
                throw Failure.malformedResponse
            }

            let escapedURI = NSRegularExpression.escapedPattern(for: documentURI)
            let patterns = [
                #"<link\b[^>]*\brel\s*=\s*[\"']site\.standard\.document[\"'][^>]*\bhref\s*=\s*[\"']"# + escapedURI + #"[\"'][^>]*>"#,
                #"<link\b[^>]*\bhref\s*=\s*[\"']"# + escapedURI + #"[\"'][^>]*\brel\s*=\s*[\"']site\.standard\.document[\"'][^>]*>"#
            ]
            let range = NSRange(html.startIndex..<html.endIndex, in: html)
            let found = patterns.contains { pattern in
                guard let expression = try? NSRegularExpression(pattern: pattern, options: [.caseInsensitive]) else {
                    return false
                }
                return expression.firstMatch(in: html, range: range) != nil
            }
            guard found else {
                throw Failure.documentLinkMissing(expected: documentURI)
            }
        }

        /// Builds the AT-URI discovery `<link>` tag Inkwell would emit if it ever serves a
        /// document's content as a web page (ex: a "view in browser" export).
        ///
        /// This is a *hint* only, per the spec — readers should still confirm via
        /// ``verify(publicationURI:publication:)`` rather than trusting this tag alone.
        ///
        /// - Parameters:
        ///   - recordURI: The AT-URI of the `site.standard.publication` or
        ///   `site.standard.document` record.
        ///   - relation: Either `"site.standard.publication"` or `"site.standard.document"`,
        ///   matching the kind of record `recordURI` points to.
        /// - Returns: A `<link>` tag suitable for a document's `<head>`.
        public static func discoveryLinkTag(forRecordURI recordURI: String, relation: String) -> String {
            "<link rel=\"\(relation)\" href=\"\(recordURI)\" />"
        }
    }
}
