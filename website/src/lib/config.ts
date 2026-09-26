// ── Site-wide configuration ──────────────────────────────────────
// Mirrors the pattern from ewancroft.uk — all route-level metadata
// and navigation structure lives in one place rather than scattered
// across components.

// Per-route titles and descriptions moved into the message catalogues
// (src/lib/i18n/messages/) when the site became multilingual, because they
// are visible copy. What stays here is what is *not* language-dependent:
// the product name and the canonical origin.
export const SITE = {
  title: "Inkwell",
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

// `alt` is not in the message catalogues on purpose: it has to describe
// what tools/og-cover/template.html actually renders, and that artwork is
// a single English-language PNG shared by every locale. Localising the
// alt text would make it describe words the image does not contain.
export const OG_IMAGE = {
  path: "/og-cover.png",
  type: "image/png",
  width: 1200,
  height: 630,
  alt: "The Inkwell mark beside the Inkwell wordmark and the line: Read, discover, and publish Standard.site writing from your own PDS.",
} as const;

// ── Navigation structure ─────────────────────────────────────────
// Structure (order, targets, which links leave the site) lives here;
// the labels live in the message catalogues under `nav.links` and
// `footer.links`, keyed by `key`. Internal `url`s are written in
// source-locale form and run through `localizeHref` at render time.

export const NAV_LINKS = [
  { key: "home", url: "/" },
  { key: "features", url: "/features" },
  { key: "security", url: "/security" },
  { key: "about", url: "/about" },
  { key: "download", url: "/#download" },
  { key: "source", url: "https://github.com/ewanc26/inkwell" },
] as const;

export const FOOTER_LINKS = [
  { key: "privacy", url: "/privacy", external: false },
  { key: "terms", url: "/terms", external: false },
  { key: "github", url: "https://github.com/ewanc26/inkwell", external: true },
  { key: "kofi", url: "https://ko-fi.com/ewancroft", external: true },
  {
    key: "sponsors",
    url: "https://github.com/sponsors/ewanc26",
    external: true,
  },
] as const;

// ── Install sources ──────────────────────────────────────────────
// AltStore and F-Droid are the live, self-hosted install routes
// (static/altstore, static/fdroid). Keep these in sync with
// static/altstore/source.json and static/fdroid/repo/index.html.

export const ALTSTORE_SOURCE_LINK =
  "altstore://source?url=https%3A%2F%2Finkwell.ewancroft.uk%2Faltstore%2Fsource.json";

export const FDROID_REPO_LINK =
  "https://fdroid.link/#https://inkwell.ewancroft.uk/fdroid/repo?fingerprint=6369CC624D896E379DF35A1AB0C8C7372639C55299750576A6D1048C0E26A2EA";

// PLACEHOLDERS: the App Store and Google Play listings do not exist yet.
// Replace these values with the real listing URLs when the paid £5 store
// builds launch; until then, all user-facing copy must mark them as planned.
export const APP_STORE_PLACEHOLDER_LINK =
  "https://apps.apple.com/app/inkwell/id0000000000";

export const PLAY_STORE_PLACEHOLDER_LINK =
  "https://play.google.com/store/apps/details?id=uk.ewancroft.inkwell";
