// ── Locale registry ──────────────────────────────────────────────
// British English is the source locale and is served unprefixed, so
// every URL that existed before localisation still resolves at the
// same path and stays the canonical one. Additional locales live
// under a single path segment (`/fr/...`).
//
// There is deliberately no Accept-Language redirect: a negotiated
// redirect makes one URL answer with several languages, which is the
// indexing hazard this design set out to avoid. Locale is a function
// of the URL and nothing else, so every page stays cacheable and each
// language has exactly one canonical address.

export const DEFAULT_LOCALE = "en-GB";

export type LocaleDefinition = {
  /** BCP-47 tag; also the key used throughout the message catalogues. */
  readonly code: string;
  /** Value for `<html lang>` and `hreflang`. */
  readonly htmlLang: string;
  /** OpenGraph wants the underscored `language_TERRITORY` form. */
  readonly ogLocale: string;
  /** URL segment, or `null` for the unprefixed default locale. */
  readonly segment: string | null;
  /** Writing direction, for `<html dir>`. */
  readonly dir: "ltr" | "rtl";
  /** Endonym, shown in the language switcher in its own language. */
  readonly label: string;
};

export const LOCALES = {
  "en-GB": {
    code: "en-GB",
    htmlLang: "en-GB",
    ogLocale: "en_GB",
    segment: null,
    dir: "ltr",
    label: "English (UK)",
  },
  fr: {
    code: "fr",
    htmlLang: "fr",
    ogLocale: "fr_FR",
    segment: "fr",
    dir: "ltr",
    label: "Français",
  },
} as const satisfies Record<string, LocaleDefinition>;

export type Locale = keyof typeof LOCALES;

export const LOCALE_CODES = Object.keys(LOCALES) as Locale[];

/** Locales that carry a URL segment — everything except the default. */
export const PREFIXED_LOCALES = LOCALE_CODES.filter(
  (code) => LOCALES[code].segment !== null,
);

export function isLocale(value: string): value is Locale {
  return Object.prototype.hasOwnProperty.call(LOCALES, value);
}

/** True for a path segment that names a non-default locale (`fr`). */
export function isLocaleSegment(segment: string): boolean {
  return PREFIXED_LOCALES.some((code) => LOCALES[code].segment === segment);
}

function localeForSegment(segment: string): Locale | null {
  return (
    PREFIXED_LOCALES.find((code) => LOCALES[code].segment === segment) ?? null
  );
}

/**
 * Splits a request path into its locale and the default-locale path it
 * corresponds to. `/fr/privacy` → `{ locale: "fr", path: "/privacy" }`;
 * `/privacy` → `{ locale: "en-GB", path: "/privacy" }`.
 *
 * The returned path is the key every other helper works in, so route
 * tables, canonical URLs, and `hreflang` alternates are written once in
 * the source locale rather than once per language.
 */
export function resolveLocale(pathname: string): {
  locale: Locale;
  path: string;
} {
  const [, first = "", ...rest] = pathname.split("/");
  const locale = localeForSegment(first);
  if (!locale) return { locale: DEFAULT_LOCALE, path: pathname };
  const remainder = rest.join("/");
  return { locale, path: remainder === "" ? "/" : `/${remainder}` };
}

/**
 * Rewrites a source-locale href into `locale`. External URLs, mailto
 * links, and bare fragments are returned untouched — protocol
 * identifiers and third-party addresses are never localised.
 */
export function localizeHref(href: string, locale: Locale): string {
  if (!href.startsWith("/")) return href;
  const segment = LOCALES[locale].segment;
  if (segment === null) return href;
  const prefix = `/${segment}`;
  if (href === "/") return prefix;
  // "/#download" has to become "/fr#download", not "/fr/#download".
  if (href.startsWith("/#")) return `${prefix}${href.slice(1)}`;
  return `${prefix}${href}`;
}
