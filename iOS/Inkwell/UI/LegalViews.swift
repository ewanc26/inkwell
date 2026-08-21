//
//  LegalViews.swift
//  Inkwell
//
//  Privacy Policy and Terms of Service screens. The documents themselves
//  live in the shared KMP module (SharedLegalDocuments, generated from the
//  repo-root legal directory by tools/legal/render.mjs -- do not hand-edit
//  LegalDocuments.kt, edit the source and regenerate) and render with the
//  same MarkdownRendererView used for reading Standard.site content, so
//  this file only wires the shared markdown to a navigable screen.
//

import SwiftUI

enum LegalDocumentType: Hashable, Identifiable {
    var id: Self { self }

    case privacyPolicy
    case termsOfService

    var title: String {
        switch self {
        case .privacyPolicy: return "Privacy Policy"
        case .termsOfService: return "Terms of Service"
        }
    }

    var markdown: String {
        switch self {
        case .privacyPolicy: return SharedLegalDocuments.privacyMarkdown
        case .termsOfService: return SharedLegalDocuments.termsMarkdown
        }
    }
}

struct LegalDocumentView: View {
    let documentType: LegalDocumentType
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        ScrollView {
            MarkdownRendererView(
                markdown: documentType.markdown,
                theme: ReaderTheme(colorScheme: colorScheme)
            )
            .padding()
        }
        .navigationTitle(documentType.title)
        .navigationBarTitleDisplayMode(.inline)
    }
}

#Preview("Privacy Policy") {
    NavigationStack {
        LegalDocumentView(documentType: .privacyPolicy)
    }
}

#Preview("Terms of Service") {
    NavigationStack {
        LegalDocumentView(documentType: .termsOfService)
    }
}
