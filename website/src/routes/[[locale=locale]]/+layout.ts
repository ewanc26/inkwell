// ── Locale group layout data ─────────────────────────────────────
// Every localised page lives under this optional route segment, so this
// is the one place that turns the matched URL segment into a locale for
// the pages beneath it. British English is served unprefixed, so the
// param is absent for it and `localeFromSegment` resolves the default.
//
// Universal (not server) load: it is pure arithmetic on the matched param
// with nothing to fetch, so it runs during SSR and again on client-side
// navigation without a round trip.

import type { LayoutLoad } from "./$types";
import {
  DEFAULT_LOCALE,
  LOCALES,
  PREFIXED_LOCALES,
  type Locale,
} from "$lib/i18n";

function localeFromSegment(segment: string | undefined): Locale {
  if (segment === undefined) return DEFAULT_LOCALE;
  return (
    PREFIXED_LOCALES.find((code) => LOCALES[code].segment === segment) ??
    DEFAULT_LOCALE
  );
}

export const load: LayoutLoad = ({ params }) => {
  return { locale: localeFromSegment(params.locale) };
};
