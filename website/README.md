# Website

Inkwell marketing, legal, and OAuth-metadata site — `inkwell.ewancroft.uk`.

## Contents

- `src/` — SvelteKit app source (landing page, legal routes, OAuth metadata)
- `static/` — favicon, fonts, AltStore metadata, F-Droid repo
- `package.json` / `pnpm-lock.yaml` / `pnpm-workspace.yaml` — Node/pnpm config
- `svelte.config.js` / `vite.config.ts` — build config
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

## Deployment

Deployed to Vercel. The `vercel.json` configures pnpm as the install command and points to the SvelteKit Vercel adapter.

## AI-assisted contributions

AI tools may be used when contributing. Add `Co-authored-by:` trailers crediting AI agents when they materially contributed — attribution should be honest and accurate.

## Licence

AGPL 3.0 — see `../LICENSE`
