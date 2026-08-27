# Inkwell 2.5.0 release notes (draft)

Inkwell 2.5.0 is the next iOS (build 58) and Android (version code 12) release. These notes describe code currently in the repository; store packages have not yet been published.

## Highlights

- Cross-platform moderation parity: iOS now includes the same Muted & Blocked account management available on Android.
- Cross-platform reader verification: Android now verifies publication-backed documents using their AT URIs, matching iOS behaviour.
- Publication-aware reading: full publication views and reader cards use publication names, descriptions, icons, and readable publication themes instead of falling back to hostnames.
- Account profiles and reporting: both apps include profile views and native account/content reporting flows.
- Enhanced moderation: labeler preferences, keyword filters, moderation labels, and explicit reveal controls are available on both platforms.
- Offline-first reading: cached feeds and full documents appear immediately, while queued subscriptions, recommendations, and comments replay after connectivity returns.
- Accessibility and readability improvements across Reader, Discover, Writer, profiles, and Settings.

## Still in progress

- Manual VoiceOver, TalkBack, largest-text, reduced-motion, and increased-contrast validation remains in progress.
- APNs/FCM push delivery and official App Store/Play Store distribution remain roadmap work.
