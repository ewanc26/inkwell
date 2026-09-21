import Foundation

/// Maps delete failures to stable, user-actionable writer messages.
func writerDeleteErrorMessage(_ error: Error) -> String {
    let message = error.localizedDescription
    if message.localizedCaseInsensitiveContains("swap") {
        return "This document changed elsewhere. Reload it before deleting."
    }
    return "Failed to delete document: \(message)"
}

func writerEditErrorMessage(_ error: Error) -> String {
    let message = error.localizedDescription
    if message.localizedCaseInsensitiveContains("swap") {
        return "This document changed elsewhere. Reload it before saving."
    }
    return "Failed to update document: \(message)"
}
