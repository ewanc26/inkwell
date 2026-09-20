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
or assembled elsewhere. Until a channel artifact is produced in GitHub
Actions, document that signing/build limitation instead of creating a
misleading attestation.

When the release workflow produces an artifact, attach its provenance
attestation and a machine-readable SBOM alongside the binary. Verify the
artifact and its exact release commit with:

```sh
gh attestation verify Inkwell-<version>.apk --repo ewanc26/inkwell
gh attestation verify Inkwell-<version>.ipa --repo ewanc26/inkwell
```

Run the command only for an artifact that was actually attested; an externally
signed artifact must instead be verified using the channel's published SHA-256
and the documented signing metadata. Keep the SBOM digest and binary hashes
aligned with the AltStore/F-Droid metadata generated for that same release.
