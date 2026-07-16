# AGENTS.md

Guidance for agents working on `inkwell.ewancroft.uk`, the SvelteKit/Vercel product, legal, and OAuth-metadata site shared by the primary iOS Inkwell app and its experimental Android port.

## Read First and Authority

- Read `PRODUCT.md`, `DESIGN.md`, `.impeccable/design.json`, `src/lib/config.ts`, and every touched route/style. There is no README in this repository.
- Audit claims against the owned sibling repos `../inkwell` and `../inkwell-android` before changing product, security, privacy, moderation, platform, or availability copy. The website must not turn planned/model-only code into a shipped feature.
- `+page.svelte` is the landing page; `/privacy` and `/terms` are substantive legal promises; `/client-metadata.json` is a live OAuth client identity consumed by PDS servers. `+layout.svelte` owns metadata/navigation/footer and client-side mobile navigation.
- `src/lib/styles/` is a token-first Tailwind v4/CSS system. `static/` currently contains only favicon, robots, and wordmark—font CSS references `/fonts/inter.woff2` and `/fonts/jetbrains-mono.woff2`, but those assets are absent and therefore fall back to system fonts.

## Product and Legal Accuracy

- iOS is the primary implementation; Android is experimental and materially incomplete. Its current writer ignores format selection, full detail/comments/interactions and verification are unfinished, and WorkManager notifications do not exist. Do not repeat the landing page's stronger parity claims without implementing and verifying them.
- The Terms claim native mute/block/report tools. Confirm those exact user-facing flows in both applicable clients before retaining or expanding the promise. Legal copy, App Store status, and “available” wording require evidence, not roadmap intent.
- Privacy text currently describes Apple Keychain only while the site links Android, which uses encrypted shared preferences. It also omits public index/Constellation and other service details used by app features. Keep platform-specific storage and third-party disclosures accurate.
- There is no App Store listing/badge wired today; GitHub is the iOS CTA and the Android repository is labelled experimental. Never add placeholder store IDs or imply production distribution.
- The site states no analytics or proprietary collection. Adding analytics, forms, pixels, remote scripts, logs, cookies, or server-side user data is a privacy/legal change requiring explicit review and policy updates.

## OAuth Contract

- The exact endpoint URL is the OAuth `client_id`. Keep `client_id`, `client_uri`, native callback, application type, grant/response types, DPoP requirement, and scope synchronized with both apps and the publicly deployed response.
- The live endpoint currently advertises granular publication/document/subscription/recommend/blob scopes. The iOS repo's checked-in `oauth/client-metadata.json` still says only `atproto`, even though iOS runtime and Android use granular scopes; resolve that discrepancy deliberately.
- iOS implements writes to `pub.leaflet.comment`, but the website/runtime granular scopes omit that collection. Do not claim working comments until authorization and hosted metadata are proven together.
- Treat metadata edits as authentication changes. Validate response content type/cache behavior from production and perform fresh login, cancel/state failure, refresh, restore, and logout against representative PDS servers.

## Design and Accessibility

- Preserve the calm, cool-toned editorial system: typography-first hierarchy, a single restrained `#139500`-family accent, light/dark parity via `light-dark()`, app-matched card elevation only, and functional header blur only. Respect the explicit anti-pattern list.
- Keep semantic landmarks/headings, skip link, 44px mobile target, focus-visible rings, contrast, responsive nav, reduced-motion overrides, keyboard Escape/dismissal, and readable 65ch legal prose.
- Mobile-nav focus is not trapped or restored and route changes do not explicitly close it except link clicks; test these lifecycle details when editing the shell.
- Favor server-rendered/static markup and CSS. The OAuth endpoint must remain dynamic/available; do not globally prerender it without verifying the resulting deployment semantics.

## Tooling and Validation

- pnpm is authoritative (`pnpm-lock.yaml` and `pnpm-workspace.yaml`; no npm lock). Vercel installs with `pnpm install` on Node 22. Run `pnpm install --frozen-lockfile`, `pnpm check`, and `pnpm build`.
- There is no `lint` or test script. `pnpm format` writes files, so use `pnpm exec prettier --check --ignore-unknown .` for a non-mutating formatting check.
- Preview home/privacy/terms, inspect `/client-metadata.json`, check all external/CTA/legal links, production metadata, 404s, missing font/network requests, light/dark contrast, reduced motion, keyboard/mobile navigation, and narrow/zoomed layouts.
- Do not commit `node_modules/`, `.svelte-kit/`, `.vercel/`, build output, `.env`, local design-tool settings, or generated deployment state.
