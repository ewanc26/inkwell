//
//  ReadView.swift
//  Inkwell
//
//  Created by Ewan Croft on 20/06/2026.
//

import SwiftUI
import ATProtoKit

struct ReadView: View {
    @Environment(LoginStateManager.self) private var loginStateManager
    @Environment(\.colorScheme) private var colorScheme

    let document: SiteStandardLexicon.DocumentRecord
    let publication: SiteStandardLexicon.PublicationRecord?
    var documentURI: String? = nil
    var authorDID: String? = nil

    @State private var pages: [LeafletPage] = []
    @State private var markdownContent: String? = nil
    @State private var isLoading = true
    @State private var errorMessage: String? = nil
    @State private var isVerified: Bool?

    // Subscribe (to the publication) / Recommend (this document) state.
    // Loaded once on appearance and mutated optimistically on tap — see
    // `loadActionState()`, `toggleSubscription()`, and `toggleRecommend()`.
    @State private var isSubscribed = false
    @State private var subscriptionRecordKey: String?
    @State private var isTogglingSubscription = false
    @State private var isRecommended = false
    @State private var recommendRecordKey: String?
    @State private var isSubmittingRecommend = false
    @State private var actionMessage: String?

    // Comment state
    @State private var comments: [CommentEntry] = []
    @State private var newCommentText = ""
    @State private var isSubmittingComment = false
    @State private var isLoadingComments = false
    @State private var replyToComment: CommentEntry? = nil

    // Resolves Leaflet's rich theme (light/dark palettes, fonts, page
    // width) first, falling back to standard.site's basicTheme, then
    // system defaults — see ReaderTheme.swift. A document-level theme
    // override takes priority over the publication's.
    private var theme: ReaderTheme {
        ReaderTheme(document: document, publication: publication, colorScheme: colorScheme)
    }

    private var backgroundColor: Color { theme.background }
    private var foregroundColor: Color { theme.foreground }
    private var accentColor: Color { theme.accent }

    /// The publication's AT-URI, when `document.site` actually points at one
    /// (rather than a bare HTTPS URL) — the same shape `createSubscription`
    /// requires. Standalone documents published straight to a URL have
    /// nothing to subscribe to, so the action simply doesn't appear for them.
    private var publicationURI: String? {
        guard ATURI.parse(document.site)?.collection == SiteStandardLexicon.PublicationRecord.type else {
            return nil
        }
        return document.site
    }

