import SwiftUI
import UIKit

/// Loads an AT Protocol blob from the repository owner's current PDS.
struct PDSBlobImage: View {
    let did: String
    let cid: String
    let loginStateManager: LoginStateManager
    var height: CGFloat
    var contentMode: ContentMode = .fill

    @State private var image: Image?
    @State private var failed = false

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
        .task(id: "\(did):\(cid)") {
            do {
                let data = try await loginStateManager.downloadBlob(
                    cid: cid,
                    fromDID: did,
                    declaredSize: nil
                )
                guard let uiImage = UIImage(data: data) else { return }
                image = Image(uiImage: uiImage)
            } catch {
                image = nil
                failed = true
            }
        }
    }
}
