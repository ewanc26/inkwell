# F-Droid Build Metadata

This directory contains the F-Droid build metadata file ready for submission to the [fdroiddata](https://gitlab.com/fdroid/fdroiddata) repository.

## Submitting

1. Fork [fdroiddata](https://gitlab.com/fdroid/fdroiddata) on GitLab.
2. Clone your fork and create a branch named `uk.ewancroft.inkwell`.
3. Copy `uk.ewancroft.inkwell.yml` to `metadata/uk.ewancroft.inkwell.yml` in your fdroiddata clone.
4. Test the metadata:

   ```bash
   fdroid readmeta
   fdroid rewritemeta uk.ewancroft.inkwell
   fdroid lint uk.ewancroft.inkwell
   fdroid build uk.ewancroft.inkwell
   ```

5. Commit and push:

   ```bash
   git add metadata/uk.ewancroft.inkwell.yml
   git commit -m "New App: uk.ewancroft.inkwell"
   git push origin uk.ewancroft.inkwell
   ```

6. Open a merge request against fdroiddata on GitLab.

## Prerequisites

- [ ] **TODO — not yet done:** git tag `v1.0.1` must be created and pushed to the `main` branch of the inkwell-android repo before this metadata can be submitted. It does not exist yet as of writing. `uk.ewancroft.inkwell.yml`'s `Builds.commit` references `v1.0.1` to match `app/build.gradle.kts`'s `versionName = "1.0.1"` / `versionCode = 2` — F-Droid's build system will fail to check out a nonexistent tag, so do not submit until the tag exists.
- [x] Fastlane metadata is present in `fastlane/metadata/android/en-GB/` (already in this repo).
- [x] Confirmed all dependencies are FOSS — `./gradlew app:dependencies --configuration releaseRuntimeClasspath` resolved cleanly with no Firebase/GMS. The only `com.google.*` packages present (Gson, Guava, Tink, Dagger/Hilt, Accompanist) are FOSS, Apache 2.0-licensed. Re-run this check before actually submitting if dependencies have changed since.
- [ ] `./gradlew assembleRelease` builds cleanly (verified — see repo history), but there is no `signingConfigs` block in `app/build.gradle.kts`, so the release build is currently unsigned. F-Droid signs its own builds from source, so this is fine for F-Droid specifically — but confirm this is still true (or add one) before using the same build for any other signed-release channel.
