# Pseudolocalisation / locale regression CI (#88)

Status: device-stack blockers resolved; structural coverage landed, rendering coverage still open.

## What's in place

- `iOS/Inkwell/Localizable.xcstrings` — source locale `en` (#84 done).
- Android `values-fr/strings.xml` — full French catalogue.
- Website `fr` message catalogue, compile-checked against `en-GB` via
  `Record<Locale, Messages>` in `messages/index.ts` (`pnpm check` fails on
  a missing key).
- Android `ComposeHardcodedTextDetector` lint rule — a newly hard-coded
  `Text("…")` literal fails `lintDebug` unless suppressed with an explicit
  `// noinspection` justification.
- `LocaleResourcesParityTest` (Android unit, no device needed) — default/French
  key-set equality, per-key placeholder equality, per-locale CLDR quantity
  sets (`en`: one/other, `fr`: one/many/other — the rule that would have
  caught #161), per-quantity placeholder checks with the documented
  French-`one`-covers-zero exemption, blank/duplicate detection, and a
  coverage tripwire if a new `values-<locale>` dir appears.
- `LocalizationCatalogTests` (iOS unit) — catalog source language, every key
  has a non-empty English value, format-specifier consistency across
  localizations (length modifiers included), no dangling `%`.
- Android debug builds set `isPseudoLocalesEnabled = true`, so every debug
  APK ships `en-rXA` (accented/expanded) and `ar-rXB` (RTL) variants —
  verified via `aapt2 dump resources`. Release builds stay clean.

## Blocker update (2026-11-02)

- #53 (Android instrumentation smoke suite) is CLOSED — the
  `android-instrumentation` CI job runs real-lifecycle tests on an emulator.
- #83 (a11y regression coverage) is CLOSED.
- The "do not create a second CI device stack" constraint from the original
  notes is therefore satisfiable: any future pseudo-locale instrumentation
  must reuse the existing `android-instrumentation` job (e.g. parameterise
  the emulator locale), not stand up a parallel stack.

## What's still needed (kept open on #88)

- CI exercising an expanded LTR and an RTL pseudo-locale end to end:
  - iOS: `xcodebuild test` with a pseudolanguage (`-testLanguage` / scheme
    application language) over the existing `InkwellTests` plus a targeted
    Writer/Login smoke pass;
  - Android: boot the existing CI emulator image with `ar-XB`/`en-XA` and
    run the existing instrumentation suite against it, comparing with the
    `en` baseline.
- Screenshot/UI fixtures covering login, Reader cards/detail, Discover,
  Writer, Settings/moderation, notifications, and accessibility labels under
  both pseudo-locales.
- A failure-report mechanism that names the screen/string rather than only
  producing an image diff.
- New-user-facing-string detection for SwiftUI (Android has the lint rule;
  iOS has no equivalent check yet).
- Failures must compose with the accessibility-size coverage from #83.
