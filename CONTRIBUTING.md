# Contributing to Inkwell

Thanks for wanting to contribute to Inkwell.

This guide is written for **human contributors** and is intended to be enough on its own. You do not need to read the repository's `AGENTS.md` files; those exist for automated coding tools and contain much more implementation detail than a normal contributor should need.

Inkwell is a monorepo containing:

| Path | What lives there | Main tool |
| --- | --- | --- |
| `shared/` | Kotlin Multiplatform business logic used by both apps | Android Studio / Gradle |
| `iOS/` | Primary SwiftUI app | Xcode |
| `Android/` | Jetpack Compose app and the Gradle root | Android Studio |
| `website/` | SvelteKit website, install metadata, and OAuth metadata | Node.js / pnpm |
| `legal/` | Canonical privacy policy and terms | Markdown + renderer |

If you only want to work on one platform, you do not need every toolchain installed. A website-only contributor does not need Xcode, for example.

## Before you start

Please:

1. Search the [open issues](https://github.com/ewanc26/inkwell/issues) before starting substantial work.
2. For a bug, describe what is broken, which platform it affects, and how to reproduce it.
3. For a feature, explain the user problem first. Large cross-platform changes are much easier to review when the intended behaviour is agreed before implementation.
4. Keep pull requests focused. A bug fix should not quietly become a redesign, dependency upgrade, release, and refactor in the same PR.
5. Read and follow the [Code of Conduct](CODE_OF_CONDUCT.md).

Small fixes do not need an issue first if the problem and solution are obvious.

## Clone the repository

The checked-in iOS Kotlin Multiplatform framework contains binaries stored with **Git LFS**, so install Git LFS before cloning or pull the LFS objects immediately afterwards.

```bash
git lfs install
git clone https://github.com/ewanc26/inkwell.git
cd inkwell
git lfs pull
```

If you are contributing from a fork, clone your fork instead and add this repository as `upstream`:

```bash
git remote add upstream https://github.com/ewanc26/inkwell.git
git fetch upstream
```

Create a branch for your change:

```bash
git switch -c fix/short-description
```

Do not work directly on `main`.

## Toolchain overview

### iOS

- macOS
- **Xcode 26 or newer**
- an installed iOS simulator runtime compatible with the app's **iOS 18.0 deployment target**
- Git LFS

The deployment target is iOS 18.0, but the project itself requires Xcode 26+ because `iOS/Inkwell/Inkwell.icon` uses Apple's Icon Composer format. Xcode 16 and older cannot build that asset correctly.

Apple's current Xcode compatibility table is maintained at <https://developer.apple.com/xcode/system-requirements/>.

### Android

- a current stable Android Studio release that supports Android Gradle Plugin 8.13
- JDK 17 for CI-equivalent builds
- Android SDK Platform 36
- Git

The project currently uses:

- Android Gradle Plugin **8.13.2**
- Gradle **8.13** via the checked-in wrapper
- Kotlin **2.3.0**
- `compileSdk` / `targetSdk` **36**
- `minSdk` **26**
- JVM target **17**

Current Android Studio releases support AGP 8.13; Google's compatibility table is at <https://developer.android.com/studio/releases>. AGP 8.13 itself requires at least JDK 17 and Gradle 8.13: <https://developer.android.com/build/releases/agp-8-13-0-release-notes>.

### Website

- Node.js 22
- pnpm 11

CI uses these versions, so matching them locally gives the least surprising result.

---

# iOS development in Xcode

## 1. Open the project

Open:

```text
iOS/Inkwell.xcodeproj
```

You can double-click it in Finder or run:

```bash
open iOS/Inkwell.xcodeproj
```

There is no workspace you need to open instead.

On the first launch, let Xcode resolve the Swift Package Manager dependencies. Inkwell currently pulls AT Protocol-related packages through Swift Package Manager.

## 2. Make sure you are using Xcode 26+

From Terminal:

```bash
xcodebuild -version
```

If the build fails with an error similar to:

```text
None of the input catalogs contained a matching app icon set
```

check your Xcode version first. The Inkwell app icon is an Xcode 26 Icon Composer `.icon` bundle; that error on an older Xcode does **not** mean the icon is missing from the repository.

If you have multiple Xcodes installed, make sure the command-line tools point at the one you intend to use.

## 3. Install an iOS simulator runtime

In Xcode, choose **Xcode → Settings → Components** and install an iOS simulator runtime if you do not already have one. Any runtime that satisfies the iOS 18.0 deployment target is suitable for ordinary development; using a current runtime is recommended.

Apple documents simulator/runtime installation here:
<https://developer.apple.com/documentation/xcode/downloading-and-installing-additional-xcode-components>

## 4. Build and run

In Xcode's toolbar:

1. Select the **Inkwell** scheme.
2. Choose an iPhone simulator as the run destination.
3. Press **Run** (`⌘R`).

Apple's guide to schemes and run destinations is here:
<https://developer.apple.com/documentation/xcode/running-your-app-on-simulated-or-physical-devices>

A simulator is the easiest development target because it does not require you to configure your own signing identity.

### Running on a physical iPhone

You can also run on a real device. Xcode may require an Apple ID and a development team for local signing. If you make local signing changes to the project, do **not** include unrelated signing/team changes in your pull request.

Do not ask for or use the project's distribution certificates, provisioning profiles, or release credentials. They are not required for normal development.

## 5. Run the iOS tests

From Xcode, use **Product → Test** (`⌘U`).

For a CI-like command-line run, first list your available simulator destinations if necessary:

```bash
xcrun simctl list devices available
```

Then run:

```bash
cd iOS
xcodebuild \
  -project Inkwell.xcodeproj \
  -scheme Inkwell \
  -destination 'platform=iOS Simulator,name=<your simulator name>' \
  build test
```

The app and unit-test targets currently deploy to iOS 18.0.

**Important:** the Xcode unit tests do not test the implementation inside `shared/`. If your change touches shared Kotlin code, also run the shared Gradle tests described below.

## 6. Useful testing mode

For UI work where you want to use your real signed-in account without accidentally writing records, Inkwell has a testing launch mode.

In **Product → Scheme → Edit Scheme → Run → Arguments**, add:

```text
-testing
```

Testing mode keeps real reads and the real signed-in session, but intercepts writes. Optional launch arguments can open a particular tab directly:

```text
-tab-reader
-tab-discover
-tab-writer
```

This is useful for screenshots and visual checks. It is not a substitute for testing the real write path when your change actually modifies writing behaviour.

## 7. iOS-specific code belongs in `iOS/`

Good examples:

- SwiftUI views and navigation
- Keychain access
- notification/background APIs
- iOS lifecycle behaviour
- platform accessibility behaviour
- wrappers around the shared Kotlin framework

Portable business rules should normally go in `shared/`, not be implemented a second time in Swift.

### Common Xcode setup problems

**`InkwellShared` is missing or the framework binary looks like a tiny text file**

Run:

```bash
git lfs pull
```

The XCFramework executable is tracked through Git LFS.

**No suitable simulator appears**

Install an iOS runtime under **Xcode → Settings → Components**.

**Swift packages did not resolve**

Use Xcode's package-resolution controls and check your network connection before changing package versions. Do not update dependencies just to work around a transient package-resolution failure.

---

# Android development in Android Studio

## 1. Open the correct directory

From the Android Studio welcome screen choose **Open** and select:

```text
<repository>/Android
```

Open the **`Android/` directory**, not the repository root. `Android/settings.gradle.kts` is the Gradle root and maps the sibling `../shared` directory into the project as the `:shared` module.

Wait for the initial Gradle sync to complete before treating editor errors as real compile errors.

## 2. Configure the Gradle JDK

CI runs the Android build with **JDK 17**.

In Android Studio, open the Gradle settings:

- macOS: **Android Studio → Settings → Build, Execution, Deployment → Build Tools → Gradle**
- Windows/Linux: **File → Settings → Build, Execution, Deployment → Build Tools → Gradle**

Choose a JDK 17 installation for **Gradle JDK** if you want to match CI exactly. Android Studio can also download a JDK for you.

Google's current Gradle JDK instructions are here:
<https://developer.android.com/build/jdks>

A newer JDK may work with your Android Studio/Gradle combination, but a contribution still has to pass CI on JDK 17.

## 3. Install the Android SDK

Use **Tools → SDK Manager** and make sure Android SDK Platform **36** is installed. Android Studio/AGP will prompt for other required build tools if they are missing.

Do not commit `local.properties`; Android Studio creates it with your local SDK path.

## 4. Create an emulator

Open **View → Tool Windows → Device Manager**, then create a virtual device.

Inkwell supports API 26 and newer. For ordinary development, use a current API 36 image. If your change touches compatibility-sensitive APIs, also test an API 26 device/emulator where practical.

Google's Device Manager guide is here:
<https://developer.android.com/studio/run/managing-avds>

## 5. Build and run

Select the **app** run configuration and your emulator/device, then press **Run**.

The debug build has the application ID:

```text
uk.ewancroft.inkwell.debug
```

so it can be installed alongside a release build.

You do not need release signing credentials for normal development.

## 6. Run the Android checks

Android Studio can run individual JUnit tests from the gutter. Before opening a PR that changes Android or shared code, run the same combined command used by CI:

```bash
cd Android
./gradlew clean assembleDebug lint test :shared:jvmTest
```

That command covers:

- Android debug compilation
- Android lint
- Android JVM unit tests
- shared Kotlin JVM tests

Some app tests exercise live public Standard.site resources, so an internet connection may be required for the complete test run.

### A note about `./gradlew test`

`./gradlew test` does **not** run the shared Kotlin Multiplatform test suite. The shared module exposes `jvmTest`, so always add:

```bash
./gradlew :shared:jvmTest
```

when shared code changed.

### Common Android Studio setup problems

**Gradle says it requires Java 17**

Set the project's Gradle JDK to 17 as described above.

**Android SDK Platform 36 is missing**

Install it with SDK Manager, then sync again.

**The `:shared` module is missing**

Make sure you opened `Android/` itself as the project and that the repository still has `shared/` next to it.

**A release build is unsigned**

That is expected on a normal contributor checkout. Release credentials live in ignored local files and are not distributed to contributors.

---

# Shared Kotlin Multiplatform code

The `shared/` module is the preferred home for logic that both apps should agree on.

Examples include:

- Standard.site/AT Protocol models and portable wire rules
- markdown parsing and serialization helpers
- facets and UTF-8 byte-offset handling
- content-format conversion
- verification/canonical URL construction
- reader policy and other platform-neutral decisions

Platform UI and operating-system integrations stay native.

## Source sets

The important source sets are:

```text
shared/src/commonMain/   portable implementation
shared/src/commonTest/   portable tests
shared/src/androidMain/  Android-specific implementations
shared/src/iosMain/      iOS-specific implementations
shared/src/jvmMain/      JVM test/runtime helpers
```

When you open `Android/` in Android Studio, the shared module is included automatically.

## Run shared tests

From `Android/`:

```bash
./gradlew :shared:jvmTest
```

For a broader Kotlin Multiplatform run on a Mac with the Apple toolchain installed:

```bash
./gradlew :shared:allTests
```

The latter is slower and includes native Apple targets.

## iOS consumes a checked-in XCFramework

Android compiles `shared/` directly. iOS currently consumes the checked-in:

```text
shared/InkwellShared.xcframework
```

That means changing Kotlin source does **not** automatically change what Xcode links.

If your shared change affects iOS behaviour or changes the API exposed to Swift:

1. run `:shared:jvmTest`;
2. on macOS, build the native framework slices with the Gradle tasks in `shared/build.gradle.kts` (the `assembleXCFramework` task builds the configured iOS framework slices);
3. regenerate the checked-in `InkwellShared.xcframework` rather than editing anything inside it by hand;
4. make sure the regenerated binary remains tracked through Git LFS;
5. build and test the iOS app against the refreshed framework.

The repository does not currently provide a one-command wrapper for the final XCFramework packaging step. If you are changing shared code but are unsure how to package the framework correctly, say so clearly in the issue or pull request rather than hand-editing generated framework contents. Source + tests are much easier to review than a broken binary artefact.

Swift wrappers around shared APIs live in files such as `iOS/Inkwell/SharedKMP.swift` and the focused `SharedKMP+*.swift` companions. Keep those wrappers thin; avoid reimplementing the same rule independently in Swift and Kotlin.

---

# Website development

The website is in `website/` and uses SvelteKit.

## Install dependencies

Use Node.js 22 and pnpm 11:

```bash
cd website
pnpm install --frozen-lockfile
```

## Run the development server

```bash
pnpm dev
```

Vite prints the local URL in the terminal.

## Validate website changes

Before opening a website PR:

```bash
pnpm check
pnpm exec prettier --check --ignore-unknown .
pnpm build
```

To apply Prettier formatting rather than just check it:

```bash
pnpm format
```

There is currently no separate website unit-test or lint script; `svelte-check`, Prettier, and the production build are the main local gates.

## Distribution mirrors

The website serves copies of the install metadata. If your change deliberately touches those files, CI requires these pairs to remain identical:

```text
iOS/altstore/source.json
website/static/altstore/source.json
```

and:

```text
Android/fdroid-repo/repo/
website/static/fdroid/repo/
```

Do not change release metadata, app versions, signed packages, or store publishing files as part of an ordinary feature/fix PR unless the issue is explicitly release-related.

## Website behaviour to preserve

The site also hosts OAuth metadata and legal/privacy pages. Changes to those are functional changes, not just copy edits. Do not add analytics, tracking pixels, cookies, remote scripts, or collection of user data without prior discussion and corresponding policy work.

---

# Legal documents

`legal/privacy.md` and `legal/terms.md` are the canonical legal sources. Copies are rendered into the apps and website.

If you intentionally edit legal content, regenerate the derived copies:

```bash
node tools/legal/render.mjs
```

Then verify that everything is in sync:

```bash
node tools/legal/render.mjs --check
```

CI runs the `--check` form for every pull request, regardless of which platform changed.

Do not edit a generated platform copy of the privacy policy/terms and leave the canonical `legal/` source unchanged.

---

# Where should a change go?

A useful rule of thumb:

| Change | Put it in |
| --- | --- |
| Portable parsing/conversion/model/policy logic | `shared/` |
| SwiftUI, Keychain, Apple notifications/background work | `iOS/` |
| Compose UI, Android services/storage/work scheduling | `Android/` |
| Marketing site, install pages, web OAuth endpoint | `website/` |
| Privacy policy or terms | `legal/` first, then regenerate |

For behaviour shared by iOS and Android, prefer **one shared implementation with thin platform adapters** rather than two similar implementations that can drift apart.

Cross-platform PRs are welcome when the change genuinely needs to land together. If the work can be reviewed and shipped independently, smaller platform-specific PRs are usually easier.

---

# Security and authentication

Never commit secrets, credentials, tokens, signing material, or personal session data.

In particular, do not commit:

- Android `local.properties`
- Android `keystore.properties`
- `*.keystore`, `*.jks`, `.p12`, provisioning profiles, or signing certificates
- OAuth access/refresh tokens
- DPoP private keys
- PKCE verifier/state values
- real auth codes
- Apple DerivedData / `xcuserdata`
- Android `.idea/`, `.gradle/`, or build output
- website `node_modules/` or `.svelte-kit/`

OAuth changes often affect all three surfaces: iOS, Android, and the hosted metadata on the website. Treat scopes, redirect URIs, client IDs, DPoP behaviour, and token storage as security-sensitive contracts.

If you accidentally commit a real secret, removing it in a later commit is not enough. Treat it as compromised and report it immediately so it can be rotated.

---

# Accessibility and manual testing

Accessibility regressions are bugs.

For UI changes, check the platform behaviours that are relevant to your work:

### iOS

- VoiceOver
- Dynamic Type / larger text sizes
- light and dark appearance
- reduced motion
- safe areas and keyboard presentation

### Android

- TalkBack
- system font scaling / display size
- light and dark theme
- reduced animation settings where relevant
- navigation/back behaviour

### Website

- keyboard-only navigation
- visible focus state
- semantic headings/landmarks
- screen-reader labelling
- reduced motion
- light/dark colour contrast

The website targets WCAG 2.1 AA.

---

# What to test before a pull request

You do not need to run every toolchain for a one-platform change, but run the checks that cover what you touched.

| Changed area | Minimum useful local validation |
| --- | --- |
| `iOS/**` | Xcode build + `Product → Test` (or equivalent `xcodebuild`) |
| `Android/**` | `./gradlew clean assembleDebug lint test` |
| `shared/**` | `./gradlew :shared:jvmTest`, plus affected app builds |
| `website/**` | `pnpm check`, Prettier check, `pnpm build` |
| `legal/**` | `node tools/legal/render.mjs --check` after regeneration |
| OAuth/auth | affected app(s) + hosted metadata consistency + manual login/logout flow |
| UI | platform build/tests + a manual accessibility/visual pass |

CI automatically skips unrelated platform builds where possible. A shared-code change intentionally triggers both the Android and iOS paths.

---

# Code style

Follow the style already present in the files you are changing.

### Swift / SwiftUI

- prefer Swift concurrency rather than blocking work;
- keep UI state on the appropriate actor;
- keep platform wrappers around shared Kotlin logic small;
- use availability checks for APIs newer than the iOS 18.0 deployment target instead of casually raising the minimum OS version.

### Kotlin / Compose

- keep state in ViewModels/flows rather than hiding business state in composables;
- use structured coroutines;
- keep blocking/network work off the main thread;
- encode XRPC query values correctly;
- put portable business rules in `shared/`.

### Svelte / TypeScript

- keep the site server-renderable;
- use the existing design tokens/components rather than introducing isolated visual systems;
- run Prettier instead of manually fighting formatting;
- preserve the dynamic OAuth metadata route.

Avoid drive-by reformatting of unrelated files.

---

# Commits and pull requests

There is no requirement to use a particular conventional-commit tool, but commit messages should be short, specific, and scoped to what changed.

Examples:

```text
fix(ios): preserve publication theme override
fix(android): handle malformed search records
fix(shared): correct UTF-8 facet offsets
docs: improve contributor setup guide
```

Before opening a PR:

- rebase/update from `main` if your branch has become stale;
- review your own diff for generated files, secrets, signing changes, and accidental IDE metadata;
- run the relevant checks above;
- explain **what changed and why** in the PR description;
- include screenshots or a short recording for meaningful UI changes;
- mention any test you could not run and why.

Do not bump app versions, publish packages, regenerate store listings, or create releases for an ordinary contribution. Release publishing is a maintainer task unless explicitly coordinated.

## Pull request checklist

- [ ] The change is focused and has a clear user/developer reason.
- [ ] I changed portable logic in `shared/` rather than duplicating it across apps where appropriate.
- [ ] I ran the checks relevant to the files I changed.
- [ ] I manually tested user-facing behaviour that automated tests do not cover.
- [ ] I considered VoiceOver/TalkBack/keyboard and text scaling where relevant.
- [ ] I did not commit credentials, signing material, local IDE state, or build output.
- [ ] Legal/OAuth/install metadata copies are still in sync if I touched them.
- [ ] I included screenshots/recordings for significant visual changes.
- [ ] I noted anything that still needs maintainer-only signing/release work.

---

# AI-assisted contributions

Using AI tools is allowed. You are still responsible for understanding and reviewing what you submit.

When an AI agent materially contributed code or documentation, add an accurate `Co-authored-by:` trailer to the relevant commit(s). Do not attribute trivial autocomplete as though it were a co-author, and do not hide substantial generated work.

The same review, testing, licensing, security, and quality expectations apply regardless of how the contribution was produced.

---

# Licence of contributions

Inkwell is licensed under AGPL-3.0 with the [App Store Distribution Exception](APP_STORE_EXCEPTION.md).

By submitting a contribution, you agree to license it under those same terms unless an alternative has been explicitly agreed in writing before merge. You must have the right to contribute every code, text, image, or other asset included in your change.

Do not copy code or assets from sources whose licence is incompatible with this repository.

---

# Getting help

If setup fails and the error is not covered here, open an issue or comment on the issue you are working from. Include:

- your platform and OS version;
- Xcode or Android Studio version;
- relevant simulator/emulator/device version;
- the command you ran;
- the first useful error message, not just the final generic build-failed line.

Questions are welcome. A good contributing guide should make the common path boring, not make you reverse-engineer the maintainer's machine before you can change a button.
