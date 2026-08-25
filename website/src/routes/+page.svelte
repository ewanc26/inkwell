<!--
  Landing page — the top-of-funnel pitch. Hero wordmark -> download
  (AltStore / F-Droid) -> screenshots -> compressed feature teaser ->
  security teaser -> comparison -> FAQ -> availability -> final CTA.
  Deeper content now lives on /features, /security, and /about; this
  page links out rather than cramming everything into one scroll.
-->

<script lang="ts">
  import { reveal } from "$lib/motion";
  import {
    SITE,
    ALTSTORE_SOURCE_LINK,
    FDROID_REPO_LINK,
    APP_STORE_PLACEHOLDER_LINK,
    PLAY_STORE_PLACEHOLDER_LINK,
  } from "$lib/config";
  import type { PageData } from "./$types";

  let data: any = {};
  import {
    BookOpen,
    Compass,
    PenLine,
    Shield,
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
    aria-label="Inkwell"
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
    Read, discover, and publish
    <span class="text-accent">Standard.site</span>
    writing
  </h1>

  <p class="page-desc hero-item" style="--i: 1">
    Inkwell is a reader and writer for the Standard.site publishing ecosystem
    on AT Protocol. Available for iOS and Android. Your writing lives on your
    Personal Data Server — no silos, no lock-in.
  </p>

  <p class="page-meta hero-item" style="--i: 2" aria-label="Product metadata">
    <span>AT Protocol</span>
    <span>OAuth 2.1</span>
    <span>iOS &amp; Android</span>
  </p>

  <div class="hero-item flex flex-wrap items-center gap-4" style="--i: 3">
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

  <p
    class="hero-item mt-6 flex flex-wrap items-center gap-2"
    style="--i: 4"
    aria-label="Project badges"
  >
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
    Free direct installs are available now through AltStore Classic and F-Droid.
    £5 App Store and Google Play builds are planned; the store links below are
    placeholders until those listings go live.
  </p>

  <div class="feature-grid">
    <div class="feature-card reveal" use:reveal={0}>
      <div class="feature-icon"><Apple class="h-5 w-5" /></div>
      <h3>iOS</h3>
      <p>
        Install Inkwell free through its self-hosted AltStore Classic source.
        A £5 App Store build is planned as an optional mainstream install route.
      </p>
      <div class="flex flex-wrap gap-3">
        <a href={ALTSTORE_SOURCE_LINK} class="btn btn-primary active-press">
          <Download class="h-4 w-4" />
          Add AltStore source
        </a>
        <a
          href={APP_STORE_PLACEHOLDER_LINK}
          class="btn btn-outline active-press"
          aria-label="App Store listing placeholder — planned £5 build"
        >
          <Apple class="h-4 w-4" />
          App Store — planned £5
        </a>
      </div>
      <p class="mt-3 text-sm text-muted">
        This source works with
        <a href="https://altstore.io" class="text-accent underline">AltStore Classic</a> —
        the free, worldwide sideloading AltStore (requires a computer for the
        first install, apps refresh every 7 days). The App Store button currently
        points at a placeholder listing URL and will be replaced when the paid
        listing exists.
      </p>
    </div>

    <div class="feature-card reveal" use:reveal={0}>
      <div class="feature-icon"><Smartphone class="h-5 w-5" /></div>
      <h3>Android</h3>
      <p>
        Install the experimental Android build free from Inkwell&rsquo;s self-hosted
        F-Droid repository. A £5 Google Play build is planned as an optional
        mainstream install route.
      </p>
      <div class="flex flex-wrap gap-3">
        <a href={FDROID_REPO_LINK} class="btn btn-primary active-press">
          <Download class="h-4 w-4" />
          Add F-Droid repo
        </a>
        <a
          href={PLAY_STORE_PLACEHOLDER_LINK}
          class="btn btn-outline active-press"
          aria-label="Google Play listing placeholder — planned £5 build"
        >
          <Smartphone class="h-4 w-4" />
          Google Play — planned £5
        </a>
      </div>
      <p class="mt-3 text-sm text-muted">
        Or open
        <a href="/fdroid/repo" class="text-accent underline">inkwell.ewancroft.uk/fdroid/repo</a>
        directly to browse it or scan its QR code. The Google Play button uses
        the expected package URL as a placeholder until the listing is live.
        Source on
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
      <figure class="reveal" use:reveal={0}>
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
      <figure class="reveal" use:reveal={1}>
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
      <figure class="reveal" use:reveal={2}>
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
      <figure class="reveal" use:reveal={3}>
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
      <figure class="reveal" use:reveal={0}>
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
      <figure class="reveal" use:reveal={1}>
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
      <figure class="reveal" use:reveal={2}>
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
      <figure class="reveal" use:reveal={3}>
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

<!-- Features (compressed) -->
<section class="site-container pb-12">
  <h2 class="section-title">
    What Inkwell does
  </h2>

  <div class="feature-grid">
    <div class="feature-card reveal" use:reveal={0}>
      <div class="feature-icon"><BookOpen class="h-5 w-5" /></div>
      <h3>Read &amp; Discover</h3>
      <p>
        Browse publications and documents across the Standard.site
        ecosystem — Markpub, Leaflet, pckt, and Offprint content, rendered
        natively from their owning PDS. Search the public index and
        subscribe to the publications you follow.
      </p>
    </div>

    <div class="feature-card reveal" use:reveal={0}>
      <div class="feature-icon"><PenLine class="h-5 w-5" /></div>
      <h3>Write</h3>
      <p>
        Compose in Markdown and publish to your own PDS in your choice of
        format. Your writing stays a portable record in your repository —
        not locked into Inkwell.
      </p>
    </div>

    <div class="feature-card reveal" use:reveal={0}>
      <div class="feature-icon"><Shield class="h-5 w-5" /></div>
      <h3>Native &amp; verified</h3>
      <p>
        OAuth sign-in with no app passwords, and every publication or
        document can be checked against its <code>.well-known</code>
        endpoint and canonical link before you trust it.
      </p>
    </div>
  </div>

  <a href="/features" class="section-link">See all features</a>
</section>

<!-- Secure by design (teaser) -->
<section class="site-container py-12">
  <div class="callout reveal" use:reveal={2}>
    <h2>Secure by design</h2>
    <p>
      Inkwell uses OAuth 2.1 to sign in to your Personal Data Server.
      Your browser opens once to approve access — no password is ever
      seen or stored by the app. Tokens are DPoP-bound and held in the
      platform&rsquo;s secure store, and there's no analytics or tracking
      in the app or on this site.
    </p>
    <a href="/security" class="active-press">
      Read about security <ArrowRight class="h-3 w-3" />
    </a>
  </div>
</section>

<!-- Why not just the browser -->
<section class="site-container py-12">
  <h2 class="section-title">Why not just use the browser?</h2>
  <p class="mb-6 max-w-[42rem] text-lg leading-relaxed text-pretty">
    Standard.site content is on the open web, so you can always read it in
    a browser tab. Here's what a native client adds.
  </p>
  <dl class="compare">
    <div class="compare-head" aria-hidden="true">
      <span></span>
      <span>In a browser tab</span>
      <span>In Inkwell</span>
    </div>
    <div class="compare-row">
      <dt>Sign-in</dt>
      <dd class="compare-web">
        <span class="compare-label">In a browser tab</span>
        Sign in again at each publication you visit
      </dd>
      <dd class="compare-app">
        <span class="compare-label">In Inkwell</span>
        One OAuth sign-in to your PDS, then you're done
      </dd>
    </div>
    <div class="compare-row">
      <dt>Credentials</dt>
      <dd class="compare-web">
        <span class="compare-label">In a browser tab</span>
        Held in ordinary browser session state
      </dd>
      <dd class="compare-app">
        <span class="compare-label">In Inkwell</span>
        Held in the Keychain or EncryptedSharedPreferences
      </dd>
    </div>
    <div class="compare-row">
      <dt>Following writers</dt>
      <dd class="compare-web">
        <span class="compare-label">In a browser tab</span>
        Bookmark each site and check back manually
      </dd>
      <dd class="compare-app">
        <span class="compare-label">In Inkwell</span>
        One subscribed feed across every publication
      </dd>
    </div>
    <div class="compare-row">
      <dt>Authenticity</dt>
      <dd class="compare-web">
        <span class="compare-label">In a browser tab</span>
        Judge it by the address bar
      </dd>
      <dd class="compare-app">
        <span class="compare-label">In Inkwell</span>
        Checked against <code>.well-known</code> and the canonical link
      </dd>
    </div>
    <div class="compare-row">
      <dt>Writing</dt>
      <dd class="compare-web">
        <span class="compare-label">In a browser tab</span>
        A separate web editor, wherever you publish
      </dd>
      <dd class="compare-app">
        <span class="compare-label">In Inkwell</span>
        Compose and publish without leaving the app
      </dd>
    </div>
  </dl>
</section>

<!-- FAQ -->
<section class="site-container py-12">
  <h2 class="section-title">Frequently asked</h2>
  <div class="faq-list">
    <details class="faq-item">
      <summary>Is Inkwell in the App Store or Play Store?</summary>
      <p>
        Not yet. Inkwell installs from its own self-hosted AltStore source
        (iOS) or F-Droid repository (Android). The
        <a href="/#download">download section</a> includes the planned £5
        App Store and Google Play routes too, but those buttons currently use
        placeholder listing URLs until the real store pages exist. AltStore
        Classic and F-Droid remain the free install routes.
      </p>
    </details>
    <details class="faq-item">
      <summary>Do I need a Bluesky account?</summary>
      <p>
        No. Inkwell needs an AT Protocol account and a PDS, not specifically
        an account hosted by Bluesky. A Bluesky-hosted account works, but so
        does an account on another compatible PDS or one you self-host. You
        sign in with your AT Protocol handle through OAuth.
      </p>
    </details>
    <details class="faq-item">
      <summary>Which publishing formats does Inkwell support?</summary>
      <p>
        Inkwell reads and publishes Markpub Markdown, Leaflet, pckt, and
        Offprint content. It renders those formats natively and keeps portable
        <code>textContent</code> available as a fallback when a client does not
        understand the richer body format. See <a href="/features">Features</a>
        for the format-by-format detail.
      </p>
    </details>
    <details class="faq-item">
      <summary>Can I edit documents I already published?</summary>
      <p>
        Yes. Inkwell can open existing documents for editing and publish an
        updated revision back to your repository. If you choose to convert a
        document between formats, the editor reports content that cannot
        round-trip cleanly before you publish the conversion.
      </p>
    </details>
    <details class="faq-item">
      <summary>What happens to my writing if Inkwell shuts down?</summary>
      <p>
        Nothing happens to it — your documents are AT Protocol records in
        your own repository on your PDS, not data stored by Inkwell. Any
        client that speaks the Standard.site record schemas can read or
        edit them, with or without Inkwell.
      </p>
    </details>
    <details class="faq-item">
      <summary>Does Inkwell store my writing or account on its own servers?</summary>
      <p>
        No. Inkwell reads and writes your content directly on your PDS and has
        no intermediary database of your documents. Your OAuth session and
        DPoP key are stored on your device in the platform's secure storage;
        the app contains no analytics or tracking SDK. See
        <a href="/security">Security</a> and <a href="/privacy">Privacy</a>
        for the full detail.
      </p>
    </details>
    <details class="faq-item">
      <summary>Do I need to know AT Protocol to use this?</summary>
      <p>
        No. You'll need an AT Protocol account and a PDS to sign in with —
        Inkwell doesn't create one for you — but from there it works like
        any reading and writing app. See <a href="/about">About</a> if
        you'd like the background.
      </p>
    </details>
    <details class="faq-item">
      <summary>How do notifications work?</summary>
      <p>
        Inkwell does not send your subscriptions to a push-notification
        service. The app periodically checks the publications you follow and
        creates local notifications on your device when it finds new
        documents. You can turn the OS banners off without losing the in-app
        notification history.
      </p>
    </details>
    <details class="faq-item">
      <summary>Can I use the same account on iOS and Android?</summary>
      <p>
        Yes. Sign in to the same AT Protocol account and both apps read the
        same repository-backed publications, documents, subscriptions, and
        recommendations from your PDS. Device-local preferences such as
        appearance, accessibility, and notification settings are configured
        separately on each device.
      </p>
    </details>
    <details class="faq-item">
      <summary>What versions of iOS and Android are supported?</summary>
      <p>
        The iOS app supports iOS 18 and later. The Android app supports
        Android 8.0 (API 26) and later, and currently targets Android API 36.
      </p>
    </details>
    <details class="faq-item">
      <summary>Is Android ready?</summary>
      <p>
        Yes, for normal use. Android now covers the core Inkwell experience:
        reading and discovery, publishing and editing across the supported
        Standard.site formats, comments and interactions, subscriptions and
        recommendations, verification, notifications, settings, and
        accessibility and customisation controls. It is still labelled
        experimental because its Android-specific automated coverage is
        comparatively thin and iOS remains the primary, more polished
        implementation.
      </p>
    </details>
    <details class="faq-item">
      <summary>What does verification protect against?</summary>
      <p>
        A publication or document can claim a canonical web address, but a
        claim alone isn't proof. Inkwell checks that claim against the
        site's <code>.well-known</code> endpoint and its canonical
        <code>&lt;link&gt;</code> tag, surfacing a mismatch instead of
        silently trusting it. More detail on <a href="/security">Security</a>.
      </p>
    </details>
  </div>
</section>

<!-- Availability -->
<section class="site-container py-12">
  <h2 class="section-title">Availability</h2>
  <div class="callout">
    <h3>iOS primary, Android usable today</h3>
    <p class="text-pretty">
      Inkwell&rsquo;s iOS app remains the primary and more polished implementation,
      but Android now covers the core reading, discovery, writing, interaction,
      verification, notification, settings, and accessibility flows. Android is
      still labelled experimental while its platform-specific testing and the
      remaining edge cases catch up.
    </p>
  </div>
</section>

<!-- Users already using Inkwell -->
<section class="site-container py-12">
  <h2 class="section-title">Users already using Inkwell</h2>
  <p class="mb-6 max-w-[42rem] text-lg leading-relaxed text-pretty">
    Inkwell users across the network. This carousel is generated from
    Constellation backlinks to the <code>uk.ewancroft.inkwell.user</code>
    lexicon record, resolving DIDs to Bluesky avatars and handle aliases via
    Slingshot.
  </p>

  {#if data.users.length > 0}
    <div class="user-carousel">
      {#each data.users as user (user.handle)}
        <div class="user-card reveal">
          {#if user.avatar}
            <img
              class="user-avatar"
              src={user.avatar}
              alt="{user.handle} avatar"
              loading="lazy"
              width="56"
              height="56"
            />
          {:else}
            <div class="user-avatar" aria-hidden="true"></div>
          {/if}
          {#if user.displayName}
            <div class="user-displayname">{user.displayName}</div>
          {/if}
          <div class="user-handle">@{user.handle}</div>
        </div>
      {/each}
    </div>
  {:else}
    <div class="user-empty">
      No one has declared themselves an Inkwell user yet. Flip
      <strong>Declare me as an Inkwell user</strong> in the app's Settings and
      you'll show up here — the record lives in your own PDS, and the list is
      built entirely from public Constellation backlinks.
    </div>

  {/if}
</section>

<!-- Final CTA -->
<section class="site-container pb-16 text-center">
  <a href="#download" class="btn btn-primary active-press">
    <Download class="h-4 w-4" />
    Get Inkwell
  </a>
</section>
