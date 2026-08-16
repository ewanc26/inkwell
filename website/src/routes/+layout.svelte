<!--
  Root layout — the app shell that wraps every page.
  Renders the sticky header with desktop + mobile nav, the main content
  slot, and the footer.  Mobile breakpoint is at 800px; the mobile menu
  is an in-flow dropdown panel under the header, matching ewancroft.uk.
-->

<script lang="ts">
  import "../routes/layout.css";
  import { SITE, NAV_LINKS } from "$lib/config";
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
  <meta name="twitter:card" content="summary" />
  <meta name="twitter:title" content={SITE.title} />
  <meta name="twitter:description" content={SITE.description} />
  <link rel="icon" href="/favicon.svg" type="image/svg+xml" />
</svelte:head>

<svelte:window onkeydown={onKeydown} />

<a class="skip-to-content" href="#main-content">Skip to content</a>

<!-- Header -->
<nav class="nav" aria-label="Primary navigation">
  <div class="nav-inner">
    <a href="/" class="nav-brand" aria-label="{SITE.title}, home">
      <!--
        Inline SVG: currentColor & var(--color-accent) resolve because it
        lives in the page DOM, unlike an <img> src. The capsule shapes and
        ink-drop circle are the app icon's defining forms.
      -->
      <svg
        xmlns="http://www.w3.org/2000/svg"
        viewBox="0 0 512 512"
        fill="none"
        class="nav-logo"
        aria-hidden="true"
      >
        <rect x="108" y="64" width="296" height="44" rx="16" fill="currentColor" />
        <rect x="189" y="108" width="134" height="290" fill="currentColor" />
        <rect x="108" y="398" width="296" height="44" rx="16" fill="currentColor" />
        <circle cx="256" cy="468" r="20" fill="var(--color-accent, #64BB44)" />
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
