import CoreGraphics
import ImageIO
import UIKit
import UniformTypeIdentifiers

enum ImageUploadSanitizer {
    enum Failure: LocalizedError {
        case invalidImage
        case dimensionsTooLarge
        case encodingFailed

        var errorDescription: String? {
            switch self {
            case .invalidImage: return "The selected file is not a supported image."
            case .dimensionsTooLarge: return "That image is too large to upload safely."
            case .encodingFailed: return "The image could not be prepared for upload."
            }
        }
    }

    struct Output {
        let data: Data
        let mimeType: String
    }

    /// Image decoding and re-encoding are CPU-heavy; callers can invoke this
    /// from a Swift concurrency task without blocking SwiftUI's main actor.
    @concurrent
    static func sanitizeAsync(_ data: Data) async throws -> Output {
        try sanitize(data)
    }

    nonisolated static func sanitize(_ data: Data) throws -> Output {
        guard let source = CGImageSourceCreateWithData(data as CFData, nil),
              let properties = CGImageSourceCopyPropertiesAtIndex(source, 0, nil) as? [CFString: Any],
              let width = properties[kCGImagePropertyPixelWidth] as? Int,
              let height = properties[kCGImagePropertyPixelHeight] as? Int,
              width > 0, height > 0 else {
            throw Failure.invalidImage
        }
        guard width <= 8_192, height <= 8_192, width.multipliedReportingOverflow(by: height).overflow == false,
              width * height <= 40_000_000 else {
            throw Failure.dimensionsTooLarge
        }
        guard let image = UIImage(data: data) else { throw Failure.invalidImage }

        let hasAlpha = image.cgImage?.alphaInfo == .first || image.cgImage?.alphaInfo == .last ||
            image.cgImage?.alphaInfo == .premultipliedFirst || image.cgImage?.alphaInfo == .premultipliedLast
        let format = UIGraphicsImageRendererFormat()
        format.scale = 1
        format.opaque = !hasAlpha
        let renderer = UIGraphicsImageRenderer(size: image.size, format: format)
        let normalized = renderer.image { _ in
            image.draw(in: CGRect(origin: .zero, size: image.size))
        }
        if hasAlpha, let png = normalized.pngData() {
            return Output(data: png, mimeType: UTType.png.preferredMIMEType ?? "image/png")
        }
        guard let jpeg = normalized.jpegData(compressionQuality: 0.9) else { throw Failure.encodingFailed }
        return Output(data: jpeg, mimeType: UTType.jpeg.preferredMIMEType ?? "image/jpeg")
    }
}
