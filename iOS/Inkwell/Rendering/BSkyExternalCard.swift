//
//  BSkyExternalCard.swift
//  Inkwell
//
//  Renders the `external` card inside a Bluesky post embed
//  (BSkyPostEmbed.swift). Bluesky's May 2026 Standard.site integration
//  enriches `app.bsky.embed.external#viewExternal` with source/reading-time/
//  label/associated-content metadata; this file renders that enriched shape
//  as a "definitely one of ours" card in the style of
//  StandardSitePostEmbed.swift, while plain (non-enriched) external links
//  keep the original minimal card unchanged.
//
//  Decoding lives in BSkyPostModels.swift.
//

import SwiftUI

// MARK: - Theme Color Conversion

extension BSkyColorRGB {
    /// A SwiftUI `Color` from this RGB triple, or `nil` if any channel is
    /// missing or out of range. Callers only use this for decorative
    /// accents (borders, icon tints) alongside text that stays in the
    /// caller's own foreground color, so an unusual publication theme can
    /// never make body text illegible.
    var color: Color? {
        guard let r, let g, let b,
              (0...255).contains(r), (0...255).contains(g), (0...255).contains(b) else { return nil }
        return Color(red: Double(r) / 255, green: Double(g) / 255, blue: Double(b) / 255)
    }
}

// MARK: - External Card

/// The `external` embed card: a plain link-preview card for ordinary
/// Bluesky links, or an enriched Standard.site card (source identity,
/// reading time, publish/update date, content-warning labels, and tappable
/// links into any associated Inkwell documents) when the AppView returns
/// Standard.site metadata.
struct BSkyExternalCardView: View {
    let card: BSkyExternalEmbed.BSkyExternal
    var foregroundColor: Color = .primary
    var accentColor: Color = .blue

    var body: some View {
        if card.isStandardSiteEnriched {
            standardSiteCard
        } else {
            plainCard
        }
    }

    // MARK: - Plain Card

    /// The original plain-link card: title, description, host, thumbnail.
    /// Unchanged from before Standard.site enrichment support so ordinary
    /// Bluesky link cards keep rendering exactly as they did.
    private var plainCard: some View {
        HStack(spacing: 12) {
            VStack(alignment: .leading, spacing: 4) {
                if let title = card.title {
                    Text(title)
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(foregroundColor)
                        .lineLimit(2)
                }
                if let desc = card.description {
                    Text(desc)
                        .font(.caption)
                        .foregroundStyle(foregroundColor.opacity(0.6))
                        .lineLimit(2)
                }
                if let uri = card.uri {
                    Text(host(from: uri))
                        .font(.caption2)
                        .foregroundStyle(foregroundColor.opacity(0.4))
                }
            }
            Spacer()
            if let thumb = card.thumb {
                AsyncImage(url: URL(string: thumb)) { phase in
                    if let image = phase.image {
                        image.resizable().scaledToFill()
                            .frame(width: 60, height: 60)
                            .clipShape(RoundedRectangle(cornerRadius: 6))
                    }
                }
                .accessibilityHidden(true)
            }
        }
        .padding(10)
        .background(foregroundColor.opacity(0.03))
        .clipShape(RoundedRectangle(cornerRadius: 8))
        .overlay(
            RoundedRectangle(cornerRadius: 8)
                .stroke(foregroundColor.opacity(0.08), lineWidth: 1)
        )
        .accessibilityElement(children: .combine)
        .accessibilityLabel(accessibilityLabel)
    }

    // MARK: - Standard.site Enriched Card

