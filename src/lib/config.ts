// Site-wide configuration — mirrors the pattern from ewancroft.uk
export const SITE = {
  title: "Inkwell",
  description:
    "A native reader and writer for the Standard.site publishing ecosystem on AT Protocol. Read, discover, and publish portable writing from your own PDS.",
  url: "https://inkwell.app",
};

export const NAV_LINKS = [
  { label: "Home", url: "/" },
  { label: "Privacy", url: "/privacy" },
  { label: "Terms", url: "/terms" },
  { label: "Source", url: "https://github.com/ewanc26/inkwell" },
] as const;
