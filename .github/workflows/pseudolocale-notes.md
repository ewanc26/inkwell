# Pseudolocalisation / locale regression CI (#88)

Status: framework added, full execution depends on #53/#83 emulator device stack.

## What's in place
- `iOS/Inkwell/Localizable.xcstrings` — 357 strings, source locale `en` (#84 done)
- Xcode builds with `GenerateStringSymbols_Localizable.swift` (verified)
- CI has `iOS build + test` job (`.github/workflows/ci.yml` iOS section)

## What's needed (depends on #53/#83)
- Real iOS simulator runtime with pseudo-locale support (`en-XA`, `ar-XB`, Double-Length, Accented, Bounded String, RTL, Tall) available to `xcodebuild -test`
- Android emulator with `ar-XB` / `en-XA` locales and instrumentation smoke tests (`android-instrumentation` job)
- A failure-report mechanism that names the screen/string rather than just a screenshot diff

## Recommended CI additions (do when #53/#83 ready)
- Add `iOS pseudo-locale` job: `xcodebuild test` with `-AppleLanguages (ar)` / `-AppleLocale (ar_XB)` running `InkwellTests` + a targeted `Writer`/`Login` smoke test
- Add `Android pseudo-locale` instrumentation: boot emulator, `adb shell setprop persist.sys.locale ar-XB`, run existing instrumentation, compare against `en` baseline
- Add a non-mutating script (e.g., `grep -R 'Text("[^$]' iOS/`) to detect new unlocalised user-facing strings in PRs

## Blocker note
Do not create a second independent device stack; coordinate with #53/#83 (emulator-runner / instrumentation setup) per the issue's own instruction.
