//
//  SharedKMP+Jetstream.swift
//  Inkwell
//
//  Swift wrappers around the shared KMP Jetstream client and feed cache.
//  The InkwellShared.xcframework must be rebuilt after changing any
//  shared/ source file.
//

import Foundation
import ATProtoKit
import InkwellShared

private final class JetstreamFlowCollector: NSObject, Kotlinx_coroutines_coreFlowCollector {
    let continuation: AsyncStream<JetstreamPayload>.Continuation

    init(continuation: AsyncStream<JetstreamPayload>.Continuation) {
        self.continuation = continuation
    }

    func emit(value: Any?, completionHandler: @escaping (Error?) -> Void) {
        if let payload = value as? JetstreamPayload {
            continuation.yield(payload)
        }
        completionHandler(nil)
    }
}

func streamJetstreamPayloads(
    client: JetstreamClient,
    config: JetstreamConfig
) -> AsyncStream<JetstreamPayload> {
    AsyncStream { continuation in
        let collector = JetstreamFlowCollector(continuation: continuation)
        client.connect(config: config).collect(collector: collector) { _ in
            continuation.finish()
        }
    }
}

// MARK: - Jetstream Client

/// Creates a platform-specific JetstreamClient backed by Ktor + Darwin.
func createSharedJetstreamClient() -> JetstreamClient {
    CreateJetstreamClient_iosKt.createJetstreamClient()
}

/// Creates a JetstreamConfig for the given subscription DIDs.
func createJetstreamConfig(
    dids: [String],
    collections: [String] = ["site.standard.document"],
    cursor: Int64? = nil
) -> JetstreamConfig {
    JetstreamConfig(
        collections: collections,
        dids: dids,
        cursor: cursor.map { KotlinLong(value: $0) }
    )
}

// MARK: - Feed Cache

/// Creates a platform-specific FeedCache backed by a JSON file.
func createSharedFeedCache() -> FeedCache {
    let cacheDir = FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask).first!
    return CreateFeedCache_iosKt.createFeedCache(cacheDirPath: cacheDir.path)
}

// MARK: - CachedFeedItem Conversion

extension CachedFeedItem {
    /// Converts a shared CachedFeedItem to a native iOS ReaderFeedItem.
    /// The caller is responsible for resolving profiles separately.
    func toReaderFeedItem(
        profile: BSkyActorProfile? = nil,
        publication: PublicationEntry? = nil
    ) -> ReaderFeedItem {
        let publishedDate = ISO8601DateFormatter().date(from: publishedAt) ?? Date(timeIntervalSince1970: 0)
        let docRecord = SiteStandardLexicon.DocumentRecord(
            site: site,
            title: title,
            publishedAt: publishedDate,
            path: path,
            description: description,
            coverImage: coverImageUrl.map { BlobRef(link: $0, size: 0, type: "image/jpeg", mimeType: "image/jpeg").toiOS() },
            textContent: textContent
        )
        let docEntry = DocumentEntry(uri: uri, authorDID: authorDID, record: docRecord)
        return ReaderFeedItem(
            document: docEntry,
            publication: publication,
            authorProfile: profile
        )
    }
}

extension ReaderFeedItem {
    /// Converts a native iOS ReaderFeedItem to a shared CachedFeedItem for cache storage.
    func toCachedFeedItem() -> CachedFeedItem {
        CachedFeedItem(
            uri: document.uri,
            authorDID: document.authorDID,
            site: document.record.site,
            title: document.record.title,
            publishedAt: ISO8601DateFormatter().string(from: document.record.publishedAt),
            path: document.record.path,
            description: document.record.description,
            textContent: document.record.textContent,
            coverImageUrl: document.record.coverImage?.reference.link,
            publicationUri: publication?.uri,
            publicationName: publication?.record.name,
            publicationUrl: publication?.record.url,
            authorDisplayName: authorProfile?.displayName,
            authorAvatar: authorProfile?.avatar,
            cachedAt: Int64(Date().timeIntervalSince1970 * 1000)
        )
    }
}
