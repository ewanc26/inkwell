// ── Localised route table ────────────────────────────────────────
// Every page route is written once, in its source-locale (`en-GB`)
// form, and every other locale's address is derived from it. That is
// what keeps canonical URLs and `hreflang` alternates from drifting
// apart: there is exactly one list of pages, and both are generated
// from it.
//
// Routes that are *not* in this table are deliberately not localised:
//   /client-metadata.json  OAuth client identity — a protocol artefact
//                          whose URL is the client_id. Never translated,
//                          never prefixed, never given alternates.
//   /sitemap.xml           one document describing every locale.
//   /altstore, /fdroid, …  static release artefacts served from static/.

import { SITE } from "$lib/config";
import {
  DEFAULT_LOCALE,
  LOCALES,
  LOCALE_CODES,
  localizeHref,
  type Locale,
} from "./locales";

/**
 * Source-locale paths that exist in every locale, with the relative
 * priority the sitemap advertises for them.
 */
export const LOCALISED_ROUTES = [
  { path: "/", priority: "1.0" },
  { path: "/features", priority: "0.8" },
  { path: "/security", priority: "0.8" },
  { path: "/about", priority: "0.6" },
  { path: "/privacy", priority: "0.5" },
  { path: "/terms", priority: "0.5" },
] as const;

export type LocalisedPath = (typeof LOCALISED_ROUTES)[number]["path"];

const LOCALISED_PATHS: readonly string[] = LOCALISED_ROUTES.map((r) => r.path);

export function isLocalisedPath(path: string): path is LocalisedPath {
  return LOCALISED_PATHS.includes(path);
}

/** Absolute URL for a source-locale path in a given locale. */
export function absoluteUrl(path: string, locale: Locale): string {
  return new URL(localizeHref(path, locale), SITE.url).href;
}

export type Alternate = {
  readonly locale: Locale;
  /** `hreflang` value — also used for `lang` on a language-switcher link. */
  readonly hreflang: string;
  readonly href: string;
};

/**
 * Every locale's address for one source-locale path, in registry order.
 * Used both for `<link rel="alternate">` and for the footer language
 * switcher, so a locale can never appear in one and not the other.
 */
export function alternatesFor(path: string): Alternate[] {
  return LOCALE_CODES.map((locale) => ({
    locale,
    hreflang: LOCALES[locale].htmlLang,
    href: absoluteUrl(path, locale),
  }));
}

/**
 * The `x-default` target: the address a crawler should show when it has
 * no better match for the user's language. British English is the source
 * locale, so that is the unprefixed URL.
 */
export function xDefaultFor(path: string): string {
  return absoluteUrl(path, DEFAULT_LOCALE);
}
