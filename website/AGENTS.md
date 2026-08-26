# AGENTS.md

Guidance for agents working on `inkwell.ewancroft.uk`, the SvelteKit/Vercel product, legal, and OAuth-metadata site shared by the primary iOS Inkwell app and its experimental Android port. This site lives under `website/` in the Inkwell monorepo.

## Principles

1. **Platform fidelity first** — SvelteKit SSR, Vercel deployment, and the site's calm editorial system come first. Don't port app UI patterns without explicit adaptation.
2. **Legal accuracy** — privacy, terms, and OAuth metadata are live promises to users and PDS servers. Claims must match both apps' actual behaviour.
3. **Honest stubs** — unimplemented features say so explicitly. Never fabricate distribution channels or capabilities.
4. **No duplication** — reuse config, metadata, and legal text from sibling directories rather than copy-pasting.

For monorepo-wide rules, see [`../AGENTS.md`](../AGENTS.md). For iOS-specific boundaries, see [`../iOS/AGENTS.md`](../iOS/AGENTS.md). For Android-specific boundaries, see [`../Android/AGENTS.md`](../Android/AGENTS.md).

## AI-assisted contributions

AI tools may be used when contributing. Add `Co-authored-by:` trailers crediting AI agents when they materially contributed — attribution should be honest and accurate.

## Read First and Authority

- Read `README.md`, `PRODUCT.md`, `DESIGN.md`, `.impeccable/design.json`, `src/lib/config.ts`, and every touched route/style.
- Audit claims against the owned sibling directories `../iOS/` and `../Android/` before changing product, security, privacy, moderation, platform, or availability copy. The website must not turn planned/model-only code into a shipped feature.
- `src/routes/+page.svelte` is the landing page; `/privacy` and `/terms` are substantive legal promises; `/client-metadata.json` is a live OAuth client identity consumed by PDS servers. `src/routes/+layout.svelte` owns metadata/navigation/footer and client-side mobile navigation.
- `static/og-cover.png` is the Open Graph/Twitter card advertised by `src/routes/+layout.svelte`. It is generated artwork, not hand-drawn: edit `tools/og-cover/template.html`, run `pnpm og`, and commit the regenerated PNG in the same change. Keep `OG_IMAGE`'s dimensions and alt text in `src/lib/config.ts` matching what the template actually renders.
- `src/lib/styles/` is a token-first Tailwind v4/CSS system. `static/` currently contains only favicon, robots, and wordmark—font CSS references `/fonts/inter.woff2` and `/fonts/jetbrains-mono.woff2`, but those assets are absent and therefore fall back to system fonts.

## Product and Legal Accuracy

