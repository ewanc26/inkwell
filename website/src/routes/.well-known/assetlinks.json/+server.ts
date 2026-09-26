// ── Android App Links verification ───────────────────────────────
// Android's Digital Asset Links check fetches this exact path (over
// HTTPS, with no redirect) to decide whether inkwell.ewancroft.uk may
// hand off matching links to the Inkwell app as a verified App Link —
// see the `android:autoVerify="true"` intent-filter for
// `https://inkwell.ewancroft.uk/open` in `Android/app/src/main/AndroidManifest.xml`.
//
// Served dynamically (not prerendered), matching
// client-metadata.json/+server.ts and the sibling AASA endpoint — kept
// as a real function response rather than static/prerendered output so
// the `json()` helper's `application/json` header is guaranteed, not
// dependent on adapter-vercel's static-asset MIME inference.

import { json } from "@sveltejs/kit";

export const prerender = false;

// PLACEHOLDER — replace with the SHA-256 fingerprint of the actual Android
// release-signing certificate before this can pass live Digital Asset
// Links verification. Obtain it from the signed release APK with
// `apksigner verify --print-certs Inkwell-<version>.apk` (see
// `Android/AGENTS.md`'s release checklist, which references the current
// key's fingerprint by its first bytes, `1a020456…`, but not the full
// value). This is deliberately NOT the F-Droid repo index signing key
// fingerprint used in `FDROID_REPO_LINK` (src/lib/config.ts) — Digital
// Asset Links requires the APK's own signing certificate, not the repo's.
const ANDROID_RELEASE_CERT_SHA256_PLACEHOLDER =
  "REPLACE_WITH_RELEASE_SIGNING_SHA256_FINGERPRINT";

export function GET() {
  return json(
    [
      {
        relation: ["delegate_permission/common.handle_all_urls"],
        target: {
          namespace: "android_app",
          package_name: "uk.ewancroft.inkwell",
          sha256_cert_fingerprints: [ANDROID_RELEASE_CERT_SHA256_PLACEHOLDER],
        },
      },
    ],
    {
      headers: {
        "cache-control": "public, max-age=3600",
      },
    },
  );
}
