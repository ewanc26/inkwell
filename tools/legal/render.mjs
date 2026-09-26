#!/usr/bin/env node
// Regenerates the shared KMP legal-doc source (consumed by both iOS, via
// SharedKMP.swift, and Android directly) and the website copy from the
// canonical sources in legal/.
//
// legal/meta.json names the source-locale documents (legal/privacy.md,
// legal/terms.md) and, per translated locale, the translated source plus
// the provenance that makes the translation trustworthy: which version of
// the source it was translated from, when it was last reviewed against it,
// and a digest of the source text as it stood at that review.
//
// That digest is the whole point. A translation whose recorded digest no
// longer matches the source is *stale*: its wording may carry an older
// legal meaning. A stale translation is still rendered -- taking a legal
// page offline is worse -- but it is rendered without the current
// effective date and with a notice pointing at the authoritative source,
// and `--check` fails so CI cannot go green on it.
//
// The apps remain source-locale only: LegalDocuments.kt is generated from
// legal/privacy.md and legal/terms.md exactly as before, so translations
// never force an xcframework rebuild.
//
// Usage: node tools/legal/render.mjs [--check] [--bless <locale>]
//   --check           don't write files; exit 1 if regenerating would change
//                     anything, or if any translation is stale (used in CI to
//                     catch hand-edited copies that drifted from the source, a
//                     source edit that was never regenerated, or a translation
//                     left behind by one).
//   --bless <locale>  record the current source version and digests against
//                     <locale>, marking its translation as reviewed today.
//                     This is the deliberate human act of saying "I have
//                     checked this translation against the current source" --
//                     nothing else ever writes those fields, because a tool
//                     that refreshed them automatically would silently
//                     certify a translation nobody read.

import {
  readFileSync,
  writeFileSync,
  existsSync,
  mkdirSync,
} from "node:fs";
import { createHash } from "node:crypto";
import { fileURLToPath } from "node:url";
import path from "node:path";

const here = path.dirname(fileURLToPath(import.meta.url));
const root = path.resolve(here, "..", "..");
const legalDir = path.join(root, "legal");
const metaPath = path.join(legalDir, "meta.json");

const meta = JSON.parse(readFileSync(metaPath, "utf8"));
const SOURCE_LOCALE = meta.sourceLocale;
const DOC_KEYS = Object.keys(meta.documents);

// The iOS/Android version strings quoted in the legal text are read from the
// real build files rather than hand-typed in meta.json, so they can't drift
// from what's actually shipped -- the same failure mode that let the 2.1.0
// AltStore manifest go unadvertised.
function readIosVersion() {
  const pbxprojPath = path.join(root, "iOS", "Inkwell.xcodeproj", "project.pbxproj");
  const lines = readFileSync(pbxprojPath, "utf8").split("\n");
  for (let i = 0; i < lines.length; i++) {
    if (!lines[i].includes("PRODUCT_BUNDLE_IDENTIFIER = uk.ewancroft.Inkwell;")) continue;
    const window = lines.slice(Math.max(0, i - 30), i).join("\n");
    const marketing = window.match(/MARKETING_VERSION = ([^;]+);/);
    const build = window.match(/CURRENT_PROJECT_VERSION = ([^;]+);/);
    if (marketing && build) {
      return `version ${marketing[1].trim()}, build ${build[1].trim()}`;
    }
  }
  throw new Error(`Could not find the Inkwell app target's version in ${path.relative(root, pbxprojPath)}`);
}

function readAndroidVersion() {
  const gradlePath = path.join(root, "Android", "app", "build.gradle.kts");
  const gradle = readFileSync(gradlePath, "utf8");
  const match = gradle.match(/versionName\s*=\s*"([^"]+)"/);
  if (!match) {
    throw new Error(`Could not find versionName in ${path.relative(root, gradlePath)}`);
  }
  return `version ${match[1]}`;
}

