<!--
  Terms of Service / EULA page.
  AGPL-3.0 licence with user-conduct guidelines for the AT Protocol
  network. Inkwell is sideloaded via AltStore / F-Droid, so no
  store-imposed EULA applies. Prose layout identical to /privacy.
-->

<script lang="ts">
  import { SITE } from "$lib/config";
</script>

<svelte:head>
  <title>Terms of Service — {SITE.title}</title>
</svelte:head>

<article class="site-container prose py-16 animate-in">
  <h1>Terms of Service &amp; EULA</h1>

  <p>
    <strong>Version 1.1 &mdash; Effective Date: <time datetime="2026-08-13">13 August 2026</time></strong>
  </p>

  <p>
    By downloading or using Inkwell (the &ldquo;App&rdquo;), you agree to these
    terms. Inkwell is provided for iOS (version 1.0, build 49) via AltStore and
    for Android (version 1.2.0) via a self-hosted F-Droid repository. It is not
    distributed through the Apple App Store or Google Play.
  </p>

  <p>
    Inkwell is free and open-source software licensed under the
    <a href="https://www.gnu.org/licenses/agpl-3.0.en.html">GNU Affero General
    Public License v3.0 (AGPL-3.0)</a>. No store-imposed end-user license
    agreement applies to the AltStore or F-Droid distribution channels.
  </p>

  <h2>1. User-Generated Content</h2>
  <p>
    Inkwell acts as a portal to the AT Protocol network. You are solely
    responsible for the content you publish through it. You must not publish
    illegal, harmful, or abusive content, and you must comply with the terms of
    the Personal Data Server (PDS) you use and of the AT Protocol network. The
    developer does not host user content and does not act as a moderator of the
    network.
  </p>

  <h2>2. Decentralized Network Disclaimer</h2>
  <p>
    Because Inkwell connects to a decentralized network, the developer has no
    control over the content published by other users, nor over the availability
    of PDSs, search indexes, and other network infrastructure. You may encounter
    content you find objectionable, and the developer is not responsible for it.
  </p>

  <h2>3. &ldquo;As Is&rdquo; Basis</h2>
  <p>
    Inkwell is provided &ldquo;as is,&rdquo; without warranty of any kind, express
    or implied, including merchantability and fitness for a particular purpose.
    The developer is not liable for any data loss, service interruptions, or
    issues arising from your PDS or the AT Protocol network.
  </p>

  <h2>4. Experimental Software</h2>
  <p>
    The Android build is an experimental prototype. Features, behavior, and
    performance may be incomplete or change without notice. Install and use at
    your own risk.
  </p>

  <h2>5. AI-assisted contributions</h2>
  <p>
    AI tools may be used when contributing to Inkwell, but contributors should
    not add <code>Co-authored-by:</code> trailers crediting AI agents.
    Attribution is reserved for human contributors only.
  </p>
</article>
