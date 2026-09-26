import type { Handle } from "@sveltejs/kit";
import { LOCALES, resolveLocale } from "$lib/i18n/locales";

const securityHeaders: Record<string, string> = {
  "Content-Security-Policy":
    "default-src 'self'; base-uri 'none'; object-src 'none'; frame-ancestors 'none'; form-action 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' data: https:; font-src 'self' data:; connect-src 'self' https:; upgrade-insecure-requests",
  "Referrer-Policy": "strict-origin-when-cross-origin",
  "Permissions-Policy": "camera=(), microphone=(), geolocation=()",
  "X-Content-Type-Options": "nosniff",
  "X-Frame-Options": "DENY",
  "Strict-Transport-Security": "max-age=31536000; includeSubDomains",
};

export const handle: Handle = async ({ event, resolve }) => {
  // Locale is a pure function of the path — no Accept-Language sniffing,
  // so one URL only ever answers in one language and stays cacheable.
  const { locale } = resolveLocale(event.url.pathname);
  const definition = LOCALES[locale];
  event.locals.locale = locale;

  const response = await resolve(event, {
    // `%lang%`/`%dir%` are placeholders in src/app.html. They only exist
    // in the page shell, so endpoints (notably /client-metadata.json) are
    // untouched by this transform.
    //
    // `replaceAll`, not `replace`: a chunk carrying the token twice would
    // otherwise keep the second one, and a `%lang%` left in the markup is
    // exactly the bug this attribute exists to prevent.
    transformPageChunk: ({ html }) =>
      html
        .replaceAll("%lang%", definition.htmlLang)
        .replaceAll("%dir%", definition.dir),
  });

  for (const [name, value] of Object.entries(securityHeaders)) {
    response.headers.set(name, value);
  }

  // Only documents get Content-Language. /client-metadata.json is a
  // protocol artefact consumed by PDS servers: its response must keep
  // exactly the shape and headers it has always had.
  if (response.headers.get("content-type")?.startsWith("text/html")) {
    response.headers.set("Content-Language", definition.htmlLang);
  }

  return response;
};
