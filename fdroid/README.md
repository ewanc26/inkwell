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

- [ ] **TODO — not yet done:** git tag `v1.0.0` must be created and pushed to the `main` branch of the inkwell-android repo before this metadata can be submitted. It does not exist yet as of writing (only `v0.1.1`, `v0.2.0`, and dated `v2026-*` tags exist). `uk.ewancroft.inkwell.yml`'s `Builds.commit` references `v1.0.0` to match `app/build.gradle.kts`'s `versionName = "1.0.0"` / `versionCode = 1` — F-Droid's build system will fail to check out a nonexistent tag, so do not submit until the tag exists.
- [x] Fastlane metadata is present in `fastlane/metadata/android/en-GB/` (already in this repo).
- [ ] Confirm all dependencies are FOSS (no Firebase, GMS, or other proprietary libraries) — verify with `./gradlew app:dependencies --configuration releaseRuntimeClasspath` before submitting.