- iOS is the primary implementation; Android is still labelled experimental but has closed most of the parity gap: the writer honours format selection (Leaflet/Markpub/pckt/Offprint via `MarkdownConverter`), the detail screen renders full content plus comments/interactions, subscribe/recommend are wired end-to-end, publication/document verification is implemented (`StandardSiteVerifier.kt`, with full feed-card coverage for direct `https://` sites and `site.standard.publication` AT-URI-site documents), and WorkManager-based background notification polling exists. Do not claim parity beyond this — see `../Android/AGENTS.md`'s "Current Capability Gaps" section for the authoritative, current boundary before writing or updating Android-facing copy.
- Both clients expose Settings → Account → Muted & Blocked management with lists plus unmute/unblock actions. This is management parity only: neither client currently exposes profile-level mute/block creation actions in that surface.
- The Terms claim native mute/block/report tools. Mute/block management and reporting now have user-facing evidence on both platforms. Do not expand the reporting promise beyond the implemented account and post report flows without additional client work. Legal copy, App Store status, and "available" wording require evidence, not roadmap intent.
- `/privacy` and `/terms` are generated from `../legal/privacy.md` and `../legal/terms.md` (the single source also compiled into the shared KMP module's `LegalDocuments.kt`, which iOS and Android render natively in-app) via `node tools/legal/render.mjs` — do not hand-edit the HTML between the `GENERATED-LEGAL` markers in `src/routes/{privacy,terms}/+page.svelte`, edit the source `.md` and regenerate. Keep platform-specific storage and third-party disclosures in the source accurate; see `../iOS/AGENTS.md` and `../Android/AGENTS.md` for platform-specific storage details.
- There is no App Store or Play Store listing. The hero's "Get Inkwell" CTA anchors to `#download`, which links the real, self-hosted AltStore source (`static/altstore/source.json`) and F-Droid repo (`static/fdroid/repo/`) — see `ALTSTORE_SOURCE_LINK`/`FDROID_REPO_LINK` in `src/lib/config.ts`. The Android card is explicitly labelled experimental. Never add placeholder store IDs or imply production distribution; if either source is unpublished or moved, update or remove its CTA in the same change.
- The site states no analytics or proprietary collection. Adding analytics, forms, pixels, remote scripts, logs, cookies, or server-side user data is a privacy/legal change requiring explicit review and policy updates.

## OAuth Contract

- The exact endpoint URL is the OAuth `client_id`. Keep `client_id`, `client_uri`, native callback, application type, grant/response types, DPoP requirement, and scope synchronized with both apps and the publicly deployed response.
- The live endpoint, both checked-in client metadata files, and both runtime clients align on granular publication/document/subscription/recommend/userinput/blob access plus the personal moderation permissions Inkwell actually uses: create/delete for `app.bsky.graph.block`, `muteActor`, `unmuteActor`, `getMutes`, `getBlocks`, and `com.atproto.moderation.createReport` RPC access for the Bluesky AppView audience. A scope change requires a fresh authorization before an existing account can use newly granted permissions.
- iOS implements writes to `pub.leaflet.comment`, but the website/runtime granular scopes omit that collection. Do not claim working comments until authorization and hosted metadata are proven together. See `../iOS/AGENTS.md` and `../Android/AGENTS.md` for client-side OAuth rules.
- Treat metadata edits as authentication changes. Validate response content type/cache behaviour from production and perform fresh login, cancel/state failure, refresh, restore, and logout against representative PDS servers.

## Design and Accessibility

- Preserve the calm, cool-toned editorial system: typography-first hierarchy, a single restrained `#139500`-family accent, light/dark parity via `light-dark()`, app-matched card elevation only, and functional header blur only. Respect the explicit anti-pattern list.
- Keep semantic landmarks/headings, skip link, 44px mobile target, focus-visible rings, contrast, responsive nav, reduced-motion overrides, keyboard Escape/dismissal, and readable 65ch legal prose.
- Mobile-nav focus is not trapped or restored and route changes do not explicitly close it except link clicks; test these lifecycle details when editing the shell.
- Favor server-rendered/static markup and CSS. The OAuth endpoint must remain dynamic/available; do not globally prerender without verifying the resulting deployment semantics.

## Tooling and Validation

- pnpm is authoritative (`pnpm-lock.yaml` and `pnpm-workspace.yaml`; no npm lock). Vercel installs with `pnpm install` on Node 22. Run `pnpm install --frozen-lockfile`, `pnpm check`, and `pnpm build`.
- `pnpm og` re-renders the social cover through headless Chromium; it needs a Chrome/Chromium binary (set `CHROME_PATH` if it isn't in a standard location) but no extra dependencies. It is not part of `pnpm build` — the PNG is committed, so the site builds and deploys without a browser.
- There is no `lint` or test script. `pnpm format` writes files, so use `pnpm exec prettier --check --ignore-unknown .` for a non-mutating formatting check.
- Preview home/privacy/terms, inspect `/client-metadata.json`, check all external/CTA/legal links, production metadata, 404s, missing font/network requests, light/dark contrast, reduced motion, keyboard/mobile navigation, and narrow/zoomed layouts.
- Do not commit `node_modules/`, `.svelte-kit/`, `.vercel/`, build output, `.env`, local design-tool settings, or generated deployment state.

## Things that look wrong but are not

- **Website links to self-hosted AltStore and F-Droid** rather than App Store / Play Store listings — those stores do not have published listings yet.
- **`static/og-cover.png` is a committed binary** that duplicates what `tools/og-cover/template.html` describes — social scrapers don't run CSS, webfonts, or `light-dark()`, so the card has to ship as pixels.
- **`static/` contains only favicon, robots, and wordmark** — font assets are referenced but absent, falling back to system fonts by design.