const iosVersion = readIosVersion();
const androidVersion = readAndroidVersion();

const KOTLIN_FILE = path.join(
  root,
  "shared",
  "src",
  "commonMain",
  "kotlin",
  "uk",
  "ewancroft",
  "inkwell",
  "shared",
  "legal",
  "LegalDocuments.kt",
);

const WEBSITE_FILE = path.join(
  root,
  "website",
  "src",
  "lib",
  "legal",
  "documents.ts",
);

// --- sources --------------------------------------------------------------

function sourcePath(file) {
  const resolved = path.join(legalDir, file);
  if (!existsSync(resolved)) {
    throw new Error(`Legal source missing: ${path.relative(root, resolved)}`);
  }
  return resolved;
}

/**
 * Digest of a legal source file as it sits on disk -- before the app version
 * placeholders are substituted, so shipping a new app build does not
 * invalidate every translation.
 */
function digestOf(file) {
  return createHash("sha256").update(readFileSync(sourcePath(file))).digest("hex");
}

function loadSourceText(file) {
  const text = readFileSync(sourcePath(file), "utf8");
  return text.replaceAll("{{IOS_VERSION}}", iosVersion).replaceAll("{{ANDROID_VERSION}}", androidVersion).trim();
}

function loadBlocks(file) {
  return loadSourceText(file)
    .split(/\n\s*\n/)
    .map((block) => block.trim());
}

function versionLine() {
  return `**Version ${meta.version} — Effective Date: ${meta.effectiveDate}**`;
}

/**
 * The effective date written the way the locale writes dates. The source
 * locale keeps the hand-written string from meta.json verbatim, because the
 * apps quote it through LegalDocuments.EFFECTIVE_DATE.
 */
function effectiveDateFor(locale) {
  if (locale === SOURCE_LOCALE) return meta.effectiveDate;
  return new Intl.DateTimeFormat(locale, {
    day: "numeric",
    month: "long",
    year: "numeric",
    timeZone: "UTC",
  }).format(new Date(`${meta.effectiveDateISO}T00:00:00Z`));
}

// --- translation provenance ----------------------------------------------

/**
 * Resolves one locale's view of one document: which file to render, what
 * version its wording actually means, and whether it has fallen behind the
 * source since it was last reviewed.
 */
function resolveDocument(key, locale) {
  if (locale === SOURCE_LOCALE) {
    return {
      locale,
      file: meta.documents[key],
      version: meta.version,
      effectiveDate: meta.effectiveDate,
      effectiveDateISO: meta.effectiveDateISO,
      translation: null,
    };
  }

  const entry = meta.translations[locale];
  const document = entry?.documents?.[key];
  if (!document) {
    throw new Error(
      `legal/meta.json has no ${key} translation for ${locale}. Add one, or remove the locale.`,
    );
  }

  const stale =
    entry.sourceVersion !== meta.version ||
    document.sourceDigest !== digestOf(meta.documents[key]);

  return {
    locale,
    file: document.file,
    // A stale translation means the version it was translated from, not the
    // version currently in force, and carries no effective date at all.
    version: entry.sourceVersion,
    effectiveDate: stale ? null : effectiveDateFor(locale),
    effectiveDateISO: stale ? null : meta.effectiveDateISO,
    translation: {
      sourceLocale: SOURCE_LOCALE,
      sourceVersion: entry.sourceVersion,
      reviewedOn: entry.reviewedOn,
      stale,
    },
  };
}

const LOCALES = [SOURCE_LOCALE, ...Object.keys(meta.translations ?? {})];

const RESOLVED = Object.fromEntries(
  DOC_KEYS.map((key) => [
    key,
    Object.fromEntries(LOCALES.map((locale) => [locale, resolveDocument(key, locale)])),
  ]),
);

const staleDocuments = DOC_KEYS.flatMap((key) =>
  LOCALES.filter((locale) => RESOLVED[key][locale].translation?.stale).map(
    (locale) => `${key} (${locale})`,
  ),
);

