<!--
  Landing page — the product pitch, one page, one purpose.
  Hero wordmark -> download (AltStore / F-Droid) -> screenshots ->
  feature grid -> security callout -> what's new. No secondary
  navigation; every link either installs the app or leads to the
  privacy/terms that build trust.
-->

<script lang="ts">
  import { SITE, ALTSTORE_SOURCE_LINK, FDROID_REPO_LINK } from "$lib/config";
  import {
    BookOpen,
    Compass,
    PenLine,
    Shield,
    Globe,
    ArrowRight,
    Download,
    Apple,
    Smartphone,
  } from "@lucide/svelte";
</script>

<svelte:head>
  <title>{SITE.title} — Reader &amp; Writer for Standard.site on AT Protocol</title>
</svelte:head>

<!-- Hero -->
<section class="site-container page-hd hero-reveal">
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
    class="mb-8 h-20 w-auto sm:h-24"
    aria-label="Inkwell"
    role="img"
  >
    <rect x="40" y="40" width="320" height="80" rx="16" fill="currentColor" />
    <rect x="125" y="120" width="150" height="640" fill="currentColor" />
    <rect x="40" y="760" width="320" height="80" rx="16" fill="currentColor" />
    <circle cx="200" cy="880" r="32" fill="var(--color-accent, #64BB44)" />
  </svg>

  <h1 class="page-title page-title--hero">
    Read, discover, and publish
    <span class="text-accent">Standard.site</span>
    writing
  </h1>

  <p class="page-desc">
    Inkwell is a reader and writer for the Standard.site publishing ecosystem
    on AT Protocol. Available for iOS and Android. Your writing lives on your
    Personal Data Server — no silos, no lock-in.
  </p>

  <p class="page-meta" aria-label="Product metadata">
    <span>AT Protocol</span>
    <span>OAuth 2.1</span>
    <span>iOS &amp; Android</span>
  </p>

  <div class="flex flex-wrap items-center gap-4">
    <a href="#download" class="btn btn-primary active-press">
      <Download class="h-4 w-4" />
      Get Inkwell
    </a>
    <a href="https://github.com/ewanc26/inkwell" class="btn btn-outline active-press">
      <svg class="h-4 w-4" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
        <path d="M12 0C5.37 0 0 5.37 0 12c0 5.3 3.438 9.8 8.205 11.385.6.113.82-.258.82-.577 0-.285-.01-1.04-.015-2.04-3.338.724-4.042-1.61-4.042-1.61-.546-1.385-1.335-1.755-1.335-1.755-1.087-.744.084-.729.084-.729 1.205.084 1.838 1.236 1.838 1.236 1.07 1.835 2.809 1.305 3.495.998.108-.776.417-1.305.76-1.605-2.665-.3-5.466-1.332-5.466-5.93 0-1.31.465-2.38 1.235-3.22-.135-.303-.54-1.523.105-3.176 0 0 1.005-.322 3.3 1.23.96-.267 1.98-.399 3-.405 1.02.006 2.04.138 3 .405 2.28-1.552 3.285-1.23 3.285-1.23.645 1.653.24 2.873.12 3.176.765.84 1.23 1.91 1.23 3.22 0 4.61-2.805 5.625-5.475 5.92.42.36.81 1.096.81 2.22 0 1.605-.015 2.896-.015 3.286 0 .315.21.69.825.57C20.565 21.795 24 17.295 24 12 24 5.37 18.63 0 12 0z"/>
      </svg>
      View source
    </a>
  </div>

  <p class="mt-6 flex flex-wrap items-center gap-2" aria-label="Project badges">
    <a href="https://github.com/ewanc26/inkwell/releases/latest?q=ios-v">
      <img
        src="https://img.shields.io/github/v/release/ewanc26/inkwell?filter=ios-v*&label=iOS&logo=apple&logoColor=white"
        alt="Latest iOS release"
        height="20"
      />
    </a>
    <a href="https://github.com/ewanc26/inkwell/releases/latest?q=android-v">
      <img
        src="https://img.shields.io/github/v/release/ewanc26/inkwell?filter=android-v*&label=Android&logo=android&logoColor=white"
        alt="Latest Android release"
        height="20"
      />
    </a>
    <a href="https://github.com/ewanc26/inkwell/blob/main/LICENSE">
      <img
        src="https://img.shields.io/github/license/ewanc26/inkwell"
        alt="AGPL-3.0"
        height="20"
      />
    </a>
    <a href="https://github.com/sponsors/ewanc26">
      <img
        src="https://img.shields.io/github/sponsors/ewanc26?logo=githubsponsors&logoColor=white&label=sponsors"
        alt="Sponsor"
        height="20"
      />
    </a>
  </p>
