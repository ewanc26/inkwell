//
//  ReadView.swift
//  Inkwell
//
//  Created by Ewan Croft on 20/06/2026.
//
//  This file holds the ReadView struct's state and body. Content-block
//  rendering lives in ReadView+Blocks.swift; comment/subscribe/recommend
//  logic lives in ReadView+Actions.swift. Standalone subviews used here
//  live in their own files in this directory.
//

import SwiftUI
import ATProtoKit
import WebKit

struct ReadView: View {
    @Environment(LoginStateManager.self) var loginStateManager
    @Environment(\.colorScheme) var colorScheme

    let document: SiteStandardLexicon.DocumentRecord
    let publication: SiteStandardLexicon.PublicationRecord?
    var documentURI: String? = nil
    var authorDID: String? = nil

    // Prev/Next navigation (from the feed that pushed this view).
    var previousItem: ReaderFeedItem? = nil
    var nextItem: ReaderFeedItem? = nil

    @State var pages: [LeafletPage] = []
    @State var markdownContent: String? = nil
    @State var isLoading = true
    @State var errorMessage: String? = nil
    @State var isVerified: Bool?

    // Subscribe (to the publication) / Recommend (this document) state.
    // Loaded once on appearance and mutated optimistically on tap — see
    // `loadActionState()`, `toggleSubscription()`, and `toggleRecommend()`.
    @State var isSubscribed = false
    @State var subscriptionRecordKey: String?
    @State var isTogglingSubscription = false
    @State var isRecommended = false
    @State var recommendRecordKey: String?
    @State var isSubmittingRecommend = false
    @State var actionMessage: String?

    // Local read/bookmark tracking — see ArticleStateStore.swift.
    @State var articleState = ArticleStateStore.shared

    // Comment state
    @State var comments: [CommentEntry] = []
    @State var newCommentText = ""
    @State var isSubmittingComment = false
    @State var isLoadingComments = false
    @State var replyToComment: CommentEntry? = nil

    @State private var showingReportSheet = false

    // Resolves Leaflet's rich theme (light/dark palettes, fonts, page
    // width) first, falling back to standard.site's basicTheme, then
    // system defaults — see ReaderTheme.swift. A document-level theme
    // override takes priority over the publication's.
    var theme: ReaderTheme {
        ReaderTheme(document: document, publication: publication, colorScheme: colorScheme)
    }

    var backgroundColor: Color { theme.background }
    var foregroundColor: Color { theme.foreground }
    var accentColor: Color { theme.accent }

    /// The publication's AT-URI, when `document.site` actually points at one
    /// (rather than a bare HTTPS URL) — the same shape `createSubscription`
    /// requires. Standalone documents published straight to a URL have
    /// nothing to subscribe to, so the action simply doesn't appear for them.
    var publicationURI: String? {
        guard parseAtUri(document.site)?.collection == SiteStandardLexicon.PublicationRecord.type else {
            return nil
        }
        return document.site
    }

    /// The stable identifier local read/bookmark state is keyed on.
    var articleID: String? { documentURI ?? resolvedDocumentURI }

    private var documentTitleAccessibilityLabel: String {
        guard let authorDID, !authorDID.isEmpty else { return document.title }
        return "\(document.title). Author: \(authorDID)"
    }

