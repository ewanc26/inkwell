<!--
  Features page — deeper dive on what Inkwell does than the landing
  page's compressed grid can hold. Read / Discover / Write / AT
  Protocol native / verification / iOS & Android, each with real
  elaboration rather than restated headlines.
-->

<script lang="ts">
  import { reveal } from "$lib/motion";
  import { SITE } from "$lib/config";
  import {
    BookOpen,
    Compass,
    PenLine,
    Globe,
    ShieldCheck,
    Apple,
    Smartphone,
    Palette,
  } from "@lucide/svelte";
</script>

<svelte:head>
  <title>{SITE.title} — Features</title>
  <meta
    name="description"
    content="What Inkwell actually does: reading Markpub, Leaflet, pckt, and Offprint content, discovering publications, writing and publishing to your own PDS, native AT Protocol OAuth, and built-in verification."
  />
</svelte:head>

<section class="site-container page-hd">
  <h1 class="page-title">Features</h1>
  <p class="page-desc">
    Inkwell is a native reader and writer for the Standard.site publishing
    ecosystem. Here's what that means in practice, on both iOS and Android.
  </p>
</section>

<section class="site-container py-12">
  <div class="feature-grid">
    <div class="feature-card reveal" use:reveal={0}>
      <div class="feature-icon"><BookOpen class="h-5 w-5" /></div>
      <h3>Read</h3>
      <p>
        Inkwell renders content natively rather than loading a web view.
        It understands four Standard.site formats: Markpub Markdown (plain
        long-form writing), Leaflet's block and blob pages (rich layouts
        with embedded media), pckt's block arrays, and Offprint's block
        arrays — each converted and rendered in the app's own reading
        surface, with your system's Dynamic Type and light/dark theme
        respected throughout. Every document is fetched straight from its
        owning PDS, not a cached copy on a third-party server.
      </p>
    </div>

    <div class="feature-card reveal" use:reveal={0}>
      <div class="feature-icon"><Compass class="h-5 w-5" /></div>
      <h3>Discover</h3>
      <p>
        Search the cross-platform public index to find publications across
        the Standard.site ecosystem, then subscribe to the ones you want to
        follow. Background refresh polls subscriptions periodically and
        raises a local notification when one has a new document — on-device,
        with no push provider and no server-side notification
        infrastructure involved. A notification list keeps a history you
        can review or clear, and a Settings toggle turns the OS banner off
        without losing that list.
      </p>
    </div>

    <div class="feature-card reveal" use:reveal={0}>
      <div class="feature-icon"><PenLine class="h-5 w-5" /></div>
      <h3>Write</h3>
      <p>
        Compose in Markdown and publish directly to your own PDS in your
        choice of format — Markpub, Leaflet, pckt, or Offprint. Because the
        underlying record is a standard AT Protocol record in your
        repository, your writing isn't locked into Inkwell: any client that
        speaks the same lexicons can read, edit, or migrate it. That's the
        point of publishing on your own PDS instead of a platform's
        database.
      </p>
    </div>

    <div class="feature-card reveal" use:reveal={1}>
      <div class="feature-icon"><Globe class="h-5 w-5" /></div>
      <h3>AT Protocol native</h3>
      <p>
        Signing in opens your system browser once, where you approve access
        directly with your PDS using OAuth 2.1 — the same flow as signing
        into a website with your identity provider. Inkwell never sees or
        stores your account password or an app password. Day to day, this
        means the browser handshake happens once at login; after that,
        Inkwell holds a short-lived, DPoP-bound access token and refreshes
        it automatically, so you stay signed in without repeating the
        approval step.
      </p>
    </div>

    <div class="feature-card reveal" use:reveal={1}>
      <div class="feature-icon"><ShieldCheck class="h-5 w-5" /></div>
      <h3>Verification built in</h3>
      <p>
        Publications and documents on Standard.site can claim a canonical
        web address, but a claim on its own isn't proof. Inkwell checks it:
        it fetches the publication's <code>.well-known</code> endpoint and
        compares it against the record, and it looks for a matching
        canonical <code>&lt;link&gt;</code> tag on the published page. This
        catches the case where a document or publication record points at
        a site it doesn't actually control — a spoofed or stale
        <code>.well-known</code> response, or a canonical link that doesn't
        match — and surfaces that mismatch to you rather than silently
        treating every claimed link as trustworthy.
      </p>
    </div>

    <div class="feature-card reveal" use:reveal={1}>
      <div class="feature-icon"><Palette class="h-5 w-5" /></div>
      <h3>Accessibility &amp; appearance</h3>
      <p>
        Text size, bold text, an increase-contrast mode that snaps
        foreground to pure black or white against the current background,
        and underlined links are all free, unconditional settings — never
        gated behind anything. A separate accent colour, reading font, and
        appearance override let you make the reading surface your own.
        Inkwell has no ads, no paywalls, and no premium tier; a single
        dismissible prompt towards Ko-fi appears once, the first time you
        change a setting, and never again after that.
      </p>
    </div>

    <div class="feature-card reveal" use:reveal={1}>
      <div class="feature-icon"><Apple class="h-5 w-5" /></div>
      <h3>iOS &amp; Android</h3>
      <p>
        The primary app is a SwiftUI client for iOS, built to respect
        Dynamic Type, accessibility, and system theme. There's also a
        Jetpack Compose port for Android, built on the same shared Kotlin
        Multiplatform core so record handling, markdown parsing, and
        verification logic stay consistent across platforms. It's still
        labelled experimental, but reading, discovery, writing, comments,
        and verification all work — see the
        <Smartphone class="inline h-4 w-4" aria-hidden="true" /> note below.
      </p>
    </div>
  </div>
</section>

<section class="site-container py-12">
  <h2 class="section-title">Where iOS and Android differ</h2>
  <p class="mb-6 max-w-[42rem] text-lg leading-relaxed text-pretty">
    iOS is Inkwell's primary implementation and gets new features first. The
    Android app is still labelled experimental, but it has closed most of
    the gap: reading, discovery, writing across every Standard.site format,
    comments, verification, and background notifications are all
    implemented and usable today.
  </p>
  <a href="/#download" class="section-link">Get Inkwell for either platform</a>
</section>
