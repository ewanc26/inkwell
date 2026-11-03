//
//  ImageGalleryBlockView.swift
//  Inkwell
//

import SwiftUI
import ATProtoKit

/// A `pub.leaflet.blocks.imageGallery` block. Images are PDS blobs, loaded by
/// the same view as single-image blocks; the lexicon's gallery formats all
/// show the same ordered set, so they render as one horizontal strip with
/// each image's own aspect ratio.
struct ImageGalleryBlockView: View {
    let images: [LeafletGalleryImage]
    let authorDID: String
    let loginStateManager: LoginStateManager
    let foregroundColor: Color
    let gap: CGFloat
    let maxTileWidth: CGFloat

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            LazyHStack(alignment: .top, spacing: gap) {
                ForEach(Array(images.enumerated()), id: \.offset) { _, item in
                    if let cid = item.image?.reference.link {
                        tile(item, cid: cid)
                    }
                }
            }
            .padding(.vertical, 4)
        }
    }

    private func tile(_ item: LeafletGalleryImage, cid: String) -> some View {
        let width = tileWidth(for: item.aspectRatio)

        return VStack(alignment: .leading, spacing: 6) {
            PDSBlobImage(
                did: authorDID,
                cid: cid,
                loginStateManager: loginStateManager,
                height: 220,
                contentMode: .fill
            )
            .frame(width: width, height: 220)
            .clipped()
            .clipShape(RoundedRectangle(cornerRadius: 8))

            if let alt = item.alt, !alt.isEmpty {
                Text(alt)
                    .font(.caption)
                    .italic()
                    .foregroundStyle(foregroundColor.opacity(0.6))
                    .frame(width: width, alignment: .leading)
                    .lineLimit(3)
            }
        }
    }

    private func tileWidth(for ratio: LeafletAspectRatio?) -> CGFloat {
        guard let width = ratio?.width, let height = ratio?.height,
              width > 0, height > 0 else {
            return 240
        }
        return max(140, min(maxTileWidth, 220 * CGFloat(width) / CGFloat(height)))
    }
}
