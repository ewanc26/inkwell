// ── Site route table ─────────────────────────────────────────────
// The site is English-only and a short, fixed list of pages. Keeping the
// page list in one place means the sitemap and the layout's canonical URL
// helper cannot drift apart.
//
// Routes deliberately absent:
//   /client-metadata.json  OAuth client identity — a protocol artefact
//                          whose URL is the client_id.
//   /sitemap.xml           one document describing the pages.
//   /altstore, /fdroid, …  static release artefacts served from static/.

import { SITE } from "$lib/config";

export const SITE_ROUTES = [
  { path: "/", priority: "1.0" },
  { path: "/features", priority: "0.8" },
  { path: "/security", priority: "0.8" },
  { path: "/about", priority: "0.6" },
  { path: "/privacy", priority: "0.5" },
  { path: "/terms", priority: "0.5" },
] as const;

/** Absolute URL for a site path, built from the configured origin. */
export function absoluteUrl(path: string): string {
  return new URL(path, SITE.url).href;
}
