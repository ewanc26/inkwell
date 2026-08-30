import type { PageServerLoad } from "./$types";

const CONTRIBUTORS_URL =
  "https://api.github.com/repos/ewanc26/inkwell/contributors?per_page=100";
const CACHE_TTL_MS = 60 * 60 * 1000;

export type Contributor = {
  login: string;
  avatarUrl: string;
  profileUrl: string;
  contributions: number;
};

type GitHubContributor = {
  login?: string;
  avatar_url?: string;
  html_url?: string;
  contributions?: number;
  type?: string;
};

type Cache = {
  at: number;
  contributors: Contributor[];
};

let cache: Cache | null = null;

function avatarUrl(url: string): string {
  return `${url}${url.includes("?") ? "&" : "?"}s=96`;
}

function isHumanContributor(
  contributor: GitHubContributor,
): contributor is GitHubContributor &
  Required<Pick<GitHubContributor, "login" | "avatar_url" | "html_url">> {
  return (
    contributor.type === "User" &&
    Boolean(contributor.login) &&
    Boolean(contributor.avatar_url) &&
    Boolean(contributor.html_url) &&
    !contributor.login?.endsWith("[bot]")
  );
}

export const load: PageServerLoad = async ({ setHeaders }) => {
  if (cache && Date.now() - cache.at < CACHE_TTL_MS) {
    setHeaders({
      "cache-control":
        "public, max-age=0, s-maxage=3600, stale-while-revalidate=86400",
    });
    return { contributors: cache.contributors };
  }

  try {
    const response = await fetch(CONTRIBUTORS_URL, {
      headers: {
        Accept: "application/vnd.github+json",
        "User-Agent": "inkwell.ewancroft.uk",
        "X-GitHub-Api-Version": "2022-11-28",
      },
      signal: AbortSignal.timeout(5000),
    });

    if (!response.ok) {
      throw new Error(`GitHub contributors request failed: ${response.status}`);
    }

    const payload = (await response.json()) as GitHubContributor[];
    const contributors = payload.filter(isHumanContributor).map((contributor) => ({
      login: contributor.login,
      avatarUrl: avatarUrl(contributor.avatar_url),
      profileUrl: contributor.html_url,
      contributions: contributor.contributions ?? 0,
    }));

    cache = { at: Date.now(), contributors };
    setHeaders({
      "cache-control":
        "public, max-age=0, s-maxage=3600, stale-while-revalidate=86400",
    });

    return { contributors };
  } catch {
    setHeaders({
      "cache-control":
        "public, max-age=0, s-maxage=300, stale-while-revalidate=3600",
    });
    return { contributors: cache?.contributors ?? [] };
  }
};