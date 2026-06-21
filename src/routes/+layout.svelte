<script lang="ts">
  import "../routes/layout.css";
  import { SITE, NAV_LINKS } from "$lib/config";
  import { Menu, X } from "@lucide/svelte";

  let { children } = $props();
  let mobileOpen = $state(false);
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

<div class="flex min-h-screen flex-col">
  <!-- Header -->
  <header class="container flex items-center justify-between py-4">
    <a href="/" class="flex items-center gap-3 text-xl font-bold no-underline">
      <img src="/favicon.svg" alt="" class="h-8 w-8" />
      {SITE.title}
    </a>

    <!-- Desktop nav -->
    <nav class="hidden items-center gap-6 sm:flex">
      {#each NAV_LINKS as link}
        <a href={link.url} class="text-sm font-medium text-(--color-muted) transition-colors hover:text-(--color-ink) dark:hover:text-(--color-paper)">
          {link.label}
        </a>
      {/each}
    </nav>

    <!-- Mobile toggle -->
    <button
      class="sm:hidden"
      aria-label="Toggle menu"
      onclick={() => (mobileOpen = !mobileOpen)}
    >
      {#if mobileOpen}
        <X class="h-6 w-6" />
      {:else}
        <Menu class="h-6 w-6" />
      {/if}
    </button>
  </header>

  <!-- Mobile nav drawer -->
  {#if mobileOpen}
    <nav class="flex flex-col gap-3 border-b px-6 pb-4 sm:hidden">
      {#each NAV_LINKS as link}
        <a
          href={link.url}
          class="text-base font-medium text-(--color-muted)"
          onclick={() => (mobileOpen = false)}
        >
          {link.label}
        </a>
      {/each}
    </nav>
  {/if}

  <!-- Content -->
  <main class="flex-1">{@render children()}</main>

  <!-- Footer -->
  <footer class="container py-8 text-center text-sm text-(--color-muted)">
    <p>
      &copy; {new Date().getFullYear()} Inkwell. Built for the
      <a href="https://atproto.com" class="underline">AT Protocol</a>.
      <a href="https://standard.site" class="underline">Standard.site</a>.
    </p>
  </footer>
</div>
