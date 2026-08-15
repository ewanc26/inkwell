// ── SvelteKit app type declarations ──────────────────────────────
// Extends the global App namespace for route-level type overrides.
// Currently empty — all routes return page data inferred from
// load functions, so no explicit Error/Locals/PageData types needed.

import type { SvelteKitApp } from "@sveltejs/kit";
import "../app";

declare global {
  namespace App {
    // interface Error {}
    // interface Locals {}
    // interface PageData {}
    // interface PageState {}
    // interface Platform {}
  }
}

export type App = SvelteKitApp;
