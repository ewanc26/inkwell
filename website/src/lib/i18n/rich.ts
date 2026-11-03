// ── Inline markup inside catalogue strings ───────────────────────
// Marketing copy is full of sentences that wrap a link or a protocol
// identifier mid-clause. Splitting those into "before"/"after"
// fragments and gluing them back together in the template forces every
// sentence into a rigid word order, so instead a message may carry a
// tiny, fixed subset of inline markup, rendered here:
//
//   `code`            → <code>code</code>
//   **bold**          → <strong>bold</strong>
//   [label](linkKey)  → <a href="…">label</a>
//
// `linkKey` is a symbolic name from LINKS below, never a URL: a typo
// fails the build instead of producing a dead link.
//
// Everything outside those three forms is HTML-escaped. The catalogues
// are repository constants rather than user input, but escaping keeps
// the rule simple: a message is text, and only the three constructs
// above become markup. This deliberately mirrors the inline renderer in
// tools/legal/render.mjs so the two places that turn our prose into
// HTML behave the same way.

export type LinkTarget = {
  readonly href: string;
};

export const LINKS = {
  home: { href: "/" },
  features: { href: "/features" },
  security: { href: "/security" },
  about: { href: "/about" },
  privacy: { href: "/privacy" },
  terms: { href: "/terms" },
  download: { href: "/#download" },
  fdroidRepo: { href: "/fdroid/repo" },
  standardSite: { href: "https://standard.site" },
  atproto: { href: "https://atproto.com" },
  altstore: { href: "https://altstore.io" },
  github: { href: "https://github.com/ewanc26/inkwell" },
  githubContributors: {
    href: "https://github.com/ewanc26/inkwell/graphs/contributors",
  },
  kofi: { href: "https://ko-fi.com/ewancroft" },
  sponsors: { href: "https://github.com/sponsors/ewanc26" },
  agpl: {
    href: "https://www.gnu.org/licenses/agpl-3.0.en.html",
  },
  appStoreException: {
    href: "https://github.com/ewanc26/inkwell/blob/main/APP_STORE_EXCEPTION.md",
  },
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
 */
export function rich(template: string): string {
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
      const target = (LINKS as Record<string, LinkTarget | undefined>)[key];
      if (!target) {
        // A typo'd key would otherwise render a dead link; fail loudly
        // during `pnpm build` instead.
        throw new Error("Unknown link key in message: " + whole);
      }
      return hold(
        '<a href="' +
          escapeHtml(target.href) +
          '">' +
          escapeHtml(label) +
          "</a>",
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