    /// Styled to read as "definitely one of ours" the way
    /// `StandardSitePostEmbedView` does, while text stays in
    /// `foregroundColor` so an arbitrary publication theme accent never has
    /// to carry contrast on its own.
    private var standardSiteCard: some View {
        let themeAccent = card.source?.theme?.accentRGB?.color ?? accentColor

        return VStack(alignment: .leading, spacing: 8) {
            if let thumb = card.thumb {
                AsyncImage(url: URL(string: thumb)) { phase in
                    if let image = phase.image {
                        image.resizable().scaledToFill()
                            .frame(maxWidth: .infinity)
                            .frame(height: 120)
                            .clipped()
                    }
                }
                .accessibilityHidden(true)
            }

            VStack(alignment: .leading, spacing: 6) {
                if let sourceTitle = card.sourceDisplayTitle {
                    HStack(spacing: 6) {
                        if let icon = card.source?.icon {
                            AsyncImage(url: URL(string: icon)) { phase in
                                if let image = phase.image {
                                    image.resizable().scaledToFill()
                                } else {
                                    Color.clear
                                }
                            }
                            .frame(width: 14, height: 14)
                            .clipShape(RoundedRectangle(cornerRadius: 3))
                            .accessibilityHidden(true)
                        }
                        Text(sourceTitle.uppercased())
                            .font(.caption2.weight(.bold))
                            .foregroundStyle(foregroundColor.opacity(0.5))
                            .tracking(1)
                            .lineLimit(1)
                    }
                }

                if let title = card.title {
                    Text(title)
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(foregroundColor)
                        .lineLimit(2)
                }

                if let desc = card.description {
                    Text(desc)
                        .font(.caption)
                        .foregroundStyle(foregroundColor.opacity(0.6))
                        .lineLimit(2)
                }

                HStack(spacing: 10) {
                    if let readingTimeLabel = card.readingTimeLabel {
                        Label(readingTimeLabel, systemImage: "clock")
                    }
                    if let dateLabel {
                        Text(dateLabel)
                    }
                }
                .font(.caption2)
                .foregroundStyle(foregroundColor.opacity(0.5))

                if !card.contentWarningLabels.isEmpty {
                    Label(card.contentWarningLabels.joined(separator: ", "), systemImage: "exclamationmark.triangle")
                        .font(.caption2.weight(.medium))
                        .foregroundStyle(.orange)
                }

                if let refs = card.associatedRefs, !refs.isEmpty {
                    associatedRefsRow(refs, accent: themeAccent)
                }
            }
            .padding(.horizontal, card.thumb != nil ? 10 : 0)
            .padding(.bottom, card.thumb != nil ? 10 : 0)
        }
        .padding(card.thumb != nil ? 0 : 10)
        .background(foregroundColor.opacity(0.03))
        .clipShape(RoundedRectangle(cornerRadius: 8))
        .overlay(
            RoundedRectangle(cornerRadius: 8)
                .stroke(themeAccent.opacity(0.25), lineWidth: 1)
        )
        .accessibilityElement(children: .combine)
        .accessibilityLabel(accessibilityLabel)
    }

    /// Tappable chips for `associatedRefs`, pushing the referenced document
    /// onto the enclosing Reader `NavigationStack` (the same `String`-typed
    /// `navigationDestination` used throughout the app, e.g.
    /// `BrowseDocumentsView` and `DiscoverView`) rather than only falling
    /// back to opening the external web URL.
    private func associatedRefsRow(_ refs: [BSkyStrongRef], accent: Color) -> some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                ForEach(Array(refs.enumerated()), id: \.offset) { _, ref in
                    if let uri = ref.uri {
                        NavigationLink(value: uri) {
                            HStack(spacing: 4) {
                                Image(systemName: "doc.text")
                                Text("Related")
                            }
                            .font(.caption2.weight(.semibold))
                            .foregroundStyle(accent)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 4)
                            .background(accent.opacity(0.12))
                            .clipShape(Capsule())
                        }
                        .buttonStyle(.plain)
                        .accessibilityLabel("Related Standard.site content")
                        .accessibilityHint("Opens the associated document in Inkwell")
                    }
                }
            }
        }
    }

    // MARK: - Helpers

    private var dateLabel: String? {
        let iso = ISO8601DateFormatter()
        let raw = card.updatedAt ?? card.createdAt
        guard let raw, let date = iso.date(from: raw) else { return nil }
        let formatter = DateFormatter()
        formatter.dateStyle = .medium
        formatter.timeStyle = .none
        let prefix = card.updatedAt != nil ? "Updated" : "Published"
        return "\(prefix) \(formatter.string(from: date))"
    }

    private var accessibilityLabel: String {
        var parts = [card.sourceDisplayTitle.map { "\($0) link" } ?? "External link"]
        if let title = card.title, !title.isEmpty { parts.append(title) }
        if let description = card.description, !description.isEmpty { parts.append(description) }
        if let readingTimeLabel = card.readingTimeLabel { parts.append(readingTimeLabel) }
        if !card.contentWarningLabels.isEmpty {
            parts.append("Content warning: \(card.contentWarningLabels.joined(separator: ", "))")
        }
        if let uri = card.uri { parts.append(host(from: uri)) }
        return parts.joined(separator: ". ")
    }

    private func host(from urlString: String) -> String {
        URL(string: urlString)?.host ?? urlString
    }
}

#Preview {
    VStack(spacing: 20) {
        BSkyExternalCardView(
            card: BSkyExternalEmbed.BSkyExternal(
                uri: "https://example.com/story",
                title: "A story",
                description: "A description",
                thumb: nil,
                createdAt: nil,
                updatedAt: nil,
                readingTime: nil,
                labels: nil,
                source: nil,
                associatedRefs: nil,
                associatedProfiles: nil
            )
        )
    }
    .padding()
    .background(Color(.systemBackground))
}
