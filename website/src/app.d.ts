// ── SvelteKit app type declarations ──────────────────────────────
// Extends the global App namespace for route-level type overrides.
// `Locals.locale` is set by src/hooks.server.ts from the URL, so server
// code never has to re-derive the active locale.

import type { SvelteKitApp } from "@sveltejs/kit";
import type { Locale } from "$lib/i18n/locales";
import "../app";

declare global {
  namespace App {
    // interface Error {}
    interface Locals {
      locale: Locale;
    }
    // interface PageData {}
    // interface PageState {}
    // interface Platform {}
  }
}

export type App = SvelteKitApp;
