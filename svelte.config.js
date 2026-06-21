import adapter from "@sveltejs/adapter-vercel";

const config = {
  compilerOptions: {
    runes: ({ filename }) =>
      filename.split(/[/\\]/).includes("node_modules") ? undefined : true,
  },
  kit: {
    adapter: adapter(),
  },
};

export default config;
