//
//  SharedKMP+Jetstream.swift
//  Inkwell
//
//  Swift wrappers around the shared KMP Jetstream client and feed cache.
//  The InkwellShared.xcframework must be rebuilt after changing any
//  shared/ source file.
//

import Foundation
import InkwellShared

// MARK: - Jetstream Client

/// Creates a platform-specific JetstreamClient backed by Ktor + Darwin.
func createSharedJetstreamClient() -> JetstreamClient {
    SharedKMPJetstreamKt.createJetstreamClient()
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
    return SharedKMPFeedKt.createFeedCache(cacheDirPath: cacheDir.path)
}

// MARK: - CachedFeedItem Conversion

extension CachedFeedItem {
    /// Converts a shared CachedFeedItem to a native iOS ReaderFeedItem.
    /// The caller is responsible for resolving profiles separately.
    func toReaderFeedItem(
        profile: BSkyActorProfile? = nil,
        publication: PublicationEntry? = nil
    ) -> ReaderFeedItem {
        let docRecord = SiteStandardLexicon.DocumentRecord(
            site: site,
            title: title,
            publishedAt: publishedAt,
            path: path,
            description: description,
            textContent: textContent,
            coverImage: coverImageUrl.map { BlobRef(link: $0, size: 0, type: "image/jpeg", mimeType: "image/jpeg") }
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
            publishedAt: document.record.publishedAt,
            path: document.record.path,
            description: document.record.description,
            textContent: document.record.textContent,
            coverImageUrl: document.record.coverImage?.link,
            publicationUri: publication?.uri,
            publicationName: publication?.record.name,
            publicationUrl: publication?.record.url,
            authorDisplayName: authorProfile?.displayName,
            authorAvatar: authorProfile?.avatar,
            cachedAt: Int64(Date().timeIntervalSince1970 * 1000)
        )
    }
}
