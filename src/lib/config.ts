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

export const NAV_LINKS = [
  { label: "Home", url: "/" },
  { label: "Get Inkwell", url: "/#download" },
  { label: "Privacy", url: "/privacy" },
  { label: "Terms", url: "/terms" },
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
