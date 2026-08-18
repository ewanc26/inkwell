//
//  ReadView+Blocks.swift
//  Inkwell
//

import SwiftUI
import ATProtoKit

extension ReadView {
    // MARK: - Block Renderers

    @ViewBuilder
    func renderBlock(_ block: LeafletBlock, alignment: String?) -> some View {
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
                                        .tint(accentColor)
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

            // MARK: Embeds

            case "pub.leaflet.blocks.bskyPost":
                if let uri = block.subject?.recordURI {
                    BSkyPostEmbedView(
                        postURI: uri,
                        foregroundColor: foregroundColor,
                        accentColor: accentColor
                    )
                }

            case "pub.leaflet.blocks.standardSitePost":
                if let uri = block.standardSitePostSubject {
                    StandardSitePostEmbedView(
                        subjectURI: uri,
                        size: block.size,
                        showPublicationTheme: block.showPublicationTheme ?? true,
                        foregroundColor: foregroundColor,
                        accentColor: accentColor
                    )
                }

            case "pub.leaflet.blocks.poll":
                if let ref = block.subject {
                    PollEmbedView(
                        pollRef: ref,
                        foregroundColor: foregroundColor,
                        accentColor: accentColor
                    )
                }

            case "pub.leaflet.blocks.website":
                WebsitePreviewBlock(
                    url: block.url ?? "",
                    title: block.websiteTitle,
                    description: block.websiteDescription,
                    foregroundColor: foregroundColor,
                    accentColor: accentColor
                )

            case "pub.leaflet.blocks.iframe":
                if let urlString = block.url, let url = URL(string: urlString) {
                    IframeBlockView(
                        url: url,
                        height: block.height,
                        aspectRatio: block.aspectRatio,
                        foregroundColor: foregroundColor
                    )
                    .frame(height: block.height ?? 300)
                }

            case "pub.leaflet.blocks.button":
                if let urlString = block.url, let text = block.text, let url = URL(string: urlString) {
                    Link(destination: url) {
                        Text(text)
                            .font(.subheadline.weight(.semibold))
                            .foregroundStyle(theme.accentForeground)
                            .padding(.horizontal, 20)
                            .padding(.vertical, 10)
                            .background(accentColor)
                            .clipShape(Capsule())
                    }
                    .buttonStyle(.plain)
                }

            case "pub.leaflet.blocks.page":
                VStack(spacing: 4) {
                    Divider().background(accentColor.opacity(0.2))
                    if let idx = block.pageIndex {
                        Text("Page \(idx + 1)")
                            .font(.caption.weight(.semibold))
                            .foregroundStyle(accentColor)
                    }
                    Divider().background(accentColor.opacity(0.2))
                }
                .padding(.vertical, 8)

            default:
                // Fallback for truly unknown blocks
                HStack {
                    Image(systemName: "questionmark.square.dashed")
                        .foregroundStyle(foregroundColor.opacity(0.5))
                    Text("Unsupported content block: \(block.type)")
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

    func renderList(_ items: [LeafletListItem]?, ordered: Bool, startIndex: Int?) -> AnyView {
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

    func renderText(_ plaintext: String, facets: [LeafletFacet]?) -> AttributedString {
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

    func stringRange(from byteStart: Int, byteEnd: Int, in string: String) -> Range<String.Index>? {
        let utf8 = string.utf8
        guard byteStart >= 0, byteEnd >= byteStart, byteEnd <= utf8.count else { return nil }

        guard let startIdx = utf8.index(utf8.startIndex, offsetBy: byteStart, limitedBy: utf8.endIndex),
              let endIdx = utf8.index(utf8.startIndex, offsetBy: byteEnd, limitedBy: utf8.endIndex) else {
            return nil
        }

        return startIdx..<endIdx
    }

    func formatDate(_ date: Date) -> String {
        let outputFormatter = DateFormatter()
        outputFormatter.dateStyle = .medium
        outputFormatter.timeStyle = .none
        return outputFormatter.string(from: date)
    }
}
