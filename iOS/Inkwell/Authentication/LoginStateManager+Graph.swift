//
//  LoginStateManager+Graph.swift
//  Inkwell
//
//  Personal moderation against app.bsky.graph.*. Mutes are server-side;
//  blocks are app.bsky.graph.block records in the signed-in user's repo.
//

import Foundation
import ATProtoKit

private let blueskyAppViewProxy = "did:web:api.bsky.app#bsky_appview"

struct ModeratedActor: Identifiable, Hashable, Sendable {
    let did: String
    let handle: String
    let displayName: String?

    var id: String { did }
}

struct BlockedActorEntry: Identifiable, Hashable, Sendable {
    let actor: ModeratedActor
    let recordKey: String

    var id: String { recordKey }
}

struct AppBskyGraphBlockRecord: ATRecordProtocol, Sendable {
    static let type = "app.bsky.graph.block"

    let subject: String
    let createdAt: Date

    init(subject: String, createdAt: Date) {
        self.subject = subject
        self.createdAt = createdAt
    }

    init(from decoder: any Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        subject = try container.decode(String.self, forKey: .subject)
        createdAt = try container.decodeDate(forKey: .createdAt)
    }

    func encode(to encoder: any Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(Self.type, forKey: .type)
        try container.encode(subject, forKey: .subject)
        try container.encodeDate(createdAt, forKey: .createdAt)
    }

    private enum CodingKeys: String, CodingKey {
        case type = "$type"
        case subject
        case createdAt
    }
}

private struct GraphActorDTO: Decodable {
    let did: String
    let handle: String?
    let displayName: String?

    var actor: ModeratedActor {
        ModeratedActor(did: did, handle: handle ?? did, displayName: displayName)
    }
}

private struct GraphActorPage: Decodable {
    let cursor: String?
    let mutes: [GraphActorDTO]?
    let blocks: [GraphActorDTO]?
}

private struct GraphActorInput: Encodable {
    let actor: String
}

private enum GraphActorList {
    case mutes
    case blocks
}

extension LoginStateManager {
    // MARK: - Personal Moderation

    func muteActor(did: String) async throws {
        if TestingMode.isEnabled {
            TestingModeNotice.shared.report("Mute actor")
            throw LoginError.testingMode
        }

        let body = try JSONEncoder().encode(GraphActorInput(actor: did))
        _ = try await authenticatedData(
            path: sharedXrpcGraphMuteActor(),
            method: "POST",
            body: body,
            proxy: blueskyAppViewProxy
        )
    }

    func unmuteActor(did: String) async throws {
        if TestingMode.isEnabled {
            TestingModeNotice.shared.report("Unmute actor")
            throw LoginError.testingMode
        }

        let body = try JSONEncoder().encode(GraphActorInput(actor: did))
        _ = try await authenticatedData(
            path: sharedXrpcGraphUnmuteActor(),
            method: "POST",
            body: body,
            proxy: blueskyAppViewProxy
        )
    }

    func fetchMutedActors() async throws -> [ModeratedActor] {
        try await fetchGraphActors(path: sharedXrpcGraphGetMutes(), list: .mutes)
    }

    @discardableResult
    func createBlock(did: String) async throws -> ComAtprotoLexicon.Repository.StrongReference {
        let block = AppBskyGraphBlockRecord(subject: did, createdAt: Date())
        return try await createRecord(
            collection: sharedGraphBlockCollection(),
            record: UnknownType.record(block)
        )
    }

    func deleteBlock(recordKey: String) async throws {
        try await deleteRecord(collection: sharedGraphBlockCollection(), recordKey: recordKey)
    }

    func fetchBlockedActors() async throws -> [BlockedActorEntry] {
        guard let did = currentDID else { throw LoginError.notAuthenticated }

        let records = try await listAllRecords(from: did, collection: sharedGraphBlockCollection())
        var recordKeyBySubject: [String: String] = [:]
        for record in records {
            guard let recordKey = parseAtUri(record.uri)?.recordKey,
                  let block = record.value?.getRecord(ofType: AppBskyGraphBlockRecord.self) else {
                continue
            }
            recordKeyBySubject[block.subject] = recordKey
        }
        guard !recordKeyBySubject.isEmpty else { return [] }

        let actors = try await fetchGraphActors(path: sharedXrpcGraphGetBlocks(), list: .blocks)
        let actorByDID = Dictionary(uniqueKeysWithValues: actors.map { ($0.did, $0) })

        return recordKeyBySubject.map { subjectDID, recordKey in
            BlockedActorEntry(
                actor: actorByDID[subjectDID]
                    ?? ModeratedActor(did: subjectDID, handle: subjectDID, displayName: nil),
                recordKey: recordKey
            )
        }
    }

    private func fetchGraphActors(path: String, list: GraphActorList) async throws -> [ModeratedActor] {
        var all: [ModeratedActor] = []
        var cursor: String?

        repeat {
            var queryItems = [URLQueryItem(name: "limit", value: "100")]
            if let cursor {
                queryItems.append(URLQueryItem(name: "cursor", value: cursor))
            }

            let data = try await authenticatedData(
                path: path,
                queryItems: queryItems,
                proxy: blueskyAppViewProxy
            )
            let page = try JSONDecoder().decode(GraphActorPage.self, from: data)
            let actors: [GraphActorDTO]
            switch list {
            case .mutes:
                actors = page.mutes ?? []
            case .blocks:
                actors = page.blocks ?? []
            }

            all.append(contentsOf: actors.map(\.actor))
            let nextCursor = page.cursor
            if nextCursor == nil || nextCursor == cursor || actors.isEmpty {
                break
            }
            cursor = nextCursor
        } while true

        return all
    }
}
