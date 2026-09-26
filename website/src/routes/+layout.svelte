<!--
  Root layout — the app shell that wraps every page.
  Renders the sticky header with desktop + mobile nav, the main content
  slot, and the footer.  Mobile breakpoint is at 800px; the mobile menu
  is an in-flow dropdown panel under the header, matching ewancroft.uk.

  This is also where localisation is resolved for the shell: the active
  locale is derived from the URL (never from Accept-Language), and every
  <title>, description, canonical URL, and hreflang alternate on the site
  is emitted from here rather than per route, so metadata localises in one
  place. Pages own their own body copy only.
-->

<script lang="ts">
  import "../routes/layout.css";
  import { SITE, NAV_LINKS, FOOTER_LINKS, OG_IMAGE } from "$lib/config";
  import {
    LOCALES,
    absoluteUrl,
    alternatesFor,
    fmt,
    isLocalisedPath,
    localizeHref,
    messagesFor,
    resolveLocale,
    rich,
    routeMeta,
    xDefaultFor,
  } from "$lib/i18n";
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

  // ── Locale ───────────────────────────────────────────────────
  // `path` is the source-locale path (`/fr/privacy` -> `/privacy`), which
  // is the key route metadata, nav matching, and alternates all work in.
  const resolved = $derived(resolveLocale(page.url.pathname));
  const locale = $derived(resolved.locale);
  const path = $derived(resolved.path);
  const m = $derived(messagesFor(locale));
  const meta = $derived(routeMeta(m, path));

  // Absolute, per-route URL for canonical + og:url, in the active locale.
  // Built from the configured origin so previews/localhost never leak
  // into metadata.
  const canonical = $derived(absoluteUrl(path, locale));

  // Alternates are only advertised for paths that genuinely exist in
  // every locale; a 404 or a static artefact rendered through the shell
  // must not claim translations it doesn't have.
  const translated = $derived(isLocalisedPath(path));
  const alternates = $derived(translated ? alternatesFor(path) : []);

  // Scrapers won't resolve a root-relative image path, so the cover is
  // advertised absolutely — same reasoning as og:url above.
  const ogImage = new URL(OG_IMAGE.path, SITE.url).href;
</script>

<svelte:head>
  <title>{meta.title}</title>
  <meta name="description" content={meta.description} />
  <link rel="canonical" href={canonical} />
  {#each alternates as alternate (alternate.locale)}
    <link rel="alternate" hreflang={alternate.hreflang} href={alternate.href} />
  {/each}
  {#if translated}
    <!-- British English is the source locale, so it is also the fallback
         a crawler should offer when it can't match the user's language. -->
    <link rel="alternate" hreflang="x-default" href={xDefaultFor(path)} />
  {/if}
  <meta property="og:site_name" content={SITE.title} />
  <meta property="og:title" content={meta.title} />
  <meta property="og:description" content={meta.description} />
  <meta property="og:type" content="website" />
  <meta property="og:url" content={canonical} />
  <meta property="og:locale" content={LOCALES[locale].ogLocale} />
  {#each alternates as alternate (alternate.locale)}
    {#if alternate.locale !== locale}
      <meta
        property="og:locale:alternate"
        content={LOCALES[alternate.locale].ogLocale}
      />
    {/if}
  {/each}
  <meta property="og:image" content={ogImage} />
  <meta property="og:image:type" content={OG_IMAGE.type} />
  <meta property="og:image:width" content={String(OG_IMAGE.width)} />
  <meta property="og:image:height" content={String(OG_IMAGE.height)} />
  <meta property="og:image:alt" content={OG_IMAGE.alt} />
  <!-- summary_large_image, not summary: with a cover this wide, the small
       card would centre-crop the mark out of the frame. -->
  <meta name="twitter:card" content="summary_large_image" />
  <meta name="twitter:title" content={meta.title} />
  <meta name="twitter:description" content={meta.description} />
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

<a class="skip-to-content" href="#main-content">{m.nav.skipToContent}</a>

<!-- Header -->
<nav class="nav" aria-label={m.nav.primaryLabel}>
  <div class="nav-inner">
    <a href={localizeHref("/", locale)} class="nav-brand" aria-label={m.nav.brandHome}>
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
      aria-label={mobileOpen ? m.nav.closeMenu : m.nav.openMenu}
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
    <nav class="nav-links" class:open={mobileOpen} id="primary-navigation" aria-label={m.nav.mainLabel}>
      {#each NAV_LINKS as link (link.key)}
        <!-- Active state is compared against the source-locale path, so it
             works identically at /features and /fr/features. -->
        {@const isActive =
          path === link.url ||
          (link.url !== "/" && link.url.startsWith("/") && path.startsWith(link.url))}
        <a
          href={localizeHref(link.url, locale)}
          class="nav-link"
          class:active={isActive}
          aria-current={isActive ? "page" : undefined}
          onclick={() => closeMobile()}
        >
          {m.nav.links[link.key]}
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
        {@html rich(
          fmt(m.footer.copyright, { year: new Date().getFullYear() }),
          locale,
        )}
      </p>
      <nav class="footer-nav" aria-label={m.footer.navLabel}>
        {#each FOOTER_LINKS as link (link.key)}
          <a
            href={localizeHref(link.url, locale)}
            class="footer-link"
            target={link.external ? "_blank" : undefined}
            rel={link.external ? "noopener" : undefined}
          >
            {m.footer.links[link.key]}
          </a>
        {/each}
      </nav>
    </div>

    <!--
      Language switcher. Plain links, no JavaScript and no cookie: each
      locale has exactly one address for the current page, so switching
      language is ordinary navigation. Hidden entirely on pages that have
      no translated counterpart rather than linking to a 404.
    -->
    {#if alternates.length > 1}
      <nav class="footer-langs" aria-label={m.footer.languageLabel}>
        <span class="footer-langs-label">{m.footer.languageLabel}</span>
        {#each alternates as alternate (alternate.locale)}
          <a
            href={localizeHref(path, alternate.locale)}
            class="footer-link"
            hreflang={alternate.hreflang}
            lang={alternate.hreflang}
            aria-current={alternate.locale === locale ? "true" : undefined}
          >
            {LOCALES[alternate.locale].label}
          </a>
        {/each}
      </nav>
    {/if}
  </div>
</footer>