// --- shared KMP: LegalDocuments.kt --------------------------------------
//
// Source locale only. The apps render British English in-app; a translated
// in-app surface would be a separate piece of work on both clients, and
// generating unused locales here would churn LegalDocuments.kt (and with it
// the xcframework) for no shipped benefit.
//
// The compiled markdown is exactly the resolved source with the version
// line prepended -- Android's MarkdownRendererView and iOS's
// MarkdownRendererView.swift (both backed by this same shared parser, via
// MarkdownParserEngine on the iOS side) already understand this syntax
// (## headings, - lists, **bold**, `code`, [text](url) links, with real
// tappable links on both platforms), so there's no per-platform text
// transform to write.

function kotlinMultilineString(text) {
  // Kotlin raw strings have no escape sequence for a literal `"""`, so
  // fail loudly rather than emit a string that silently truncates at the
  // first occurrence. `$` starts a template expression -- escape it so a
  // future source edit (e.g. quoting a price) can't produce a string that
  // fails to compile or evaluates something other than what was written.
  if (text.includes('"""')) {
    throw new Error('Legal source text contains a literal """, which Kotlin raw strings cannot represent.');
  }
  return text
    .replaceAll("$", '${"$"}')
    .split("\n")
    .map((line) => (line === "" ? "        |" : `        |${line}`))
    .join("\n");
}

function renderKotlinFile() {
  const header = `// GENERATED by tools/legal/render.mjs from legal/privacy.md and legal/terms.md.
// Do not hand-edit -- edit the source and run \`node tools/legal/render.mjs\`.
package uk.ewancroft.inkwell.shared.legal

/**
 * The Privacy Policy and Terms of Service, as markdown, rendered natively
 * in-app on both iOS and Android by the same shared markdown renderer used
 * for reading Standard.site content -- see MarkdownRendererView on each
 * platform. iOS consumes this via SharedKMP.swift; Android consumes it
 * directly. The website renders its own HTML copy from the same source,
 * generated separately since it isn't part of this KMP module.
 */
object LegalDocuments {
    const val VERSION = "${meta.version}"
    const val EFFECTIVE_DATE = "${meta.effectiveDate}"
    const val EFFECTIVE_DATE_ISO = "${meta.effectiveDateISO}"

    val privacyMarkdown: String = """
${kotlinMultilineString(`${versionLine()}\n\n${loadSourceText(meta.documents.privacy)}`)}
        """.trimMargin()

    val termsMarkdown: String = """
${kotlinMultilineString(`${versionLine()}\n\n${loadSourceText(meta.documents.terms)}`)}
        """.trimMargin()
}
`;
  return header;
}

// --- website: HTML -------------------------------------------------------

function escapeHtml(text) {
  return text.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
}

