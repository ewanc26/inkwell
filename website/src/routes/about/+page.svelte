<!--
  About page — the "why this exists" story. Standard.site and AT
  Protocol explained just enough to orient a newcomer (full detail
  lives at standard.site / atproto.com), why a native client rather
  than a web view, who builds it, and honest availability status.
-->

<script lang="ts">
  import { reveal } from "$lib/motion";
  import { SITE } from "$lib/config";
  import { Heart, ArrowRight } from "@lucide/svelte";
</script>

<svelte:head>
  <title>{SITE.title} — About</title>
  <meta
    name="description"
    content="Why Inkwell exists: a native reader and writer for Standard.site on AT Protocol, built as free and open-source software instead of a web view or a Bluesky tab."
  />
</svelte:head>

<section class="site-container page-hd">
  <h1 class="page-title">About</h1>
  <p class="page-desc">
    Why a dedicated app exists for this, who builds it, and where things
    stand today.
  </p>
</section>

<section class="site-container prose py-12">
  <h2>Standard.site and AT Protocol, briefly</h2>
  <p>
    AT Protocol is the decentralized network behind Bluesky and a growing
    set of independent apps — your identity and data live in a repository
    you control, on a Personal Data Server (PDS) you or someone you trust
    operates, rather than inside one company's database. For the full
    picture, <a href="https://atproto.com">atproto.com</a> covers the
    protocol itself.
  </p>
  <p>
    Standard.site is a publishing ecosystem built on top of that network —
    a shared set of record schemas for long-form writing, so publications
    and documents you write in one client stay portable and readable in
    another. Markpub, Leaflet, pckt, and Offprint are all Standard.site
    publishing formats; Inkwell reads and writes all four. See
    <a href="https://standard.site">standard.site</a> for the ecosystem
    itself.
  </p>

  <h2>Why a native app, not a web view</h2>
  <p>
    Decentralized publishing deserves a first-class native client — not a
    browser tab wrapped in an app shell, and not a feature bolted onto a
    social app that wasn't designed for long-form writing. A native app
    can integrate properly with the platform's secure credential storage,
    respect system typography and accessibility settings, work with
    background refresh and local notifications, and feel like it belongs
    on your device rather than borrowed from the web. That's the premise
    Inkwell starts from.
  </p>

  <h2>Who builds it</h2>
  <p>
    Inkwell is free and open-source software, licensed under the
    <a href="https://www.gnu.org/licenses/agpl-3.0.en.html">AGPL-3.0</a>
    with an
    <a href="https://github.com/ewanc26/inkwell/blob/main/APP_STORE_EXCEPTION.md"
      >App Store Distribution Exception</a
    >. The exception is a narrow additional permission for app-store
    distribution; it does not remove the AGPL's source-availability or
    copyleft requirements. The source, issues, and releases are all on
    <a href="https://github.com/ewanc26/inkwell">GitHub</a>. If Inkwell is
    useful to you, you can support its development through
    <a href="https://ko-fi.com/ewancroft">Ko-fi</a> or
    <a href="https://github.com/sponsors/ewanc26">GitHub Sponsors</a> —
    entirely optional, and unrelated to any feature gate.
  </p>

  <h2>Where things stand</h2>
  <p>
    iOS is the primary implementation, built with SwiftUI. Android is a
    Jetpack Compose port that shares a common Kotlin Multiplatform core
    with iOS for record handling and verification logic. It's still
    labelled experimental, but reading, discovery, writing, comments,
    verification, and background notifications are all implemented and
    usable today — iOS simply gets new features first.
    Neither app is distributed through the App Store or Play Store yet;
    both install from Inkwell's own self-hosted AltStore source and
    F-Droid repository. See <a href="/features">Features</a> for what
    each platform actually does today.
  </p>
  <p>
    The longer-term plan is to add the Apple App Store and Google Play as
    optional mainstream distribution routes for a flat £5 purchase. Those
    builds are intended to be the same open-source app, not a premium
    edition: AltStore Classic on iOS and F-Droid on Android will remain
    free alternatives, with the source continuing to be available under
    the AGPL-3.0.
  </p>
</section>

<section class="site-container py-12">
  <div class="callout reveal" use:reveal={0}>
    <h2>Open source</h2>
    <p>
      Read the code, file an issue, or open a pull request. Contributions
      are welcome under the AGPL-3.0 with Inkwell's App Store Distribution
      Exception.
    </p>
    <a href="https://github.com/ewanc26/inkwell">
      View the repository <ArrowRight class="h-3 w-3" />
    </a>
  </div>
</section>

<section class="site-container pb-16">
  <div class="callout reveal" use:reveal={1}>
    <h2><Heart class="mr-1 inline h-4 w-4" aria-hidden="true" /> Support the project</h2>
    <p>
      Inkwell has no ads, no paywalls, and no premium tier. If you'd like
      to support ongoing development, sponsorship is optional and
      appreciated.
    </p>
    <a href="https://ko-fi.com/ewancroft">Ko-fi <ArrowRight class="h-3 w-3" /></a>
    <a href="https://github.com/sponsors/ewanc26" class="ml-4">
      GitHub Sponsors <ArrowRight class="h-3 w-3" />
    </a>
  </div>
</section>
