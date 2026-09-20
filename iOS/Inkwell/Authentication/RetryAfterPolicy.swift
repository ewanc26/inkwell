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
        return min(Double(1 << min(max(attempt, 0), 9)) * 0.1, maxDelay)
    }
}
