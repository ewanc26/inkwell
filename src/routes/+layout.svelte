<!--
  Root layout — the app shell that wraps every page.
  Renders the sticky header with desktop + mobile nav, the main content
  slot, and the footer.  Responsive breakpoint is at 640px.
-->

<script lang="ts">
  import "../routes/layout.css";
  import { SITE, NAV_LINKS } from "$lib/config";
  import { page } from "$app/state";
  import { Menu, X } from "@lucide/svelte";
  import { fade, slide } from "svelte/transition";

  let { children } = $props();
  let mobileOpen = $state(false);
  let toggleEl = $state<HTMLButtonElement | null>(null);

  // Dismissing the drawer returns focus to the control that opened it,
  // otherwise Escape/backdrop dismissal drops focus onto <body>.
  function closeMobile(restoreFocus = false) {
    if (mobileOpen && restoreFocus) toggleEl?.focus();
    mobileOpen = false;
  }

  // Dismiss mobile nav on Escape, matching native sheet behaviour
  function onKeydown(e: KeyboardEvent) {
    if (e.key === "Escape") closeMobile(true);
  }

  // The drawer's links close it on click, but history navigation
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

<div class="flex min-h-screen flex-col">
  <!-- Header -->
  <header class="site-header">
    <div class="container">
      <a href="/" class="nav-brand">
        <svg
          xmlns="http://www.w3.org/2000/svg"
          viewBox="0 0 512 512"
          fill="none"
          class="nav-icon"
          aria-hidden="true"
        >
          <!-- Inline SVG: currentColor & var(--color-accent) resolve
               because it lives in the page DOM, unlike an <img> src. -->
          <rect x="108" y="64" width="296" height="44" rx="16" fill="currentColor" />
          <rect x="189" y="108" width="134" height="290" fill="currentColor" />
          <rect x="108" y="398" width="296" height="44" rx="16" fill="currentColor" />
          <circle cx="256" cy="468" r="20" fill="var(--color-accent, #139500)" />
        </svg>
        {SITE.title}
      </a>

      <!-- Desktop nav -->
      <nav class="nav-links" aria-label="Main navigation">
        {#each NAV_LINKS as link}
          <a
            href={link.url}
            aria-current={page.url.pathname === link.url ? "page" : undefined}
            class="nav-link"
          >
            {link.label}
          </a>
        {/each}
      </nav>

      <!-- Mobile toggle — 44x44 tap target meets Apple HIG minimum -->
      <button
        bind:this={toggleEl}
        class="nav-toggle"
        aria-label={mobileOpen ? "Close menu" : "Open menu"}
        aria-expanded={mobileOpen}
        aria-controls="mobile-nav"
        onclick={() => (mobileOpen = !mobileOpen)}
      >
        {#if mobileOpen}
          <X class="h-6 w-6" />
        {:else}
          <Menu class="h-6 w-6" />
        {/if}
      </button>
    </div>
  </header>

  <!-- Mobile nav backdrop -->
  {#if mobileOpen}
    <button
      class="nav-backdrop"
      aria-label="Close menu"
      onclick={() => closeMobile(true)}
      transition:fade={{ duration: 150 }}
    ></button>
  {/if}

  <!-- Mobile nav drawer -->
  {#if mobileOpen}
    <nav
      id="mobile-nav"
      class="nav-mobile"
      aria-label="Mobile navigation"
      transition:slide={{ duration: 200 }}
    >
      {#each NAV_LINKS as link}
        <a
          href={link.url}
          aria-current={page.url.pathname === link.url ? "page" : undefined}
          onclick={() => closeMobile()}
        >
          {link.label}
        </a>
      {/each}
    </nav>
  {/if}

  <!-- Content -->
  <main id="main-content" class="flex-1">{@render children()}</main>

  <!-- Footer -->
  <footer class="site-footer">
    <div class="container">
      <p>
        &copy; {new Date().getFullYear()} Inkwell. Built for the
        <a href="https://atproto.com" class="underline">AT Protocol</a>
        &middot;
        <a href="https://standard.site" class="underline">Standard.site</a>
      </p>
      <nav class="footer-links" aria-label="Footer links">
        <a href="/privacy">Privacy</a>
        <a href="/terms">Terms</a>
        <a href="https://github.com/ewanc26/inkwell">GitHub</a>
        <a href="https://ko-fi.com/ewancroft">Ko-fi</a>
        <a href="https://github.com/sponsors/ewanc26">GitHub Sponsors</a>
      </nav>
    </div>
  </footer>
</div>
