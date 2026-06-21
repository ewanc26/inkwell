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