function inlineHtml(text) {
  // Split out code spans and links first (their contents must not be
  // re-escaped or re-matched by the bold/italic passes), escape the plain
  // text runs, then reassemble. The placeholder is wrapped in U+0000
  // bytes, which cannot appear in the source text -- a plain
  // "space, digits, space" placeholder collides with any ordinary number
  // already in the prose (ages, years, section/statute references) and
  // silently renders "undefined" in their place.
  const tokens = [];
  const placeholder = (html) => {
    tokens.push(html);
    return `\u0000${tokens.length - 1}\u0000`;
  };

  let working = text.replace(/`([^`]+)`/g, (_, code) => placeholder(`<code>${escapeHtml(code)}</code>`));
  working = working.replace(/\[([^\]]+)\]\(([^)]+)\)/g, (_, label, url) =>
    placeholder(`<a href="${escapeHtml(url)}">${escapeHtml(label)}</a>`),
  );

  working = escapeHtml(working);
  working = working.replace(/\*\*([^*]+)\*\*/g, "<strong>$1</strong>");
  working = working.replace(/\*([^*]+)\*/g, "<em>$1</em>");

  return working.replace(/\u0000(\d+)\u0000/g, (_, i) => tokens[Number(i)]);
}

function renderHtml(blocks) {
  const lines = [];
  for (const block of blocks) {
    if (block.startsWith("## ")) {
      lines.push(`<h2>${inlineHtml(block.slice(3).trim())}</h2>`);
    } else if (block.split("\n").every((line) => line.startsWith("- "))) {
      lines.push("<ul>");
      for (const line of block.split("\n")) {
        lines.push(`  <li>${inlineHtml(line.slice(2).trim())}</li>`);
      }
      lines.push("</ul>");
    } else {
      lines.push(`<p>${inlineHtml(block)}</p>`);
    }
  }
  return lines;
}

// The website gets a generated TypeScript module rather than HTML spliced
// into each route: one document now has one rendering per locale plus its
// provenance, which is a data structure, not a blob of markup. The route
// components render the version line and the provenance notice themselves,
// from the site's own message catalogues, so no user-visible string is
// hard-coded in this generator.

function renderWebsiteFile() {
  const body = DOC_KEYS.map((key) => {
    const locales = LOCALES.map((locale) => {
      const resolved = RESOLVED[key][locale];
      const html = renderHtml(loadBlocks(resolved.file)).join("\n");
      const translation = resolved.translation;
      const fields = [
        `      locale: ${JSON.stringify(locale)},`,
        `      version: ${JSON.stringify(resolved.version)},`,
        `      effectiveDate: ${JSON.stringify(resolved.effectiveDate)},`,
        `      effectiveDateISO: ${JSON.stringify(resolved.effectiveDateISO)},`,
        translation === null
          ? "      translation: null,"
          : [
              "      translation: {",
              `        sourceLocale: ${JSON.stringify(translation.sourceLocale)},`,
              `        sourceVersion: ${JSON.stringify(translation.sourceVersion)},`,
              `        reviewedOn: ${JSON.stringify(translation.reviewedOn)},`,
              `        stale: ${translation.stale},`,
              "      },",
            ].join("\n"),
        `      html: ${JSON.stringify(html)},`,
      ];
      return [
        `    ${JSON.stringify(locale)}: {`,
        ...fields,
        "    },",
      ].join("\n");
    });
    return [`  ${JSON.stringify(key)}: {`, ...locales, "  },"].join("\n");
  }).join("\n");

  return `// GENERATED by tools/legal/render.mjs from legal/meta.json and legal/*.md.
// Do not hand-edit -- edit the sources and run \`node tools/legal/render.mjs\`.
//
// One rendering per document per locale, each carrying the provenance the
// route needs to be honest about what it is showing. See tools/legal/render.mjs
// for the staleness rule; the short version is that \`effectiveDate\` is null
// exactly when a translation has fallen behind its source, so a page can never
// present old wording under the current date.

export type LegalDocKey = ${DOC_KEYS.map((key) => JSON.stringify(key)).join(" | ")};

export type LegalTranslation = {
  // Locale of the text this translation was made from.
  sourceLocale: string;
  // Version of that source text.
  sourceVersion: string;
  // ISO date the translation was last reviewed against the source.
  reviewedOn: string;
  // True once the source has changed since that review.
  stale: boolean;
};

export type LegalRendering = {
  locale: string;
  // The version this wording actually means -- for a stale translation,
  // the version it was translated from, not the one now in force.
  version: string;
  // Null exactly when this is a stale translation.
  effectiveDate: string | null;
  effectiveDateISO: string | null;
  translation: LegalTranslation | null;
  html: string;
};

// The version and date currently in force, from the source locale. A stale
// translation's page quotes these to say what it is behind.
export const LEGAL_CURRENT = {
  sourceLocale: ${JSON.stringify(SOURCE_LOCALE)},
  version: ${JSON.stringify(meta.version)},
  effectiveDate: ${JSON.stringify(meta.effectiveDate)},
  effectiveDateISO: ${JSON.stringify(meta.effectiveDateISO)},
};

export const LEGAL_DOCUMENTS = {
${body}
} satisfies Record<LegalDocKey, Record<string, LegalRendering>>;
`;
}

// --- main ----------------------------------------------------------------

const argv = process.argv.slice(2);
const checkOnly = argv.includes("--check");
const blessIndex = argv.indexOf("--bless");
const blessLocale = blessIndex === -1 ? null : argv[blessIndex + 1];

if (blessIndex !== -1) {
  if (!blessLocale || blessLocale.startsWith("--")) {
    console.error("--bless needs a locale, e.g. `--bless fr`.");
    process.exit(1);
  }
  if (checkOnly) {
    console.error("--bless writes legal/meta.json, so it cannot be combined with --check.");
    process.exit(1);
  }
  const entry = meta.translations?.[blessLocale];
  if (!entry) {
    console.error(`legal/meta.json has no translation entry for ${blessLocale}.`);
    process.exit(1);
  }
  entry.sourceVersion = meta.version;
  entry.reviewedOn = new Date().toISOString().slice(0, 10);
  for (const key of DOC_KEYS) {
    entry.documents[key].sourceDigest = digestOf(meta.documents[key]);
  }
  writeFileSync(metaPath, `${JSON.stringify(meta, null, 2)}\n`);
  console.log(
    `Blessed ${blessLocale}: reviewed against version ${meta.version} on ${entry.reviewedOn}.`,
  );
  console.log("Re-run without --bless to regenerate the rendered copies.");
  process.exit(0);
}

let dirty = false;

const kotlinCurrent = existsSync(KOTLIN_FILE) ? readFileSync(KOTLIN_FILE, "utf8") : null;
const kotlinNext = renderKotlinFile();
if (kotlinNext !== kotlinCurrent) {
  dirty = true;
  if (checkOnly) {
    console.error(`Stale: ${path.relative(root, KOTLIN_FILE)}`);
  } else {
    mkdirSync(path.dirname(KOTLIN_FILE), { recursive: true });
    writeFileSync(KOTLIN_FILE, kotlinNext);
    console.log(`Wrote ${path.relative(root, KOTLIN_FILE)}`);
  }
}

const websiteCurrent = existsSync(WEBSITE_FILE) ? readFileSync(WEBSITE_FILE, "utf8") : null;
const websiteNext = renderWebsiteFile();
if (websiteNext !== websiteCurrent) {
  dirty = true;
  if (checkOnly) {
    console.error(`Stale: ${path.relative(root, WEBSITE_FILE)}`);
  } else {
    mkdirSync(path.dirname(WEBSITE_FILE), { recursive: true });
    writeFileSync(WEBSITE_FILE, websiteNext);
    console.log(`Wrote ${path.relative(root, WEBSITE_FILE)}`);
  }
}

if (staleDocuments.length > 0) {
  const message = `Stale translation(s) against version ${meta.version}: ${staleDocuments.join(", ")}`;
  if (checkOnly) {
    console.error(message);
    console.error(
      "Update the translated source, then run `node tools/legal/render.mjs --bless <locale>`.",
    );
  } else {
    console.warn(`Warning: ${message}`);
    console.warn(
      "Those locales now render without an effective date and with an out-of-date notice.",
    );
    console.warn(
      "Update the translated source, then run `node tools/legal/render.mjs --bless <locale>`.",
    );
  }
}

if (checkOnly && (dirty || staleDocuments.length > 0)) {
  if (dirty) {
    console.error("\nRun `node tools/legal/render.mjs` and commit the result.");
    console.error("If the shared LegalDocuments.kt changed, also rebuild InkwellShared.xcframework (see AGENTS.md).");
  }
  process.exit(1);
}
if (!dirty) {
  console.log("Legal docs already up to date.");
}
