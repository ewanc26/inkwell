<!--
  Root layout — the app shell that wraps every page.
  Renders the sticky header with desktop + mobile nav, the main content
  slot, and the footer.  Mobile breakpoint is at 800px; the mobile menu
  is an in-flow dropdown panel under the header, matching ewancroft.uk.
-->

<script lang="ts">
  import "../routes/layout.css";
  import { SITE, NAV_LINKS, OG_IMAGE } from "$lib/config";
  import { page } from "$app/state";
  import { Menu, X } from "@lucide/svelte";

  let { children } = $props();
  let mobileOpen = $state(false);
  let toggleEl = $state<HTMLButtonElement | null>(null);

  // Dismissing the dropdown returns focus to the control that opened it,
  // otherwise Escape/backdrop dismissal drops focus onto <body>.
  function closeMobile(restoreFocus = false) {
    if (mobileOpen && restoreFocus) toggleEl?.focus();
    mobileOpen = false;
  }

  // Dismiss mobile nav on Escape, matching native sheet behaviour
  function onKeydown(e: KeyboardEvent) {
    if (e.key === "Escape") closeMobile(true);
  }

  // The dropdown's links close it on click, but history navigation
  // (back/forward) would otherwise leave it open over the new route.
  $effect(() => {
    page.url.pathname;
    mobileOpen = false;
  });

  // Absolute, per-route URL for canonical + og:url. Built from the
  // configured origin so previews/localhost never leak into metadata.
  const canonical = $derived(new URL(page.url.pathname, SITE.url).href);

  // Scrapers won't resolve a root-relative image path, so the cover is
  // advertised absolutely — same reasoning as og:url above.
  const ogImage = new URL(OG_IMAGE.path, SITE.url).href;
</script>

<svelte:head>
  <title>{SITE.title}</title>
  <meta name="description" content={SITE.description} />
  <link rel="canonical" href={canonical} />
  <meta property="og:site_name" content={SITE.title} />
  <meta property="og:title" content={SITE.title} />
  <meta property="og:description" content={SITE.description} />
  <meta property="og:type" content="website" />
  <meta property="og:url" content={canonical} />
  <meta property="og:locale" content="en_GB" />
  <meta property="og:image" content={ogImage} />
  <meta property="og:image:type" content={OG_IMAGE.type} />
  <meta property="og:image:width" content={String(OG_IMAGE.width)} />
  <meta property="og:image:height" content={String(OG_IMAGE.height)} />
  <meta property="og:image:alt" content={OG_IMAGE.alt} />
  <!-- summary_large_image, not summary: with a cover this wide, the small
       card would centre-crop the mark out of the frame. -->
  <meta name="twitter:card" content="summary_large_image" />
  <meta name="twitter:title" content={SITE.title} />
  <meta name="twitter:description" content={SITE.description} />
  <meta name="twitter:image" content={ogImage} />
  <meta name="twitter:image:alt" content={OG_IMAGE.alt} />
  <link rel="icon" href="/favicon.svg" type="image/svg+xml" />
  <link rel="icon" href="/favicon.ico" sizes="48x48" type="image/x-icon" />
  <link rel="icon" href="/favicon-16x16.png" sizes="16x16" type="image/png" />
  <link rel="icon" href="/favicon-32x32.png" sizes="32x32" type="image/png" />
  <link rel="apple-touch-icon" href="/apple-touch-icon.png" sizes="180x180" />
  <link rel="icon" href="/android-chrome-192x192.png" sizes="192x192" type="image/png" />
  <link rel="icon" href="/android-chrome-512x512.png" sizes="512x512" type="image/png" />
</svelte:head>

<svelte:window onkeydown={onKeydown} />

<a class="skip-to-content" href="#main-content">Skip to content</a>

<!-- Header -->
<nav class="nav" aria-label="Primary navigation">
  <div class="nav-inner">
    <a href="/" class="nav-brand" aria-label="{SITE.title}, home">
      <!--
        Inline SVG: currentColor & var(--color-accent) resolve because it
        lives in the page DOM, unlike an <img> src. The mark uses the same
        centred 952x952 geometry as the favicon so its proportions stay
        consistent everywhere.
      -->
      <svg
        xmlns="http://www.w3.org/2000/svg"
        viewBox="0 0 952 952"
        fill="none"
        class="nav-logo"
        aria-hidden="true"
      >
        <g transform="translate(276 0)">
          <rect x="40" y="40" width="320" height="80" rx="16" fill="currentColor" />
          <rect x="125" y="120" width="150" height="640" fill="currentColor" />
          <rect x="40" y="760" width="320" height="80" rx="16" fill="currentColor" />
          <circle cx="200" cy="880" r="32" fill="var(--color-accent, #64BB44)" />
        </g>
      </svg>
      {SITE.title}
    </a>

    <button
      bind:this={toggleEl}
      class="menu-toggle"
      aria-label={mobileOpen ? "Close menu" : "Open menu"}
      aria-expanded={mobileOpen}
      aria-controls="primary-navigation"
      onclick={() => (mobileOpen = !mobileOpen)}
    >
      {#if mobileOpen}
        <X aria-hidden="true" />
      {:else}
        <Menu aria-hidden="true" />
      {/if}
    </button>

    <!-- Desktop nav + mobile dropdown panel -->
    <nav class="nav-links" class:open={mobileOpen} id="primary-navigation" aria-label="Main navigation">
      {#each NAV_LINKS as link}
        {@const isActive =
          page.url.pathname === link.url ||
          (link.url !== "/" && link.url.startsWith("/") && page.url.pathname.startsWith(link.url))}
        <a
          href={link.url}
          class="nav-link"
          class:active={isActive}
          aria-current={isActive ? "page" : undefined}
          onclick={() => closeMobile()}
        >
          {link.label}
        </a>
      {/each}
    </nav>
  </div>
</nav>

<!-- Content -->
<main id="main-content" tabindex="-1">{@render children()}</main>

<!-- Footer -->
<footer class="site-footer">
  <div class="footer-inner">
    <div class="footer-bottom">
      <p class="footer-copyright">
        &copy; {new Date().getFullYear()} Inkwell — a reader &amp; writer for
        <a href="https://standard.site" class="underline">Standard.site</a> on the
        <a href="https://atproto.com" class="underline">AT Protocol</a>
      </p>
      <nav class="footer-nav" aria-label="Footer navigation">
        <a href="/privacy" class="footer-link">Privacy</a>
        <a href="/terms" class="footer-link">Terms</a>
        <a href="https://github.com/ewanc26/inkwell" class="footer-link" target="_blank" rel="noopener">GitHub</a>
        <a href="https://ko-fi.com/ewancroft" class="footer-link" target="_blank" rel="noopener">Ko-fi</a>
        <a href="https://github.com/sponsors/ewanc26" class="footer-link" target="_blank" rel="noopener">GitHub Sponsors</a>
      </nav>
    </div>
  </div>
</footer>
