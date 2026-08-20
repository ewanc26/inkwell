/**
 * Renders tools/og-cover/template.html to static/og-cover.png.
 *
 *   node tools/og-cover/render.mjs
 *
 * The card has to be a raster image: social scrapers don't run CSS custom
 * properties, webfonts, or light-dark(), so the design is baked to pixels
 * once, here, and the PNG is committed alongside its source.
 *
 * Chromium is the renderer because it's the engine the site is designed
 * against — no second layout implementation to keep honest. Two details are
 * deliberate:
 *
 *   - The template is served over a throwaway localhost server rather than
 *     opened as file://, because Chromium refuses cross-directory font loads
 *     from file:// origins and would silently fall back to a system face.
 *   - The capture goes through the DevTools protocol rather than Chromium's
 *     --screenshot flag, which sizes its output from the window (chrome
 *     included) and can't be pinned to an exact 1200×630. DevTools also lets
 *     us wait on document.fonts.ready, so the card never shoots mid-load.
 *
 * Dependency-free on purpose: it needs a Chromium binary and nothing from
 * node_modules, so it can't rot the site's install graph. Point CHROME_PATH
 * at a browser if none of the usual locations has one.
 */

import { createServer } from "node:http";
import { spawn } from "node:child_process";
import { readFile, writeFile, mkdtemp, rm } from "node:fs/promises";
import { existsSync } from "node:fs";
import { tmpdir } from "node:os";
import { join, extname, resolve, dirname } from "node:path";
import { fileURLToPath } from "node:url";

const WIDTH = 1200;
const HEIGHT = 630;

const here = dirname(fileURLToPath(import.meta.url));
const siteRoot = resolve(here, "../..");
const output = join(siteRoot, "static/og-cover.png");

const MIME = {
  ".html": "text/html; charset=utf-8",
  ".css": "text/css; charset=utf-8",
  ".woff2": "font/woff2",
  ".svg": "image/svg+xml",
  ".png": "image/png",
};

/** First Chromium-ish binary that actually exists on this machine. */
function findChrome() {
  const browsers = process.env.PLAYWRIGHT_BROWSERS_PATH;
  const candidates = [
    process.env.CHROME_PATH,
    browsers && join(browsers, "chromium"),
    "/usr/bin/chromium",
    "/usr/bin/chromium-browser",
    "/usr/bin/google-chrome",
    "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome",
    "/Applications/Chromium.app/Contents/MacOS/Chromium",
  ].filter(Boolean);

  const found = candidates.find((path) => existsSync(path));
  if (!found) {
    throw new Error(
      `No Chromium binary found. Tried:\n  ${candidates.join("\n  ")}\n` +
        "Set CHROME_PATH to a Chrome or Chromium executable and re-run.",
    );
  }
  return found;
}

/**
 * Static file server scoped to the site root. Request paths are resolved and
 * then checked to still sit inside that root, so a `..` in a URL can't reach
 * out of the repo.
 */
function serve() {
  const server = createServer(async (req, res) => {
    const path = resolve(
      siteRoot,
      "." + decodeURIComponent(req.url.split("?")[0]),
    );
    if (!path.startsWith(siteRoot + "/")) {
      res.writeHead(403).end();
      return;
    }
    try {
      const body = await readFile(path);
      res.writeHead(200, {
        "content-type": MIME[extname(path)] ?? "application/octet-stream",
      });
      res.end(body);
    } catch {
      res.writeHead(404).end();
    }
  });

  return new Promise((ok) => server.listen(0, "127.0.0.1", () => ok(server)));
}

/** Launches headless Chromium and resolves once it announces its DevTools port. */
function launchChrome(profile) {
  const child = spawn(findChrome(), [
    "--headless=new",
    "--no-sandbox",
    "--disable-gpu",
    "--hide-scrollbars",
    "--force-color-profile=srgb",
    "--remote-debugging-port=0",
    // The compositing surface comes from the window, not from the metrics
    // override. Start it large enough for the card or the capture tiles a
    // too-small surface across the requested clip.
    `--window-size=${WIDTH},${HEIGHT + 200}`,
    `--user-data-dir=${profile}`,
    "about:blank",
  ]);

  return new Promise((ok, fail) => {
    let stderr = "";
    const timer = setTimeout(
      () =>
        fail(
          new Error(`Chromium never opened a DevTools port.\n${stderr.trim()}`),
        ),
      30_000,
    );

    child.on("error", fail);
    child.stderr.on("data", (chunk) => {
      stderr += chunk;
      const port = stderr.match(
        /DevTools listening on ws:\/\/[^:]+:(\d+)\//,
      )?.[1];
      if (port) {
        clearTimeout(timer);
        ok({ child, port });
      }
    });
  });
}

/** Minimal DevTools client: send(method, params) -> result. */
async function connect(wsUrl) {
  const socket = new WebSocket(wsUrl);
  const pending = new Map();
  let nextId = 0;

  socket.addEventListener("message", (event) => {
    const message = JSON.parse(event.data);
    const waiter = pending.get(message.id);
    if (!waiter) return;
    pending.delete(message.id);
    message.error
      ? waiter.fail(new Error(message.error.message))
      : waiter.ok(message.result);
  });

  await new Promise((ok, fail) => {
    socket.addEventListener("open", ok, { once: true });
    socket.addEventListener(
      "error",
      () => fail(new Error(`Cannot reach ${wsUrl}`)),
      {
        once: true,
      },
    );
  });

  return {
    send(method, params = {}) {
      const id = ++nextId;
      return new Promise((ok, fail) => {
        pending.set(id, { ok, fail });
        socket.send(JSON.stringify({ id, method, params }));
      });
    },
    close: () => socket.close(),
  };
}

const profile = await mkdtemp(join(tmpdir(), "og-cover-"));
const server = await serve();
const url = `http://127.0.0.1:${server.address().port}/tools/og-cover/template.html`;
const { child, port } = await launchChrome(profile);

try {
  const targets = await (
    await fetch(`http://127.0.0.1:${port}/json/list`)
  ).json();
  const page = targets.find((t) => t.type === "page");
  const cdp = await connect(page.webSocketDebuggerUrl);

  // Pin the viewport to the card so layout never depends on window chrome.
  await cdp.send("Emulation.setDeviceMetricsOverride", {
    width: WIDTH,
    height: HEIGHT,
    deviceScaleFactor: 1,
    mobile: false,
  });
  await cdp.send("Page.navigate", { url });
  // Webfonts resolve after load; screenshotting before they land would bake
  // the fallback system face into the artwork.
  await cdp.send("Runtime.evaluate", {
    expression:
      "document.fonts.ready.then(() => new Promise(requestAnimationFrame))",
    awaitPromise: true,
  });

  const { data } = await cdp.send("Page.captureScreenshot", {
    format: "png",
    captureBeyondViewport: true,
    clip: { x: 0, y: 0, width: WIDTH, height: HEIGHT, scale: 1 },
  });

  const png = Buffer.from(data, "base64");
  await writeFile(output, png);
  cdp.close();

  console.log(
    `static/og-cover.png — ${WIDTH}×${HEIGHT}, ${(png.length / 1024).toFixed(1)} KiB`,
  );
} finally {
  // Chromium keeps flushing its profile after kill(); removing the directory
  // before it exits races the last writes and throws ENOTEMPTY.
  const exited = new Promise((ok) => child.once("exit", ok));
  child.kill();
  server.close();
  await exited;
  await rm(profile, { recursive: true, force: true });
}
