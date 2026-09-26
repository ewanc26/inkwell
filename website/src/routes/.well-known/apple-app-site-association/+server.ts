// ── iOS Associated Domains verification ──────────────────────────
// Apple fetches this exact path (no extension, over HTTPS, with no
// redirect) to decide whether inkwell.ewancroft.uk may hand off
// `/open` links to the Inkwell app as a universal link — see
// `iOS/Inkwell.entitlements` / `iOS/Inkwell-Debug.entitlements`
// (`applinks:inkwell.ewancroft.uk`) and `iOS/Inkwell/App/InkwellApp.swift`'s
// `onContinueUserActivity(NSUserActivityTypeBrowsingWeb)` handler.
//
// `paths` is scoped to the hand-off endpoint only (`/open`, `/open/*`) —
// Inkwell does not claim the whole domain, matching issue #97's directive
// not to intercept ordinary website navigation.
//
// Served dynamically (not prerendered) for the same reason
// client-metadata.json/+server.ts is: this path has no file extension,
// and adapter-vercel's static/prerendered output serves extensionless
// files by content-sniffing/defaulting the MIME type rather than
// preserving the `json()` helper's `application/json` header — Apple's
// verifier expects exactly that content type. A real function response
// always carries the header this handler actually sets.

import { json } from "@sveltejs/kit";

export const prerender = false;

// PLACEHOLDER — replace with the real 10-character Apple Developer Team ID
// before this can pass Apple's live AASA verification. Find it at
// https://developer.apple.com/account (Membership details) or via
// `xcodebuild -showBuildSettings` (`DEVELOPMENT_TEAM`). The app ID format
// is "<TEAM_ID>.<bundle identifier>".
const APPLE_TEAM_ID_PLACEHOLDER = "REPLACE_WITH_APPLE_TEAM_ID";
const IOS_BUNDLE_ID = "uk.ewancroft.Inkwell";

export function GET() {
  return json(
    {
      applinks: {
        apps: [],
        details: [
          {
            appID: `${APPLE_TEAM_ID_PLACEHOLDER}.${IOS_BUNDLE_ID}`,
            paths: ["/open", "/open/*"],
          },
        ],
      },
    },
    {
      headers: {
        "cache-control": "public, max-age=3600",
      },
    },
  );
}
