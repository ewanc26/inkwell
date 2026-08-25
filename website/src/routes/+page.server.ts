import type { PageServerLoad } from "./$types";

const CONSTELLATION = "https://constellation.microcosm.blue";
const SLINGSHOT = "https://slingshot.microcosm.blue";
const BSKY = "https://public.api.bsky.app";
const APP_URI = "https://inkwell.ewancroft.uk";
const BACKLINK_SOURCE = "uk.ewancroft.inkwell.user:app";
const MAX_USERS = 48;
const CACHE_TTL_MS = 10 * 60 * 1000;

export const prerender = false;

export type InkwellUser = {
  handle: string;
  displayName: string | null;
  avatar: string | null;
};

type Cache = { at: number; users: InkwellUser[] };
let cache: Cache | null = null;

async function fetchJson(url: string): Promise<unknown> {
  const res = await fetch(url, { headers: { Accept: "application/json" } });
  if (!res.ok) throw new Error(`Request failed: ${res.status}`);
  return res.json();
}

export const load: PageServerLoad = async () => {
  if (cache && Date.now() - cache.at < CACHE_TTL_MS) {
    return { users: cache.users };
  }

  let users: InkwellUser[] = [];
  try {
    const backlinks = (await fetchJson(
      `${CONSTELLATION}/xrpc/blue.microcosm.links.getBacklinks` +
        `?subject=${encodeURIComponent(APP_URI)}` +
        `&source=${encodeURIComponent(BACKLINK_SOURCE)}` +
        `&limit=100`,
    )) as { records?: Array<{ did?: string }> };

    const dids = Array.from(
      new Set((backlinks.records ?? []).map((r) => r.did).filter(Boolean)),
    ).slice(0, MAX_USERS) as string[];

    const resolved = await Promise.all(
      dids.map(
        async (did): Promise<InkwellUser | null> => {
          try {
            const [mini, profile] = await Promise.all([
              fetchJson(
                `${SLINGSHOT}/xrpc/com.bad-example.identity.resolveMiniDoc?identifier=${encodeURIComponent(did)}`,
              ) as Promise<{ handle?: string } | null>,
              fetchJson(
                `${BSKY}/xrpc/app.bsky.actor.getProfile?actor=${encodeURIComponent(did)}`,
              ) as Promise<{ handle?: string; displayName?: string; avatar?: string } | null>,
            ]);
            const handle =
              mini?.handle ?? profile?.handle ?? did.replace(/^did:plc:/, "");
            return {
              handle,
              displayName: profile?.displayName ?? null,
              avatar: profile?.avatar ?? null,
            };
          } catch {
            return null;
          }
        },
      ),
    );
    users = resolved.filter((u): u is InkwellUser => u !== null);
  } catch {
    users = [];
  }

  cache = { at: Date.now(), users };
  return { users };
};