    var body: some View {
        ScrollView {
            VStack(spacing: 0) {
                // Give Liquid Glass a system-colored surface to sample
                // instead of the publication's dark theme background.
                // Gradient from system background to theme colour so the
                // Liquid Glass nav bar samples light pixels for its tint,
                // keeping the back button visible on dark publications.
                LinearGradient(
                    colors: [Color(.systemBackground), backgroundColor],
                    startPoint: .top,
                    endPoint: .bottom
                )
                .frame(height: 60)
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
                    }

                    Text(document.title)
                        .font(theme.headingFont(.largeTitle, weight: .bold))
                        .foregroundStyle(foregroundColor)
                        .lineSpacing(4)

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
                            .foregroundStyle(isVerified ? Color.green : Color.orange)
                            .lineLimit(1)
                        }
                        if let url = document.canonicalURL(publication: publication) {
                            Link("Open original", destination: url)
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
                } else {
                    // No cover image: a small Inkwell-branded gradient banner
                    // instead of leaving a gap, so plain-text documents still
                    // have some visual presence on the way in. Purely
                    // decorative chrome, tinted from the resolved theme's own
                    // accent so it never fights whatever the publication has
                    // actually set — it just keeps the page from feeling bare
                    // before the content underneath kicks in.
                    LinearGradient(
                        colors: [accentColor.opacity(0.16), accentColor.opacity(0.04)],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                    .frame(maxWidth: .infinity)
                    .aspectRatio(16 / 9, contentMode: .fit)
                    .overlay {
                        InkwellMark()
                            .frame(width: 30, height: 30 * 952 / 400)
                            .foregroundStyle(accentColor.opacity(0.45))
                    }
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                }

                // Content Section
                if isLoading {
                    HStack {
                        Spacer()
                        ProgressView("Loading publication content...")
                        Spacer()
                    }
                    .padding(.vertical, 40)
                } else if let errorMessage = errorMessage {
                    ContentUnavailableView("Failed to load content", systemImage: "exclamationmark.triangle", description: Text(errorMessage))
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

            // MARK: - Comments
            if documentURI != nil || authorDID != nil {
                VStack(alignment: .leading, spacing: 12) {
                    Divider()
                        .padding(.vertical, 8)

                    Text("Comments")
                        .font(theme.headingFont(.headline, weight: .bold))
                        .foregroundStyle(foregroundColor)

                    if isLoadingComments {
                        HStack { Spacer(); ProgressView(); Spacer() }
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
                        TextField("Add a comment...", text: $newCommentText, axis: .vertical)
                            .textFieldStyle(.plain)
                            .font(theme.bodyFont(.subheadline))
                            .padding(10)
                            .background(foregroundColor.opacity(0.08))
                            .clipShape(RoundedRectangle(cornerRadius: 10))

                        Button {
                            Task { await submitComment() }
                        } label: {
                            if isSubmittingComment {
                                ProgressView().scaleEffect(0.8)
                            } else {
                                Image(systemName: "arrow.up.circle.fill")
                                    .font(.title2)
                                    .foregroundStyle(accentColor)
                            }
                        }
                        .disabled(newCommentText.trimmingCharacters(in: .whitespaces).isEmpty || isSubmittingComment)
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
        .navigationBarTitleDisplayMode(.inline)
        .task {
            await loadContent()
        }
        .task(id: documentURI) {
            await verifySource()
        }
        .task(id: documentURI) {
            await loadActionState()
        }
    }

    // MARK: - Comments

    private func loadComments() async {
        guard let uri = documentURI ?? resolvedDocumentURI else { return }
        isLoadingComments = true
        defer { isLoadingComments = false }
        do {
            comments = try await loginStateManager.fetchComments(documentURI: uri)
        } catch {
            // Comments are best-effort — don't show errors inline
            print("[ReadView] loadComments failed: \(error)")
        }
    }

    private func submitComment() async {
        guard let uri = documentURI ?? resolvedDocumentURI else { return }
        let text = newCommentText.trimmingCharacters(in: .whitespaces)
        guard !text.isEmpty else { return }

        isSubmittingComment = true
        defer { isSubmittingComment = false }

        do {
            try await loginStateManager.createComment(
                subject: uri,
                plaintext: text,
                replyTo: replyToComment?.uri,
                onPage: nil
            )
            newCommentText = ""
            replyToComment = nil
            await loadComments()  // refresh
        } catch {
            print("[ReadView] submitComment failed: \(error)")
        }
    }

    /// The resolved AT-URI of the current document, derived from the author
    /// DID + document path when an explicit documentURI isn't provided.
    private var resolvedDocumentURI: String? {
        guard let did = authorDID ?? loginStateManager.currentDID else { return nil }
        // Reconstruct from the document record's identity
        return "at://\(did)/site.standard.document/\(document.path ?? "")"
    }

    // MARK: - Action Pills

    @ViewBuilder
    private var actionPills: some View {
        if let publicationURI {
            ReaderActionPill(
                icon: isSubscribed ? "bell.fill" : "bell",
                label: isSubscribed ? "Subscribed" : "Subscribe",
                isActive: isSubscribed,
                isLoading: isTogglingSubscription,
                tint: accentColor,
                activeForeground: theme.accentForeground
            ) {
                Task { await toggleSubscription(publicationURI: publicationURI) }
            }
            .disabled(isTogglingSubscription)
        }

        if let documentURI {
            ReaderActionPill(
                icon: isRecommended ? "star.fill" : "star",
                label: isRecommended ? "Recommended" : "Recommend",
                isActive: isRecommended,
                isLoading: isSubmittingRecommend,
                tint: accentColor,
                activeForeground: theme.accentForeground
            ) {
                Task { await toggleRecommend(documentURI: documentURI) }
            }
            .disabled(isSubmittingRecommend)
        }
    }

    // MARK: - Action State

    private func loadActionState() async {
        if let publicationURI {
            if let subs = try? await loginStateManager.fetchSubscriptions(),
               let match = subs.first(where: { $0.record.publication == publicationURI }) {
                isSubscribed = true
                subscriptionRecordKey = match.recordKey
            } else {
                isSubscribed = false
                subscriptionRecordKey = nil
            }
        }

        if let documentURI {
            if let recs = try? await loginStateManager.fetchRecommends(),
               let match = recs.first(where: { $0.record.document == documentURI }) {
                isRecommended = true
                recommendRecordKey = match.recordKey
            } else {
                isRecommended = false
                recommendRecordKey = nil
            }
        }
    }

    private func toggleSubscription(publicationURI: String) async {
        guard !isTogglingSubscription else { return }
        isTogglingSubscription = true
        actionMessage = nil
        defer { isTogglingSubscription = false }

        do {
            if isSubscribed, let key = subscriptionRecordKey {
                try await loginStateManager.deleteSubscription(recordKey: key)
                isSubscribed = false
                subscriptionRecordKey = nil
            } else {
                if let publication {
                    // Best-effort: subscribing shouldn't be blocked on the
                    // verification round-trip, only informed by it.
                    _ = try? await SiteStandardLexicon.Verification.verify(
                        publicationURI: publicationURI,
                        publication: publication
                    )
                }
                let reference = try await loginStateManager.createSubscription(publicationURI: publicationURI)
                isSubscribed = true
                subscriptionRecordKey = ATURI.parse(reference.recordURI)?.recordKey
                await NotificationManager.shared.requestPermission()
            }
        } catch {
            actionMessage = "Couldn't update subscription: \(error.localizedDescription)"
        }
    }

    private func toggleRecommend(documentURI: String) async {
        guard !isSubmittingRecommend else { return }
        isSubmittingRecommend = true
        actionMessage = nil
        defer { isSubmittingRecommend = false }

        do {
            if isRecommended, let key = recommendRecordKey {
                try await loginStateManager.deleteRecommend(recordKey: key)
                isRecommended = false
                recommendRecordKey = nil
            } else {
                let reference = try await loginStateManager.createRecommend(documentURI: documentURI)
                isRecommended = true
                recommendRecordKey = ATURI.parse(reference.recordURI)?.recordKey
            }
        } catch {
            actionMessage = "Couldn't update recommendation: \(error.localizedDescription)"
        }
    }

    // MARK: - Content Loader

    private func loadContent() async {
        guard let contentUnknown = document.content else {
            self.markdownContent = document.textContent
            isLoading = false
            return
        }

        // Leaflet carries layout, alignment, and PDS blob references that
        // cannot survive a Markdown round-trip. Render it natively first.
        if let leaflet = contentUnknown.getRecord(ofType: LeafletContent.self) {
            if let inlinePages = leaflet.pages, !inlinePages.isEmpty {
                pages = inlinePages
                isLoading = false
                return
            }

            if let blobPages = leaflet.blobPages {
                do {
                    let data: Data
                    if let authorDID {
                        data = try await loginStateManager.downloadBlob(
                            cid: blobPages.reference.link,
                            fromDID: authorDID
                        )
                    } else {
                        data = try await loginStateManager.downloadBlob(cid: blobPages.reference.link)
                    }
                    pages = try JSONDecoder().decode([LeafletPage].self, from: data)
                    isLoading = false
                    return
                } catch {
                    errorMessage = "Failed to download this Leaflet: \(error.localizedDescription)"
                    isLoading = false
                    return
                }
            }
        }

        // Other supported formats convert cleanly to the shared Markdown
        // renderer.
        if let provider = ProviderRegistry.detectProvider(contentUnknown) {
            let result = provider.toMarkdown(contentUnknown)
            if !result.markdown.isEmpty {
                self.markdownContent = result.markdown
                self.isLoading = false
                return
            }
        }

        if markdownContent == nil && pages.isEmpty {
            markdownContent = document.textContent
        }

        self.isLoading = false
    }

    private func verifySource() async {
        guard let documentURI,
              document.canonicalURL(publication: publication) != nil else {
            return
        }
        do {
            try await SiteStandardLexicon.Verification.verify(
                documentURI: documentURI,
                document: document,
                publication: publication
            )
            isVerified = true
        } catch {
            isVerified = false
        }
    }

    // MARK: - Block Renderers

    @ViewBuilder
    private func renderBlock(_ block: LeafletBlock, alignment: String?) -> some View {
        let align: Alignment = {
            if let a = alignment {
                if a.hasSuffix("textAlignRight") { return .trailing }
                if a.hasSuffix("textAlignCenter") { return .center }
            }
            return .leading
        }()

        let textAlignment: TextAlignment = {
            if let a = alignment {
                if a.hasSuffix("textAlignRight") { return .trailing }
                if a.hasSuffix("textAlignCenter") { return .center }
            }
            return .leading
        }()

        VStack(alignment: align.horizontal, spacing: 12) {
            switch block.type {
            case "pub.leaflet.blocks.header":
                let level = block.level ?? 1
                let style: Font.TextStyle = switch level {
                case 1: .largeTitle
                case 2: .title
                case 3: .title2
                default: .title3
                }

                Text(renderText(block.plaintext ?? "", facets: block.facets))
                    .font(theme.headingFont(style, weight: .bold))
                    .foregroundStyle(foregroundColor)
                    .multilineTextAlignment(textAlignment)
                    .padding(.top, 8)

            case "pub.leaflet.blocks.text":
                if let text = block.plaintext, !text.isEmpty {
                    Text(renderText(text, facets: block.facets))
                        .font(theme.bodyFont(.body))
                        .foregroundStyle(foregroundColor)
                        .lineSpacing(6)
                        .multilineTextAlignment(textAlignment)
                } else {
                    Spacer().frame(height: 12) // empty block spacing
                }

            case "pub.leaflet.blocks.blockquote":
                HStack(spacing: 0) {
                    Rectangle()
                        .fill(accentColor)
                        .frame(width: 4)
                        .padding(.trailing, 16)

                    Text(renderText(block.plaintext ?? "", facets: block.facets))
                        .font(theme.bodyFont(.body))
                        .italic()
                        .foregroundStyle(foregroundColor.opacity(0.7))
                        .lineSpacing(6)
                        .multilineTextAlignment(textAlignment)
                }
                .padding(.vertical, 8)

            case "pub.leaflet.blocks.code":
                VStack(alignment: .leading, spacing: 6) {
                    if let lang = block.language, !lang.isEmpty {
                        Text(lang.uppercased())
                            .font(.system(.caption2, design: .monospaced))
                            .foregroundStyle(foregroundColor.opacity(0.6))
                            .padding(.horizontal, 8)
                            .padding(.vertical, 2)
                            .background(foregroundColor.opacity(0.06))
                            .cornerRadius(4)
                    }

                    ScrollView(.horizontal, showsIndicators: false) {
                        Text(block.plaintext ?? "")
                            .font(.system(.subheadline, design: .monospaced))
                            .foregroundStyle(foregroundColor)
                            .padding(12)
                    }
                    .background(foregroundColor.opacity(0.04))
                    .cornerRadius(8)
                    .overlay(
                        RoundedRectangle(cornerRadius: 8)
                            .stroke(foregroundColor.opacity(0.12), lineWidth: 1)
                    )
                }
                .padding(.vertical, 6)

            case "pub.leaflet.blocks.math":
                HStack {
                    Spacer()
                    Text("\u{0024}\u{0024} \(block.tex ?? "") \u{0024}\u{0024}")
                        .font(theme.bodyFont(.body))
                        .italic()
                        .foregroundStyle(foregroundColor)
                        .lineLimit(10)
                        .padding(12)
                        .background(foregroundColor.opacity(0.04))
                        .cornerRadius(8)
                    Spacer()
                }
                .padding(.vertical, 8)

            case "pub.leaflet.blocks.horizontalRule":
                Divider()
                    .background(accentColor.opacity(0.2))
                    .padding(.vertical, 12)

            case "pub.leaflet.blocks.image":
                if let img = block.image, let did = authorDID ?? loginStateManager.currentDID {
                    let urlString = "https://cdn.bsky.app/img/feed_thumbnail/plain/\(did)/\(img.reference.link)"
                    if let url = URL(string: urlString) {
                        VStack(alignment: .center, spacing: 8) {
                            AsyncImage(url: url) { phase in
                                switch phase {
                                case .empty:
                                    ProgressView()
                                        .frame(minHeight: 180)
                                case .success(let image):
                                    image
                                        .resizable()
                                        .scaledToFit()
                                        .frame(maxHeight: 400)
                                        .clipShape(RoundedRectangle(cornerRadius: 8))
                                case .failure:
                                    Image(systemName: "photo")
                                        .font(.largeTitle)
                                        .foregroundStyle(foregroundColor.opacity(0.5))
                                        .frame(minHeight: 180)
                                @unknown default:
                                    EmptyView()
                                }
                            }

                            if let alt = block.alt, !alt.isEmpty {
                                Text(alt)
                                    .font(.caption)
                                    .italic()
                                    .foregroundStyle(foregroundColor.opacity(0.6))
                                    .multilineTextAlignment(.center)
                            }
                        }
                        .frame(maxWidth: .infinity)
                    }
                }

            case "pub.leaflet.blocks.unorderedList":
                renderList(block.children, ordered: false, startIndex: nil)

            case "pub.leaflet.blocks.orderedList":
                renderList(block.children, ordered: true, startIndex: block.startIndex ?? 1)

            default:
                // Fallback for unsupported blocks
                HStack {
                    Image(systemName: "questionmark.square.dashed")
                        .foregroundStyle(foregroundColor.opacity(0.5))
                    Text("Unsupported standard.site content block type")
                        .font(.caption)
                        .foregroundStyle(foregroundColor.opacity(0.5))
                }
                .padding(8)
                .background(foregroundColor.opacity(0.03))
                .cornerRadius(6)
            }
        }
        .frame(maxWidth: .infinity, alignment: align)
    }

    private func renderList(_ items: [LeafletListItem]?, ordered: Bool, startIndex: Int?) -> AnyView {
        if let items = items {
            return AnyView(
                VStack(alignment: .leading, spacing: 8) {
                    ForEach(items.indices, id: \.self) { index in
                        let item = items[index]
                        HStack(alignment: .top, spacing: 8) {
                            if ordered {
                                let itemNumber = (startIndex ?? 1) + index
                                Text("\(itemNumber).")
                                    .font(theme.bodyFont(.body))
                                    .foregroundStyle(accentColor)
                                    .frame(minWidth: 24, alignment: .trailing)
                            } else {
                                Text("•")
                                    .font(.title3)
                                    .foregroundStyle(accentColor)
                                    .frame(minWidth: 16, alignment: .center)
                            }

                            VStack(alignment: .leading, spacing: 8) {
                                if let content = item.content {
                                    renderBlock(content, alignment: nil)
                                }

                                if let nestedUnordered = item.children {
                                    renderList(nestedUnordered, ordered: false, startIndex: nil)
                                        .padding(.leading, 12)
                                }

                                if let nestedOrdered = item.orderedListChildren?.value.block {
                                    renderBlock(nestedOrdered, alignment: nil)
                                        .padding(.leading, 12)
                                }
                            }
                        }
                    }
                }
                .padding(.leading, 4)
            )
        }
        return AnyView(EmptyView())
    }

    // MARK: - AttributedString Rich Text Parser

    private func renderText(_ plaintext: String, facets: [LeafletFacet]?) -> AttributedString {
        var attrString = AttributedString(plaintext)
        guard let facets = facets, !facets.isEmpty else { return attrString }

        for facet in facets {
            let byteStart = facet.index.byteStart
            let byteEnd = facet.index.byteEnd

            guard let range = stringRange(from: byteStart, byteEnd: byteEnd, in: plaintext) else { continue }
            guard let attrRange = Range(range, in: attrString) else { continue }

            for feature in facet.features {
                switch feature.type {
                case "pub.leaflet.richtext.facet#bold":
                    attrString[attrRange].inlinePresentationIntent = .stronglyEmphasized
                case "pub.leaflet.richtext.facet#italic":
                    attrString[attrRange].inlinePresentationIntent = .emphasized
                case "pub.leaflet.richtext.facet#code":
                    attrString[attrRange].font = .system(.body, design: .monospaced)
                case "pub.leaflet.richtext.facet#strikethrough":
                    attrString[attrRange].strikethroughStyle = .single
                case "pub.leaflet.richtext.facet#link":
                    if let uriString = feature.uri, let url = URL(string: uriString) {
                        attrString[attrRange].link = url
                    }
                default:
                    break
                }
            }
        }

        return attrString
    }

    private func stringRange(from byteStart: Int, byteEnd: Int, in string: String) -> Range<String.Index>? {
        let utf8 = string.utf8
        guard byteStart >= 0, byteEnd >= byteStart, byteEnd <= utf8.count else { return nil }

        guard let startIdx = utf8.index(utf8.startIndex, offsetBy: byteStart, limitedBy: utf8.endIndex),
              let endIdx = utf8.index(utf8.startIndex, offsetBy: byteEnd, limitedBy: utf8.endIndex) else {
            return nil
        }

        return startIdx..<endIdx
    }

    private func formatDate(_ date: Date) -> String {
        let outputFormatter = DateFormatter()
        outputFormatter.dateStyle = .medium
        outputFormatter.timeStyle = .none
        return outputFormatter.string(from: date)
    }
}

// MARK: - Comment Row

struct CommentRow: View {
    let comment: CommentEntry
    let foregroundColor: Color
    let accentColor: Color
    var onReply: (() -> Void)? = nil

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(comment.record.plaintext)
                .font(.body)
                .foregroundStyle(foregroundColor)

            HStack(spacing: 8) {
                Text(comment.record.createdAt.formatted(date: .abbreviated, time: .shortened))
                    .font(.caption)
                    .foregroundStyle(foregroundColor.opacity(0.5))

                if onReply != nil {
                    Button("Reply") {
                        onReply?()
                    }
                    .font(.caption)
                    .foregroundStyle(accentColor)
                }
            }
        }
        .padding(.vertical, 4)
    }
}

extension Alignment {
    var horizontal: HorizontalAlignment {
        switch self {
        case .trailing: return .trailing
        case .center: return .center
        default: return .leading
        }
    }
}

// MARK: - Action Pill

/// A small capsule button used for the Subscribe / Recommend actions.
/// Tinted from the resolved `ReaderTheme` so it stays in step with whatever
/// the publication actually set, while the capsule shape, fill transition,
/// and bounce give Inkwell's own chrome a bit more personality than a bare
/// toolbar icon would.
private struct ReaderActionPill: View {
    let icon: String
    let label: String
    let isActive: Bool
    let isLoading: Bool
    let tint: Color
    let activeForeground: Color
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 6) {
                if isLoading {
                    ProgressView()
                        .scaleEffect(0.7)
                        .tint(isActive ? activeForeground : tint)
                } else {
                    Image(systemName: icon)
                        .symbolEffect(.bounce, value: isActive)
                }
                Text(label)
                    .lineLimit(1)
                    .truncationMode(.tail)
            }
            .font(.subheadline.weight(.semibold))
            .foregroundStyle(isActive ? activeForeground : tint)
            .padding(.horizontal, 14)
            .padding(.vertical, 8)
            .background(
                Capsule()
                    .fill(isActive ? tint : tint.opacity(0.12))
            )
            .overlay(
                Capsule()
                    .strokeBorder(tint.opacity(isActive ? 0 : 0.35), lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
        .animation(.spring(response: 0.3, dampingFraction: 0.7), value: isActive)
    }
}
