# AGENTS.md

Guidance for agents working on Inkwell, a native reader and writer for the Standard.site publishing ecosystem on AT Protocol. This monorepo contains all three clients: iOS (`iOS/`), Android (`Android/`), and the marketing/legal site (`website/`).

## Principles

1. **Platform fidelity** — each platform's native conventions, security model, and UX come first. Don't port iOS patterns to Android or vice versa without explicit adaptation.
2. **Protocol truth over README claims** — if the README says something the code doesn't actually do, the code is right and the README is wrong. Update both together.
3. **Honest stubs** — unimplemented features say so explicitly in the UI, return errors, or are gated behind runtime checks. Never silent no-ops or fabricated successes.
4. **Security material stays put** — OAuth tokens, DPoP keys, PKCE state, and session secrets belong in platform secure storage (Keychain / EncryptedSharedPreferences) only. Never in UserDefaults, SharedPreferences, logs, or git.
5. **AT Protocol correctness** — cross-reference the atproto spec and upstream reference implementations for wire formats (DAG-CBOR, MST, XRPC, DID documents). Don't infer protocol behavior from observation.
6. **No duplication** — if a piece of logic already exists in the codebase, reuse it. Two independent implementations of the same rule silently drift apart.

## Current state

- **iOS** (`uk.ewancroft.Inkwell`): primary SwiftUI client, marketing version `1.0` build `50`. OAuth with DPoP complete. Reader, Discover, Writer tabs functional. Leaflet blocks, Markpub/Offprint/pckt rendering. Background notification polling. Comments, subscriptions, recommends, publication/document verification, and blob handling implemented. AltStore distribution with live screenshots.
- **Android** (`uk.ewancroft.inkwell`): Kotlin/Compose client, version `1.3.1` build `6`. OAuth complete. Reader, Discover, Writer functional. Reader publication theming (Leaflet rich theme, legacy palette, basicTheme cascade). Leaflet blocks with rich-text facets. Markpub markdown rendering with headings, lists, code blocks, blockquotes, images, task lists, horizontal rules, and inline formatting. pckt/Offprint block arrays converted to markdown with facet-aware inline formatting. Bluesky post embeds with live fetching and author/image/link/quote rendering. Standard.site post embeds with document fetch and cover image. Comments, subscriptions, recommends, and interactions (likes/reposts/replies) implemented. WorkManager background notification polling. Verification wired to post detail with no caching. F-Droid self-hosted repo with fastlane screenshots.
- **Website** (`inkwell.ewancroft.uk`): SvelteKit/Vercel marketing, legal, and OAuth-metadata site. Hosts live `/client-metadata.json`, AltStore `source.json`, F-Droid repo index, and web-optimized screenshots for both platforms.

## Commits

Conventional commits, scoped by area:
```
feat(ios): add post-detail screenshot to AltStore
fix(android): correct F-Droid repo URLs to monorepo
docs(website): update OAuth contract for comment scope
chore(fdroid): regenerate signed index with screenshots
```

Never mix unrelated changes in a single commit. AI-assisted contributions are welcome, but do not add `Co-authored-by:` trailers crediting AI agents. Attribution is reserved for human contributors only.

## Things that look wrong but are not

- **Android's `ContentUnion` is intentionally incomplete** — it would silently lose unmodelled formats if round-tripped, so the partial set is deliberate.
- **iOS `UserDefaults` stores only non-secret hints** (handle/PDS hints, notification state, seen URIs) — credentials and proof material stay in Keychain.
- **Website links to self-hosted AltStore and F-Droid** rather than App Store / Play Store listings — those stores do not have published listings yet.

Platform-specific guidance lives in:
- [`iOS/AGENTS.md`](iOS/AGENTS.md) — iOS app source boundaries, Keychain/DPoP rules, and Xcode build/test workflow
- [`Android/AGENTS.md`](Android/AGENTS.md) — Android app source boundaries, EncryptedSharedPreferences, and Gradle workflow
- [`website/AGENTS.md`](website/AGENTS.md) — SvelteKit site authority, legal/oauth accuracy, and Vercel deployment

