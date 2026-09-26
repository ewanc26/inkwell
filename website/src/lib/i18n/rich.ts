// ── Inline markup inside message catalogue strings ───────────────
// Marketing copy is full of sentences that wrap a link or a protocol
// identifier mid-clause. Splitting those into "before"/"after"
// fragments and gluing them back together in the template forces every
// translation into English word order, so instead a message may carry a
// tiny, fixed subset of inline markup, rendered here:
//
//   `code`            → <code>code</code>
//   **bold**          → <strong>bold</strong>
//   [label](linkKey)  → <a href="…">label</a>
//
// `linkKey` is a symbolic name from LINKS below, never a URL: a
// translator writes the visible label and nothing else, so no
// translation can rewrite a protocol identifier, an NSID, or a
// third-party address. Internal site links resolve through
// localizeHref, so a French paragraph links to /fr/privacy without the
// catalogue having to know that.
//
// Everything outside those three forms is HTML-escaped. The catalogues
// are repository constants rather than user input, but escaping keeps
// the rule simple: a message is text, and only the three constructs
// above become markup. This deliberately mirrors the inline renderer in
// tools/legal/render.mjs so the two places that turn our prose into
// HTML behave the same way.

import { localizeHref, type Locale } from "./locales";

export type LinkTarget = {
  readonly href: string;
  /** Site-internal links get the locale prefix; static/external ones don't. */
  readonly localise: boolean;
};

export const LINKS = {
  home: { href: "/", localise: true },
  features: { href: "/features", localise: true },
  security: { href: "/security", localise: true },
  about: { href: "/about", localise: true },
  privacy: { href: "/privacy", localise: true },
  terms: { href: "/terms", localise: true },
  download: { href: "/#download", localise: true },
  // Served straight out of static/, so it has no localised counterpart.
  fdroidRepo: { href: "/fdroid/repo", localise: false },
  standardSite: { href: "https://standard.site", localise: false },
  atproto: { href: "https://atproto.com", localise: false },
  altstore: { href: "https://altstore.io", localise: false },
  github: { href: "https://github.com/ewanc26/inkwell", localise: false },
  githubContributors: {
    href: "https://github.com/ewanc26/inkwell/graphs/contributors",
    localise: false,
  },
  kofi: { href: "https://ko-fi.com/ewancroft", localise: false },
  sponsors: { href: "https://github.com/sponsors/ewanc26", localise: false },
  agpl: {
    href: "https://www.gnu.org/licenses/agpl-3.0.en.html",
    localise: false,
  },
  appStoreException: {
    href: "https://github.com/ewanc26/inkwell/blob/main/APP_STORE_EXCEPTION.md",
    localise: false,
  },
} as const satisfies Record<string, LinkTarget>;

/**
 * The British English legal pages, addressed without a locale prefix on
 * purpose: a translated page's provenance notice has to point at the
 * authoritative source text, not at its own translation.
 */
export const SOURCE_LEGAL_LINKS = {
  privacy: { href: "/privacy", localise: false },
  terms: { href: "/terms", localise: false },
} as const satisfies Record<string, LinkTarget>;

export type LinkKey = keyof typeof LINKS;

// U+0000, built rather than written as a literal so this source file
// stays plain ASCII. It cannot occur in a catalogue string, which is
// what makes it a safe placeholder delimiter — a bare "space, digits,
// space" marker would collide with any ordinary number in the prose.
const MARK = String.fromCharCode(0);
const MARKED = new RegExp(MARK + "(\\d+)" + MARK, "g");

function escapeHtml(text: string): string {
  return text
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}

/**
 * Renders a catalogue string's inline markup to HTML for `{@html}`.
 *
 * Code spans and links are lifted out first so their contents are never
 * re-escaped or re-matched by the bold pass, then the remaining plain
 * text is escaped and the pieces are reassembled.
 *
 * `extraLinks` adds call-site link keys on top of LINKS, for the few
 * messages whose target depends on which page is rendering them (the
 * legal provenance notices, which link to their own source document).
 */
export function rich(
  template: string,
  locale: Locale,
  extraLinks: Record<string, LinkTarget> = {},
): string {
  const tokens: string[] = [];
  const hold = (html: string): string => {
    tokens.push(html);
    return MARK + String(tokens.length - 1) + MARK;
  };

  let working = template.replace(/`([^`]+)`/g, (_, code: string) =>
    hold("<code>" + escapeHtml(code) + "</code>"),
  );

  working = working.replace(
    /\[([^\]]+)\]\(([A-Za-z]+)\)/g,
    (whole: string, label: string, key: string) => {
      const target =
        extraLinks[key] ??
        (LINKS as Record<string, LinkTarget | undefined>)[key];
      if (!target) {
        // A typo'd key would otherwise render a dead link; fail loudly
        // during `pnpm build` instead.
        throw new Error("Unknown link key in message: " + whole);
      }
      const href = target.localise
        ? localizeHref(target.href, locale)
        : target.href;
      return hold(
        '<a href="' + escapeHtml(href) + '">' + escapeHtml(label) + "</a>",
      );
    },
  );

  working = escapeHtml(working);
  working = working.replace(/\*\*([^*]+)\*\*/g, "<strong>$1</strong>");

  return working.replace(
    MARKED,
    (_, index: string) => tokens[Number(index)] ?? "",
  );
}
