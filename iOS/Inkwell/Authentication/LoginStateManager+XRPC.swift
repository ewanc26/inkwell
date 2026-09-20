//
//  LoginStateManager+XRPC.swift
//  Inkwell
//

import Foundation
import OSLog
import OAuthenticator
import ATResolve

private struct RetryableHTTPError: Error {
    let retryAfter: TimeInterval?
}

private enum XRPCResponseLimits {
    nonisolated static let maxResponseBytes = 2 * 1024 * 1024
}

internal func didDocumentURL(for did: String) -> URL? {
    if did.hasPrefix("did:plc:") {
        var components = URLComponents()
        components.scheme = "https"
        components.host = "plc.directory"
        components.path = "/\(did)"
        return components.url
    }
    guard did.hasPrefix("did:web:") else { return nil }
    let parts = String(did.dropFirst("did:web:".count)).split(separator: ":", omittingEmptySubsequences: false)
    guard let first = parts.first, !first.isEmpty,
          !parts.contains(where: { $0.isEmpty || $0 == "." || $0 == ".." }) else { return nil }
    let host = String(first).removingPercentEncoding ?? String(first)
    guard URL(string: "https://\(host)")?.host != nil else { return nil }
    let path = parts.count == 1
        ? "/.well-known/did.json"
        : "/\(parts.dropFirst().joined(separator: "/"))/did.json"
    return URL(string: "https://\(host)\(path)")
}

internal func atprotoPDSURL(from document: [String: Any], did: String) -> URL? {
    guard let services = document["service"] as? [[String: Any]] else { return nil }
    for service in services {
        let id = service["id"] as? String
        guard (id == "#atproto_pds" || id == "\(did)#atproto_pds"),
              service["type"] as? String == "AtprotoPersonalDataServer",
              let endpoint = service["serviceEndpoint"] as? String,
              let url = URL(string: endpoint),
              url.scheme?.lowercased() == "https",
              url.user == nil, url.query == nil, url.fragment == nil else { continue }
        return url
    }
    return nil
}

extension LoginStateManager {
    // MARK: - XRPC Helpers

    /// Makes an authenticated request to the user's PDS.
    ///
    /// - Parameters:
    ///   - path: The XRPC path (e.g. `/xrpc/com.atproto.repo.listRecords`).
    ///   - method: The HTTP method.
    ///   - body: Optional JSON-encoded request body.
    ///   - queryItems: Optional URL query parameters.
    ///   - proxy: Optional AT Protocol service proxy identifier for RPCs whose
    ///     audience is a service such as the Bluesky AppView.
    /// - Returns: The response data.
    func authenticatedData(
        path: String,
        method: String = "GET",
        body: Data? = nil,
        queryItems: [URLQueryItem]? = nil,
        proxy: String? = nil,
        expectJSON: Bool = true,
        maxResponseBytes: Int = XRPCResponseLimits.maxResponseBytes
    ) async throws -> Data {
        guard let authenticator, let pdsURL = resolvedPDSURL else {
            throw LoginError.notAuthenticated
        }

        var components = URLComponents(
            url: pdsURL.appendingPathComponent(path),
            resolvingAgainstBaseURL: false
        )
        if let queryItems, !queryItems.isEmpty {
            components?.queryItems = queryItems
        }
        guard let url = components?.url else {
            throw URLError(.badURL)
        }
        let origin = RetryAfterPolicy.origin(for: url)

        var request = URLRequest(url: url)
        request.httpMethod = method
        if let body {
            request.setValue("application/json", forHTTPHeaderField: "Content-Type")
            request.httpBody = body
        }
        if let proxy {
            request.setValue(proxy, forHTTPHeaderField: "atproto-proxy")
        }
        request.setValue("application/json", forHTTPHeaderField: "Accept")

        let isIdempotentRead = method.caseInsensitiveCompare("GET") == .orderedSame
            || method.caseInsensitiveCompare("HEAD") == .orderedSame
        let (data, response) = try await withRetry {
            try await RateLimitCoordinator.shared.wait(for: origin)
            let result = try await authenticator.response(for: request)
            if isIdempotentRead,
               let http = result.1 as? HTTPURLResponse,
               http.statusCode == 429 {
                let delay = Self.retryAfter(from: http)
                await RateLimitCoordinator.shared.record(origin: origin, delay: delay ?? 0)
                throw RetryableHTTPError(retryAfter: delay)
            }
            return result
        }

        guard let http = response as? HTTPURLResponse,
              (200...299).contains(http.statusCode) else {
            let status = (response as? HTTPURLResponse)?.statusCode ?? 0
            logger.error("[authenticatedData] HTTP \(status)")
            throw LoginError.httpError(status: status)
        }

        if http.expectedContentLength > Int64(maxResponseBytes) {
            logger.error("[authenticatedData] advertised response exceeded safety budget")
            throw URLError(.dataLengthExceedsMaximum)
        }

        guard data.count <= maxResponseBytes else {
            logger.error("[authenticatedData] response exceeded safety budget")
            throw URLError(.dataLengthExceedsMaximum)
        }
        if expectJSON {
            try JSONSafety.validate(data)
        }
        return data
    }

