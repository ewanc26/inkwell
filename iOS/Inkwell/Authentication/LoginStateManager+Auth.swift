//
//  LoginStateManager+Auth.swift
//  Inkwell
//

import Foundation
import OSLog
import ATProtoKit
import OAuthenticator
import ATResolve
import CryptoKit

extension LoginStateManager {
    // MARK: - Authentication

    /// Starts the OAuth sign-in flow for the given handle.
    ///
    /// 1. Resolves the handle to a DID + PDS URL via ATResolve.
    /// 2. Fetches the PDS's OAuth server metadata.
    /// 3. Opens `ASWebAuthenticationSession` for user approval.
    /// 4. Exchanges the authorization code for DPoP-bound tokens.
    /// 5. Persists the `Login` and DPoP key in the Keychain.
    ///
    /// - Parameter handle: The AT Protocol handle (e.g. `alice.bsky.social`).
    /// - Returns: `true` if authentication succeeded.
    func signIn(handle: String) async -> Bool {
        let trimmed = handle.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else {
            errorMessage = "Enter your handle."
            return false
        }

        do {
            // 1. Resolve handle → DID + PDS
            guard let identity = try await resolver.resolveHandle(trimmed),
                  let pdsHost = identity.serviceEndpoint.flatMap({ URL(string: $0) })?.host,
                  let pdsURL = URL(string: identity.serviceEndpoint ?? "") else {
                errorMessage = "Could not resolve handle. Make sure it's a valid AT Protocol handle."
                return false
            }

            logger.info("[SignIn] resolved \(identity.handle) → DID \(identity.did), PDS \(pdsURL.absoluteString)")

            // 2. Load OAuth server metadata from the PDS
            let serverMetadata = try await ServerMetadata.load(for: pdsHost, provider: URLSession.defaultProvider)

            // 3. Generate or load DPoP key (must persist across sessions)
            let key: P256.Signing.PrivateKey
            if let existingData = try? dpopKeyStore.read(),
               let existingKey = try? P256.Signing.PrivateKey(rawRepresentation: existingData) {
                key = existingKey
            } else {
                key = P256.Signing.PrivateKey()
                try? dpopKeyStore.write(key.rawRepresentation)
            }

            // 4. Build token handling for Bluesky AT Protocol OAuth.
            //
            // Some PDSes (e.g. pds.croft.click) require a DPoP nonce on the
            // token endpoint and burn the authorization code the instant a
            // request arrives without one — even though that's exactly the
            // request whose *rejection* is supposed to teach the client
            // what the nonce is. `nonceCache` records every `DPoP-Nonce`
            // response header this sign-in attempt sees (via `loader`
            // below); `DPoPJWTGenerator`'s `nonceOverride` falls back to it
            // whenever a signer doesn't have one of its own yet. That lets
            // a *retry* — which needs a brand-new `Authenticator` (and so a
            // brand-new, never-yet-presented PKCE challenge, since
            // `Bluesky.tokenHandling` mints one internally on every call —
            // reusing the same `Authenticator` reuses the same challenge,
            // which this class of PDS treats as one-time-use too) still
            // carry the nonce learned from the first attempt's real server
            // response, so the retry's first token request already has a
            // valid nonce instead of needing (and losing) its own
            // burn-then-learn round trip.
            let nonceCache = LockedBox<String?>(nil)
            let validator: Bluesky.TokenSubscriberValidator = { [weak self] tokenResponse, issuer in
                // Verify that the token's subject (DID) resolves to a PDS
                // whose issuer matches the token's issuer. This is a
                // critical security check per AT Protocol OAuth spec.
                guard let self else { return false }
                guard let resolved = try? await self.resolver.resolveHandle(tokenResponse.sub),
                      let subPDSURL = resolved.serviceEndpoint.flatMap({ URL(string: $0) }),
                      subPDSURL.absoluteString.caseInsensitiveCompare(issuer) == .orderedSame
                        || issuer.contains(subPDSURL.host ?? "") else {
                    return false
                }
                return true
            }
            func makeTokenHandling() -> TokenHandling {
                Bluesky.tokenHandling(
                    account: trimmed,
                    server: serverMetadata,
                    jwtGenerator: DPoPJWTGenerator.generator(key: key, nonceOverride: { nonceCache.value }),
                    validator: validator
                )
            }

            // 5. Create Authenticator in manual mode to trigger auth.
            let loader: URLResponseProvider = { [logger] request in
                let (data, response) = try await URLSession.defaultProvider(request)
                if let http = response as? HTTPURLResponse {
                    if let nonce = http.value(forHTTPHeaderField: "DPoP-Nonce") {
                        nonceCache.value = nonce
                    }
                    if http.statusCode >= 400 {
                        let body = String(decoding: data, as: UTF8.self)
                        logger.error("[SignIn] token endpoint returned HTTP \(http.statusCode): \(body)")
                    }
                }
                return (data, response)
            }
            func makeAuth() -> (Authenticator, TokenHandling) {
                let handling = makeTokenHandling()
                let config = Authenticator.Configuration(
                    appCredentials: appCredentials,
                    loginStorage: makeLoginStorage(),
                    tokenHandling: handling,
                    mode: .manualOnly
                )
                return (Authenticator(config: config, urlLoader: loader), handling)
            }

            logger.info("[SignIn] starting ASWebAuthenticationSession…")
            var (auth, tokenHandling) = makeAuth()
            do {
                try await auth.authenticate()
            } catch is DecodingError {
                logger.warning("[SignIn] first token exchange failed to decode (likely an auth code burned by a DPoP nonce challenge) — retrying with a fresh authorization code and the now-cached nonce")
                // The first ASWebAuthenticationSession's view controller is
                // still mid-dismissal when this catch runs; presenting a
                // second one immediately fails silently (SFAuthenticationSession
                // logs "Attempted to present ... from a view controller that
                // is being dismissed" and the retry never shows). Give the
                // dismissal animation time to finish first.
                try await Task.sleep(nanoseconds: 600_000_000)
                (auth, tokenHandling) = makeAuth()
                try await auth.authenticate()
            }
            logger.info("[SignIn] OAuth flow completed")

            // 6. Rebuild Authenticator in automatic mode for subsequent requests
            let autoConfig = Authenticator.Configuration(
                appCredentials: appCredentials,
                loginStorage: makeLoginStorage(),
                tokenHandling: tokenHandling,
                mode: .automatic
            )
            self.authenticator = Authenticator(config: autoConfig)
            self.dpopKey = key
            self.resolvedPDSURL = pdsURL
            self.currentHandle = identity.handle
            self.currentDID = identity.did
            self.isAuthenticated = true
            self.errorMessage = nil
            self.defaults.set(identity.handle, forKey: storedHandleKey)
            self.defaults.set(pdsURL.absoluteString, forKey: storedPDSKey)

            // 7. Best-effort profile fetch (cosmetic — don't block sign-in)
            Task { [weak self] in
                guard let self else { return }
                do {
                    let profile = try await self.fetchProfile(did: identity.did)
                    await MainActor.run {
                        let trimmedName = profile.displayName?.trimmingCharacters(in: .whitespacesAndNewlines)
                        self.displayName = (trimmedName?.isEmpty == false) ? trimmedName : nil
                        self.avatarURL = profile.avatarURL
                    }
                } catch {
                    logger.error("[SignIn] profile fetch failed: \(error.localizedDescription)")
                }
            }

            return true
        } catch let error as DecodingError {
            // Decoding errors are ambiguous — surface the full context.
            let detail: String
            switch error {
            case .keyNotFound(let key, let context):
                detail = "missing key '\(key.stringValue)' (path: \(context.codingPath.map(\.stringValue)))"
            case .valueNotFound(let type, let context):
                detail = "null value for \(type) (path: \(context.codingPath.map(\.stringValue)))"
            case .typeMismatch(let type, let context):
                detail = "type mismatch, expected \(type) (path: \(context.codingPath.map(\.stringValue)))"
            case .dataCorrupted(let context):
                detail = "corrupted data: \(context.debugDescription)"
            @unknown default:
                detail = "unknown decoding error"
            }
            logger.error("[SignIn] DecodingError: \(detail)")
            logger.error("[SignIn] The token endpoint returned a response that doesn't match the expected format. This usually means the PDS rejected the token request. Check that https://inkwell.ewancroft.uk/client-metadata.json is publicly accessible and returns valid JSON with the correct client_id, redirect_uris, and grant_types.")
            errorMessage = "Your PDS rejected the token exchange. Make sure it supports AT Protocol OAuth and that https://inkwell.ewancroft.uk/client-metadata.json is publicly accessible."
            clearSession()
            return false
        } catch {
            logger.error("[SignIn] error: \(type(of: error)) — \(error.localizedDescription)")
            errorMessage = error.localizedDescription
            clearSession()
            return false
        }
    }

