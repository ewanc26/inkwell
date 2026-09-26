import Foundation
import UIKit

/// Resolves an unverified HTTPS hand-off candidate (see ``HttpsDeepLinkPolicy``) into a
/// routable document only once the shared verification logic confirms the document's own
/// PDS record — and its author's published discovery link — actually agrees with the
/// claimed AT-URI. This is the trust boundary the `inkwell://document` custom scheme and
/// the OAuth callback intent don't need to cross, since an `https://` link can be
/// constructed and shared by anyone, not just Inkwell itself.
///
/// On any failure (malformed record, unreachable PDS, mismatched/missing discovery link),
/// this falls back to opening the original link in the system browser rather than ever
/// routing an unverified URL into the app's authenticated Reader.
@MainActor
enum VerifiedDeepLinkResolver {
    static func resolve(
        candidateDocumentURI: String,
        originalURL: URL,
        loginStateManager: LoginStateManager,
        notificationNavigation: NotificationNavigationCoordinator
    ) async {
        guard await isVerified(candidateDocumentURI, loginStateManager: loginStateManager) else {
            await UIApplication.shared.open(originalURL, options: [:])
            return
        }
        notificationNavigation.enqueue(documentURI: candidateDocumentURI)
    }

    private static func isVerified(
        _ documentURI: String,
        loginStateManager: LoginStateManager
    ) async -> Bool {
        do {
            let document = try await loginStateManager.fetchDocument(uri: documentURI)

            var publication: SiteStandardLexicon.PublicationRecord?
            if let parsedSite = parseAtUri(document.record.site),
               parsedSite.collection == SiteStandardLexicon.PublicationRecord.type {
                publication = try? await loginStateManager.fetchPublication(uri: document.record.site).record
            }

            try await SiteStandardLexicon.Verification.verify(
                documentURI: documentURI,
                document: document.record,
                publication: publication
            )
            return true
        } catch {
            return false
        }
    }
}
