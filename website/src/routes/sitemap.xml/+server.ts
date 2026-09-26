// ── Sitemap ──────────────────────────────────────────────────────
// robots.txt advertises /sitemap.xml, so it has to exist. SvelteKit does
// not generate one automatically; the site is a short, fixed list of
// routes, so they come from the same LOCALISED_ROUTES table the layout
// builds canonical URLs and hreflang alternates from rather than being
// listed again here.
//
// Every locale of every page gets its own <url> entry, and each entry
// carries xhtml:link alternates for all the others plus x-default. That
// is the sitemap half of the same contract as the <link rel="alternate">
// tags in the shell: one canonical address per language, cross-referenced
// both ways, so no page looks like duplicate content.
//
// Deliberately absent: /client-metadata.json (an OAuth protocol artefact,
// not a page) and the static release artefacts under /altstore and
// /fdroid.

import {
  LOCALISED_ROUTES,
  absoluteUrl,
  alternatesFor,
  xDefaultFor,
  LOCALE_CODES,
} from "$lib/i18n";

export const prerender = true;

function escapeXml(value: string): string {
  return value
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}

export function GET() {
  const entries: string[] = [];

  for (const { path, priority } of LOCALISED_ROUTES) {
    const alternates = [
      ...alternatesFor(path).map(
        ({ hreflang, href }) =>
          `    <xhtml:link rel="alternate" hreflang="${hreflang}" href="${escapeXml(href)}" />`,
      ),
      `    <xhtml:link rel="alternate" hreflang="x-default" href="${escapeXml(xDefaultFor(path))}" />`,
    ].join("\n");

    for (const locale of LOCALE_CODES) {
      entries.push(
        [
          "  <url>",
          `    <loc>${escapeXml(absoluteUrl(path, locale))}</loc>`,
          alternates,
          `    <priority>${priority}</priority>`,
          "  </url>",
        ].join("\n"),
      );
    }
  }

  return new Response(
    `<?xml version="1.0" encoding="UTF-8"?>\n` +
      `<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9" xmlns:xhtml="http://www.w3.org/1999/xhtml">\n` +
      `${entries.join("\n")}\n</urlset>\n`,
    {
      headers: {
        "content-type": "application/xml; charset=utf-8",
        "cache-control": "public, max-age=3600",
      },
    },
  );
}
