import Foundation
import InkwellShared

/**
 * Swift adapter for the shared bounded cache. The shared layer owns retention
 * and file storage; this adapter only encodes the app's native wire models.
 */
@MainActor
final class OfflineContentStore {
    static let shared = OfflineContentStore()

    private let cache: OfflineContentCache
    private let feedCache: FeedCache

    private init() {
        let directory = FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask).first!
        cache = CreateOfflineContentCache_iosKt.createOfflineContentCache(cacheDirPath: directory.path)
        feedCache = createSharedFeedCache()
    }

    func cache(document: DocumentEntry) async {
        guard let json = try? encoded(document.record) else { return }
        let record = CachedOfflineRecord(
            uri: document.uri,
            kind: .document,
            authorDid: document.authorDID,
            cid: document.cid,
            recordJson: json,
            cachedAtMillis: Int64(Date().timeIntervalSince1970 * 1_000),
            lastAccessedAtMillis: Int64(Date().timeIntervalSince1970 * 1_000)
        )
        try? await cache.upsert(record: record)
    }

    func cache(publication: PublicationEntry) async {
        guard let json = try? encoded(publication.record) else { return }
        let now = Int64(Date().timeIntervalSince1970 * 1_000)
        let record = CachedOfflineRecord(
            uri: publication.uri,
            kind: .publication,
            authorDid: publication.authorDID,
            cid: nil,
            recordJson: json,
            cachedAtMillis: now,
            lastAccessedAtMillis: now
        )
        try? await cache.upsert(record: record)
    }

    func document(uri: String) async -> DocumentEntry? {
        guard let cached = try? await cache.load(uri: uri), cached.kind == .document,
              let record = try? decoded(SiteStandardLexicon.DocumentRecord.self, from: cached.recordJson) else {
            return nil
        }
        return DocumentEntry(uri: cached.uri, cid: cached.cid, authorDID: cached.authorDid, record: record)
    }

    func publication(uri: String) async -> PublicationEntry? {
        guard let cached = try? await cache.load(uri: uri), cached.kind == .publication,
              let record = try? decoded(SiteStandardLexicon.PublicationRecord.self, from: cached.recordJson) else {
            return nil
        }
        return PublicationEntry(uri: cached.uri, authorDID: cached.authorDid, record: record)
    }

    func documents(authorDID: String) async -> [DocumentEntry] {
        let records = (try? await cache.loadAll()) ?? []
        return records.compactMap { cached -> DocumentEntry? in
            guard cached.kind == .document, cached.authorDid == authorDID,
                  let record = try? decoded(SiteStandardLexicon.DocumentRecord.self, from: cached.recordJson) else {
                return nil
            }
            return DocumentEntry(uri: cached.uri, cid: cached.cid, authorDID: cached.authorDid, record: record)
        }
    }

    func publications(authorDID: String) async -> [PublicationEntry] {
        let records = (try? await cache.loadAll()) ?? []
        return records.compactMap { cached -> PublicationEntry? in
            guard cached.kind == .publication, cached.authorDid == authorDID,
                  let record = try? decoded(SiteStandardLexicon.PublicationRecord.self, from: cached.recordJson) else {
                return nil
            }
            return PublicationEntry(uri: cached.uri, authorDID: cached.authorDid, record: record)
        }
    }

    func publications(uris: Set<String>) async -> [String: PublicationEntry] {
        guard !uris.isEmpty else { return [:] }
        let records = (try? await cache.loadAll()) ?? []
        var publications: [String: PublicationEntry] = [:]
        for cached in records where cached.kind == .publication && uris.contains(cached.uri) {
            guard let record = try? decoded(SiteStandardLexicon.PublicationRecord.self, from: cached.recordJson) else {
                continue
            }
            publications[cached.uri] = PublicationEntry(
                uri: cached.uri,
                authorDID: cached.authorDid,
                record: record
            )
        }
        return publications
    }

    func clear() async {
        try? await cache.clear()
        try? await feedCache.clear()
    }

    private func encoded<T: Encodable>(_ record: T) throws -> String {
        let data = try JSONEncoder().encode(record)
        guard let string = String(data: data, encoding: .utf8) else {
            throw CocoaError(.fileWriteInapplicableStringEncoding)
        }
        return string
    }

    private func decoded<T: Decodable>(_ type: T.Type, from json: String) throws -> T {
        guard let data = json.data(using: .utf8) else {
            throw CocoaError(.fileReadCorruptFile)
        }
        return try JSONDecoder().decode(type, from: data)
    }
}
