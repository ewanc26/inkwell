// ── English message catalogue ────────────────────────────────────
// `Messages` is inferred from the British English catalogue, so the
// source copy and every consumer of it share one contract. The site is
// English-only; adding a translation would mean adding a separate
// catalogue and an explicit lookup, not quietly widening this one.

import { enGB } from "./en-GB";

export type Messages = typeof enGB;
export const messages = enGB;

export type RouteMeta = { title: string; description: string };

/**
 * Title/description for a site path. Unknown paths (a 404, or a static
 * artefact rendered through the shell) fall back to the home page's
 * metadata rather than rendering an empty `<title>`.
 */
export function routeMeta(path: string): RouteMeta {
  const table = messages.meta as Record<string, RouteMeta | undefined>;
  return table[path] ?? messages.meta["/"];
}

export { enGB };
