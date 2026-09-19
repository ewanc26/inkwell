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
        pendingDocumentURI = documentURI
    }

    func consumePendingDocumentURI() -> String? {
        defer { pendingDocumentURI = nil }
        return pendingDocumentURI
    }
}
