<!--
  Landing page — the top-of-funnel pitch. Hero wordmark -> download
  (AltStore / F-Droid) -> screenshots -> compressed feature teaser ->
  security teaser -> comparison -> FAQ -> availability -> final CTA.
  Deeper content now lives on /features, /security, and /about; this
  page links out rather than cramming everything into one scroll.

  All copy comes from the message catalogue for `data.locale`; the
  <title>/description live in the root layout's route-metadata table.
-->

<script lang="ts">
  import { reveal } from "$lib/motion";
  import {
    ALTSTORE_SOURCE_LINK,
    FDROID_REPO_LINK,
    APP_STORE_PLACEHOLDER_LINK,
    PLAY_STORE_PLACEHOLDER_LINK,
  } from "$lib/config";
  import {
    fmt,
    localizeHref,
    messagesFor,
    rich,
    splitAt,
  } from "$lib/i18n";
  import UserCarousel from "$lib/components/UserCarousel.svelte";
  import type { PageData } from "./$types";

  let { data }: { data: PageData } = $props();

  const locale = $derived(data.locale);
  const m = $derived(messagesFor(locale).home);
  const shared = $derived(messagesFor(locale));

  // The hero heading wraps its brand name in an accent <span>, so the
  // translated string is split around the `{brand}` placeholder rather
  // than being glued together from fragments in English word order.
  const heroTitle = $derived(splitAt(m.hero.title, "brand"));

  // Screenshot grid: `dir`/`file` address the committed PNGs under
  // static/screenshots and are never localised; `key` indexes the
  // caption/alt catalogues.
  const PLATFORMS = [
    { dir: "ios", width: 428, height: 930 },
    { dir: "android", width: 412, height: 915 },
  ] as const;
  const SHOTS = [
    { key: "read", file: "reader" },
    { key: "discover", file: "discover" },
    { key: "write", file: "writer" },
    { key: "article", file: "post" },
  ] as const;

  import {
    BookOpen,
    PenLine,
    Shield,
    ArrowRight,
    Download,
    Apple,
    Smartphone,
  } from "@lucide/svelte";
</script>

