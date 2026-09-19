# Release provenance

The AltStore IPA is currently exported outside GitHub Actions because the
release flow requires an interactive Xcode export/signing step. The release
tool therefore hashes the exact IPA supplied to
`tools/release/publish.mjs`, and the same bytes are copied to the hosted
AltStore path and attached to the GitHub Release.

Do not create a GitHub Actions artifact attestation for an IPA that was built
or signed elsewhere. Such an attestation would describe different bytes from
the distributed IPA. The current integrity check is the SHA-256 value in both
AltStore manifests:

```sh
node tools/release/validate-altstore.mjs --hosted
```

When iOS release assembly moves into GitHub Actions, generate an artifact
attestation for the final IPA after the exact bytes are produced and verify it
with:

```sh
gh attestation verify Inkwell-<version>.ipa -R ewanc26/inkwell
```

Android and iOS provenance should then be recorded against the release tag,
commit SHA, workflow run, final artifact hashes, and the attached SBOM.
