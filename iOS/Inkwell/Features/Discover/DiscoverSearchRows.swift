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
        .accessibilityHidden(true)
    }

    private var placeholder: some View {
        Image(systemName: placeholderSystemImage)
            .foregroundStyle(.secondary)
    }
}

struct ActorSearchRow: View {
    let actor: ReaderSearchActorResult

    var body: some View {
        HStack(spacing: 12) {
            SearchResultThumbnail(
                urlString: actor.avatar,
                placeholderSystemImage: "person.circle",
                size: 44
            )

            VStack(alignment: .leading, spacing: 4) {
                Text(actor.displayName ?? actor.handle)
                    .font(.headline)
                    .lineLimit(2)
                Text("@\(actor.handle)")
                    .font(.caption2)
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .padding(.vertical, 4)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(accessibilityLabel)
    }

    private var accessibilityLabel: String {
        let displayName: String
        if let name = actor.displayName, !name.isEmpty {
            displayName = name
        } else {
            displayName = actor.handle
        }
        return "\(displayName), @\(actor.handle)"
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
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(accessibilityLabel)
        .accessibilityHint("Opens this article")
    }

    private var accessibilityLabel: String {
        var parts = [result.title]

        if let snippet = result.snippet, !snippet.isEmpty {
            parts.append(snippet)
        }

        parts.append("Published on \(result.platform ?? "standard.site")")

        if let handle = result.handle, !handle.isEmpty {
            parts.append("By \(handle)")
        }

        return parts.joined(separator: ". ")
    }
}

struct PublicationDiscoveryRow: View {
    let publication: PublicationResult

    var body: some View {
        HStack(spacing: 12) {
            SearchResultThumbnail(
                urlString: publication.coverImage,
                placeholderSystemImage: "building.2.crop.left.right.fill",
                size: 44
            )

            VStack(alignment: .leading, spacing: 4) {
                Text(publication.name)
                    .font(.headline)
                    .lineLimit(1)
                Text(publication.domain)
                    .font(.caption2)
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
                    .truncationMode(.middle)
            }
            .frame(maxWidth: .infinity, alignment: .leading)

            Image(systemName: "arrow.up.forward")
                .font(.footnote)
                .foregroundStyle(.tertiary)
                .accessibilityHidden(true)
        }
        .padding(.vertical, 4)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("Publication: \(publication.name). \(publication.domain)")
        .accessibilityHint("Opens this publication")
    }
}