</section>

<!-- Download -->
<section id="download" class="site-container scroll-mt-24 pb-12">
  <h2 class="section-title">
    Get Inkwell
  </h2>
  <p class="mb-12 max-w-[42rem] text-lg leading-relaxed text-pretty">
    No App Store, no Play Store — Inkwell installs straight from its own
    hosted sources, so updates ship the moment they&rsquo;re built.
  </p>

  <div class="feature-grid">
    <div class="feature-card animate-in stagger-1">
      <div class="feature-icon"><Apple class="h-5 w-5" /></div>
      <h3>iOS, via AltStore</h3>
      <p>
        Inkwell ships through its own AltStore source rather than the App
        Store. Add the source once and AltStore keeps the app updated
        over the air.
      </p>
      <a href={ALTSTORE_SOURCE_LINK} class="btn btn-primary active-press">
        <Download class="h-4 w-4" />
        Add AltStore source
      </a>
      <p class="mt-3 text-sm text-muted">
        This source works with
        <a href="https://altstore.io" class="text-accent underline">AltStore Classic</a> —
        the free, worldwide sideloading AltStore (requires a computer for the
        first install, apps refresh every 7 days). It will
        <em>not</em> work in AltStore PAL, which only installs Apple-notarized
        marketplace apps.
      </p>
    </div>

    <div class="feature-card animate-in stagger-1">
      <div class="feature-icon"><Smartphone class="h-5 w-5" /></div>
      <h3>Android, via F-Droid</h3>
      <p>
        The experimental Android port is built with Jetpack Compose and
        Material 3. Add this self-hosted repo in F-Droid, Droid-ify, or
        Obtainium to install it and receive updates.
      </p>
      <a href={FDROID_REPO_LINK} class="btn btn-primary active-press">
        <Download class="h-4 w-4" />
        Add F-Droid repo
      </a>
      <p class="mt-3 text-sm text-muted">
        Or open
        <a href="/fdroid/repo" class="text-accent underline">inkwell.ewancroft.uk/fdroid/repo</a>
        directly to browse it or scan its QR code. Source on
        <a href="https://github.com/ewanc26/inkwell" class="text-accent underline"
          >GitHub</a
        >.
      </p>
    </div>
  </div>
</section>

<!-- Screenshots -->
<section class="site-container py-12">
  <h2 class="section-title">
    See it in action
  </h2>
  <p class="mb-12 max-w-[42rem] text-lg leading-relaxed text-pretty">
    The same calm, three-tab workspace on iOS and Android — read,
    discover, and write, all from your own PDS.
  </p>

  <div class="mb-16">
    <h3 class="section-heading">iOS</h3>
    <div class="flex flex-wrap items-center justify-start gap-8">
      <figure class="animate-in stagger-1">
        <img
          src="/screenshots/ios/reader.png"
          alt="Inkwell reader on iOS showing a published document"
          class="h-72 w-auto screenshot-frame sm:h-80"
          width="428"
          height="930"
          loading="lazy"
        />
        <figcaption class="screenshot-caption">Read</figcaption>
      </figure>
      <figure class="animate-in stagger-2">
        <img
          src="/screenshots/ios/discover.png"
          alt="Inkwell discover on iOS showing search results"
          class="h-72 w-auto screenshot-frame sm:h-80"
          width="428"
          height="930"
          loading="lazy"
        />
        <figcaption class="screenshot-caption">Discover</figcaption>
      </figure>
      <figure class="animate-in stagger-3">
        <img
          src="/screenshots/ios/writer.png"
          alt="Inkwell writer on iOS showing the compose screen"
          class="h-72 w-auto screenshot-frame sm:h-80"
          width="428"
          height="930"
          loading="lazy"
        />
        <figcaption class="screenshot-caption">Write</figcaption>
      </figure>
      <figure class="animate-in stagger-4">
        <img
          src="/screenshots/ios/post.png"
          alt="A verified article open in Inkwell on iOS"
          class="h-72 w-auto screenshot-frame sm:h-80"
          width="428"
          height="930"
          loading="lazy"
        />
        <figcaption class="screenshot-caption">Article</figcaption>
      </figure>
    </div>
  </div>

  <div>
    <h3 class="section-heading">Android</h3>
    <div class="flex flex-wrap items-center justify-start gap-8">
      <figure class="animate-in stagger-1">
        <img
          src="/screenshots/android/reader.png"
          alt="Inkwell reader on Android showing a published document"
          class="h-72 w-auto screenshot-frame sm:h-80"
          width="412"
          height="915"
          loading="lazy"
        />
        <figcaption class="screenshot-caption">Read</figcaption>
      </figure>
      <figure class="animate-in stagger-2">
        <img
          src="/screenshots/android/discover.png"
          alt="Inkwell discover on Android showing search results"
          class="h-72 w-auto screenshot-frame sm:h-80"
          width="412"
          height="915"
          loading="lazy"
        />
        <figcaption class="screenshot-caption">Discover</figcaption>
      </figure>
      <figure class="animate-in stagger-3">
        <img
          src="/screenshots/android/writer.png"
          alt="Inkwell writer on Android showing the compose screen"
          class="h-72 w-auto screenshot-frame sm:h-80"
          width="412"
          height="915"
          loading="lazy"
        />
        <figcaption class="screenshot-caption">Write</figcaption>
      </figure>
      <figure class="animate-in stagger-4">
        <img
          src="/screenshots/android/post.png"
          alt="A verified article open in Inkwell on Android"
          class="h-72 w-auto screenshot-frame sm:h-80"
          width="412"
          height="915"
          loading="lazy"
        />
        <figcaption class="screenshot-caption">Article</figcaption>
      </figure>
    </div>
  </div>
