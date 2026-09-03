//
//  ReaderPostCard.swift
//  Inkwell
//

import SwiftUI
import ATProtoKit

struct ReaderPostCard: View {
    let item: ReaderFeedItem
    var reservesOverflowSpace = false
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize

    private var document: SiteStandardLexicon.DocumentRecord { item.document.record }
    private var publication: SiteStandardLexicon.PublicationRecord? { item.publication?.record }
    private var publicationName: String? { publication?.name ?? item.cachedPublicationName }

    // Same theme resolution as ReadView (Leaflet's rich theme falling back
    // to basicTheme, then system defaults) so a card in the feed matches
    // what the publication actually looks like once opened. Untethemed
    // documents fall back to a grouped-background card instead of
    // ReaderTheme's plain systemBackground default, so cards stay visually
    // distinct from the surrounding scroll view when no theme is set.
    private var theme: ReaderTheme {
        ReaderTheme(document: document, publication: publication, colorScheme: colorScheme)
    }

    private var hasExplicitTheme: Bool {
        document.theme != nil || publication?.theme != nil || publication?.basicTheme != nil
    }

    private var background: Color {
        hasExplicitTheme ? theme.background : Color(uiColor: .secondarySystemGroupedBackground)
    }
    private var foreground: Color { theme.foreground }
    private var accent: Color { theme.accent }
    private var usesAccessibilityTextLayout: Bool { dynamicTypeSize.isAccessibilitySize }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            if let coverURL {
                AsyncImage(url: coverURL) { phase in
                    switch phase {
                    case .success(let image):
                        image.resizable().scaledToFill()
                            .frame(maxWidth: .infinity)
                            .aspectRatio(16 / 9, contentMode: .fit)
                            .clipped()
                            .accessibilityHidden(true)
                    case .failure, .empty:
                        EmptyView()
                    @unknown default:
                        EmptyView()
                    }
                }
            }

            VStack(alignment: .leading, spacing: 9) {
                HStack(spacing: 10) {
                    if let avatarURL = item.authorProfile?.avatar.flatMap({ URL(string: $0) }) {
                        AsyncImage(url: avatarURL) { phase in
                            switch phase {
                            case .success(let image):
                                image.resizable().scaledToFill()
                                    .frame(width: 28, height: 28)
                                    .clipShape(.circle)
                                    .accessibilityHidden(true)
                            case .failure, .empty:
                                EmptyView()
                            @unknown default:
                                EmptyView()
                            }
                        }
                    }
                    if let displayName = item.authorProfile?.displayName {
                        Text(displayName)
                            .font(.caption.weight(.medium))
                            .foregroundStyle(foreground.opacity(0.7))
                    }
                    if reservesOverflowSpace {
                        Spacer(minLength: 44)
                    }
                }
                .frame(minHeight: reservesOverflowSpace ? 44 : 0, alignment: .leading)

                Text(document.title)
                    .font(theme.headingFont(.title3, weight: .bold))
                    .foregroundStyle(foreground)
                    .lineLimit(usesAccessibilityTextLayout ? nil : 2)
                    .fixedSize(horizontal: false, vertical: true)

                if let description = document.description, !description.isEmpty {
                    Text(description)
                        .font(theme.bodyFont(.subheadline))
                        .foregroundStyle(foreground.opacity(0.7))
                        .lineLimit(usesAccessibilityTextLayout ? nil : 3)
                        .fixedSize(horizontal: false, vertical: true)
                }

                if usesAccessibilityTextLayout {
                    accessibilityMetadata
                } else {
                    compactMetadata
                }
            }
            .padding(16)
        }
        .background(background)
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .stroke(foreground.opacity(0.1), lineWidth: 1)
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(accessibilityLabel)
        .accessibilityHint("Opens this article")
    }

    private var compactMetadata: some View {
        HStack(spacing: 6) {
            Text(formattedDate)
            if item.isCached {
                Label("Cached", systemImage: "archivebox")
                    .labelStyle(.titleAndIcon)
            }
            if let publicationName {
                Text("·")
                Text(publicationName)
                    .fontWeight(.semibold)
                    .foregroundStyle(accent)
                    .lineLimit(1)
            }
            Spacer(minLength: 8)
            Image(systemName: "arrow.up.right")
                .accessibilityHidden(true)
        }
        .font(.caption)
        .foregroundStyle(foreground.opacity(0.55))
    }

    private var accessibilityMetadata: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(formattedDate)
            if item.isCached {
                Label("Cached", systemImage: "archivebox")
                    .labelStyle(.titleAndIcon)
            }
            if let publicationName {
                Text(publicationName)
                    .fontWeight(.semibold)
                    .foregroundStyle(accent)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
        .font(.caption)
        .foregroundStyle(foreground.opacity(0.55))
    }

    private var coverURL: URL? {
        guard let cover = document.coverImage else { return nil }
        return URL(string: "https://cdn.bsky.app/img/feed_thumbnail/plain/\(item.document.authorDID)/\(cover.reference.link)")
    }

    private var formattedDate: String {
        document.publishedAt.formatted(date: .abbreviated, time: .omitted)
    }

    private var accessibilityLabel: String {
        var parts = [document.title]

        if let author = item.authorProfile?.displayName, !author.isEmpty {
            parts.append("By \(author)")
        }

        if let description = document.description, !description.isEmpty {
            parts.append(description)
        }

        if let publicationName, !publicationName.isEmpty {
            parts.append("Published in \(publicationName)")
        }

        if item.isCached {
            parts.append("Available offline")
        }

        parts.append("Published \(formattedDate)")
        return parts.joined(separator: ". ")
    }
}
