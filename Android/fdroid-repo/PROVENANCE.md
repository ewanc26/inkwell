# Release provenance

The F-Droid APK is currently signed and assembled through the local
fdroid-repo workflow described in `README.md`; it is not produced as the
exact final distributed byte stream by GitHub Actions. Do not create an
artifact attestation for a different CI APK and present it as provenance for
the F-Droid APK.

For each release, verify the exact APK copied into `repo/` before publishing:

```sh
shasum -a 256 repo/Inkwell-<version>.apk
```

The resulting digest must be recorded alongside the GitHub Release and the
F-Droid index revision. The F-Droid repository signature authenticates the
index and its listed APK hashes, but is not a GitHub Actions build-provenance
attestation.

When Android release assembly moves into GitHub Actions, generate an
attestation only after the exact signed APK bytes are produced there, then
verify those same bytes with:

```sh
gh attestation verify Inkwell-<version>.apk -R ewanc26/inkwell
```

The release should also attach a machine-readable SBOM whose digest is
recorded with the APK and IPA hashes. Until CI produces the final artifacts,
the release must describe this provenance limitation explicitly.
