# Website

Inkwell marketing, legal, and OAuth-metadata site — `inkwell.ewancroft.uk`.

## Contents

- `src/` — SvelteKit app source (landing page, legal routes, OAuth metadata)
- `static/` — favicon, fonts, AltStore metadata, F-Droid repo
- `package.json` / `pnpm-lock.yaml` / `pnpm-workspace.yaml` — Node/pnpm config
- `svelte.config.ts` / `vite.config.ts` — build config
- `vercel.json` — Vercel deployment config

## Development

```bash
pnpm install
pnpm dev       # start dev server
pnpm check     # svelte-kit sync + svelte-check
pnpm build     # production build
pnpm preview   # preview production build
pnpm format    # prettier --write
```

## Language

The site is British English only and is served unprefixed, so every address
the site has ever had remains the canonical one. No prefixed locale is
currently registered; `/fr`, `/es`, and `/gd` are ordinary 404s.

The optional `[[locale=locale]]` route group, the locale registry and the
message-catalogue contract remain in place if a reviewed translation is
added later. Copy and per-route `<title>`/description live in
`src/lib/i18n/messages/`; `en-GB.ts` is the source catalogue and its inferred
type is the contract any future translation must match. `src/routes/+layout.svelte`
emits the canonical URL and `x-default` from one route table, shared with
`/sitemap.xml`. `/client-metadata.json` is an OAuth protocol artefact and is
never localised.

## Deployment

Deployed to Vercel. The `vercel.json` configures pnpm as the install command and points to the SvelteKit Vercel adapter.

Availability copy must describe distribution channels that actually exist. App Store and Google Play builds are planned, but the site should continue to present AltStore Classic and F-Droid as the current install routes until mainstream-store listings are live.

`src/lib/config.ts` contains clearly named placeholder App Store and Google Play URLs so the planned links can be rendered now without pretending the listings exist. Replace those constants with the real listing URLs when the £5 store builds launch, and remove the placeholder wording from the landing page in the same change.

## AI-assisted contributions

AI tools may be used when contributing. Add `Co-authored-by:` trailers crediting AI agents when they materially contributed — attribution should be honest and accurate.

## Licence

AGPL-3.0 with the [App Store Distribution Exception](../APP_STORE_EXCEPTION.md) — see `../LICENSE` for the base licence.
