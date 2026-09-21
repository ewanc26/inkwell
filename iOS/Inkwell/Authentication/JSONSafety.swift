import Foundation

enum JSONSafety {
    static let maxResponseBytes = 2 * 1024 * 1024
    static let maxDepth = 32
    static let maxContainerElements = 131_072
    static let maxStringBytes = 1_048_576

    /// Reads an untrusted JSON response incrementally so the byte budget is
    /// enforced while the body is arriving, rather than after URLSession has
    /// already buffered it in memory.
    static func boundedData(from url: URL, using session: URLSession) async throws -> (Data, URLResponse) {
        let (bytes, response) = try await session.bytes(from: url)
        var data = Data()
        data.reserveCapacity(min(response.expectedContentLength > 0 ? Int(response.expectedContentLength) : 0, maxResponseBytes))
        for try await byte in bytes {
            guard data.count < maxResponseBytes else {
                throw URLError(.dataLengthExceedsMaximum)
            }
            data.append(byte)
        }
        return (data, response)
    }

    static func validate(_ data: Data) throws {
        let object = try JSONSerialization.jsonObject(with: data, options: [.fragmentsAllowed])
        var containers = 0

        func visit(_ value: Any, depth: Int) throws {
            guard depth <= maxDepth else { throw URLError(.cannotParseResponse) }
            if let dictionary = value as? [String: Any] {
                containers += dictionary.count
                guard containers <= maxContainerElements else { throw URLError(.cannotParseResponse) }
                for (key, child) in dictionary {
                    guard key.utf8.count <= maxStringBytes else { throw URLError(.cannotParseResponse) }
                    try visit(child, depth: depth + 1)
                }
            } else if let array = value as? [Any] {
                containers += array.count
                guard containers <= maxContainerElements else { throw URLError(.cannotParseResponse) }
                for child in array { try visit(child, depth: depth + 1) }
            } else if let string = value as? String {
                guard string.utf8.count <= maxStringBytes else { throw URLError(.cannotParseResponse) }
            }
        }

        try visit(object, depth: 0)
    }

    static func validateResponse(_ data: Data) throws {
        guard data.count <= maxResponseBytes else {
            throw URLError(.cannotParseResponse)
        }
        try validate(data)
    }
}
