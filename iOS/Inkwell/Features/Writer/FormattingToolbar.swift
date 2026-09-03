//
//  FormattingToolbar.swift
//  Inkwell
//
//  A horizontal toolbar of markdown formatting buttons for the writer.
//  Each button inserts markdown syntax at the cursor position.
//

import SwiftUI

struct FormattingToolbar: View {
    @Binding var markdown: String
    /// The `TextEditor`'s current selection, so formatting is inserted at
    /// the cursor (or wraps the actual selected text) instead of always
    /// being appended to the end of the document.
    @Binding var selection: TextSelection?
    var canUploadImages: Bool
    var onImagePicker: () -> Void

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 4) {
                FormatButton(icon: "bold", label: "Bold") {
                    wrapSelection(before: "**", after: "**")
                }
                FormatButton(icon: "italic", label: "Italic") {
                    wrapSelection(before: "*", after: "*")
                }
                FormatButton(icon: "heading", label: "Heading") {
                    prependToLine("## ")
                }
                FormatButton(icon: "text.quote", label: "Quote") {
                    prependToLine("> ")
                }
                FormatButton(icon: "curlybraces", label: "Code") {
                    wrapSelection(before: "`", after: "`")
                }
                FormatButton(icon: "link", label: "Link") {
                    insertText("[text](url)")
                }
                FormatButton(icon: "photo", label: "Image") {
                    onImagePicker()
                }
                .disabled(!canUploadImages)
                .opacity(canUploadImages ? 1 : 0.4)
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 6)
        }
        .background(Color(.secondarySystemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 8))
    }

    // MARK: - Markdown Insertion Helpers
    //
    // All three helpers operate at the TextEditor's actual cursor/selection
    // rather than always appending to the end of the document — the
    // selection binding is a RangeSet<String.Index> over `markdown` itself,
    // per SwiftUI's TextSelection API (iOS 17+).

    /// The current selection as a single range, collapsed to the cursor
    /// position (start == end) if nothing is selected or the selection
    /// spans multiple discontiguous runs. Falls back to the end of the
    /// document if there's no selection at all (e.g. the editor was never
    /// focused).
    private var selectedRange: Range<String.Index> {
        guard let indices = selection?.indices else {
            return markdown.endIndex..<markdown.endIndex
        }
        switch indices {
        case .selection(let range):
            return range
        case .multiSelection(let rangeSet):
            return rangeSet.ranges.first ?? (markdown.endIndex..<markdown.endIndex)
        @unknown default:
            return markdown.endIndex..<markdown.endIndex
        }
    }

    /// Wraps the selected text in `before`/`after` (e.g. `**bold**`). With
    /// no selection, inserts an empty pair at the cursor with the cursor
    /// left between them, ready to type.
    private func wrapSelection(before: String, after: String) {
        let range = selectedRange
        let selectedText = String(markdown[range])
        // Computed before mutating `markdown` — String.Index values from
        // the old string aren't valid on the mutated one, so the cursor
        // target has to be captured as a UTF-16 offset first.
        let startOffset = utf16Offset(of: range.lowerBound)
        let replacement = "\(before)\(selectedText)\(after)"

        markdown.replaceSubrange(range, with: replacement)

        let cursorOffset = startOffset + (selectedText.isEmpty ? before.utf16.count : replacement.utf16.count)
        setCursor(atUTF16Offset: cursorOffset)
    }

    /// Inserts `prefix` at the start of the line containing the cursor
    /// (e.g. `## ` for a heading, `> ` for a quote) rather than always
    /// starting a new line at the end of the document.
    private func prependToLine(_ prefix: String) {
        let range = selectedRange
        let lineStart = markdown.lineRange(for: range.lowerBound..<range.lowerBound).lowerBound
        let lineStartOffset = utf16Offset(of: lineStart)

        markdown.insert(contentsOf: prefix, at: lineStart)

        setCursor(atUTF16Offset: lineStartOffset + prefix.utf16.count)
    }

    /// Inserts literal text at the cursor, replacing any selection.
    private func insertText(_ text: String) {
        let range = selectedRange
        let startOffset = utf16Offset(of: range.lowerBound)

        markdown.replaceSubrange(range, with: text)

        setCursor(atUTF16Offset: startOffset + text.utf16.count)
    }

    private func utf16Offset(of index: String.Index) -> Int {
        markdown.utf16.distance(from: markdown.utf16.startIndex, to: index.samePosition(in: markdown.utf16) ?? markdown.utf16.startIndex)
    }

    /// Places the cursor (collapsed selection) at the given UTF-16 offset
    /// into the (already-updated) `markdown` string.
    private func setCursor(atUTF16Offset offset: Int) {
        guard let utf16Index = markdown.utf16.index(markdown.utf16.startIndex, offsetBy: offset, limitedBy: markdown.utf16.endIndex),
              let stringIndex = utf16Index.samePosition(in: markdown) else {
            selection = TextSelection(insertionPoint: markdown.endIndex)
            return
        }
        selection = TextSelection(insertionPoint: stringIndex)
    }
}

// MARK: - Format Button

private struct FormatButton: View {
    let icon: String
    let label: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Image(systemName: icon)
                .font(.system(size: 15, weight: .medium))
                .frame(width: 44, height: 44)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(label)
    }
}

#Preview {
    @Previewable @State var markdown = "# Hello\n\nSome text here."
    @Previewable @State var selection: TextSelection?
    FormattingToolbar(
        markdown: $markdown,
        selection: $selection,
        canUploadImages: true,
        onImagePicker: {}
    )
}
