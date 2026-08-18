//
//  LoginError.swift
//  Inkwell
//

import Foundation

// MARK: - Error

enum LoginError: LocalizedError {
    case notAuthenticated
    case invalidURI
    case unexpectedRecordType
    case contentConversionFailed
    case pdsResolutionFailed
    case httpError(status: Int)

    var errorDescription: String? {
        switch self {
        case .notAuthenticated:
            return "Not authenticated. Please sign in."
        case .invalidURI:
            return "The provided AT-URI is not valid for this operation."
        case .unexpectedRecordType:
            return "The record is not the expected type."
        case .contentConversionFailed:
            return "Failed to convert content to the output format."
        case .pdsResolutionFailed:
            return "Could not resolve the repository's PDS. Check that the DID or handle is correct."
        case .httpError(let status):
            return httpErrorMessage(status: status)
        }
    }

    /// Maps AT Protocol HTTP status codes to user-friendly messages.
    /// See https://atproto.com/specs/xrpc#error-responses
    private func httpErrorMessage(status: Int) -> String {
        switch status {
        case 400:
            return "Bad request. The server could not understand the request."
        case 401:
            return "Authentication required. Your session may have expired — sign in again."
        case 403:
            return "Access denied. You don't have permission to view this content."
        case 404:
            return "Not found. The record, repository, or endpoint does not exist."
        case 408:
            return "Request timed out. The PDS took too long to respond."
        case 429:
            return "Rate limited. The PDS is throttling requests — try again shortly."
        case 500, 502, 503, 504:
            return "The PDS server encountered an error (HTTP \(status)). It may be temporarily unavailable."
        default:
            return "Unexpected server response (HTTP \(status))."
        }
    }
}

extension URLError {
    /// True for transient network errors worth retrying (timeouts, DNS,
    /// connection lost, cannot connect). False for permanent errors like
    /// bad URLs, cancelled requests, or authentication failures.
    var isTransient: Bool {
        switch code {
        case .timedOut, .cannotFindHost, .cannotConnectToHost,
                .networkConnectionLost, .dnsLookupFailed,
                .notConnectedToInternet, .resourceUnavailable,
                .secureConnectionFailed:
            return true
        default:
            return false
        }
    }
}

/// A lock-protected box for sharing mutable state between `@Sendable`
/// closures — e.g. the `URLResponseProvider` and `DPoPSigner.JWTGenerator`
/// closures passed to `Authenticator`, which run concurrently and can't
/// otherwise share a plain `var`.
nonisolated final class LockedBox<Value>: @unchecked Sendable {
    private let lock = NSLock()
    private var _value: Value

    init(_ value: Value) {
        self._value = value
    }

    var value: Value {
        get { lock.lock(); defer { lock.unlock() }; return _value }
        set { lock.lock(); defer { lock.unlock() }; _value = newValue }
    }
}
