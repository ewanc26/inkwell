// ── Vite configuration ──────────────────────────────────────────
// Tailwind CSS v4 (via the Vite plugin, not PostCSS) + SvelteKit.
// @lucide/svelte is forced external from SSR to avoid ESM/CJS
// interop issues with its icon components.

import tailwindcss from "@tailwindcss/vite";
import { sveltekit } from "@sveltejs/kit/vite";
import { defineConfig } from "vite";

export default defineConfig({
  plugins: [tailwindcss(), sveltekit()],
  ssr: {
    noExternal: ["@lucide/svelte"],
  },
});
