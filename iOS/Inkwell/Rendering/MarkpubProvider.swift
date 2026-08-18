//
//  MarkpubProvider.swift
//  Inkwell
//

import Foundation
import InkwellShared
import ATProtoKit

// MARK: - Markpub Provider

/// Markpub provider (`at.markpub.markdown`). Markpub stores GFM markdown
/// directly, so conversion is near-identity: read the inline `text.markdown`
/// and write it straight back. Nothing is ever lost.
struct MarkpubProvider: ContentProvider {
    let id = "markpub"
    let label = "Markdown (markpub)"
    let contentType = "at.markpub.markdown"
    let supportsImages = false

    func matches(_ content: UnknownType?) -> Bool {
        content?.getRecord(ofType: MarkpubContent.self) != nil
    }

    func toMarkdown(_ content: UnknownType?) -> ConvertResult {
        guard let markpub = content?.getRecord(ofType: MarkpubContent.self) else {
            return ConvertResult(markdown: "", lost: [])
        }
        return ConvertResult(markdown: markpub.text.markdown ?? "", lost: [])
    }

    func fromMarkdown(_ markdown: String, ctx: WriteContext) -> UnknownType? {
        let text = MarkpubText(type: "at.markpub.text", markdown: markdown)
        let content = MarkpubContent(text: text)
        return UnknownType.record(content)
    }
}
