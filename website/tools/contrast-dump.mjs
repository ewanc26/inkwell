// Debug companion to the CI accessibility audit (.pa11yci.json, the
// "Accessibility audit" job in ci.yml): runs axe's color-contrast rule
// against a URL and prints every violation or unverifiable ("incomplete")
// element with the foreground/background colours axe resolved.
//
// This exists because pa11y fails the build on axe *incomplete* results
// too: a gradient or translucent background makes text contrast formally
// indeterminable, and the site's rule is that every sentence must sit on
// a verifiable solid surface (see the --surface-tonal tokens).
//
// Usage (needs a build + preview running, e.g. pnpm preview --port 4173):
//   node tools/contrast-dump.mjs [url]      # summary of violations + incomplete
//   DETAILS=1 node tools/contrast-dump.mjs  # one line per violation

import { readFileSync } from "node:fs";
import { createRequire } from "node:module";

const require = createRequire(
  new URL(
    "../node_modules/.pnpm/puppeteer-core@24.43.1/node_modules/",
    import.meta.url,
  ),
);
const axeMinPath = new URL(
  "../node_modules/.pnpm/axe-core@4.11.4/node_modules/axe-core/axe.min.js",
  import.meta.url,
);

const puppeteer = require("puppeteer-core");
const axeSource = readFileSync(axeMinPath, "utf8");

const url = process.argv[2] ?? "http://localhost:4173/";

const browser = await puppeteer.launch({
  executablePath:
    process.env.CHROME_PATH ??
    "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome",
});
const page = await browser.newPage();
await page.setViewport({ width: 1280, height: 720 });
await page.goto(url, { waitUntil: "networkidle0" });
await page.evaluate(axeSource);
const results = await page.evaluate(async () => {
  const r = await window.axe.run(document, {
    runOnly: { type: "rule", values: ["color-contrast"] },
  });
  const detail = (nodes) =>
    nodes.flatMap((v) =>
      v.nodes.map((n) => ({
        target: n.target.join(" "),
        html: n.html.slice(0, 80),
        data: n.any[0]?.data ?? {},
      })),
    );
  return { violations: detail(r.violations), incomplete: detail(r.incomplete) };
});

console.log(
  `${url}: ${results.violations.length} violations, ${results.incomplete.length} unverifiable (incomplete) nodes`,
);
if (process.env.DETAILS) {
  for (const n of [...results.violations, ...results.incomplete]) {
    console.log(
      `${n.data.fgColor ?? "?"} on ${n.data.bgColor ?? "?"} ratio=${n.data.contrastRatio} :: ${n.target} :: ${n.html}`,
    );
  }
} else {
  for (const n of results.incomplete)
    console.log(`  unverifiable: ${n.target}`);
}
await browser.close();
