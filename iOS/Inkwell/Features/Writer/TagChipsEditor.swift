//
//  TagChipsEditor.swift
//  Inkwell
//
//  Wrapping row of removable tag chips plus an entry field. Chips wrap onto
//  new lines rather than scrolling sideways so they stay readable at the
//  largest Dynamic Type sizes.
//

import SwiftUI

struct TagChipsEditor: View {
    let tags: [String]
    let onAdd: (String) -> Void
    let onRemove: (String) -> Void
    @State private var entry = ""

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            if !tags.isEmpty {
                FlowLayout(spacing: 6) {
                    ForEach(tags, id: \.self) { tag in
                        chip(tag)
                    }
                }
            }
            HStack {
                TextField("Add tag", text: $entry)
                    .autocorrectionDisabled()
                    .textInputAutocapitalization(.never)
                    .submitLabel(.done)
                    .onSubmit(commit)
                    .accessibilityHint("Separate several tags with commas.")
                Button("Add", action: commit)
                    .disabled(entry.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
            }
        }
    }

    private func commit() {
        onAdd(entry)
        entry = ""
    }

    private func chip(_ tag: String) -> some View {
        Button {
            onRemove(tag)
        } label: {
            HStack(spacing: 4) {
                Text(tag)
                Image(systemName: "xmark.circle.fill")
                    .imageScale(.small)
                    .foregroundStyle(.secondary)
            }
            .font(.subheadline)
            .padding(.horizontal, 10)
            .padding(.vertical, 5)
            .background(Color(.secondarySystemFill), in: Capsule())
        }
        .buttonStyle(.plain)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("Tag \(tag)")
        .accessibilityHint("Removes this tag.")
        .accessibilityAddTraits(.isButton)
    }
}

/// Lays subviews out left-to-right, wrapping to a new line when a row is full.
struct FlowLayout: Layout {
    var spacing: CGFloat = 8

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let rows = arrange(subviews, width: proposal.width ?? .infinity)
        let height = rows.last.map { $0.y + $0.height } ?? 0
        let width = rows.map(\.width).max() ?? 0
        return CGSize(width: proposal.width ?? width, height: height)
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        for row in arrange(subviews, width: bounds.width) {
            var x = bounds.minX
            for index in row.indices {
                let size = subviews[index].sizeThatFits(.unspecified)
                subviews[index].place(
                    at: CGPoint(x: x, y: bounds.minY + row.y),
                    proposal: ProposedViewSize(width: min(size.width, bounds.width), height: size.height)
                )
                x += min(size.width, bounds.width) + spacing
            }
        }
    }

    private struct Row {
        var indices: [Int] = []
        var y: CGFloat = 0
        var width: CGFloat = 0
        var height: CGFloat = 0
    }

    private func arrange(_ subviews: Subviews, width maxWidth: CGFloat) -> [Row] {
        var rows: [Row] = []
        var current = Row()
        for index in subviews.indices {
            let size = subviews[index].sizeThatFits(.unspecified)
            let itemWidth = min(size.width, maxWidth)
            let proposedWidth = current.indices.isEmpty ? itemWidth : current.width + spacing + itemWidth
            if !current.indices.isEmpty && proposedWidth > maxWidth {
                let nextY = current.y + current.height + spacing
                rows.append(current)
                current = Row(y: nextY)
            }
            current.width = current.indices.isEmpty ? itemWidth : current.width + spacing + itemWidth
            current.height = max(current.height, size.height)
            current.indices.append(index)
        }
        if !current.indices.isEmpty { rows.append(current) }
        return rows
    }
}
