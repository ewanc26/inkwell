import type { PageServerLoad } from "./$types";
import { loadInkwellUsers } from "#lib/server/inkwellUsers.ts";

// Same live carousel as the landing page, from the same cached loader.
export const prerender = false;

export const load: PageServerLoad = async () => {
  return { users: await loadInkwellUsers() };
};