</section>

<!-- Features -->
<section class="site-container pb-12">
  <h2 class="section-title">
    What Inkwell does
  </h2>

  <div class="feature-grid">
    <div class="feature-card animate-in stagger-1">
      <div class="feature-icon"><BookOpen class="h-5 w-5" /></div>
      <h3>Read</h3>
      <p>
        Browse publications and documents from across the Standard.site
        ecosystem. Renders Markpub Markdown, Leaflet pages, pckt blocks,
        and Offprint content — all from their owning PDS.
      </p>
    </div>

    <div class="feature-card animate-in stagger-1">
      <div class="feature-icon"><Compass class="h-5 w-5" /></div>
      <h3>Discover</h3>
      <p>
        Search the cross-platform public index and subscribe to publications.
        On iOS, background refresh can keep your feed current and notify you
        when new documents appear.
      </p>
    </div>

    <div class="feature-card animate-in stagger-1">
      <div class="feature-icon"><PenLine class="h-5 w-5" /></div>
      <h3>Write</h3>
      <p>
        Compose in Markdown and publish to your PDS in your choice of
        format — Markpub, Leaflet, pckt, or Offprint. Your writing, your
        data portability.
      </p>
    </div>

    <div class="feature-card animate-in stagger-2">
      <div class="feature-icon"><Globe class="h-5 w-5" /></div>
      <h3>AT Protocol Native</h3>
      <p>
        Sign in to your PDS with OAuth — no app passwords needed.
        Records live in your repository under your control.
      </p>
    </div>

    <div class="feature-card animate-in stagger-2">
      <div class="feature-icon"><Shield class="h-5 w-5" /></div>
      <h3>Verification Built In</h3>
      <p>
        Every publication and document can be verified against its
        .well-known endpoint and canonical &lt;link&gt; tags. Know your
        sources are authentic.
      </p>
    </div>

    <div class="feature-card animate-in stagger-2">
      <div class="feature-icon"><Apple class="h-5 w-5" /></div>
      <h3>iOS &amp; Android</h3>
      <p>
        The primary iOS app is built with SwiftUI and respects Dynamic Type,
        accessibility, and system theme. The Jetpack Compose Android port is
        experimental and does not yet include every iOS feature.
      </p>
    </div>
  </div>
</section>

<!-- Secure by design -->
<section class="site-container py-12">
  <div class="callout animate-in stagger-3">
    <h2>Secure by design</h2>
    <p>
      Inkwell uses OAuth 2.1 to sign in to your Personal Data Server.
      Your browser opens once to approve access — no password is ever
      seen or stored by the app. Tokens are DPoP-bound and held in the
      platform&rsquo;s secure store — Apple&rsquo;s Keychain on iOS,
      encrypted shared preferences on Android.
    </p>
    <a href="/privacy" class="active-press">
      Read the Privacy Policy <ArrowRight class="h-3 w-3" />
    </a>
  </div>
</section>

<!-- What's new -->
<section class="site-container py-12">
  <h2 class="section-title">Availability</h2>
  <div class="callout">
    <h3>iOS first, Android in progress</h3>
    <p class="text-pretty">
      Inkwell&rsquo;s iOS app is the primary implementation. The Android app is
      available as an experimental build for readers who would like to follow
      its development; its writer, detail, comment, and notification features
      are still catching up.
    </p>
  </div>
</section>
