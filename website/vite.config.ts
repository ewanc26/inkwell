// ── Vite configuration ──────────────────────────────────────────
// Tailwind CSS v4 (via the Vite plugin, not PostCSS) + SvelteKit.
// SvelteKit is configured here too: adapter Vercel (Node 22 runtime),
// with runes mode forced on for all non-node_modules files.
// @lucide/svelte is forced external from SSR to avoid ESM/CJS
// interop issues with its icon components.

import adapter from "@sveltejs/adapter-vercel";
import tailwindcss from "@tailwindcss/vite";
import { sveltekit } from "@sveltejs/kit/vite";
import { defineConfig } from "vite";

export default defineConfig({
  plugins: [
    tailwindcss(),
    sveltekit({
      compilerOptions: {
        // Force Svelte 5 runes mode everywhere except node_modules,
        // so we never accidentally fall into legacy mode
        runes: ({ filename }) =>
          filename.split(/[/\\]/).includes("node_modules") ? undefined : true,
      },
      adapter: adapter({ runtime: "nodejs22.x" }),
    }),
  ],
  ssr: {
    noExternal: ["@lucide/svelte"],
  },
});
