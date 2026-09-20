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
