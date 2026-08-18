//
//  ProviderRegistry.swift
//  Inkwell
//

import Foundation
import ATProtoKit

// MARK: - Provider Registry

/// All providers, markpub first (the default for new posts, matching standard.horse).
enum ProviderRegistry {
    static let providers: [ContentProvider] = [
        MarkpubProvider(),
        LeafletProvider(),
        PcktProvider(),
        OffprintProvider(),
    ]

    static let defaultProvider = providers[0]

    /// The provider that handles a stored content object, if any.
    static func detectProvider(_ content: UnknownType?) -> ContentProvider? {
        providers.first(where: { $0.matches(content) })
    }

    /// Find a provider by its id.
    static func providerById(_ id: String) -> ContentProvider? {
        providers.first(where: { $0.id == id })
    }

    /// The provider whose `$type` matches the given content type string.
    static func providerByContentType(_ type: String?) -> ContentProvider? {
        guard let type = type else { return nil }
        return providers.first(where: { $0.contentType == type })
    }
}