    var body: some View {
        ScrollView {
            VStack(spacing: 0) {
                // Wrapper to scope the theme background to content only
                VStack(alignment: .leading, spacing: 24) {
                // Header section
                VStack(alignment: .leading, spacing: 12) {
                    if let pubName = publication?.name {
                        Text(pubName.uppercased())
                            .font(theme.headingFont(.caption, weight: .bold))
                            .foregroundStyle(accentColor)
                            .tracking(2)
                            .lineLimit(1)
                            .accessibilityLabel("Publication: \(pubName)")
                    }

                    Text(document.title)
                        .font(theme.headingFont(.largeTitle, weight: .bold))
                        .foregroundStyle(foregroundColor)
                        .lineSpacing(4)
                        .accessibilityAddTraits(.isHeader)
                        .accessibilityLabel(documentTitleAccessibilityLabel)

                    HStack(spacing: 8) {
                        Text("Published")
                        Text(formatDate(document.publishedAt))
                            .fontWeight(.medium)

                        if let path = document.path {
                            Spacer(minLength: 8)
                            Text(path)
                                .font(.caption2)
                                .foregroundStyle(foregroundColor.opacity(0.5))
                                .lineLimit(1)
                                .truncationMode(.middle)
                        }
                    }
                    .font(.caption)
                    .foregroundStyle(foregroundColor.opacity(0.6))

                    HStack(spacing: 12) {
                        if let isVerified {
                            Label(
                                isVerified ? "Verified source" : "Unverified source",
                                systemImage: isVerified ? "checkmark.seal.fill" : "exclamationmark.triangle"
                            )
                            .foregroundStyle(isVerified ? accentColor : foregroundColor.opacity(0.5))
                            .lineLimit(1)
                            .accessibilityLabel(
                                isVerified
                                    ? "Source verification: verified"
                                    : "Source verification: unverified"
                            )
                        }
                        if let url = document.canonicalURL(publication: publication) {
                            Link("Open original", destination: url)
                                .foregroundStyle(accentColor)
                                .lineLimit(1)
                        }
                    }
                    .font(.caption)

                    Divider()
                        .background(accentColor.opacity(0.3))
                }
                .padding(.top, 16)

                // Subscribe / Recommend actions. Wrapped in ViewThatFits so
                // that on narrow screens or larger Dynamic Type sizes the
                // pills stack vertically instead of clipping or squeezing
                // off the trailing edge.
                if publicationURI != nil || documentURI != nil {
                    VStack(alignment: .leading, spacing: 6) {
                        ViewThatFits(in: .horizontal) {
                            HStack(spacing: 10) { actionPills }
                            VStack(alignment: .leading, spacing: 8) { actionPills }
                        }
                        if let actionMessage {
                            Text(actionMessage)
                                .font(.caption)
                                .foregroundStyle(.red)
                        }
                    }
                }

                // Excerpt/description
                if let desc = document.description, !desc.isEmpty {
                    Text(desc)
                        .font(theme.bodyFont(.body))
                        .italic()
                        .foregroundStyle(foregroundColor.opacity(0.7))
                        .lineSpacing(6)
                        .padding(.vertical, 8)
                        .padding(.horizontal, 12)
                        .background(
                            RoundedRectangle(cornerRadius: 8)
                                .fill(accentColor.opacity(0.05))
                        )
                }

                // Cover Image
                if let cover = document.coverImage, let did = authorDID ?? loginStateManager.currentDID {
                    let urlString = "https://cdn.bsky.app/img/feed_thumbnail/plain/\(did)/\(cover.reference.link)"
                    if let url = URL(string: urlString) {
                        AsyncImage(url: url) { phase in
                            switch phase {
                            case .success(let image):
                                image.resizable().scaledToFill()
                            case .failure:
                                foregroundColor.opacity(0.06)
                                    .overlay { Image(systemName: "photo").foregroundStyle(foregroundColor.opacity(0.4)) }
                            default:
                                foregroundColor.opacity(0.06)
                                    .overlay { ProgressView() }
                            }
                        }
                        // Same fix as ReaderPostCard's cover thumbnail: bound
                        // the container to a sane aspect ratio with `.fit`
                        // before cropping the photo to fill it, rather than
                        // letting a portrait-oriented cover dictate the
                        // frame's height at its full, unconstrained aspect.
                        .frame(maxWidth: .infinity)
                        .aspectRatio(16 / 9, contentMode: .fit)
                        .clipped()
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                        .shadow(color: .black.opacity(0.08), radius: 8, y: 4)
                    }
                }

                // Content Section
                if isLoading {
                    HStack {
                        Spacer()
                        ProgressView("Loading publication content...")
                            .tint(accentColor)
                            .foregroundStyle(foregroundColor.opacity(0.5))
                        Spacer()
                    }
                    .padding(.vertical, 40)
                } else if let errorMessage = errorMessage {
                    ContentUnavailableView("Failed to load content", systemImage: "exclamationmark.triangle", description: Text(errorMessage))
                        .foregroundStyle(foregroundColor)
                } else if let markdown = markdownContent {
                    // Multi-format rendering: content was converted to
                    // markdown by the ContentProvider system.
                    MarkdownRendererView(markdown: markdown, theme: theme)
                } else if !pages.isEmpty {
                    // Leaflet block rendering (original path, with blob page
                    // support that the markdown path doesn't need).
                    ForEach(pages, id: \.self) { page in
                        if let blocks = page.blocks {
                            ForEach(blocks.indices, id: \.self) { idx in
                                renderBlock(blocks[idx].block, alignment: blocks[idx].alignment)
                            }
                        }
                    }
                } else {
                    ContentUnavailableView("Empty Document", systemImage: "doc.text", description: Text("This document has no readable content."))
                }
            }
            .padding(.horizontal, 20)
            .padding(.vertical, theme.showPageBackground ? 24 : 0)
            .frame(maxWidth: theme.pageWidth)
            .background(
                Group {
                    if theme.showPageBackground {
                        RoundedRectangle(cornerRadius: 16, style: .continuous)
                            .fill(theme.pageBackground)
                    }
                }
            )
            .frame(maxWidth: .infinity)

            // MARK: - Prev / Next Navigation
            if previousItem != nil || nextItem != nil {
                Divider()
                    .padding(.vertical, 4)

                HStack(spacing: 12) {
                    if let prev = previousItem {
                        NavigationLink {
                            ReadView(
                                document: prev.document.record,
                                publication: prev.publication?.record,
                                documentURI: prev.document.uri,
                                authorDID: prev.document.authorDID
                            )
                        } label: {
                            PostNavButton(
                                direction: .previous,
                                title: prev.document.record.title,
                                theme: theme
                            )
                        }
                        .buttonStyle(.plain)
                    }
                    if let next = nextItem {
                        NavigationLink {
                            ReadView(
                                document: next.document.record,
                                publication: next.publication?.record,
                                documentURI: next.document.uri,
                                authorDID: next.document.authorDID
                            )
                        } label: {
                            PostNavButton(
                                direction: .next,
                                title: next.document.record.title,
                                theme: theme
                            )
                        }
                        .buttonStyle(.plain)
                    }
                }
                .frame(maxWidth: theme.pageWidth)
            }

            // MARK: - Comments
            if documentURI != nil || authorDID != nil {
                VStack(alignment: .leading, spacing: 12) {
                    Divider()
                        .padding(.vertical, 8)

                    Text("Comments")
                        .font(theme.headingFont(.headline, weight: .bold))
                        .foregroundStyle(foregroundColor)

                    if isLoadingComments {
                        HStack { Spacer(); ProgressView().tint(accentColor); Spacer() }
                    } else if comments.isEmpty {
                        Text("No comments yet.")
                            .font(theme.bodyFont(.subheadline))
                            .foregroundStyle(foregroundColor.opacity(0.5))
                    } else {
                        ForEach(comments) { comment in
                            CommentRow(
                                comment: comment,
                                foregroundColor: foregroundColor,
                                accentColor: accentColor,
                                onReply: { replyToComment = comment }
                            )
                        }
                    }

                    // New comment composer
                    HStack(spacing: 8) {
                        TextField("Add a comment…", text: $newCommentText, axis: .vertical)
                            .textFieldStyle(.plain)
                            .font(theme.bodyFont(.subheadline))
                            .foregroundStyle(foregroundColor)
                            .tint(accentColor)
                            .padding(10)
                            .background(foregroundColor.opacity(0.08))
                            .clipShape(RoundedRectangle(cornerRadius: 10))
                            .accessibilityLabel("New comment")
                            .accessibilityHint("Type your comment and tap the send button to publish")

                        Button {
                            Task { await submitComment() }
                        } label: {
                            if isSubmittingComment {
                                ProgressView().scaleEffect(0.8).tint(accentColor)
                            } else {
                                Image(systemName: "arrow.up.circle.fill")
                                    .font(.title2)
                                    .foregroundStyle(accentColor)
                            }
                        }
                        .disabled(newCommentText.trimmingCharacters(in: .whitespaces).isEmpty || isSubmittingComment)
                        .accessibilityLabel("Send comment")
                        .accessibilityHint("Publishes your comment to the AT Protocol")
                    }
                }
                .padding(.horizontal, 20)
                .padding(.vertical, 16)
                .frame(maxWidth: theme.pageWidth)
                .task(id: documentURI) {
                    await loadComments()
                }
            }
            }
            .background(backgroundColor)
        }
        .scrollContentBackground(.hidden)
        // The ScrollView's own content background only covers its content
        // area, leaving the system's default (light) background showing
        // through the status bar / nav bar safe area above it on dark
        // themes. Extend the theme color there too, so it reaches the
        // screen edge — the Liquid Glass back button adapts to whatever's
        // behind it on its own, no light patch needed.
        .background(backgroundColor.ignoresSafeArea(edges: .top))
        // The bar used to be titleless. Naming the publication there is what
        // a reader is expected to do — the article's own title is already
        // set in type below, so repeating it would just be noise.
        .navigationTitle(publication?.name ?? document.title)
        .navigationBarTitleDisplayMode(.inline)
        .inAppLinkHandling()
        .toolbar {
            if let url = document.canonicalURL(publication: publication) {
                ToolbarItem(placement: .topBarTrailing) {
                    ShareLink(item: url, subject: Text(document.title)) {
                        Label("Share", systemImage: "square.and.arrow.up")
                    }
                }
            }
            ToolbarItem(placement: .topBarTrailing) {
                Button {
                    showingReportSheet = true
                } label: {
                    Label("Report", systemImage: "exclamationmark.triangle")
                }
            }
        }
        .sheet(isPresented: $showingReportSheet) {
            ReportSheet(
                subjectDID: authorDID,
                subjectURI: documentURI ?? resolvedDocumentURI,
                onSubmit: {
                    actionMessage = "Report submitted."
                },
                onError: { message in
                    actionMessage = "Report failed: \(message)"
                }
            )
        }
        .task {
            await loadContent()
        }
        .task(id: documentURI) {
            await verifySource()
        }
        .task(id: documentURI) {
            await loadActionState()
        }
        .task(id: articleID) {
            markAsReadIfNeeded()
        }
    }
}
