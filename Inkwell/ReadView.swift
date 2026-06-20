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

    let document: SiteStandardLexicon.DocumentRecord
    let publication: SiteStandardLexicon.PublicationRecord?
    var documentURI: String? = nil
    var authorDID: String? = nil

    @State private var pages: [LeafletPage] = []
    @State private var markdownContent: String? = nil
    @State private var isLoading = true
    @State private var errorMessage: String? = nil
    @State private var isVerified: Bool?

    // Theme styling matching standard.site's basicTheme colors
    private var backgroundColor: Color {
        if let rgb = publication?.basicTheme?.background {
            return rgb.color
        }
        return Color(uiColor: .systemBackground)
    }

    private var foregroundColor: Color {
        if let rgb = publication?.basicTheme?.foreground {
            return rgb.color
        }
        return Color(uiColor: .label)
    }

    private var accentColor: Color {
        if let rgb = publication?.basicTheme?.accent {
            return rgb.color
        }
        return .accentColor
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 24) {
                // Header section
                VStack(alignment: .leading, spacing: 12) {
                    if let pubName = publication?.name {
                        Text(pubName.uppercased())
                            .font(.system(.caption, design: .serif))
                            .fontWeight(.bold)
                            .foregroundStyle(accentColor)
                            .tracking(2)
                    }

                    Text(document.title)
                        .font(.system(.largeTitle, design: .serif))
                        .fontWeight(.bold)
                        .foregroundStyle(foregroundColor)
                        .lineSpacing(4)

                    HStack(spacing: 8) {
                        Text("Published")
                        Text(formatDate(document.publishedAt))
                            .fontWeight(.medium)

                        if let path = document.path {
                            Spacer()
                            Text(path)
                                .font(.caption2)
                                .foregroundStyle(.secondary)
                        }
                    }
                    .font(.caption)
                    .foregroundStyle(.secondary)

                    HStack(spacing: 12) {
                        if let isVerified {
                            Label(
                                isVerified ? "Verified source" : "Unverified source",
                                systemImage: isVerified ? "checkmark.seal.fill" : "exclamationmark.triangle"
                            )
                            .foregroundStyle(isVerified ? Color.green : Color.orange)
                        }
                        if let url = document.canonicalURL(publication: publication) {
                            Link("Open original", destination: url)
                        }
                    }
                    .font(.caption)

                    Divider()
                        .background(accentColor.opacity(0.3))
                }
                .padding(.top, 16)

                // Excerpt/description
                if let desc = document.description, !desc.isEmpty {
                    Text(desc)
                        .font(.system(.body, design: .serif))
                        .italic()
                        .foregroundStyle(.secondary)
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
                    let urlString = "https://cdn.bsky.app/img/feed_fullsize/plain/\(did)/\(cover.reference.link)"
                    if let url = URL(string: urlString) {
                        AsyncImage(url: url) { image in
                            image
                                .resizable()
                                .scaledToFit()
                                .clipShape(RoundedRectangle(cornerRadius: 12))
                                .shadow(color: .black.opacity(0.08), radius: 8, y: 4)
                        } placeholder: {
                            ProgressView()
                                .frame(maxWidth: .infinity, minHeight: 200)
                        }
                    }
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
                    MarkdownRendererView(markdown: markdown, foregroundColor: foregroundColor, accentColor: accentColor)
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
        }
        .background(backgroundColor)
        .navigationBarTitleDisplayMode(.inline)
        .toolbarBackground(backgroundColor, for: .navigationBar)
        .toolbarBackground(.visible, for: .navigationBar)
        .task {
            await loadContent()
        }
        .task(id: documentURI) {
            await verifySource()
        }
    }

    // MARK: - Content Loader

    private func loadContent() async {
        guard let contentUnknown = document.content else {
            self.markdownContent = document.textContent
            isLoading = false
            return
        }

        // First, try the ContentProvider system — this handles all
        // supported formats (Leaflet, Markpub, Pckt, Offprint) by
        // detecting the $type and converting to markdown.
        if let provider = ProviderRegistry.detectProvider(contentUnknown) {
            let result = provider.toMarkdown(contentUnknown)
            if !result.markdown.isEmpty {
                self.markdownContent = result.markdown
                self.isLoading = false
                return
            }
        }

        // Fallback: try Leaflet-specific decoding with blob page support.
        // This path handles Leaflet documents that store their blocks in a
        // blob (downloaded separately), which the ContentProvider system
        // doesn't handle because it would need async blob fetching.
        if let leaflet = contentUnknown.getRecord(ofType: LeafletContent.self) {
            if let inlinePages = leaflet.pages, !inlinePages.isEmpty {
                self.pages = inlinePages
                self.isLoading = false
                return
            }

            if let blobPages = leaflet.blobPages {
                do {
                    let data: Data
                    if let authorDID {
                        data = try await loginStateManager.downloadBlob(cid: blobPages.reference.link, fromDID: authorDID)
                    } else {
                        data = try await loginStateManager.downloadBlob(cid: blobPages.reference.link)
                    }
                    let decodedPages = try JSONDecoder().decode([LeafletPage].self, from: data)
                    self.pages = decodedPages
                } catch {
                    self.errorMessage = "Failed to download pages blob: \(error.localizedDescription)"
                }
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
                let fontSize: CGFloat = {
                    switch level {
                    case 1: return 28
                    case 2: return 24
                    case 3: return 20
                    default: return 18
                    }
                }()

                Text(renderText(block.plaintext ?? "", facets: block.facets))
                    .font(.system(size: fontSize, design: .serif))
                    .fontWeight(.bold)
                    .foregroundStyle(foregroundColor)
                    .multilineTextAlignment(textAlignment)
                    .padding(.top, 8)

            case "pub.leaflet.blocks.text":
                if let text = block.plaintext, !text.isEmpty {
                    Text(renderText(text, facets: block.facets))
                        .font(.system(.body, design: .serif))
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
                        .font(.system(.body, design: .serif))
                        .italic()
                        .foregroundStyle(.secondary)
                        .lineSpacing(6)
                        .multilineTextAlignment(textAlignment)
                }
                .padding(.vertical, 8)

            case "pub.leaflet.blocks.code":
                VStack(alignment: .leading, spacing: 6) {
                    if let lang = block.language, !lang.isEmpty {
                        Text(lang.uppercased())
                            .font(.system(.caption2, design: .monospaced))
                            .foregroundStyle(.secondary)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 2)
                            .background(Color.primary.opacity(0.05))
                            .cornerRadius(4)
                    }

                    ScrollView(.horizontal, showsIndicators: false) {
                        Text(block.plaintext ?? "")
                            .font(.system(.subheadline, design: .monospaced))
                            .foregroundStyle(.primary)
                            .padding(12)
                    }
                    .background(Color.primary.opacity(0.03))
                    .cornerRadius(8)
                    .overlay(
                        RoundedRectangle(cornerRadius: 8)
                            .stroke(Color.primary.opacity(0.08), lineWidth: 1)
                    )
                }
                .padding(.vertical, 6)

            case "pub.leaflet.blocks.math":
                HStack {
                    Spacer()
                    Text("$$ \(block.tex ?? "") $$")
                        .font(.system(.body, design: .serif))
                        .italic()
                        .padding(12)
                        .background(Color.primary.opacity(0.03))
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
                    let urlString = "https://cdn.bsky.app/img/feed_fullsize/plain/\(did)/\(img.reference.link)"
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
                                        .clipShape(RoundedRectangle(cornerRadius: 8))
                                case .failure:
                                    Image(systemName: "photo")
                                        .font(.largeTitle)
                                        .foregroundStyle(.secondary)
                                        .frame(minHeight: 180)
                                @unknown default:
                                    EmptyView()
                                }
                            }

                            if let alt = block.alt, !alt.isEmpty {
                                Text(alt)
                                    .font(.caption)
                                    .italic()
                                    .foregroundStyle(.secondary)
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
                        .foregroundStyle(.secondary)
                    Text("Unsupported standard.site content block type")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                .padding(8)
                .background(Color.primary.opacity(0.02))
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
                                    .font(.system(.body, design: .serif))
                                    .foregroundStyle(accentColor)
                                    .frame(width: 20, alignment: .trailing)
                            } else {
                                Text("•")
                                    .font(.title3)
                                    .foregroundStyle(accentColor)
                                    .frame(width: 20, alignment: .center)
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

extension Alignment {
    var horizontal: HorizontalAlignment {
        switch self {
        case .trailing: return .trailing
        case .center: return .center
        default: return .leading
        }
    }
}
