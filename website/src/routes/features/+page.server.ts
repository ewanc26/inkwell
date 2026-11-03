import type { PageServerLoad } from "./$types";
import { loadInkwellUsers } from "$lib/server/inkwellUsers";

// Same live carousel as the landing page, from the same cached loader.
export const prerender = false;

export const load: PageServerLoad = async () => {
  return { users: await loadInkwellUsers() };
};
