// ── SvelteKit configuration ──────────────────────────────────────
// Adapter: Vercel (Node 22 runtime).
// Runes mode is forced on for all non-node_modules files.

import adapter from "@sveltejs/adapter-vercel";

const config = {
  compilerOptions: {
    // Force Svelte 5 runes mode everywhere except node_modules,
    // so we never accidentally fall into legacy mode
    runes: ({ filename }) =>
      filename.split(/[/\\]/).includes("node_modules") ? undefined : true,
  },
  kit: {
    adapter: adapter({ runtime: "nodejs22.x" }),
  },
};

export default config;
