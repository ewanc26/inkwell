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

    private func wrapSelection(before: String, after: String) {
        markdown += "\(before)text\(after)"
    }

    private func prependToLine(_ prefix: String) {
        markdown += "\n\(prefix)"
    }

    private func insertText(_ text: String) {
        markdown += "\n\(text)"
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
                .frame(width: 36, height: 32)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(label)
    }
}

#Preview {
    @Previewable @State var markdown = "# Hello\n\nSome text here."
    FormattingToolbar(
        markdown: $markdown,
        canUploadImages: true,
        onImagePicker: {}
    )
}
