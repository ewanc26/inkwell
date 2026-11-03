import type { Handle } from "@sveltejs/kit";

const HTML_LANG = "en-GB";

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
  const response = await resolve(event);

  for (const [name, value] of Object.entries(securityHeaders)) {
    response.headers.set(name, value);
  }

  // Only documents get Content-Language. /client-metadata.json is a
  // protocol artefact consumed by PDS servers: its response must keep
  // exactly the shape and headers it has always had.
  if (response.headers.get("content-type")?.startsWith("text/html")) {
    response.headers.set("Content-Language", HTML_LANG);
  }

  return response;
};