    /// Makes an unauthenticated request to a remote PDS (for public records).
    func unauthenticatedData(
        pdsURL: URL,
        path: String,
        method: String = "GET",
        body: Data? = nil,
        queryItems: [URLQueryItem]? = nil,
        expectJSON: Bool = true,
        maxResponseBytes: Int = XRPCResponseLimits.maxResponseBytes
    ) async throws -> Data {
        var components = URLComponents(
            url: pdsURL.appendingPathComponent(path),
            resolvingAgainstBaseURL: false
        )
        if let queryItems, !queryItems.isEmpty {
            components?.queryItems = queryItems
        }
        guard let url = components?.url else {
            throw URLError(.badURL)
        }
        let origin = RetryAfterPolicy.origin(for: url)

        var request = URLRequest(url: url)
        request.httpMethod = method
        request.timeoutInterval = 8
        if let body {
            request.setValue("application/json", forHTTPHeaderField: "Content-Type")
            request.httpBody = body
        }
        request.setValue("application/json", forHTTPHeaderField: "Accept")

        let (data, response) = try await withRetry {
            try await RateLimitCoordinator.shared.wait(for: origin)
            let result = try await URLSession.shared.data(for: request)
            if let http = result.1 as? HTTPURLResponse, http.statusCode == 429 {
                let delay = Self.retryAfter(from: http)
                await RateLimitCoordinator.shared.record(origin: origin, delay: delay ?? 0)
                throw RetryableHTTPError(retryAfter: delay)
            }
            return result
        }

        guard let http = response as? HTTPURLResponse,
              (200...299).contains(http.statusCode) else {
            let status = (response as? HTTPURLResponse)?.statusCode ?? 0
            logger.error("[unauthenticatedData] HTTP \(status)")
            throw LoginError.httpError(status: status)
        }

        if http.expectedContentLength > Int64(maxResponseBytes) {
            logger.error("[unauthenticatedData] advertised response exceeded safety budget")
            throw URLError(.dataLengthExceedsMaximum)
        }

        guard data.count <= maxResponseBytes else {
            logger.error("[unauthenticatedData] response exceeded safety budget")
            throw URLError(.dataLengthExceedsMaximum)
        }
        if expectJSON {
            try JSONSafety.validate(data)
        }
        return data
    }

    // MARK: - Retry

    /// Retries an async operation with exponential backoff.
    ///
    /// Uses jittered exponential backoff (100ms → 200ms → 400ms → 800ms)
    /// for transient network errors. Non-retryable errors (e.g. 401, 403,
    /// invalid URIs) are rethrown immediately.
    func withRetry<T>(
        maxAttempts: Int = 4,
        operation: () async throws -> T
    ) async throws -> T {
        var attempt = 0
        var lastError: Error?

        while attempt < maxAttempts {
            do {
                return try await operation()
            } catch let error as URLError where error.isTransient {
                attempt += 1
                lastError = error
                guard attempt < maxAttempts else { throw error }
                let delay = Double(1 << min(attempt, 4)) * 0.1  // 0.1, 0.2, 0.4, 0.8s
                try await Task.sleep(for: .seconds(delay))
            } catch LoginError.httpError(let status) where (500...599).contains(status) {
                attempt += 1
                lastError = LoginError.httpError(status: status)
                guard attempt < maxAttempts else { throw lastError! }
                let delay = Double(1 << min(attempt, 4)) * 0.1
                try await Task.sleep(for: .seconds(delay))
            } catch let error as RetryableHTTPError {
                attempt += 1
                lastError = LoginError.httpError(status: 429)
                guard attempt < maxAttempts else { throw lastError! }
                let fallback = RetryAfterPolicy.delay(for: nil, attempt: attempt)
                try await Task.sleep(for: .seconds(min(error.retryAfter ?? fallback, RetryAfterPolicy.maxDelay)))
            }
        }

        throw lastError ?? LoginError.httpError(status: 0)
    }

    private static func retryAfter(from response: HTTPURLResponse) -> TimeInterval? {
        guard let header = response.value(forHTTPHeaderField: "Retry-After"),
              !header.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            // Let withRetry select the attempt-based fallback when the server
            // does not provide a retry delay.
            return nil
        }
        return RetryAfterPolicy.delay(for: header, attempt: 0)
    }

    // MARK: - PDS Resolution

    /// Resolves the PDS URL for a given DID, caching the result.
    func repositoryPDSURL(for did: String) async throws -> URL {
        if let cached = repositoryPDSURLs[did] { return cached }

        // Own DID — use the stored PDS.
        if did == currentDID,
           let storedPDS = defaults.string(forKey: storedPDSKey),
           let url = URL(string: storedPDS) {
            repositoryPDSURLs[did] = url
            return url
        }

        if did.hasPrefix("did:") {
            // Resolve each DID method using its authoritative DID document.
            guard let documentURL = didDocumentURL(for: did) else {
                throw LoginError.pdsResolutionFailed
            }
            let (data, response) = try await URLSession.shared.data(from: documentURL)
            guard let http = response as? HTTPURLResponse,
                  (200...299).contains(http.statusCode) else {
                throw LoginError.pdsResolutionFailed
            }
            try JSONSafety.validateResponse(data)
            let doc = try JSONSerialization.jsonObject(with: data) as? [String: Any]
            guard let url = atprotoPDSURL(from: doc ?? [:], did: did) else {
                throw LoginError.pdsResolutionFailed
            }
            repositoryPDSURLs[did] = url
            return url
        }

        // Handle — resolve via ATResolve.
        let identity = try await resolver.resolveHandle(did)
        guard let pdsString = identity?.serviceEndpoint,
              let url = URL(string: pdsString) else {
            throw LoginError.pdsResolutionFailed
        }
        repositoryPDSURLs[did] = url
        return url
    }
}
