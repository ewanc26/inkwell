# AGENTS.md

Guidance for agents working on Inkwell, a SwiftUI iOS 26 reader, writer, discovery, social, and notification client for Standard.site content on AT Protocol.

## Read First and Source Boundaries

- Read `README.md`, the Xcode project settings, `oauth/client-metadata.json`, privacy/entitlement files, and all source in the touched flow. `Inkwell/` is authoritative; `.letta/worktrees/` contains local shadow checkouts and must not be edited as product source.
- `Authentication/LoginStateManager.swift` is the large central boundary for OAuth/DPoP, PDS resolution, public and authenticated XRPC, records, blobs, subscriptions, recommends, Leaflet comments, profiles, and caches.
- `Protocols/StandardSite/` and `Protocols/ContentFormats/` define tolerant wire models and association/verification rules. `Rendering/` handles Markpub Markdown, Leaflet block/blob pages, pckt, Offprint, Bluesky embeds, polls, and themes.
- `Features/` owns Read/Discover/Write and background subscription polling. `InkwellTests/StandardSiteTests.swift` is a focused unit suite, not end-to-end OAuth/editor/rendering coverage.

## OAuth and Data Invariants

- OAuth tokens and the P-256 DPoP private key belong in Keychain. `UserDefaults` stores non-secret handle/PDS hints plus notification state and seen URIs; never move credentials, tokens, auth codes, or proof material there or into logs.
- Keep client ID, custom callback scheme, scopes, metadata hosted at `inkwell.ewancroft.uk`, Info.plist URL type, and runtime credentials identical. They currently are not fully aligned: hosted metadata declares only `atproto`, while runtime asks for granular publication/document/subscription/recommend/blob scopes.
- Comment creation writes `pub.leaflet.comment`, but that collection is absent from the runtime granular scope list. Verify authorization for every mutation before describing comments as functional; update hosted metadata and server support together.
- Preserve issuer/subject/PDS validation, PKCE/authenticator state, DPoP key continuity, nonce retry behavior, refresh rotation, and logout deletion. The eurosky token-endpoint nonce preflight is compatibility-sensitive and must be tested against real server behavior.
- Keep handles, DIDs, PDS origins, AT URIs, CIDs, rkeys, revisions, canonical site URLs, and verification proofs distinct. Public cross-repo reads must resolve the owning DID's PDS rather than assume the signed-in service.

## Content, Concurrency, and Lifecycle

- AT Protocol open unions and unknown/malformed records must degrade without corrupting valid siblings. Always retain portable `textContent`; preserve format-specific Leaflet/pckt/Offprint/Markpub data unless the user explicitly converts it.
- Facet byte offsets are UTF-8 offsets, not Swift character indices. Blob MIME/size/ref, record `$type`, `site`, `path`, timestamps, themes, and publication association must round-trip exactly.
- `LoginStateManager`, notification/background managers, and UI state are `@MainActor`; network calls are async but much orchestration still runs on the main actor. Do not introduce blocking I/O, shared DPoP nonce races, detached unsafe mutation, or uncancelled view tasks.
- Background refresh identifiers, Info.plist permitted identifiers, scheduling, expiry handlers, notification permission, first-poll baseline, 50-item display retention, and 500-URI seen retention form one contract. Test cold launch, sign-out, denial, expiration, and concurrent foreground fetches.
- Preserve native accessibility, Dynamic Type, dark/light/tinted icon behavior, reduced motion, safe-area behavior, and the shared duotone mark coordinates.

## Build, Tests, and Distribution

- The checked-in project uses Swift 5 mode, app deployment target iOS 26.0, test target iOS 26.5, bundle `uk.ewancroft.Inkwell`, marketing version `1.0`, and build `49`. Resolve Swift packages through Xcode and build the `Inkwell` scheme on an installed compatible simulator.
- Run `xcodebuild -project Inkwell.xcodeproj -scheme Inkwell -destination 'platform=iOS Simulator,name=<available iOS 26.5 device>' build test`, adapting only the destination to installed runtimes. Inspect failures from ATProtoKit, OAuthenticator, and ATResolve resolution separately.
- Unit tests cover AT-URI parsing, association/canonical URLs, verification endpoint paths, wire keys, search decoding, notification JSON, and tolerant record pages. Manually exercise fresh/cancelled OAuth, bad state/issuer/nonce, restore/refresh/revocation/logout, every reader format, Unicode facets, blobs, create/edit/delete, subscriptions/recommends/comments, verification, pagination, offline errors, and background/local notifications.
- `altstore/source.json` must match bundle/version/build, privacy/permissions, hosted icon/IPA, byte size, and release notes. It currently has a zero-byte placeholder size and empty privacy declaration, so it is not release-ready proof. Never commit IPA archives, signing profiles, DerivedData, xcuserdata/UI state, or real credentials.
