//
//  BSkyPostEmbed.swift
//  Inkwell
//
//  Fetches and renders embedded Bluesky posts (pub.leaflet.blocks.bskyPost).
//  Uses the public Bluesky API (public.api.bsky.app) so no authentication
//  is needed. Responses are cached in-memory for the lifetime of the view.
//
//  Post models and the fetcher live in BSkyPostModels.swift.
//

import SwiftUI

// MARK: - Renderer

/// A live-rendered Bluesky post embed. Fetches the post from the public
/// Bluesky API on appearance and renders author info, post text, images,
/// external link cards, and quoted posts.
struct BSkyPostEmbedView: View {
    let postURI: String
    var foregroundColor: Color = .primary
    var accentColor: Color = .blue

    @State private var post: BSkyPostView?
    @State private var loadError: Bool = false

    var body: some View {
        Group {
            if let post {
                postContent(post)
            } else if loadError {
                fallbackCard
            } else {
                loadingCard
            }
        }
        .task {
            let posts = await BSkyPostFetcher.fetchPosts(uris: [postURI])
            if let first = posts.first {
                post = first
            } else {
                loadError = true
            }
        }
    }

    // MARK: - Post Content

    @ViewBuilder
    private func postContent(_ post: BSkyPostView) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            // Author row
            HStack(spacing: 10) {
                AsyncImage(url: URL(string: post.author.avatar ?? "")) { phase in
                    if let image = phase.image {
                        image.resizable().scaledToFill()
                    } else {
                        Color.gray.opacity(0.2)
                    }
                }
                .frame(width: 28, height: 28)
                .clipShape(Circle())

                VStack(alignment: .leading, spacing: 1) {
                    Text(post.author.displayName ?? post.author.handle ?? "Unknown")
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(foregroundColor)
                    if let handle = post.author.handle {
                        Text("@\(handle)")
                            .font(.caption)
                            .foregroundStyle(foregroundColor.opacity(0.5))
                    }
                }
                Spacer()
            }

            // Post text
            if let text = post.record.text, !text.isEmpty {
                Text(text)
                    .font(.body)
                    .foregroundStyle(foregroundColor)
                    .lineSpacing(4)
            }

            // Embeds
            if let embed = post.embed {
                embedContent(embed)
            }

            // Stats row
            HStack(spacing: 20) {
                stat(icon: "bubble.right", count: post.replyCount)
                stat(icon: "arrow.2.squarepath", count: post.repostCount)
                stat(icon: "heart", count: post.likeCount)
                Spacer()
                Image(systemName: "bubble.left.fill")
                    .font(.caption)
                    .foregroundStyle(accentColor.opacity(0.6))
                Text("Bluesky")
                    .font(.caption2)
                    .foregroundStyle(foregroundColor.opacity(0.4))
            }
            .padding(.top, 4)
        }
        .padding(14)
        .background(foregroundColor.opacity(0.04))
        .overlay(
            RoundedRectangle(cornerRadius: 12)
                .stroke(foregroundColor.opacity(0.12), lineWidth: 1)
        )
    }

    // MARK: - Embed Content

    @ViewBuilder
    private func embedContent(_ embed: BSkyEmbed) -> some View {
        switch embed {
        case .images(let imagesEmbed):
            if let first = imagesEmbed.images.first, let thumb = first.thumb {
                AsyncImage(url: URL(string: thumb)) { phase in
                    if let image = phase.image {
                        image.resizable().scaledToFill()
                            .frame(maxWidth: .infinity)
                            .frame(height: 200)
                            .clipped()
                            .clipShape(RoundedRectangle(cornerRadius: 8))
                    }
                }
            }

        case .external(let externalEmbed):
            HStack(spacing: 12) {
                VStack(alignment: .leading, spacing: 4) {
                    if let title = externalEmbed.external.title {
                        Text(title)
                            .font(.subheadline.weight(.semibold))
                            .foregroundStyle(foregroundColor)
                            .lineLimit(2)
                    }
                    if let desc = externalEmbed.external.description {
                        Text(desc)
                            .font(.caption)
                            .foregroundStyle(foregroundColor.opacity(0.6))
                            .lineLimit(2)
                    }
                    if let uri = externalEmbed.external.uri {
                        Text(host(from: uri))
                            .font(.caption2)
                            .foregroundStyle(foregroundColor.opacity(0.4))
                    }
                }
                Spacer()
                if let thumb = externalEmbed.external.thumb {
                    AsyncImage(url: URL(string: thumb)) { phase in
                        if let image = phase.image {
                            image.resizable().scaledToFill()
                                .frame(width: 60, height: 60)
                                .clipShape(RoundedRectangle(cornerRadius: 6))
                        }
                    }
                }
            }
            .padding(10)
            .background(foregroundColor.opacity(0.03))
            .clipShape(RoundedRectangle(cornerRadius: 8))
            .overlay(
                RoundedRectangle(cornerRadius: 8)
                    .stroke(foregroundColor.opacity(0.08), lineWidth: 1)
            )

        case .record(let recordEmbed):
            VStack(alignment: .leading, spacing: 4) {
                HStack(spacing: 6) {
                    Image(systemName: "quote.bubble")
                        .font(.caption)
                        .foregroundStyle(accentColor)
                    Text(recordEmbed.record.author?.displayName ?? "Quoted post")
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(foregroundColor)
                }
                if let text = recordEmbed.record.value?.text {
                    Text(text)
                        .font(.caption)
                        .foregroundStyle(foregroundColor.opacity(0.7))
                        .lineLimit(3)
                }
            }
            .padding(10)
            .background(foregroundColor.opacity(0.03))
            .clipShape(RoundedRectangle(cornerRadius: 8))
            .overlay(
                RoundedRectangle(cornerRadius: 8)
                    .stroke(foregroundColor.opacity(0.08), lineWidth: 1)
            )

        case .unknown:
            EmptyView()
        }
    }

    // MARK: - Helpers

    private func stat(icon: String, count: Int?) -> some View {
        HStack(spacing: 4) {
            Image(systemName: icon)
                .font(.caption2)
            if let count {
                Text(sharedFormatCount(count))
                    .font(.caption2)
            }
        }
        .foregroundStyle(foregroundColor.opacity(0.4))
    }

    private func host(from urlString: String) -> String {
        URL(string: urlString)?.host ?? urlString
    }

    // MARK: - States

    private var fallbackCard: some View {
        HStack(spacing: 8) {
            Image(systemName: "bubble.left.fill")
                .foregroundStyle(accentColor)
            Text("Bluesky post unavailable")
                .font(.subheadline)
                .foregroundStyle(foregroundColor.opacity(0.5))
            Spacer()
        }
        .padding(12)
        .background(foregroundColor.opacity(0.04))
        .clipShape(RoundedRectangle(cornerRadius: 10))
        .overlay(
            RoundedRectangle(cornerRadius: 10)
                .stroke(foregroundColor.opacity(0.12), lineWidth: 1)
        )
    }

    private var loadingCard: some View {
        HStack(spacing: 10) {
            InkwellInlineLoader()
            Text("Loading post…")
                .font(.subheadline)
                .foregroundStyle(foregroundColor.opacity(0.5))
            Spacer()
        }
        .padding(14)
        .background(foregroundColor.opacity(0.04))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }
}

#Preview {
    VStack(spacing: 20) {
        BSkyPostEmbedView(
            postURI: "at://did:plc:example/app.bsky.feed.post/123",
            foregroundColor: .primary,
            accentColor: .blue
        )
        .padding()
    }
    .background(Color(.systemBackground))
}
