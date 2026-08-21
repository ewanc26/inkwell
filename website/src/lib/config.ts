// ── Site-wide configuration ──────────────────────────────────────
// Mirrors the pattern from ewancroft.uk — all route-level metadata
// and navigation structure lives in one place rather than scattered
// across components.

export const SITE = {
  title: "Inkwell",
  description:
    "A native reader and writer for the Standard.site publishing ecosystem on AT Protocol. Read, discover, and publish portable writing from your own PDS.",
  url: "https://inkwell.ewancroft.uk",
};

// ── Social cover image ───────────────────────────────────────────
// The card social clients render when a link to the site is shared.
// It is a committed PNG rather than anything generated per-request:
// scrapers fetch it without running our CSS, and most cache it hard.
// Regenerate with `pnpm og` after editing tools/og-cover/template.html.
//
// Dimensions are declared alongside the URL because Slack, Discord, and
// Facebook lay the card out from the meta tags before the image itself
// finishes downloading — without them the embed reflows or falls back
// to a small thumbnail.

export const OG_IMAGE = {
  path: "/og-cover.png",
  type: "image/png",
  width: 1200,
  height: 630,
  alt: "The Inkwell mark beside the Inkwell wordmark and the line: Read, discover, and publish Standard.site writing from your own PDS.",
} as const;

export const NAV_LINKS = [
  { label: "Home", url: "/" },
  { label: "Features", url: "/features" },
  { label: "Security", url: "/security" },
  { label: "About", url: "/about" },
  { label: "Get Inkwell", url: "/#download" },
  { label: "Source", url: "https://github.com/ewanc26/inkwell" },
] as const;

// ── Install sources ──────────────────────────────────────────────
// AltStore and F-Droid are self-hosted (static/altstore, static/fdroid) —
// there is no App Store or Play Store listing. Keep these in sync with
// static/altstore/source.json and static/fdroid/repo/index.html.

export const ALTSTORE_SOURCE_LINK =
  "altstore://source?url=https%3A%2F%2Finkwell.ewancroft.uk%2Faltstore%2Fsource.json";

export const FDROID_REPO_LINK =
  "https://fdroid.link/#https://inkwell.ewancroft.uk/fdroid/repo?fingerprint=6369CC624D896E379DF35A1AB0C8C7372639C55299750576A6D1048C0E26A2EA";
