//
//  MarkdownRendererView.swift
//  Inkwell
//
//  Created by Letta on 20/06/2026.
//
//  A SwiftUI view that renders markdown text using the same MarkdownParser
//  the editor uses (see ContentProvider.swift). This is the read-side
//  counterpart to the write-side markdown editor: when a document's content
//  is in a format other than Leaflet (or when we want a unified rendering
//  path), the ContentProvider system converts it to markdown, and this view
//  turns that markdown back into SwiftUI views.
//
//  Not a full CommonMark renderer — it handles the same block types the
//  MarkdownParser supports (headings, paragraphs, code, math, blockquotes,
//  images, lists, task lists, horizontal rules) with inline formatting
//  via AttributedString.
//

import SwiftUI

struct MarkdownRendererView: View {
    let markdown: String
    let theme: ReaderTheme

    private var foregroundColor: Color { theme.foreground }
    private var accentColor: Color { theme.accent }

    private var blocks: [MarkdownBlockNode] {
        MarkdownParserEngine.parse(markdown)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            ForEach(blocks.indices, id: \.self) { idx in
                renderBlock(blocks[idx])
            }
        }
    }

    @ViewBuilder
    private func renderBlock(_ block: MarkdownBlockNode) -> some View {
        switch block {
        case .heading(let level, let text):
            let style: Font.TextStyle = switch level {
            case 1: .largeTitle
            case 2: .title
            case 3: .title2
            case 4: .title3
            case 5: .headline
            default: .headline
            }
            Text(renderInline(text))
                .font(theme.headingFont(style, weight: .bold))
                .foregroundStyle(foregroundColor)
                .padding(.top, level == 1 ? 8 : 4)

        case .paragraph(let text):
            Text(renderInline(text))
                .font(theme.bodyFont(.body))
                .foregroundStyle(foregroundColor)
                .lineSpacing(6)

        case .code(let language, let content):
            VStack(alignment: .leading, spacing: 6) {
                if let lang = language, !lang.isEmpty {
                    Text(lang.uppercased())
                        .font(.system(.caption2, design: .monospaced))
                        .foregroundStyle(foregroundColor.opacity(0.6))
                        .padding(.horizontal, 8)
                        .padding(.vertical, 2)
                        .background(foregroundColor.opacity(0.06))
                        .cornerRadius(4)
                }
                ScrollView(.horizontal, showsIndicators: false) {
                    Text(content)
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

        case .math(let tex):
            HStack {
                Spacer()
                Text("\u{0024}\u{0024} \(tex) \u{0024}\u{0024}")
                    .font(theme.bodyFont(.body))
                    .italic()
                    .foregroundStyle(foregroundColor)
                    .padding(12)
                    .background(foregroundColor.opacity(0.04))
                    .cornerRadius(8)
                Spacer()
            }
            .padding(.vertical, 8)

        case .blockquote(let text):
            HStack(spacing: 0) {
                Rectangle()
                    .fill(accentColor)
                    .frame(width: 4)
                    .padding(.trailing, 16)
                Text(renderInline(text))
                    .font(theme.bodyFont(.body))
                    .italic()
                    .foregroundStyle(foregroundColor.opacity(0.7))
                    .lineSpacing(6)
            }
            .padding(.vertical, 8)

        case .image(let alt, let url):
            // Images referenced by URL (Pckt allows external URLs).
            // Blob-referenced images are handled by the Leaflet block renderer.
            if let imgURL = URL(string: url) {
                VStack(alignment: .center, spacing: 8) {
                    AsyncImage(url: imgURL) { phase in
                        switch phase {
                        case .empty:
                            ProgressView().frame(minHeight: 180)
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
                    if !alt.isEmpty {
                        Text(alt)
                            .font(.caption)
                            .italic()
                            .foregroundStyle(foregroundColor.opacity(0.6))
                    }
                }
                .frame(maxWidth: .infinity)
            }

        case .horizontalRule:
            Divider()
                .background(accentColor.opacity(0.2))
                .padding(.vertical, 12)

        case .unorderedList(let items):
            renderListItems(items, ordered: false, startIndex: nil)

        case .orderedList(let start, let items):
            renderListItems(items, ordered: true, startIndex: start)

        case .taskList(let items):
            renderTaskList(items)
        }
    }

    private func renderListItems(_ items: [MarkdownListItemNode], ordered: Bool, startIndex: Int?) -> AnyView {
        AnyView(VStack(alignment: .leading, spacing: 8) {
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
                        Text("\u{2022}")
                            .font(.title3)
                            .foregroundStyle(accentColor)
                            .frame(minWidth: 16, alignment: .center)
                    }

                    VStack(alignment: .leading, spacing: 8) {
                        Text(renderInline(item.text))
                            .font(theme.bodyFont(.body))
                            .foregroundStyle(foregroundColor)

                        if let children = item.children, !children.isEmpty {
                            renderListItems(children, ordered: false, startIndex: nil)
                                .padding(.leading, 12)
                        }
                    }
                }
            }
        }
        .padding(.leading, 4))
    }

    private func renderTaskList(_ items: [MarkdownListItemNode]) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            ForEach(items.indices, id: \.self) { index in
                let item = items[index]
                HStack(alignment: .top, spacing: 8) {
                    Image(systemName: item.checked == true ? "checkmark.square.fill" : "square")
                        .foregroundStyle(item.checked == true ? accentColor : foregroundColor.opacity(0.5))
                        .frame(width: 20, alignment: .center)

                    Text(renderInline(item.text))
                        .font(theme.bodyFont(.body))
                        .foregroundStyle(foregroundColor)
                        .strikethrough(item.checked == true)
                }
            }
        }
        .padding(.leading, 4)
    }

    // MARK: - Inline Formatting

    /// Converts markdown inline syntax (**bold**, *italic*, `code`,
    /// ~~strike~~, [text](url)) to an AttributedString.
    private func renderInline(_ text: String) -> AttributedString {
        var attributed = (try? AttributedString(
            markdown: text,
            options: AttributedString.MarkdownParsingOptions(
                interpretedSyntax: .inlineOnlyPreservingWhitespace
            )
        )) ?? AttributedString(text)

        if AccessibilitySettings.shared.underlineLinks {
            for run in attributed.runs where run.link != nil {
                attributed[run.range].underlineStyle = .single
            }
        }

        return attributed
    }
}
