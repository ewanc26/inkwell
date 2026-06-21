<script lang="ts">
  import { SITE } from "$lib/config";
  import {
    BookOpen,
    Compass,
    PenLine,
    Shield,
    Globe,
    Sparkles,
    ArrowRight,
    Apple,
  } from "@lucide/svelte";
</script>

<svelte:head>
  <title>{SITE.title} — Reader &amp; Writer for Standard.site on AT Protocol</title>
</svelte:head>

<!-- Hero -->
<section class="container py-16 sm:py-24">
  <div class="mx-auto max-w-3xl text-center">
    <img
      src="/favicon.svg"
      alt=""
      class="mx-auto mb-8 h-20 w-20"
      aria-hidden="true"
    />
    <h1 class="mb-6 text-4xl font-extrabold tracking-tight sm:text-5xl">
      Read, discover, and publish
      <span class="text-(--color-accent)">Standard.site</span>
      writing
    </h1>
    <p class="mb-8 text-lg leading-relaxed text-(--color-muted)">
      Inkwell is a native iOS reader and writer for the Standard.site
      publishing ecosystem on AT Protocol. Your writing lives on your
      Personal Data Server — no silos, no lock-in.
    </p>

    <div class="flex flex-col items-center gap-4 sm:flex-row sm:justify-center">
      <a
        href="https://apps.apple.com/app/inkwell/id0000000000"
        class="app-store-badge"
      >
        <img
          src="/app-store-badge.svg"
          alt="Download on the App Store"
          class="h-12"
        />
      </a>
      <a
        href="https://github.com/ewanc26/inkwell"
        class="btn btn-outline"
      >
        <Sparkles class="h-4 w-4" />
        View on GitHub
      </a>
    </div>
  </div>
</section>

<!-- Features -->
<section class="container py-16">
  <h2 class="mb-12 text-center text-3xl font-bold">What Inkwell does</h2>

  <div class="feature-grid">
    <div class="feature-card">
      <div class="feature-icon"><BookOpen class="h-5 w-5" /></div>
      <h3>Read</h3>
      <p>
        Browse publications and documents from across the Standard.site
        ecosystem. Renders Markpub Markdown, Leaflet pages, pckt blocks,
        and Offprint content — all from their owning PDS.
      </p>
    </div>

    <div class="feature-card">
      <div class="feature-icon"><Compass class="h-5 w-5" /></div>
      <h3>Discover</h3>
      <p>
        Search the cross-platform public index. Subscribe to publications
        and get notified when new documents appear. Background refresh
        keeps your feed current.
      </p>
    </div>

    <div class="feature-card">
      <div class="feature-icon"><PenLine class="h-5 w-5" /></div>
      <h3>Write</h3>
      <p>
        Compose in Markdown and publish to your PDS in your choice of
        format — Markpub, Leaflet, pckt, or Offprint. Your writing, your
        data portability.
      </p>
    </div>

    <div class="feature-card">
      <div class="feature-icon"><Globe class="h-5 w-5" /></div>
      <h3>AT Protocol Native</h3>
      <p>
        Sign in to your PDS with OAuth — no app passwords needed.
        Records live in your repository under your control.
      </p>
    </div>

    <div class="feature-card">
      <div class="feature-icon"><Shield class="h-5 w-5" /></div>
      <h3>Verification Built In</h3>
      <p>
        Every publication and document can be verified against its
        .well-known endpoint and canonical &lt;link&gt; tags. Know your
        sources are authentic.
      </p>
    </div>

    <div class="feature-card">
      <div class="feature-icon"><Apple class="h-5 w-5" /></div>
      <h3>Native iOS</h3>
      <p>
        Built with SwiftUI for iOS. Respects your system theme, Dynamic
        Type, and accessibility settings. First-class citizen on iPhone
        and iPad.
      </p>
    </div>
  </div>
</section>

<!-- OAuth note -->
<section class="container pb-16">
  <div class="mx-auto max-w-2xl rounded-xl border p-8 text-center">
    <h2 class="mb-4 text-2xl font-bold">Secure by design</h2>
    <p class="leading-relaxed text-(--color-muted)">
      Inkwell uses OAuth 2.1 to sign in to your Personal Data Server.
      Your browser opens once to approve access — no password is ever
      seen or stored by the app. Tokens are DPoP-bound and stored in
      Apple's Keychain.
    </p>
    <a href="/privacy" class="mt-4 inline-flex items-center gap-1 text-sm font-medium text-(--color-accent)">
      Read the Privacy Policy <ArrowRight class="h-3 w-3" />
    </a>
  </div>
</section>
