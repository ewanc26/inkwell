# AGENTS.md

Guidance for agents working on Inkwell, the SwiftUI iOS client for long-form AT Protocol publishing.

## Architecture

- `Inkwell/` contains SwiftUI screens, models, networking, OAuth, editor, persistence, and design resources.
- `InkwellTests/` contains unit/integration coverage.
- `oauth/` and the app's metadata/entitlements define redirect and client identity behavior.
- `altstore/` contains distribution metadata; keep versions and artifacts aligned with releases.

## Invariants

- Preserve Swift concurrency isolation: UI state on `MainActor`, cancellable network tasks, and no blocking I/O on the main thread.
- AT Protocol identifiers, facets, blobs, record revisions, and open-union content must round-trip without lossy rewriting.
- Store tokens/keys in Keychain, not `UserDefaults`, logs, fixtures, or source.
- Maintain interoperability with `standard.site` and provider-specific content described in the README.
- Follow existing design tokens and native accessibility/Dynamic Type behavior.

## Validation

Build the `Inkwell` scheme with `xcodebuild` for an available simulator and run tests. Exercise fresh OAuth, cancellation/state mismatch, session restore/refresh, document create/edit/publish/reload, offline errors, Unicode, images, dark mode, Dynamic Type, and VoiceOver labels. Never commit signing profiles, derived data, or real credentials.
