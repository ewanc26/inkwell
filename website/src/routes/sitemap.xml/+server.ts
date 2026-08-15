// ── Sitemap ──────────────────────────────────────────────────────
// robots.txt advertises /sitemap.xml, so it has to exist. SvelteKit
// does not generate one automatically; the site is three static
// routes, so they are listed explicitly here rather than crawled.

import { SITE } from "$lib/config";

const ROUTES = [
  { path: "/", priority: "1.0" },
  { path: "/privacy", priority: "0.5" },
  { path: "/terms", priority: "0.5" },
];

export const prerender = true;

export function GET() {
  const urls = ROUTES.map(
    ({ path, priority }) =>
      `  <url>\n    <loc>${new URL(path, SITE.url).href}</loc>\n    <priority>${priority}</priority>\n  </url>`,
  ).join("\n");

  return new Response(
    `<?xml version="1.0" encoding="UTF-8"?>\n<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">\n${urls}\n</urlset>\n`,
    {
      headers: {
        "content-type": "application/xml; charset=utf-8",
        "cache-control": "public, max-age=3600",
      },
    },
  );
}
