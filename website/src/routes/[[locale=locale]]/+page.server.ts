import type { PageServerLoad } from "./$types";
import { loadInkwellUsers } from "$lib/server/inkwellUsers";

// Live third-party data, so the page is rendered per request rather than
// prerendered. Unchanged by localisation: the same data is shown in every
// locale, only the surrounding copy differs.
export const prerender = false;

export const load: PageServerLoad = async () => {
  return { users: await loadInkwellUsers() };
};