## Read First and Source Boundaries

- Read `README.md`, platform-specific build files, and all source in the touched flow.
- **iOS:** `iOS/` is authoritative; `.letta/worktrees/` contains local shadow checkouts and must not be edited as product source.
  - `iOS/Inkwell/Authentication/LoginStateManager.swift` is the large central boundary for OAuth/DPoP, PDS resolution, public and authenticated XRPC, records, blobs, subscriptions, recommends, Leaflet comments, profiles, and caches.
  - `iOS/Inkwell/Protocols/StandardSite/` and `iOS/Inkwell/Protocols/ContentFormats/` define tolerant wire models and association/verification rules. `iOS/Inkwell/Rendering/` handles Markpub Markdown, Leaflet block/blob pages, pckt, Offprint, Bluesky embeds, polls, and themes.
  - `iOS/Inkwell/Features/` owns Read/Discover/Write and background subscription polling. `iOS/InkwellTests/StandardSiteTests.swift` is a focused unit suite, not end-to-end OAuth/editor/rendering coverage.
- **Android:** `Android/` is the Android client. Key boundaries:
  - `Android/app/src/main/java/uk/ewancroft/inkwell/data/repository/PdsRepository.kt` performs public/authenticated XRPC, records, blobs, subscriptions, recommends, and comments.
  - `Android/app/src/main/java/uk/ewancroft/inkwell/data/model` defines partial Standard.site/Leaflet shapes. `Android/app/src/main/java/uk/ewancroft/inkwell/data/remote/ConstellationClient.kt` queries backlinks. `Android/app/src/main/java/uk/ewancroft/inkwell/data/remote/BSkyPostFetcher.kt` fetches Bluesky posts for embed rendering.
  - Hilt modules construct OAuth/network services. ViewModels own `StateFlow`; Compose screens and `NavGraph` own UI/navigation. `InkwellNotificationManager.kt` and `InkwellNotificationWorker.kt` implement WorkManager-based background notification polling.
  - `Android/app/src/main/java/uk/ewancroft/inkwell/data/remote/StandardSiteVerifier.kt` implements publication/document verification.
- **Website:** `website/` is the SvelteKit/Vercel marketing, legal, and OAuth-metadata site shared by both apps.
  - `website/src/routes/+page.svelte` is the landing page; `/privacy` and `/terms` are substantive legal promises; `/client-metadata.json` is a live OAuth client identity consumed by PDS servers.
  - `website/src/lib/config.ts` owns install-source URLs, site metadata, and nav links.

## OAuth and Data Invariants

- **iOS:** OAuth tokens and the P-256 DPoP private key belong in Keychain. `UserDefaults` stores non-secret handle/PDS hints plus notification state and seen URIs; never move credentials, tokens, auth codes, or proof material there or into logs. Keep client ID, custom callback scheme, scopes, metadata hosted at `inkwell.ewancroft.uk`, Info.plist URL type, and runtime credentials identical. Preserve issuer/subject/PDS validation, PKCE/authenticator state, DPoP key continuity, nonce retry behavior, refresh rotation, and logout deletion. See `iOS/AGENTS.md` for iOS-specific OAuth details.
- **Android:** OAuth sessions are JSON in `EncryptedSharedPreferences` backed by a MasterKey. Keep tokens, refresh state, PKCE/DPoP material, and authorization URLs out of logs and ordinary preferences. The manifest accepts every URI using the custom scheme, while `Android/app/src/main/java/uk/ewancroft/inkwell/MainActivity.kt` checks `/callback`. Preserve state validation inside the OAuth library, reject unrelated/deceptive callbacks, and test cold/warm `singleTask` delivery. Authentication changes do not automatically rebuild an existing Navigation Compose graph merely because `startDestination` changes. See `Android/AGENTS.md` for Android-specific networking rules.
- **Both:** Keep handles, DIDs, PDS origins, AT URIs, CIDs, rkeys, revisions, canonical site URLs, and verification proofs distinct. Public cross-repo reads must resolve the owning DID's PDS rather than assume the signed-in service.

