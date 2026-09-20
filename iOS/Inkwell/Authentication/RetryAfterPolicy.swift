import Foundation

enum RetryAfterPolicy {
    static let maxDelay: TimeInterval = 60

    static func delay(for header: String?, attempt: Int, now: Date = Date()) -> TimeInterval {
        let value = header?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        if let seconds = TimeInterval(value), seconds >= 0 {
            return min(seconds, maxDelay)
        }
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.timeZone = TimeZone(secondsFromGMT: 0)
        formatter.dateFormat = "EEE, dd MMM yyyy HH:mm:ss z"
        if let date = formatter.date(from: value), date >= now {
            return min(date.timeIntervalSince(now), maxDelay)
        }
        return min(Double(1 << min(max(attempt, 0), 10)) * 0.1, maxDelay)
    }

    static func origin(for url: URL) -> String {
        let scheme = (url.scheme ?? "").lowercased()
        let host = (url.host ?? "").lowercased()
        let port = url.port ?? (scheme == "http" ? 80 : 443)
        return "\(scheme)://\(host):\(port)"
    }
}

/// Shares short-lived rate-limit cooldowns between concurrent requests to the
/// same service without blocking unrelated origins.
actor RateLimitCoordinator {
    static let shared = RateLimitCoordinator()

    private var cooldownUntil: [String: Date] = [:]

    func wait(for origin: String) async throws {
        guard let until = cooldownUntil[origin] else { return }
        let delay = until.timeIntervalSinceNow
        guard delay > 0 else {
            cooldownUntil[origin] = nil
            return
        }
        try await Task.sleep(for: .seconds(delay))
    }

    func record(origin: String, delay: TimeInterval) {
        let candidate = Date().addingTimeInterval(min(max(delay, 0), RetryAfterPolicy.maxDelay))
        if candidate > (cooldownUntil[origin] ?? .distantPast) {
            cooldownUntil[origin] = candidate
        }
    }
}
