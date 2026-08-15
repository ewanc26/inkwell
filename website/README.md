# Inkwell website

The marketing, legal, and OAuth-metadata site for [Inkwell](https://github.com/ewanc26/inkwell), a native reader and writer for the [Standard.site](https://standard.site) publishing ecosystem on [AT Protocol](https://atproto.com). Lives at [inkwell.ewancroft.uk](https://inkwell.ewancroft.uk).

This repository is part of the [Inkwell monorepo](https://github.com/ewanc26/inkwell). The website source lives under `website/` in that repo. The old standalone `inkwell-website` repository has been archived; please open issues and PRs against the monorepo instead.

Inkwell is primarily an iOS app (SwiftUI), with an experimental Android port (Kotlin/Compose). Neither ships through the App Store or Play Store — this site hosts the real install sources instead:

- **iOS** — a self-hosted [AltStore](https://altstore.io) source at [`iOS/altstore/source.json`](../iOS/altstore/source.json)
- **Android** — a self-hosted, signed [F-Droid](https://f-droid.org) repo at [`Android/fdroid-repo/repo`](../Android/fdroid-repo/repo)

The landing page's "Get Inkwell" section (`#download`) links both.

## Stack

SvelteKit 2 + Svelte 5, Tailwind CSS v4 (via the Vite plugin), deployed to Vercel. pnpm is the only supported package manager (`pnpm-lock.yaml`, `pnpm-workspace.yaml` — no npm lock).

## Development

```bash
pnpm install
pnpm dev       # start the dev server
pnpm check     # svelte-kit sync + svelte-check
pnpm build     # production build
pnpm preview   # preview the production build
pnpm format    # prettier --write
```

## Structure

- `src/routes/+page.svelte` — the landing page (hero, download, features, security callout)
- `src/routes/privacy` / `src/routes/terms` — legal pages; substantive promises, not boilerplate
- `src/routes/client-metadata.json` — a live OAuth client identity consumed by PDS servers during sign-in
- `src/lib/config.ts` — site metadata, nav links, and install-source URLs in one place
- `src/lib/styles/` — the token-first design system (`tokens.css`, `components.css`, `motion.css`, `fonts.css`)
- `static/altstore/`, `static/fdroid/repo/` — the hosted install sources described above

## Contributing

Read [`../AGENTS.md`](../AGENTS.md) first — it covers product/legal accuracy requirements, the OAuth contract, and design/accessibility constraints that apply to any change here. [`DESIGN.md`](DESIGN.md) documents the visual system in full; [`PRODUCT.md`](PRODUCT.md) covers brand voice and audience.
