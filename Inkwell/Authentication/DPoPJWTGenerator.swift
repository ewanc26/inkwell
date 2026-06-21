//
//  DPoPJWTGenerator.swift
//  Inkwell
//
//  Minimal ES256 DPoP proof JWT generator using CryptoKit's P-256.
//  Plugs into OAuthenticator's `DPoPSigner.JWTGenerator` closure.
//

import Foundation
import CryptoKit
import OAuthenticator

// MARK: - Base64URL

private extension Data {
    /// Base64url-encoded string (RFC 7515 / RFC 9449).
    var base64urlString: String {
        base64EncodedString()
            .replacingOccurrences(of: "+", with: "-")
            .replacingOccurrences(of: "/", with: "_")
            .replacingOccurrences(of: "=", with: "")
    }
}

/// Creates a `DPoPSigner.JWTGenerator` backed by a persisted P-256 key.
///
/// The key must be the same one used across all sessions — the authorization
/// server binds tokens to this key, so losing it means starting a fresh OAuth
/// flow from scratch.
enum DPoPJWTGenerator {

    /// Build a JWT generator that signs DPoP proofs with the given P-256 private key.
    static func generator(key: P256.Signing.PrivateKey) -> DPoPSigner.JWTGenerator {
        let publicKey = key.publicKey

        return { params in
            // --- JWT Header ---
            let header: [String: Any] = [
                "typ": params.keyType,
                "alg": "ES256",
                "jwk": publicKeyJWK(from: publicKey),
            ]

            // --- JWT Payload ---
            var payload: [String: Any] = [
                "jti": UUID().uuidString,
                "htm": params.httpMethod,
                "htu": params.requestEndpoint,
                "iat": Int(Date().timeIntervalSince1970),
            ]
            if let nonce = params.nonce {
                payload["nonce"] = nonce
            }
            if let ath = params.tokenHash {
                payload["ath"] = ath
            }

            let headerData = try JSONSerialization.data(withJSONObject: header, options: [])
            let payloadData = try JSONSerialization.data(withJSONObject: payload, options: [])

            let headerB64 = headerData.base64urlString
            let payloadB64 = payloadData.base64urlString
            let signingInput = "\(headerB64).\(payloadB64)"

            guard let signingData = signingInput.data(using: .utf8) else {
                throw DPoPError.signingInputEncodingFailed
            }

            let signature = try key.signature(for: signingData)
            let signatureB64 = signature.rawRepresentation.base64urlString

            return "\(signingInput).\(signatureB64)"
        }
    }

    // MARK: - JWK Helpers

    /// Build a JWK representation of a P-256 public key.
    private static func publicKeyJWK(from key: P256.Signing.PublicKey) -> [String: String] {
        let raw = key.rawRepresentation // 64 bytes: x || y
        let x = raw.prefix(32)
        let y = raw.suffix(32)
        return [
            "kty": "EC",
            "crv": "P-256",
            "x": x.base64urlString,
            "y": y.base64urlString,
        ]
    }

    enum DPoPError: LocalizedError {
        case signingInputEncodingFailed

        var errorDescription: String? {
            switch self {
            case .signingInputEncodingFailed:
                return "Failed to encode DPoP signing input as UTF-8."
            }
        }
    }
}
