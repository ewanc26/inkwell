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

private let maxXRPCResponseBytes = 2 * 1024 * 1024

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
        proxy: String? = nil
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
        let origin = "\(url.scheme ?? "")://\(url.host ?? ""):\(url.port ?? 443)"

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

        guard data.count <= maxXRPCResponseBytes else {
            logger.error("[authenticatedData] response exceeded safety budget")
            throw URLError(.dataLengthExceedsMaximum)
        }
        return data
    }

    /// Makes an unauthenticated request to a remote PDS (for public records).
    func unauthenticatedData(
        pdsURL: URL,
        path: String,
        method: String = "GET",
        body: Data? = nil,
        queryItems: [URLQueryItem]? = nil
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
        let origin = "\(url.scheme ?? "")://\(url.host ?? ""):\(url.port ?? 443)"

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

        guard data.count <= maxXRPCResponseBytes else {
            logger.error("[unauthenticatedData] response exceeded safety budget")
            throw URLError(.dataLengthExceedsMaximum)
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
        RetryAfterPolicy.delay(for: response.value(forHTTPHeaderField: "Retry-After"), attempt: 0)
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
            // DID — fetch the DID document from the PLC directory.
            guard let plcURL = URL(string: "https://plc.directory/\(did)") else {
                throw LoginError.pdsResolutionFailed
            }
            let (data, _) = try await URLSession.shared.data(from: plcURL)
            let doc = try JSONSerialization.jsonObject(with: data) as? [String: Any]
            let services = doc?["service"] as? [[String: Any]]
            let atprotoService = services?.first(where: { svc in
                (svc["type"] as? String) == "AtprotoPersonalDataServer"
            })
            guard let pdsString = atprotoService?["serviceEndpoint"] as? String,
                  let url = URL(string: pdsString) else {
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
