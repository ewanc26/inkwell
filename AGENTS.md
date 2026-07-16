# AGENTS.md

Guidance for agents working on the Inkwell marketing/support website.

## Structure and design

- `src/routes/` owns public pages and metadata.
- `src/lib/` contains shared Svelte components and content.
- `static/` contains downloadable/public assets.
- `.impeccable` and existing design guidance describe the visual language; inspect them before broad UI changes.

## Rules

- Keep product claims, platform availability, privacy statements, and links consistent with the actual Inkwell apps.
- Use the existing package manager/lockfile and do not introduce a second lockfile.
- Favor semantic, fast, mostly static pages. Avoid client JavaScript where SvelteKit rendering and CSS suffice.
- Preserve accessible contrast, focus, reduced-motion, alt text, and responsive layouts.
- Do not embed analytics, forms, or external scripts without explicit privacy consideration.

## Validation

Run `npm run check`, `npm run build`, and the configured formatter check. Preview production output and inspect all routes, external links, app/download links, metadata/social cards, keyboard navigation, dark/light presentation, narrow widths, and missing assets. Do not commit build output or local environment files.
