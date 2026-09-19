import type { Handle } from "@sveltejs/kit";

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

  return response;
};
