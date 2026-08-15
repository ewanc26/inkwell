//
//  KeychainStore.swift
//  Inkwell
//
//  Lightweight Keychain wrapper for persisting OAuth `Login` and the DPoP
//  private key. Replaces ATProtoKit's `AppleSecureKeychain` which was tied
//  to app-password session management.
//

import Foundation
import Security

/// Simple Keychain-backed store for a single `Codable` value.
///
/// Uses a generic `kSecClassGenericPassword` entry identified by `service`
/// and `account`. On iOS the Keychain survives app deletion only when the
/// device is not wiped — standard iOS behaviour.
struct KeychainStore<T: Codable> {
    let service: String
    let account: String

    /// Access group for shared Keychain access across app extensions, if any.
    /// Defaults to `nil` (app-only).
    var accessGroup: String? = nil

    // MARK: - Read

    func read() throws -> T? {
        var query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account,
            kSecReturnData as String: true,
            kSecMatchLimit as String: kSecMatchLimitOne,
        ]
        if let accessGroup {
            query[kSecAttrAccessGroup as String] = accessGroup
        }

        var item: CFTypeRef?
        let status = SecItemCopyMatching(query as CFDictionary, &item)
        guard status != errSecItemNotFound else { return nil }
        guard status == errSecSuccess,
              let data = item as? Data else {
            throw KeychainError.readFailed(status: status)
        }

        return try JSONDecoder().decode(T.self, from: data)
    }

    // MARK: - Write

    func write(_ value: T) throws {
        let data = try JSONEncoder().encode(value)

        var query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account,
        ]
        if let accessGroup {
            query[kSecAttrAccessGroup as String] = accessGroup
        }

        // Check if the item already exists.
        if try read() != nil {
            // Update existing.
            let attributes: [String: Any] = [
                kSecValueData as String: data,
            ]
            let status = SecItemUpdate(query as CFDictionary, attributes as CFDictionary)
            guard status == errSecSuccess else {
                throw KeychainError.writeFailed(status: status)
            }
        } else {
            // Add new.
            var createQuery = query
            createQuery[kSecValueData as String] = data
            createQuery[kSecAttrAccessible as String] = kSecAttrAccessibleAfterFirstUnlock
            let status = SecItemAdd(createQuery as CFDictionary, nil)
            guard status == errSecSuccess else {
                throw KeychainError.writeFailed(status: status)
            }
        }
    }

    // MARK: - Delete

    func delete() throws {
        var query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account,
        ]
        if let accessGroup {
            query[kSecAttrAccessGroup as String] = accessGroup
        }

        let status = SecItemDelete(query as CFDictionary)
        guard status == errSecSuccess || status == errSecItemNotFound else {
            throw KeychainError.deleteFailed(status: status)
        }
    }
}

// MARK: - Error

enum KeychainError: LocalizedError {
    case readFailed(status: OSStatus)
    case writeFailed(status: OSStatus)
    case deleteFailed(status: OSStatus)

    var errorDescription: String? {
        switch self {
        case .readFailed(let status):
            return "Keychain read failed (status \(status))."
        case .writeFailed(let status):
            return "Keychain write failed (status \(status))."
        case .deleteFailed(let status):
            return "Keychain delete failed (status \(status))."
        }
    }
}