    /// Attempts to silently resume a previously authenticated session
    /// using the OAuth tokens stored in the Keychain.
    ///
    /// Call this once on launch from the root view's `.task`.
    func restoreSessionIfPossible() async {
        defer { isRestoringSession = false }

        guard let storedHandle = defaults.string(forKey: storedHandleKey),
              let storedPDS = defaults.string(forKey: storedPDSKey),
              let pdsURL = URL(string: storedPDS) else {
            return
        }

        // Load DPoP key and stored Login
        guard let dpopKeyData = try? dpopKeyStore.read(),
              let key = try? P256.Signing.PrivateKey(rawRepresentation: dpopKeyData),
              let storedLogin = try? loginStore.read() else {
            logger.info("[RestoreSession] no stored session found")
            clearSession()
            return
        }

        // Quick expiry check — don't bother building the authenticator
        // if the access token is already expired and there's no refresh token.
        if !storedLogin.accessToken.valid && storedLogin.refreshToken == nil {
            logger.info("[RestoreSession] access token expired, no refresh token")
            clearSession()
            return
        }

        do {
            // Resolve identity for fresh DID
            guard let identity = try? await resolver.resolveHandle(storedHandle) else {
                logger.error("[RestoreSession] identity resolution failed")
                clearSession()
                return
            }

            guard let pdsHost = pdsURL.host else {
                clearSession()
                return
            }

            let serverMetadata = try await ServerMetadata.load(for: pdsHost, provider: URLSession.defaultProvider)

            let tokenHandling = Bluesky.tokenHandling(
                account: storedHandle,
                server: serverMetadata,
                jwtGenerator: DPoPJWTGenerator.generator(key: key),
                validator: { _, _ in true } // Already validated during initial sign-in
            )

            let config = Authenticator.Configuration(
                appCredentials: appCredentials,
                loginStorage: makeLoginStorage(),
                tokenHandling: tokenHandling,
                mode: .automatic
            )
            let auth = Authenticator(config: config)

            self.authenticator = auth
            self.dpopKey = key
            self.resolvedPDSURL = pdsURL
            self.currentHandle = identity.handle
            self.currentDID = identity.did
            self.isAuthenticated = true
            self.errorMessage = nil

            logger.info("[RestoreSession] session restored for \(identity.handle)")

            // Verify the restored tokens are still accepted by the PDS
            // before declaring the session live. This catches server-side
            // revocation before the user tries to write anything — reads
            // use unauthenticated requests so they would succeed either
            // way, masking a broken token until the first mutation.
            do {
                _ = try await authenticatedData(path: sharedXrpcServerGetSession())
                logger.info("[RestoreSession] token verification succeeded")
            } catch {
                logger.warning("[RestoreSession] token verification failed (\(error.localizedDescription)) — clearing session")
                clearSession()
                return
            }

            Task { [weak self] in
                guard let self else { return }
                do {
                    let profile = try await self.fetchProfile(did: identity.did)
                    await MainActor.run {
                        let trimmedName = profile.displayName?.trimmingCharacters(in: .whitespacesAndNewlines)
                        self.displayName = (trimmedName?.isEmpty == false) ? trimmedName : nil
                        self.avatarURL = profile.avatarURL
                    }
                } catch {
                    logger.error("[RestoreSession] profile fetch failed: \(error.localizedDescription)")
                }
            }
        } catch {
            logger.error("[RestoreSession] failed: \(error.localizedDescription)")
            clearSession()
        }
    }

    /// Signs the user out, clearing all stored tokens and state.
    func signOut() {
        clearSession(clearStoredAccount: true)
        errorMessage = nil
        Task {
            try? loginStore.delete()
            try? dpopKeyStore.delete()
        }
    }

    // MARK: - Helpers

    func makeLoginStorage() -> LoginStorage {
        // KeychainStore is a value type — capture a copy so the closures
        // can use it from any isolation domain without hopping to @MainActor.
        let store = loginStore
        return LoginStorage(
            retrieveLogin: { try await store.read() },
            storeLogin: { try await store.write($0) },
            clearLogin: { try await store.delete() }
        )
    }

    func clearSession(clearStoredAccount: Bool = false) {
        isAuthenticated = false
        currentHandle = nil
        currentDID = nil
        displayName = nil
        avatarURL = nil
        authenticator = nil
        dpopKey = nil
        resolvedPDSURL = nil
        repositoryPDSURLs.removeAll()
        _cachedSubscriptions = nil

        if clearStoredAccount {
            defaults.removeObject(forKey: storedHandleKey)
            defaults.removeObject(forKey: storedPDSKey)
        }
    }
}
