# Release process

Each app version uses one unified GitHub Release tagged `v<version>`. Create or
update that release at the latest relevant release commit and attach the
platform artifacts that exist:

- `Inkwell-<version>.ipa` for iOS, when available
- `Inkwell-<version>.apk` for Android, when available

Keep iOS and Android notes in clearly labelled sections when their changes
differ. Platform tags (`ios-v<version>` and `android-v<version>`) are retained
as historical provenance; they are not release records for new versions.

## Provenance and SBOMs

Release provenance must describe the exact bytes attached to the GitHub
Release. Do not attest a CI rebuild when the distributed IPA or APK was signed
or assembled elsewhere.

The distributed APK and IPA are still assembled and signed outside GitHub
Actions (`tools/release/publish.mjs`, run locally, so the release signing
keys never touch CI). Because of that, `.github/workflows/release.yml`
deliberately does **not** generate an `actions/attest-build-provenance`
attestation for the APK/IPA themselves -- that would falsely claim the
workflow built bytes it never touched. Instead, once a version's unified
GitHub Release already carries its platform artifact(s) (published via
`tools/release/publish.mjs`), the workflow:

1. Generates a source-level dependency SBOM for each platform (Android via
   `anchore/sbom-action` against `Android/`'s Gradle project; iOS via
   `tools/release/generate-ios-sbom.mjs` against the checked-in
   `Package.resolved`).
2. Downloads the *exact* APK/IPA bytes already attached to the release and
   hashes them.
3. Writes `release-manifest-<tag>.json`
   (`tools/release/build-release-manifest.mjs`) recording those hashes, the
   SBOM hashes, and the release commit/tag/workflow-run identity.
4. Generates the build-provenance attestation for the **manifest and SBOMs**
   -- files this workflow run genuinely produced -- and attaches all three to
   the GitHub Release alongside the APK/IPA.

### Verifying a release

```sh
# 1. Download the manifest and SBOMs the workflow attached, and verify their
#    build-provenance attestation (this confirms which commit/workflow run
#    produced them, not the APK/IPA):
gh release download vX.Y.Z --repo ewanc26/inkwell \
  --pattern "release-manifest-*.json" --pattern "sbom-*.spdx.json"
gh attestation verify release-manifest-vX.Y.Z.json --owner ewanc26
gh attestation verify sbom-android-vX.Y.Z.spdx.json --owner ewanc26
gh attestation verify sbom-ios-vX.Y.Z.spdx.json --owner ewanc26

# 2. Confirm the APK/IPA you actually downloaded matches the hash the
#    attested manifest records for it -- this is what ties the attestation
#    to the binary, since the binary itself was never attested directly:
shasum -a 256 Inkwell-X.Y.Z.apk   # compare against .artifacts.apk.sha256 in the manifest
shasum -a 256 Inkwell-X.Y.Z.ipa   # compare against .artifacts.ipa.sha256 in the manifest
```

Do **not** run `gh attestation verify` directly against the APK or IPA --
no attestation was ever generated for those files, and the command correctly
reports no matching attestations. The IPA's hash is cross-checked a second,
independent way against `iOS/altstore/source.json` at manifest-build time
(recorded in the manifest's `artifacts.ipa.altstoreCrossCheck` field); see
`iOS/altstore/PROVENANCE.md` and `Android/fdroid-repo/PROVENANCE.md` for the
per-channel hash story.

**First real run:** as of this writing, `.github/workflows/release.yml` has
been validated for YAML/action-pin correctness and by exercising
`tools/release/build-release-manifest.mjs` and
`tools/release/generate-ios-sbom.mjs` against real and fixture data, but it
has never executed against a real tag push -- nothing short of cutting an
actual release can trigger that. On the first real `vX.Y.Z` release, a human
should confirm: the `android-sbom`/`ios-sbom` jobs produce non-empty SBOM
files, `manifest-and-attest` finds the release `publish.mjs` already created
(the "no GitHub Release found" guard should not fire), the attestation step
succeeds, and `gh attestation verify release-manifest-vX.Y.Z.json --owner
ewanc26` succeeds against the actually-uploaded manifest.
