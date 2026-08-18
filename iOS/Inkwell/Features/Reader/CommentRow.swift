//
//  CommentRow.swift
//  Inkwell
//

import SwiftUI

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
