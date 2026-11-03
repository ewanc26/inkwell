// ── English copy layer ───────────────────────────────────────────
// One import site for the English catalogue, the fixed route table, and
// the two string helpers (`fmt` for placeholders, `rich` for the inline
// markup subset). Everything here is pure and synchronous.

export { messages, routeMeta, type Messages, type RouteMeta } from "./messages";
export { SITE_ROUTES, absoluteUrl } from "./routes";
export { rich, LINKS, type LinkKey, type LinkTarget } from "./rich";

/**
 * Fills `{name}` placeholders in a catalogue string.
 *
 * Substitution happens before `rich()` so a value can never introduce
 * markup: whatever it contains is escaped by the same pass that escapes
 * the surrounding prose. Unknown placeholders are left in place rather
 * than replaced with "undefined", so a typo is visible instead of silent.
 */
export function fmt(
  template: string,
  values: Record<string, string | number>,
): string {
  return template.replace(/\{(\w+)\}/g, (whole, key: string) =>
    Object.prototype.hasOwnProperty.call(values, key)
      ? String(values[key])
      : whole,
  );
}

/**
 * Splits a catalogue string on a single placeholder, for the handful of
 * strings whose placeholder stands in for an element rather than text —
 * `<time>` in the legal version line, the accent `<span>` in the hero
 * heading. Keeps control of word order on both sides of it without the
 * catalogue having to carry HTML.
 *
 * Returns `[before, after]`; if the placeholder is missing the whole
 * string comes back as `before`, so the copy still renders.
 */
export function splitAt(
  template: string,
  placeholder: string,
): [string, string] {
  const token = `{${placeholder}}`;
  const at = template.indexOf(token);
  if (at === -1) return [template, ""];
  return [template.slice(0, at), template.slice(at + token.length)];
}
