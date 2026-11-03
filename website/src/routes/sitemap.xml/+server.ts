// ── Sitemap ──────────────────────────────────────────────────────
// robots.txt advertises /sitemap.xml, so it has to exist. SvelteKit does
// not generate one automatically; the site is a short, fixed list of
// routes, so they come from the same SITE_ROUTES table the layout uses
// for canonical URLs rather than being listed again here.
//
// Deliberately absent: /client-metadata.json (an OAuth protocol artefact,
// not a page) and the static release artefacts under /altstore and
// /fdroid.

import { SITE_ROUTES, absoluteUrl } from "$lib/i18n";

export const prerender = true;

function escapeXml(value: string): string {
  return value
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}

export function GET() {
  const entries = SITE_ROUTES.map(({ path, priority }) =>
    [
      "  <url>",
      `    <loc>${escapeXml(absoluteUrl(path))}</loc>`,
      `    <priority>${priority}</priority>`,
      "  </url>",
    ].join("\n"),
  );

  return new Response(
    `<?xml version="1.0" encoding="UTF-8"?>\n` +
      `<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">\n` +
      `${entries.join("\n")}\n</urlset>\n`,
    {
      headers: {
        "content-type": "application/xml; charset=utf-8",
        "cache-control": "public, max-age=3600",
      },
    },
  );
}
