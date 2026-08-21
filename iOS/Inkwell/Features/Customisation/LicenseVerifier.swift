//
//  LicenseVerifier.swift
//  Inkwell
//
//  Verifies a customisation unlock key against an embedded public key.
//  There's no server: someone pays the one-off fee via Ko-fi/GitHub
//  Sponsors (see tools/license/README.md), and a key is issued by hand
//  with tools/license/generate-key.mjs, which holds the matching private
//  key -- never committed to this repo.
//
//  This is an honour-system unlock, not DRM: Inkwell is AGPL-3.0, so
//  anyone willing to build from source can bypass this check entirely,
//  same as removing any other code. What this genuinely prevents is
//  someone guessing or mass-generating "free" keys -- an EC signature
//  can't be forged without the private key, unlike a plain string check.
//

import CryptoKit
import Foundation

enum LicenseVerifier {
    /// Must exactly match LICENSE_MESSAGE in tools/license/generate-key.mjs
    /// and Android's LicenseVerifier.MESSAGE.
    static let message = "inkwell-customisation-unlock-v1"

    /// The public half of the signing key in tools/license/README.md.
    /// Raw X9.63 EC point representation (0x04 || X || Y), base64-encoded.
    private static let publicKeyBase64 =
        "BJ8mhnZeo7F3w4iQUFU1XbR5hAHPd8FyFbV74jDhCgY4ltueL+YwzYNi+4dRbv5I2cJypHL5c20LrgTqjgvTtEw="

    /// - Parameter licenseKey: base64url-encoded DER ECDSA-P256-SHA256
    ///   signature over `message`, as produced by generate-key.mjs.
    static func isValid(licenseKey: String) -> Bool {
        guard let publicKeyData = Data(base64Encoded: publicKeyBase64),
              let publicKey = try? P256.Signing.PublicKey(x963Representation: publicKeyData) else {
            return false
        }
        guard let signatureData = base64URLDecode(licenseKey),
              let signature = try? P256.Signing.ECDSASignature(derRepresentation: signatureData) else {
            return false
        }
        guard let messageData = message.data(using: .utf8) else { return false }
        return publicKey.isValidSignature(signature, for: messageData)
    }

    private static func base64URLDecode(_ input: String) -> Data? {
        var base64 = input
            .trimmingCharacters(in: .whitespacesAndNewlines)
            .replacingOccurrences(of: "-", with: "+")
            .replacingOccurrences(of: "_", with: "/")
        while base64.count % 4 != 0 { base64 += "=" }
        return Data(base64Encoded: base64)
    }
}
