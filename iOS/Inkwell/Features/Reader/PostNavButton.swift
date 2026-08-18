//
//  PostNavButton.swift
//  Inkwell
//

import SwiftUI

// MARK: - Post Navigation Button

/// A styled prev/next navigation button for moving between posts in a feed.
/// Matches leaflet.pub's `PostPrevNextButtons.tsx` pattern.
struct PostNavButton: View {
    enum Direction { case previous, next }

    let direction: Direction
    let title: String
    let theme: ReaderTheme

    var body: some View {
        HStack(spacing: 8) {
            if direction == .previous {
                Image(systemName: "chevron.left")
                    .font(.caption.weight(.semibold))
                VStack(alignment: .leading, spacing: 2) {
                    Text("Previous")
                        .font(.caption2)
                        .foregroundStyle(theme.foreground.opacity(0.5))
                    Text(title)
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(theme.foreground)
                        .lineLimit(1)
                }
            }
            Spacer(minLength: 0)
            if direction == .next {
                VStack(alignment: .trailing, spacing: 2) {
                    Text("Next")
                        .font(.caption2)
                        .foregroundStyle(theme.foreground.opacity(0.5))
                    Text(title)
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(theme.foreground)
                        .lineLimit(1)
                }
                Image(systemName: "chevron.right")
                    .font(.caption.weight(.semibold))
            }
        }
        .padding(12)
        .frame(maxWidth: .infinity, alignment: direction == .previous ? .leading : .trailing)
        .background(theme.foreground.opacity(0.04))
        .clipShape(RoundedRectangle(cornerRadius: 10))
        .overlay(
            RoundedRectangle(cornerRadius: 10)
                .stroke(theme.accent.opacity(0.15), lineWidth: 1)
        )
    }
}