<!-- Hero -->
<section class="site-container page-hd">
  <!--
    Wordmark SVG — mirrors InkwellMark.swift in the iOS app.
    The capsule shapes and ink-drop circle are the app icon's
    defining forms, rendered here as an inline <svg> so
    currentColor and var(--color-accent) resolve in the page DOM.
  -->
  <svg
    xmlns="http://www.w3.org/2000/svg"
    viewBox="0 0 400 952"
    fill="none"
    class="hero-mark mb-8 h-20 w-auto sm:h-24"
    aria-label={m.markLabel}
    role="img"
  >
    <rect x="40" y="40" width="320" height="80" rx="16" fill="currentColor" />
    <rect x="125" y="120" width="150" height="640" fill="currentColor" />
    <rect x="40" y="760" width="320" height="80" rx="16" fill="currentColor" />
    <circle
      class="ink-drop"
      cx="200"
      cy="880"
      r="32"
      fill="var(--color-accent, #64BB44)"
    />
  </svg>

  <h1 class="page-title page-title--hero hero-item" style="--i: 0">
    {heroTitle[0]}<span class="text-accent">{m.hero.brand}</span>{heroTitle[1]}
  </h1>

  <p class="page-desc hero-item" style="--i: 1">{m.hero.description}</p>

  <div class="page-meta hero-item" style="--i: 2" role="group" aria-label={m.hero.metaLabel}>
    {#each m.hero.meta as item (item)}
      <span>{item}</span>
    {/each}
  </div>

  <div class="hero-item flex flex-wrap items-center gap-4" style="--i: 3">
    <a href="#download" class="btn btn-primary active-press">
      <Download class="h-4 w-4" />
      {m.hero.cta}
    </a>
    <a href="https://github.com/ewanc26/inkwell" class="btn btn-outline active-press">
      <svg class="h-4 w-4" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
        <path d="M12 0C5.37 0 0 5.37 0 12c0 5.3 3.438 9.8 8.205 11.385.6.113.82-.258.82-.577 0-.285-.01-1.04-.015-2.04-3.338.724-4.042-1.61-4.042-1.61-.546-1.385-1.335-1.755-1.335-1.755-1.087-.744.084-.729.084-.729 1.205.084 1.838 1.236 1.838 1.236 1.07 1.835 2.809 1.305 3.495.998.108-.776.417-1.305.76-1.605-2.665-.3-5.466-1.332-5.466-5.93 0-1.31.465-2.38 1.235-3.22-.135-.303-.54-1.523.105-3.176 0 0 1.005-.322 3.3 1.23.96-.267 1.98-.399 3-.405 1.02.006 2.04.138 3 .405 2.28-1.552 3.285-1.23 3.285-1.23.645 1.653.24 2.873.12 3.176.765.84 1.23 1.91 1.23 3.22 0 4.61-2.805 5.625-5.475 5.92.42.36.81 1.096.81 2.22 0 1.605-.015 2.896-.015 3.286 0 .315.21.69.825.57C20.565 21.795 24 17.295 24 12 24 5.37 18.63 0 12 0z"/>
      </svg>
      {m.hero.viewSource}
    </a>
  </div>

  <div
    class="hero-item mt-6 flex flex-wrap items-center gap-2"
    style="--i: 4"
    role="group"
    aria-label={m.hero.badgesLabel}
  >
    <a href="https://github.com/ewanc26/inkwell/releases/latest">
      <img
        src="https://img.shields.io/github/v/release/ewanc26/inkwell?label=iOS&logo=apple&logoColor=white"
        alt={m.hero.badgeAlt.ios}
        height="20"
      />
    </a>
    <a href="https://github.com/ewanc26/inkwell/releases/latest">
      <img
        src="https://img.shields.io/github/v/release/ewanc26/inkwell?label=Android&logo=android&logoColor=white"
        alt={m.hero.badgeAlt.android}
        height="20"
      />
    </a>
    <a href="https://github.com/ewanc26/inkwell/blob/main/LICENSE">
      <img
        src="https://img.shields.io/github/license/ewanc26/inkwell"
        alt={m.hero.badgeAlt.licence}
        height="20"
      />
    </a>
    <a href="https://github.com/sponsors/ewanc26">
      <img
        src="https://img.shields.io/github/sponsors/ewanc26?logo=githubsponsors&logoColor=white&label=sponsors"
        alt={m.hero.badgeAlt.sponsor}
        height="20"
      />
    </a>
  </div>
</section>

<!-- Download -->
<section id="download" class="site-container scroll-mt-24 pb-12">
  <h2 class="section-title">{m.download.heading}</h2>
  <p class="mb-12 max-w-[42rem] text-lg leading-relaxed text-pretty">
    {m.download.intro}
  </p>

  <div class="feature-grid">
    <div class="feature-card reveal" use:reveal={0}>
      <div class="feature-icon"><Apple class="h-5 w-5" /></div>
      <h3>{m.download.ios.heading}</h3>
      <p>{m.download.ios.body}</p>
      <div class="flex flex-wrap gap-3">
        <a href={ALTSTORE_SOURCE_LINK} class="btn btn-primary active-press">
          <Download class="h-4 w-4" />
          {m.download.ios.addSource}
        </a>
        <a
          href={APP_STORE_PLACEHOLDER_LINK}
          class="btn btn-outline active-press"
          aria-label={m.download.ios.storeAria}
        >
          <Apple class="h-4 w-4" />
          {m.download.ios.store}
        </a>
      </div>
      <p class="mt-3 text-sm text-muted">{@html rich(m.download.ios.note, locale)}</p>
    </div>

    <div class="feature-card reveal" use:reveal={0}>
      <div class="feature-icon"><Smartphone class="h-5 w-5" /></div>
      <h3>{m.download.android.heading}</h3>
      <p>{m.download.android.body}</p>
      <div class="flex flex-wrap gap-3">
        <a href={FDROID_REPO_LINK} class="btn btn-primary active-press">
          <Download class="h-4 w-4" />
          {m.download.android.addSource}
        </a>
        <a
          href={PLAY_STORE_PLACEHOLDER_LINK}
          class="btn btn-outline active-press"
          aria-label={m.download.android.storeAria}
        >
          <Smartphone class="h-4 w-4" />
          {m.download.android.store}
        </a>
      </div>
      <p class="mt-3 text-sm text-muted">
        {@html rich(m.download.android.note, locale)}
      </p>
    </div>
  </div>
</section>

<!-- Screenshots -->
<section class="site-container py-12">
  <h2 class="section-title">{m.screenshots.heading}</h2>
  <p class="mb-12 max-w-[42rem] text-lg leading-relaxed text-pretty">
    {m.screenshots.intro}
  </p>

  {#each PLATFORMS as platform, platformIndex (platform.dir)}
    {@const label = m.screenshots.platform[platform.dir]}
    <div class={platformIndex < PLATFORMS.length - 1 ? "mb-16" : ""}>
      <h3 class="section-heading">{label}</h3>
      <div class="flex flex-wrap items-center justify-start gap-8">
        {#each SHOTS as shot, index (shot.key)}
          <figure class="reveal" use:reveal={index}>
            <img
              src={`/screenshots/${platform.dir}/${shot.file}.png`}
              alt={fmt(m.screenshots.alt[shot.key], { platform: label })}
              class="h-72 w-auto screenshot-frame sm:h-80"
              width={platform.width}
              height={platform.height}
              loading="lazy"
            />
            <figcaption class="screenshot-caption">
              {m.screenshots.caption[shot.key]}
            </figcaption>
          </figure>
        {/each}
      </div>
    </div>
  {/each}
</section>

<!-- Features (compressed) -->
<section class="site-container pb-12">
  <h2 class="section-title">{m.what.heading}</h2>

  <div class="feature-grid">
    <div class="feature-card reveal" use:reveal={0}>
      <div class="feature-icon"><BookOpen class="h-5 w-5" /></div>
      <h3>{m.what.read.heading}</h3>
      <p>{m.what.read.body}</p>
    </div>

    <div class="feature-card reveal" use:reveal={0}>
      <div class="feature-icon"><PenLine class="h-5 w-5" /></div>
      <h3>{m.what.write.heading}</h3>
      <p>{m.what.write.body}</p>
    </div>

    <div class="feature-card reveal" use:reveal={0}>
      <div class="feature-icon"><Shield class="h-5 w-5" /></div>
      <h3>{m.what.verified.heading}</h3>
      <p>{@html rich(m.what.verified.body, locale)}</p>
    </div>
  </div>

  <a href={localizeHref("/features", locale)} class="section-link">{m.what.link}</a>
</section>

<!-- Secure by design (teaser) -->
<section class="site-container py-12">
  <div class="callout reveal" use:reveal={2}>
    <h2>{m.secure.heading}</h2>
    <p>{m.secure.body}</p>
    <a href={localizeHref("/security", locale)} class="active-press">
      {m.secure.link} <ArrowRight class="h-3 w-3" />
    </a>
  </div>
</section>

<!-- Why not just the browser -->
<section class="site-container py-12">
  <h2 class="section-title">{m.compare.heading}</h2>
  <p class="mb-6 max-w-[42rem] text-lg leading-relaxed text-pretty">
    {m.compare.intro}
  </p>
  <dl class="compare">
    <div class="compare-head" aria-hidden="true">
      <span></span>
      <span>{m.compare.columnBrowser}</span>
      <span>{m.compare.columnApp}</span>
    </div>
    {#each m.compare.rows as row (row.label)}
      <div class="compare-row">
        <dt>{row.label}</dt>
        <dd class="compare-web">
          <span class="compare-label">{m.compare.columnBrowser}</span>
          {@html rich(row.browser, locale)}
        </dd>
        <dd class="compare-app">
          <span class="compare-label">{m.compare.columnApp}</span>
          {@html rich(row.app, locale)}
        </dd>
      </div>
    {/each}
  </dl>
</section>

<!-- FAQ -->
<section class="site-container py-12">
  <h2 class="section-title">{m.faq.heading}</h2>
  <div class="faq-list">
    {#each m.faq.items as item (item.question)}
      <details class="faq-item">
        <summary>{item.question}</summary>
        <p>{@html rich(item.answer, locale)}</p>
      </details>
    {/each}
  </div>
</section>

<!-- Availability -->
<section class="site-container py-12">
  <h2 class="section-title">{m.availability.heading}</h2>
  <div class="callout">
    <h3>{m.availability.cardHeading}</h3>
    <p class="text-pretty">{m.availability.body}</p>
  </div>
</section>

<!-- Users already using Inkwell -->
<UserCarousel users={data.users} messages={shared} {locale} />

<!-- Final CTA -->
<section class="site-container pb-16 text-center">
  <a href="#download" class="btn btn-primary active-press">
    <Download class="h-4 w-4" />
    {m.finalCta}
  </a>
</section>
