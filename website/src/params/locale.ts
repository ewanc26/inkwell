// ── `[[locale=locale]]` param matcher ────────────────────────────
// Matches only the URL segments that actually name a prefixed locale, so
// `/features` is never mistaken for a locale and `/fr/features` is never
// mistaken for a page. British English is the source locale and carries
// no segment, which is why it is absent from this matcher: `/privacy`
// matches the optional param as *empty*, not as "en-GB".

import { isLocaleSegment } from "$lib/i18n/locales";

export function match(param: string): boolean {
  return isLocaleSegment(param);
}
