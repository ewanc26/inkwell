<script lang="ts">
  import "../routes/layout.css";
  import { SITE, NAV_LINKS } from "$lib/config";
  import { page } from "$app/state";
  import { Menu, X } from "@lucide/svelte";
  import { slide } from "svelte/transition";

  let { children } = $props();
  let mobileOpen = $state(false);

  function closeMobile() {
    mobileOpen = false;
  }

  function onKeydown(e: KeyboardEvent) {
    if (e.key === "Escape") closeMobile();
  }
</script>

<svelte:head>
  <title>{SITE.title}</title>
  <meta name="description" content={SITE.description} />
  <meta property="og:title" content={SITE.title} />
  <meta property="og:description" content={SITE.description} />
  <meta property="og:type" content="website" />
  <meta property="og:url" content={SITE.url} />
  <meta name="twitter:card" content="summary" />
  <link rel="icon" href="/favicon.svg" type="image/svg+xml" />
</svelte:head>

<svelte:window onkeydown={onKeydown} />

<a class="skip-to-content" href="#main-content">Skip to content</a>

<div class="flex min-h-screen flex-col">
  <!-- Header -->
  <header class="site-header">
    <div class="container">
      <a href="/" class="nav-brand">
        <img src="/favicon.svg" alt="" />
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

      <!-- Mobile toggle -->
      <button
        class="nav-toggle"
        aria-label="Toggle menu"
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
      onclick={closeMobile}
      transition:slide={{ duration: 150 }}
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
          onclick={closeMobile}
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
      </nav>
    </div>
  </footer>
</div>
