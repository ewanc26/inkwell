import SwiftUI
import UIKit

/// Bounded in-memory cache for PDS-resolved blob images, keyed by "did:cid".
/// Avoids re-downloading the same blob every time a view carrying a
/// `PDSBlobImage` is recreated (e.g. scrolling a list off/on screen).
final class PDSBlobImageCache {
    static let shared = PDSBlobImageCache()

    private let cache: NSCache<NSString, UIImage> = {
        let cache = NSCache<NSString, UIImage>()
        cache.countLimit = 200
        cache.totalCostLimit = 64 * 1024 * 1024
        return cache
    }()

    private init() {}

    func image(forKey key: String) -> UIImage? {
        cache.object(forKey: key as NSString)
    }

    func setImage(_ image: UIImage, forKey key: String) {
        let cost = Int(image.size.width * image.size.height * 4)
        cache.setObject(image, forKey: key as NSString, cost: cost)
    }
}

/// Loads an AT Protocol blob from the repository owner's current PDS.
struct PDSBlobImage: View {
    let did: String
    let cid: String
    let loginStateManager: LoginStateManager
    var height: CGFloat
    var contentMode: ContentMode = .fill

    @State private var image: Image?
    @State private var failed = false

    private var cacheKey: String { "\(did):\(cid)" }

    var body: some View {
        Group {
            if let image {
                if contentMode == .fit {
                    image.resizable().scaledToFit()
                } else {
                    image.resizable().scaledToFill()
                }
            } else if failed {
                Image(systemName: "photo")
                    .foregroundStyle(.secondary)
            } else {
                ProgressView()
            }
        }
        .frame(maxWidth: .infinity)
        .frame(height: height)
        .clipped()
        .task(id: cacheKey) {
            if let cached = PDSBlobImageCache.shared.image(forKey: cacheKey) {
                image = Image(uiImage: cached)
                return
            }
            do {
                let data = try await loginStateManager.downloadBlob(
                    cid: cid,
                    fromDID: did,
                    declaredSize: nil
                )
                guard let uiImage = UIImage(data: data) else { return }
                PDSBlobImageCache.shared.setImage(uiImage, forKey: cacheKey)
                image = Image(uiImage: uiImage)
            } catch {
                image = nil
                failed = true
            }
        }
    }
}
