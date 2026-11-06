//
//  NotificationNavigationCoordinator.swift
//  Inkwell
//

import Observation

/// App-lifetime hand-off for notification taps that arrive before the reader
/// navigation stack has been mounted (notably during a cold launch).
@MainActor
@Observable
final class NotificationNavigationCoordinator {
    static let shared = NotificationNavigationCoordinator()

    private(set) var pendingDocumentURI: String?

    /// With the app's default actor isolation set to `MainActor`, the compiler
    /// synthesises an isolated deinit that hops through the runtime's
    /// back-deployed executor shim. Deallocating this class synchronously from
    /// an XCTest case aborts there with "pointer being freed was not allocated"
    /// (swiftlang/swift#87316, fixed in Swift 6.3). Nothing runs at deinit, so
    /// keep it nonisolated until the toolchain floor moves past the fix.
    nonisolated deinit {}

    func enqueue(documentURI: String) {
        guard parseAtUri(documentURI)?.collection == SiteStandardLexicon.DocumentRecord.type else {
            return
        }
        pendingDocumentURI = documentURI
    }

    func consumePendingDocumentURI() -> String? {
        defer { pendingDocumentURI = nil }
        return pendingDocumentURI
    }
}
