# Inkwell self-hosted F-Droid repo — maintenance workspace

This directory is the `fdroidserver` working copy for the F-Droid repo hosted
at `https://inkwell.ewancroft.uk/fdroid/repo`. It lives inside the
`inkwell-android` git repo. The signing keystore and its credentials
(`config.yml`, `keystore.p12`, `inkwell-release.keystore`) are gitignored and
must **never** be committed; the generated `repo/`, `metadata/`, and this
README are safe to version.

## What's here

- `config.yml` — fdroidserver config, including the repo signing keystore
  password (`keystorepass`/`keypass`). Treat this file as a secret.
- `keystore.p12` — the F-Droid **repo index signing key**. This proves to
  clients that index updates genuinely came from you. If lost, there is no
  recovery — you'd have to publish a brand new repo at a new URL, and every
  existing user would need to remove the old repo and re-add the new one.
- `inkwell-release.keystore` — the **APK signing key** used to sign the
  actual Inkwell builds distributed through this repo (separate from the
  F-Droid repo key above, and separate from any Google Play signing key you
  might set up later). Its password lives in `keystore.properties` in the
  `inkwell-android` repo's root (gitignored, not duplicated here — check
  there).
- `metadata/uk.ewancroft.inkwell.yml` — the app's listing metadata (description,
  categories, links) shown in F-Droid clients.
- `repo/` — the generated, signed repo (index-v1.jar, index-v2.json, icons,
  the APK itself). This is what gets copied into the inkwell-website repo's
  `static/fdroid/` on each update.
- `inkwell-icon.png` — the repo's own icon (shown as the repo's identity in
  F-Droid client settings, separate from the app's own icon).

**Back up `config.yml`, `keystore.p12`, and `inkwell-release.keystore`
somewhere safe (password manager, encrypted backup) — losing them breaks
future updates permanently.** Copies were also sent to you directly as
downloadable files when this was set up.

## Publishing a new version

1. Build a signed release APK from the `inkwell-android` repo (needs the
   gitignored `keystore.properties` at that repo's root pointing at
   `fdroid-repo/inkwell-release.keystore` — both are gitignored, restore them
   if missing):
   ```bash
   cd /path/to/inkwell-android
   ./gradlew assembleRelease
   ```
2. Copy the new APK into this workspace's `repo/` directory, replacing the
   old one (keep the naming convention `Inkwell-<versionName>.apk`, or let
   `fdroid update --rename-apks` handle it):
   ```bash
   cp app/build/outputs/apk/release/app-release.apk \
      /Users/ewan/Developer/Git/inkwell-android/fdroid-repo/repo/Inkwell-<new-version>.apk
   ```
   If you're updating an existing version (same versionCode), delete the old
   APK first so there's exactly one file per version.
3. Update `metadata/uk.ewancroft.inkwell.yml`'s `CurrentVersion`/
   `CurrentVersionCode` if they changed.
4. Regenerate the signed index:
   ```bash
   cd /Users/ewan/Developer/Git/inkwell-android/fdroid-repo
   fdroid update --clean
   ```
   (`fdroid` here refers to the `fdroidserver` pip package, installed via
   `pipx install fdroidserver --backend pip`, plus `pipx inject fdroidserver
   "setuptools<81"` — newer setuptools removed `pkg_resources`, which this
   version of fdroidserver still imports.)
5. Copy the regenerated `repo/` into the website repo and deploy:
   ```bash
   rm -rf /path/to/inkwell-website/static/fdroid/repo
   cp -R /Users/ewan/Developer/Git/inkwell-android/fdroid-repo/repo /Users/ewan/Developer/Git/inkwell-website/static/fdroid/repo
   rm -rf /path/to/inkwell-website/static/fdroid/repo/status
   ```
   Commit and deploy the website as usual.

## Known gotcha: AGP resource path shortening

`inkwell-android`'s `gradle.properties` has
`android.enableResourceOptimizations=false`. Without it, AGP's release-build
resource shrinker flattens `res/mipmap-hdpi/ic_launcher.png` into an
obfuscated path like `res/o-.png` with no density-folder structure, which
silently breaks fdroidserver's icon extraction (no error — the app just
shows no icon in the F-Droid listing). Don't remove that flag without
re-testing icon extraction (`fdroid update -v` and check `repo/icons-640/`
for a real per-app icon file after building).
