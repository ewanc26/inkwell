import SwiftUI

/// A privacy-preserving placeholder for content that matches a reader filter.
/// It intentionally excludes the post title, image, and publication metadata.
struct ModeratedReaderPostCard: View {
    let presentation: ContentModerationPresentation
    let onReveal: () -> Void

    private var title: String {
        presentation == .warning ? "Content Warning" : "Content Hidden"
    }

    private var message: String {
        presentation == .warning
            ? "This article has a content label you chose to see behind a warning."
            : "This article matches one of your content filters."
    }

    var body: some View {
        VStack(spacing: 12) {
            Image(systemName: "eye.slash")
                .font(.title2)
                .foregroundStyle(.secondary)
                .accessibilityHidden(true)
            Text(title)
                .font(.headline)
            Text(message)
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
            Button("Reveal Article", action: onReveal)
                .buttonStyle(.bordered)
        }
        .frame(maxWidth: .infinity)
        .padding(20)
        .background(Color(uiColor: .secondarySystemGroupedBackground))
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .stroke(Color.secondary.opacity(0.15), lineWidth: 1)
        }
        .accessibilityElement(children: .combine)
        .accessibilityLabel("\(title). \(message)")
    }
}
