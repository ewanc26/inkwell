//
//  StandardSitePostEmbed.swift
//  Inkwell
//
//  Renders an embedded standard.site document (pub.leaflet.blocks.standardSitePost).
//  Fetches the referenced document from its author's PDS and shows a preview card
//  with title, publication context, cover image, and publication date.
//

import SwiftUI
import ATProtoKit
import OSLog

// MARK: - Renderer

/// A preview card for an embedded standard.site document. Fetches the
/// document record and its publication on appearance.
struct StandardSitePostEmbedView: View {
    let subjectURI: String
    let size: String?         // "small" or "medium"
    let showPublicationTheme: Bool

    @Environment(LoginStateManager.self) private var loginStateManager
    @State private var document: DocumentEntry?
    @State private var publication: PublicationEntry?
    @State private var loadError: Bool = false

    private let logger = Logger(subsystem: "uk.ewancroft.Inkwell", category: "StandardSiteEmbed")

    private var isSmall: Bool { size == "small" }

    var body: some View {
        Group {
            if let document {
                cardContent(document)
            } else if loadError {
                fallbackCard
            } else {
                loadingCard
            }
        }
        .task {
            await loadDocument()
        }
    }

    // MARK: - Card Content

    @ViewBuilder
    private func cardContent(_ doc: DocumentEntry) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            // Cover image
            if !isSmall, let cover = doc.record.coverImage {
                let imgURL = URL(string: "https://cdn.bsky.app/img/feed_thumbnail/plain/\(doc.authorDID)/\(cover.reference.link)")
                AsyncImage(url: imgURL) { phase in
                    if let image = phase.image {
                        image.resizable().scaledToFill()
                            .frame(maxWidth: .infinity)
                            .frame(height: 140)
                            .clipped()
                    }
                }
            }

            VStack(alignment: .leading, spacing: isSmall ? 4 : 8) {
                // Publication name
                if let pubName = publication?.record.name {
                    Text(pubName.uppercased())
                        .font(isSmall ? .caption2 : .caption)
                        .fontWeight(.bold)
                        .foregroundStyle(.secondary)
                        .tracking(1)
                        .lineLimit(1)
                }

                // Document title
                Text(doc.record.title)
                    .font(isSmall ? .subheadline : .headline)
                    .fontWeight(.semibold)
                    .foregroundStyle(.primary)
                    .lineLimit(isSmall ? 2 : 3)

                // Description
                if !isSmall, let desc = doc.record.description, !desc.isEmpty {
                    Text(desc)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .lineLimit(2)
                }

                // Published date
                Text(doc.record.publishedAt.formatted(date: .abbreviated, time: .omitted))
                    .font(.caption2)
                    .foregroundStyle(.secondary.opacity(0.7))
            }
            .padding(isSmall ? 10 : 14)
        }
        .background(.quaternary.opacity(0.5))
        .clipShape(RoundedRectangle(cornerRadius: isSmall ? 8 : 12))
        .overlay(
            RoundedRectangle(cornerRadius: isSmall ? 8 : 12)
                .stroke(.quaternary, lineWidth: 1)
        )
    }

    // MARK: - Loading

    private func loadDocument() async {
        guard let parsed = ATURI.parse(subjectURI) else {
            logger.error("[StandardSitePostEmbed] invalid AT-URI: \(subjectURI)")
            loadError = true
            return
        }

        do {
            let docs = try await loginStateManager.fetchDocuments(fromDID: parsed.did)
            if let match = docs.first(where: { $0.uri == subjectURI }) {
                document = match
            }
        } catch {
            logger.error("[StandardSitePostEmbed] fetch failed: \(error.localizedDescription)")
            loadError = true
            return
        }

        // Best-effort publication fetch for context
        if let doc = document {
            let pubs = (try? await loginStateManager.fetchPublications(fromDID: doc.authorDID)) ?? []
            publication = pubs.first(where: { pub in
                // Match publication by site field
                doc.record.site == pub.uri || pub.record.url == doc.record.site
            })
        }

        if document == nil {
            loadError = true
        }
    }

    // MARK: - States

    private var fallbackCard: some View {
        HStack(spacing: 8) {
            Image(systemName: "doc.text.fill")
                .foregroundStyle(.secondary)
            Text("Standard.site post unavailable")
                .font(.subheadline)
                .foregroundStyle(.secondary)
            Spacer()
        }
        .padding(12)
        .background(.quaternary.opacity(0.5))
        .clipShape(RoundedRectangle(cornerRadius: 10))
    }

    private var loadingCard: some View {
        HStack(spacing: 10) {
            InkwellInlineLoader()
            Text("Loading…")
                .font(.subheadline)
                .foregroundStyle(.secondary)
            Spacer()
        }
        .padding(14)
        .background(.quaternary.opacity(0.5))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }
}

#Preview {
    VStack(spacing: 20) {
        StandardSitePostEmbedView(
            subjectURI: "at://did:plc:example/site.standard.document/abc123",
            size: "medium",
            showPublicationTheme: true
        )
        .environment(LoginStateManager())
        .padding()
    }
}
