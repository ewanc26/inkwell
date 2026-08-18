//
//  WebsitePreviewBlock.swift
//  Inkwell
//

import SwiftUI

// MARK: - Website Preview Block

/// A link-preview card for `pub.leaflet.blocks.website` embeds.
/// Matches leaflet.pub's `PostContent.tsx` website block rendering:
/// title, description, and host shown as a tappable card.
struct WebsitePreviewBlock: View {
    let url: String
    let title: String?
    let description: String?
    let foregroundColor: Color
    let accentColor: Color

    var body: some View {
        Link(destination: URL(string: url) ?? URL(string: "about:blank")!) {
            VStack(alignment: .leading, spacing: 4) {
                if let title {
                    Text(title)
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(foregroundColor)
                        .lineLimit(1)
                }
                if let description {
                    Text(description)
                        .font(.caption)
                        .foregroundStyle(foregroundColor.opacity(0.6))
                        .lineLimit(2)
                }
                Text(URL(string: url)?.host ?? url)
                    .font(.caption2)
                    .foregroundStyle(foregroundColor.opacity(0.4))
                    .lineLimit(1)
            }
            .padding(12)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(foregroundColor.opacity(0.04))
            .clipShape(RoundedRectangle(cornerRadius: 10))
            .overlay(
                RoundedRectangle(cornerRadius: 10)
                    .stroke(foregroundColor.opacity(0.1), lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
    }
}
