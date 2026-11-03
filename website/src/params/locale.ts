// ── `[[locale=locale]]` param matcher ────────────────────────────
// Matches only URL segments that name a registered prefixed locale. The
// site currently registers British English only, and the source locale
// carries no segment; the optional param therefore always matches empty.
// `/features` is never mistaken for a locale, and `/fr` is an ordinary
// 404 rather than an unprefixed page.

import { isLocaleSegment } from "$lib/i18n/locales";

export function match(param: string): boolean {
  return isLocaleSegment(param);
}