## Content, Concurrency, and Lifecycle

- AT Protocol open unions and unknown/malformed records must degrade without corrupting valid siblings. Always retain portable `textContent`; preserve format-specific Leaflet/pckt/Offprint/Markpub data unless the user explicitly converts it.
- Facet byte offsets are UTF-8 offsets, not platform character indices. Blob MIME/size/ref, record `$type`, `site`, `path`, timestamps, themes, and publication association must round-trip exactly.
- **iOS:** `LoginStateManager`, notification/background managers, and UI state are `@MainActor`; network calls are async but much orchestration still runs on the main actor. Do not introduce blocking I/O, shared DPoP nonce races, detached unsafe mutation, or uncancelled view tasks. Background refresh identifiers, Info.plist permitted identifiers, scheduling, expiry handlers, notification permission, first-poll baseline, 50-item display retention, and 500-URI seen retention form one contract.
- **Android:** Compose state must remain lifecycle-aware and process-recreatable. Avoid writes in composition/effects, duplicate OAuth callbacks, stale concurrent feed loads, and uncancelled requests. Validate back stack, rotation, process death, deep links, and logout. Use structured concurrency and `Dispatchers.IO` for synchronous OkHttp. URL-encode every XRPC query value.
- **Website:** `website/src/routes/+page.svelte` and `website/src/routes/+layout.svelte` must remain server-rendered and static where possible. The OAuth endpoint (`/client-metadata.json`) must remain dynamic. Do not globally prerender without verifying deployment semantics.
- **Both apps:** Preserve native accessibility, Dynamic Type, dark/light behavior, reduced motion, safe-area behavior, and platform-appropriate mark/wordmark coordinates.

## Build, Tests, and Distribution

- **iOS:** The checked-in project uses Swift 5 mode, app deployment target iOS 26.0, test target iOS 26.5, bundle `uk.ewancroft.Inkwell`, marketing version `1.0`, and build `50`. Resolve Swift packages through Xcode and build the `Inkwell` scheme on an installed compatible simulator.
  - Run `xcodebuild -project iOS/Inkwell.xcodeproj -scheme Inkwell -destination 'platform=iOS Simulator,name=<available iOS 26.5 device>' build test`, adapting only the destination to installed runtimes.
  - Unit tests cover AT-URI parsing, association/canonical URLs, verification endpoint paths, wire keys, search decoding, notification JSON, and tolerant record pages.
  - `iOS/altstore/source.json` must match bundle/version/build, privacy/permissions, hosted icon/IPA, byte size, and release notes. See `iOS/AGENTS.md` for iOS-specific distribution requirements.
- **Android:** Target facts: compile/target SDK 36, minimum SDK 26, Java/Kotlin JVM 17, release minification enabled, app ID `uk.ewancroft.inkwell`, debug ID suffix `.debug`, version `1.3.0`, versionCode `5`. Run `./gradlew clean assembleDebug lint test` from `Android/` with a valid local Android SDK/JDK. For release work also run `./gradlew assembleRelease` and inspect R8 output. See `Android/AGENTS.md` for Android-specific capability gaps.
- **Website:** pnpm is authoritative (`pnpm-lock.yaml` and `pnpm-workspace.yaml`; no npm lock). Vercel installs with `pnpm install` on Node 22. Run `pnpm install --frozen-lockfile`, `pnpm check`, and `pnpm build`. There is no `lint` or test script; use `pnpm exec prettier --check --ignore-unknown .` for formatting checks. See `website/AGENTS.md` for website-specific design/accessibility constraints.
- **All:** Manually exercise fresh/cancelled OAuth, bad state/issuer/nonce, restore/refresh/revocation/logout, every reader format, Unicode facets, blobs, create/edit/delete, subscriptions/recommends/comments, verification, pagination, offline errors, and background/local notifications.
- Never commit `local.properties`, `.idea/`, `.gradle/`, `app/build/`, signing material, OAuth sessions, or real credentials.
