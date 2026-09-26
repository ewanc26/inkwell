//
//  WriterDraftStore.swift
//  Inkwell
//
//  Local, app-private autosave for the Writer: at most one draft per account
//  DID, stored as a file-protected JSON file under Application Support.
//
//  Drafts are the user's own unpublished text, not credentials, so they live
//  in ordinary files rather than the Keychain — but never in UserDefaults,
//  and always with `.completeFileProtection` so they are unreadable while
//  the device is locked. The on-disk shape and every rule about it come from
//  `WriterDraftSchema.swift`, which mirrors the shared KMP policy.
//

import Foundation
import CryptoKit

/// File I/O runs on this actor's executor, never on the main actor.
actor WriterDraftStore {
    static let shared = WriterDraftStore()

    private let directory: URL
    private let now: @Sendable () -> Date

    init(
        directory: URL = FileManager.default
            .urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
            .appendingPathComponent("WriterDrafts", isDirectory: true),
        now: @escaping @Sendable () -> Date = { Date() }
    ) {
        self.directory = directory
        self.now = now
    }

    func save(_ draft: WriterDraft) throws {
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        let data = try JSONEncoder().encode(draft)
        try data.write(to: fileURL(for: draft.accountDid), options: [.atomic, .completeFileProtection])
    }

    /// The draft for `accountDid`, or nil when there is none worth offering.
    ///
    /// An undecodable, foreign-account, or expired draft is deleted rather
    /// than left to fail the same way on every launch. A read failure returns
    /// nil but leaves the file alone: an unreadable file is not proof the
    /// draft is gone.
    func load(accountDid: String) -> WriterDraft? {
        let url = fileURL(for: accountDid)
        guard FileManager.default.fileExists(atPath: url.path) else { return nil }
        guard let data = try? Data(contentsOf: url) else { return nil }
        guard let draft = try? JSONDecoder().decode(WriterDraft.self, from: data),
              WriterDraftPolicy.isRestorable(draft, accountDid: accountDid, now: now()) else {
            clear(accountDid: accountDid)
            return nil
        }
        return draft
    }

    func clear(accountDid: String) {
        // Discarding locally must not surface as an error; the next save overwrites it anyway.
        try? FileManager.default.removeItem(at: fileURL(for: accountDid))
    }

    /// DIDs contain `:` and, for did:web, arbitrary host characters, so the
    /// filename is a digest rather than the DID itself.
    private func fileURL(for accountDid: String) -> URL {
        let digest = SHA256.hash(data: Data(accountDid.utf8))
            .map { String(format: "%02x", $0) }
            .joined()
        return directory.appendingPathComponent("\(digest).json", isDirectory: false)
    }
}
