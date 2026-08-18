# AGENTS.md

Guidance for agents working on `inkwell.ewancroft.uk`, the SvelteKit/Vercel product, legal, and OAuth-metadata site shared by the primary iOS Inkwell app and its experimental Android port. This site lives under `website/` in the Inkwell monorepo.

## Principles

1. **Platform fidelity first** — SvelteKit SSR, Vercel deployment, and the site's calm editorial system come first. Don't port app UI patterns without explicit adaptation.
2. **Legal accuracy** — privacy, terms, and OAuth metadata are live promises to users and PDS servers. Claims must match both apps' actual behavior.
3. **Honest stubs** — unimplemented features say so explicitly. Never fabricate distribution channels or capabilities.
4. **No duplication** — reuse config, metadata, and legal text from sibling directories rather than copy-pasting.

For monorepo-wide rules, see [`../AGENTS.md`](../AGENTS.md). For iOS-specific boundaries, see [`../iOS/AGENTS.md`](../iOS/AGENTS.md). For Android-specific boundaries, see [`../Android/AGENTS.md`](../Android/AGENTS.md).

## AI-assisted contributions

AI tools may be used when contributing. Add `Co-authored-by:` trailers crediting AI agents when they materially contributed — attribution should be honest and accurate.

## Read First and Authority

- Read `README.md`, `PRODUCT.md`, `DESIGN.md`, `.impeccable/design.json`, `src/lib/config.ts`, and every touched route/style.
- Audit claims against the owned sibling directories `../iOS/` and `../Android/` before changing product, security, privacy, moderation, platform, or availability copy. The website must not turn planned/model-only code into a shipped feature.
- `src/routes/+page.svelte` is the landing page; `/privacy` and `/terms` are substantive legal promises; `/client-metadata.json` is a live OAuth client identity consumed by PDS servers. `src/routes/+layout.svelte` owns metadata/navigation/footer and client-side mobile navigation.
- `src/lib/styles/` is a token-first Tailwind v4/CSS system. `static/` currently contains only favicon, robots, and wordmark—font CSS references `/fonts/inter.woff2` and `/fonts/jetbrains-mono.woff2`, but those assets are absent and therefore fall back to system fonts.

## Product and Legal Accuracy

- iOS is the primary implementation; Android is experimental and materially incomplete. Its current writer ignores format selection, full detail/comments/interactions and verification are unfinished, and WorkManager notifications do not exist. Do not repeat the landing page's stronger parity claims without implementing and verifying them. See `../Android/AGENTS.md` for Android capability gaps.
- The Terms claim native mute/block/report tools. Confirm those exact user-facing flows in both applicable clients before retaining or expanding the promise. Legal copy, App Store status, and “available” wording require evidence, not roadmap intent.
- Privacy text currently describes Apple Keychain only while the site links Android, which uses encrypted shared preferences. It also omits public index/Constellation and other service details used by app features. Keep platform-specific storage and third-party disclosures accurate. See `../iOS/AGENTS.md` and `../Android/AGENTS.md` for platform-specific storage details.
- There is no App Store or Play Store listing. The hero's "Get Inkwell" CTA anchors to `#download`, which links the real, self-hosted AltStore source (`static/altstore/source.json`) and F-Droid repo (`static/fdroid/repo/`) — see `ALTSTORE_SOURCE_LINK`/`FDROID_REPO_LINK` in `src/lib/config.ts`. The Android card is explicitly labelled experimental. Never add placeholder store IDs or imply production distribution; if either source is unpublished or moved, update or remove its CTA in the same change.
- The site states no analytics or proprietary collection. Adding analytics, forms, pixels, remote scripts, logs, cookies, or server-side user data is a privacy/legal change requiring explicit review and policy updates.

## OAuth Contract

- The exact endpoint URL is the OAuth `client_id`. Keep `client_id`, `client_uri`, native callback, application type, grant/response types, DPoP requirement, and scope synchronized with both apps and the publicly deployed response.
- The live endpoint currently advertises granular publication/document/subscription/recommend/blob scopes. The iOS repo's checked-in `oauth/client-metadata.json` still says only `atproto`, even though iOS runtime and Android use granular scopes; resolve that discrepancy deliberately.
- iOS implements writes to `pub.leaflet.comment`, but the website/runtime granular scopes omit that collection. Do not claim working comments until authorization and hosted metadata are proven together. See `../iOS/AGENTS.md` and `../Android/AGENTS.md` for client-side OAuth rules.
- Treat metadata edits as authentication changes. Validate response content type/cache behavior from production and perform fresh login, cancel/state failure, refresh, restore, and logout against representative PDS servers.

## Design and Accessibility

- Preserve the calm, cool-toned editorial system: typography-first hierarchy, a single restrained `#139500`-family accent, light/dark parity via `light-dark()`, app-matched card elevation only, and functional header blur only. Respect the explicit anti-pattern list.
- Keep semantic landmarks/headings, skip link, 44px mobile target, focus-visible rings, contrast, responsive nav, reduced-motion overrides, keyboard Escape/dismissal, and readable 65ch legal prose.
- Mobile-nav focus is not trapped or restored and route changes do not explicitly close it except link clicks; test these lifecycle details when editing the shell.
- Favor server-rendered/static markup and CSS. The OAuth endpoint must remain dynamic/available; do not globally prerender without verifying the resulting deployment semantics.

## Tooling and Validation

- pnpm is authoritative (`pnpm-lock.yaml` and `pnpm-workspace.yaml`; no npm lock). Vercel installs with `pnpm install` on Node 22. Run `pnpm install --frozen-lockfile`, `pnpm check`, and `pnpm build`.
- There is no `lint` or test script. `pnpm format` writes files, so use `pnpm exec prettier --check --ignore-unknown .` for a non-mutating formatting check.
- Preview home/privacy/terms, inspect `/client-metadata.json`, check all external/CTA/legal links, production metadata, 404s, missing font/network requests, light/dark contrast, reduced motion, keyboard/mobile navigation, and narrow/zoomed layouts.
- Do not commit `node_modules/`, `.svelte-kit/`, `.vercel/`, build output, `.env`, local design-tool settings, or generated deployment state.

## Things that look wrong but are not

- **Website links to self-hosted AltStore and F-Droid** rather than App Store / Play Store listings — those stores do not have published listings yet.
- **`static/` contains only favicon, robots, and wordmark** — font assets are referenced but absent, falling back to system fonts by design.
