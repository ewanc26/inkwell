# Contributing to Inkwell

Inkwell is a native reader and writer for the [Standard.site](https://standard.site) publishing ecosystem on AT Protocol, available on iOS and Android. This monorepo also contains the marketing/legal site that hosts the app's install sources and OAuth metadata. Thanks for wanting to help!

## Repository structure

| Directory | Purpose |
| --- | --- |
| `shared/` | Kotlin Multiplatform shared core: format conversion, markdown parsing, facet handling, content models, verification URLs, constellation deduplication |
| `iOS/` | SwiftUI app |
| `android/` | Kotlin/Compose app |
| `website/` | the marketing/legal site (install sources, OAuth metadata) |

Shared logic goes in `shared/`; platform code stays thin over it.

## Getting started

Clone, then open each platform project per its toolchain (Xcode for iOS, Android Studio for the Android app, pnpm for the website). CI builds all three on every push.

## Notes for this project

- The shared core is consumed by both platforms through different interop paths (Kotlin natively on Android, the `InkwellShared` framework on iOS): a change to `shared/` that compiles in one place can still break the other, so build both sides.
- UTF-8 byte-indexed facets are easy to get wrong by a character or two: facet changes need a test with multibyte text, not just ASCII.
- Verification URLs and OAuth metadata on the website are load-bearing for the apps — check the whole chain if you touch them.
- Follow the existing conventional-commit style; keep PRs focused, with how you verified in the description.
- AI-assisted PRs welcome; stand behind every line and run the builds yourself.

## Licence

AGPL-3.0 (see [LICENSE](LICENSE)). Contributions are accepted under the same terms.