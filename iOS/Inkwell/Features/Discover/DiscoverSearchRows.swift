//
//  DiscoverSearchRows.swift
//  Inkwell
//
//  Row views for DiscoverView's search results list.
//

import SwiftUI

/// A small bounded thumbnail shared by both search row types — fixed-size
/// regardless of the AsyncImage's loading phase, so a slow or missing image
/// never grows the row or distorts the list. Falls back to an SF Symbol on
/// a faint tinted square when there's nothing to show.
struct SearchResultThumbnail: View {
    let urlString: String?
    let placeholderSystemImage: String
    var size: CGFloat = 52
    var cornerRadius: CGFloat = 10

    var body: some View {
        Group {
            if let urlString, let url = URL(string: urlString) {
                AsyncImage(url: url) { phase in
                    switch phase {
                    case .success(let image):
                        image.resizable().scaledToFill()
                    case .failure:
                        placeholder
                    default:
                        ProgressView().controlSize(.small)
                    }
                }
            } else {
                placeholder
            }
        }
        .frame(width: size, height: size)
        // The system's grouped-content fill, so the thumbnail well tracks
        // light/dark and increased-contrast the way every other inset
        // grouped row does — an opaque 5% black never did.
        .background(Color(uiColor: .tertiarySystemFill))
        .clipShape(RoundedRectangle(cornerRadius: cornerRadius, style: .continuous))
    }

    private var placeholder: some View {
        Image(systemName: placeholderSystemImage)
            .foregroundStyle(.secondary)
    }
}

struct DocumentSearchRow: View {
    let result: ReaderSearchResult

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            SearchResultThumbnail(
                urlString: result.coverImage,
                placeholderSystemImage: "doc.text.image"
            )

            VStack(alignment: .leading, spacing: 5) {
                Text(result.title)
                    .font(.system(.body, design: .serif, weight: .semibold))
                    .lineLimit(2)
                if let snippet = result.snippet, !snippet.isEmpty {
                    Text(snippet)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .lineLimit(2)
                }
                HStack(spacing: 4) {
                    Text(result.platform ?? "standard.site")
                        .lineLimit(1)
                    if let handle = result.handle {
                        Text("·")
                        Text(handle)
                            .lineLimit(1)
                            .truncationMode(.middle)
                    }
                }
                .font(.caption2)
                .foregroundStyle(.secondary)
            }
            // Lets the text column truncate instead of pushing the row wider
            // than the list, however long the title/handle turn out to be.
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(.vertical, 4)
    }
}

struct PublicationSearchRow: View {
    let publication: ReaderSearchResult
    let isSubscribed: Bool
    let canSubscribe: Bool
    let onSubscribe: () -> Void

    var body: some View {
        HStack(spacing: 12) {
            SearchResultThumbnail(
                urlString: publication.coverImage,
                placeholderSystemImage: "building.2.crop.left.right.fill",
                size: 44
            )

            VStack(alignment: .leading, spacing: 4) {
                Text(publication.title)
                    .font(.headline)
                    .lineLimit(2)
                if let basePath = publication.basePath {
                    Text(basePath)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .lineLimit(1)
                        .truncationMode(.middle)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            subscribeButton
        }
        .padding(.vertical, 4)
    }

    /// A real system button rather than a symbol on a hand-drawn tinted
    /// circle: `.borderedProminent` when subscribed and `.bordered` when
    /// not gives the same filled/outlined read, but with the system's own
    /// contrast handling, disabled appearance, and press feedback.
    private var subscribeButton: some View {
        Button(action: onSubscribe) {
            Image(systemName: isSubscribed ? "bell.fill" : "bell")
                .symbolEffect(.bounce, value: isSubscribed)
                .accessibilityHidden(true)
        }
        .modifier(ProminentBorderedWhen(isProminent: isSubscribed))
        .buttonBorderShape(.circle)
        .disabled(!canSubscribe)
        .accessibilityLabel(isSubscribed ? "Unsubscribe" : "Subscribe")
        .accessibilityAddTraits(isSubscribed ? [.isSelected] : [])
        .animation(InkwellMotion.micro, value: isSubscribed)
    }
}

/// Switches a button between the bordered and prominent-bordered system
/// styles. The two are distinct types, so the choice can't be made with a
/// ternary inside `.buttonStyle(_:)` — it has to branch at the view level.
struct ProminentBorderedWhen: ViewModifier {
    let isProminent: Bool

    @ViewBuilder
    func body(content: Content) -> some View {
        if isProminent {
            content.buttonStyle(.borderedProminent)
        } else {
            content.buttonStyle(.bordered)
        }
    }
}
