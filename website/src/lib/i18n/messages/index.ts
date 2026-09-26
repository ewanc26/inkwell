// ── Message catalogues ───────────────────────────────────────────
// `Messages` is inferred from the British English catalogue rather than
// declared separately, so the source copy and the contract every
// translation is checked against cannot drift apart. `fr` is annotated
// `Messages` (not `satisfies`) so a missing key is an error at the point
// of declaration, which is where the fix belongs.
//
// The catalogues are written without `as const`, so string properties
// widen to `string` and lists to arrays: a translation is free to write
// different words, but not a different shape.

import { enGB } from "./en-GB";
import { fr } from "./fr";
import { DEFAULT_LOCALE, type Locale } from "../locales";

export type Messages = typeof enGB;

const CATALOGUES: Record<Locale, Messages> = {
  "en-GB": enGB,
  fr,
};

/** The catalogue for `locale`, falling back to the source locale. */
export function messagesFor(locale: Locale): Messages {
  return CATALOGUES[locale] ?? CATALOGUES[DEFAULT_LOCALE];
}

export type RouteMeta = { title: string; description: string };

/**
 * Title/description for a source-locale path. Unknown paths (a 404, or a
 * static artefact rendered through the shell) fall back to the home
 * page's metadata rather than rendering an empty `<title>`.
 */
export function routeMeta(messages: Messages, path: string): RouteMeta {
  const table = messages.meta as Record<string, RouteMeta | undefined>;
  return table[path] ?? messages.meta["/"];
}

export { enGB, fr };
