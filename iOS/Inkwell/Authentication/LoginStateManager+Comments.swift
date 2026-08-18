//
//  LoginStateManager+Comments.swift
//  Inkwell
//

import Foundation
import OSLog
import ATProtoKit

extension LoginStateManager {
    // MARK: - Comments

    /// Resolves blob-backed Leaflet pages when a document's content uses
    /// `blobPages` (large posts offloaded to a PDS blob). Returns a new
    /// UnknownType with the resolved pages inlined, or the original content
    /// when no blobPages are present.
    func resolveBlobPages(in content: UnknownType?) async -> UnknownType? {
        guard let content,
              let leaflet = content.getRecord(ofType: LeafletContent.self),
              let blobRef = leaflet.blobPages else {
            return content
        }
        do {
            let blobData = try await downloadBlob(cid: blobRef.reference.link)
            let pages = try JSONDecoder().decode([LeafletPage].self, from: blobData)
            let resolved = LeafletContent(pages: pages, blobPages: nil)
            return UnknownType.record(resolved)
        } catch {
            logger.error("[resolveBlobPages] failed to fetch blob pages: \(error)")
            return content  // fall back to whatever inline pages exist
        }
    }

    /// Fetches `pub.leaflet.comment` records referencing the given document
    /// as their `subject`, newest first.
    ///
    /// Uses two sources, merged:
    ///
    /// 1. **Constellation** (microcosm.blue) — a global AT Protocol backlink
    ///    index that discovers comment records across *all* repositories.
    ///    This catches comments from any user, not just the current one.
    ///    Each discovered backlink is hydrated from the commenter's PDS via
    ///    `com.atproto.repo.getRecord`.
    ///
    /// 2. **Local PDS** — the current user's own repo, as a fast path for
    ///    the user's own comments (avoids the Constellation round-trip and
    ///    PDS hydration for those records).
    ///
    /// Constellation results are deduplicated against local results by URI.
    func fetchComments(documentURI: String) async throws -> [CommentEntry] {
        guard let did = currentDID else { throw LoginError.notAuthenticated }

        // 1. Local: fetch the current user's own comments from their repo.
        let localRecords = (try? await listAllRecords(
            from: did, collection: PubLeafletComment.type
        )) ?? []

        var seen = Set<String>()
        var comments: [CommentEntry] = []

        for record in localRecords {
            guard let value = record.value,
                  let comment = value.getRecord(ofType: PubLeafletComment.self),
                  comment.subject == documentURI,
                  !seen.contains(record.uri) else { continue }
            seen.insert(record.uri)
            comments.append(CommentEntry(
                uri: record.uri,
                recordKey: parseAtUri(record.uri)?.recordKey ?? "",
                record: comment
            ))
        }

        // 2. Constellation: discover comments from ALL repos.
        let backlinks = await ConstellationClient.getCommentBacklinks(
            documentURI: documentURI
        )

        // Hydrate each backlink from the commenter's PDS.
        await withTaskGroup(of: CommentEntry?.self) { group in
            for backlink in backlinks {
                let uri = backlink.recordURI
                guard !seen.contains(uri) else { continue }
                seen.insert(uri)

                group.addTask { [backlink, documentURI] in
                    guard let (recordURI, _, value) = try? await self.getRepositoryRecord(
                        from: backlink.did,
                        collection: backlink.collection,
                        recordKey: backlink.rkey
                    ),
                    let comment = value?.getRecord(ofType: PubLeafletComment.self),
                    comment.subject == documentURI else {
                        return nil
                    }
                    return CommentEntry(
                        uri: recordURI,
                        recordKey: backlink.rkey,
                        record: comment
                    )
                }
            }
            for await result in group {
                if let entry = result {
                    comments.append(entry)
                }
            }
        }

        return comments.sorted { $0.record.createdAt > $1.record.createdAt }
    }

    /// Creates a `pub.leaflet.comment` record.
    @discardableResult
    func createComment(
        subject: String,
        plaintext: String,
        replyTo: String? = nil,
        onPage: String? = nil
    ) async throws -> ComAtprotoLexicon.Repository.StrongReference {
        let comment = PubLeafletComment(
            subject: subject,
            plaintext: plaintext,
            reply: replyTo.map { PubLeafletComment.ReplyRef(parent: $0) },
            onPage: onPage
        )
        return try await createRecord(
            collection: PubLeafletComment.type,
            record: UnknownType.record(comment)
        )
    }

    /// Deletes a comment record by its record key.
    func deleteComment(recordKey: String) async throws {
        try await deleteRecord(
            collection: PubLeafletComment.type,
            recordKey: recordKey
        )
    }
}
