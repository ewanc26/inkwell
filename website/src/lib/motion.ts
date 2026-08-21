// ── Scroll-triggered reveal ──────────────────────────────────────
// A Svelte action that reveals an element the first time it enters the
// viewport, rather than on page load. The distinction matters on the
// longer pages: a load-time animation on a section three folds down has
// already finished by the time it is scrolled to, which is the same as
// having no animation at all.
//
// The element is styled visible by default and only hidden while
// `js-motion` is on <html> (set pre-paint in app.html, and never set
// under prefers-reduced-motion), so a reveal can't strand content.

import type { Action } from "svelte/action";

// Delay steps are capped so a long grid doesn't trail on for seconds.
const MAX_STEP = 6;

export const reveal: Action<HTMLElement, number | undefined> = (
  node,
  index = 0,
) => {
  node.style.setProperty("--i", String(Math.min(index, MAX_STEP)));

  if (!document.documentElement.classList.contains("js-motion")) return;

  const observer = new IntersectionObserver(
    (entries) => {
      for (const entry of entries) {
        if (!entry.isIntersecting) continue;
        entry.target.classList.add("is-revealed");
        observer.unobserve(entry.target);
      }
    },
    // Fires slightly before the element is fully on screen, so the motion
    // reads as the section arriving rather than catching up.
    { rootMargin: "0px 0px -8% 0px", threshold: 0.15 },
  );

  observer.observe(node);

  return {
    destroy() {
      observer.disconnect();
    },
  };
};
