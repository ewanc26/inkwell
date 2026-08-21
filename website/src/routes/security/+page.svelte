<!--
  Security page — the full explanation behind the landing page's
  "Secure by design" teaser. Plain-language OAuth/DPoP, on-device
  key storage, no app passwords, no analytics, and the verification
  model. Precise and calm, not marketing hype.
-->

<script lang="ts">
  import { reveal } from "$lib/motion";
  import { SITE } from "$lib/config";
  import { KeyRound, Lock, ShieldCheck, EyeOff, ArrowRight } from "@lucide/svelte";
</script>

<svelte:head>
  <title>{SITE.title} — Security</title>
  <meta
    name="description"
    content="How Inkwell handles authentication, key storage, and verification: OAuth 2.1 with DPoP-bound tokens, no app passwords, on-device Keychain/EncryptedSharedPreferences storage, and no analytics or tracking."
  />
</svelte:head>

<section class="site-container page-hd">
  <h1 class="page-title">Security</h1>
  <p class="page-desc">
    Inkwell is a client for your own data. This page explains, plainly,
    how it authenticates, what it stores, and what it doesn't collect.
  </p>
</section>

<section class="site-container py-12">
  <div class="feature-grid">
    <div class="feature-card reveal" use:reveal={0}>
      <div class="feature-icon"><KeyRound class="h-5 w-5" /></div>
      <h3>OAuth 2.1, no app passwords</h3>
      <p>
        Signing in opens your system browser once, where you approve
        access directly with your PDS — the same kind of flow as "Sign in
        with" any identity provider. Inkwell never sees or stores your
        account password. There is no legacy app-password fallback: if a
        PDS only supports OAuth, so does Inkwell.
      </p>
    </div>

    <div class="feature-card reveal" use:reveal={0}>
      <div class="feature-icon"><Lock class="h-5 w-5" /></div>
      <h3>DPoP-bound tokens</h3>
      <p>
        Inkwell requests DPoP (Demonstrating Proof-of-Possession) tokens
        rather than plain bearer tokens. Each request is signed with a
        private key that never leaves your device, so a stolen access
        token by itself isn't enough to impersonate a request — it also
        has to be replayed alongside a valid proof from that specific
        key. Sessions refresh automatically in the background; the token
        that's actually sent over the network is always short-lived.
      </p>
    </div>

    <div class="feature-card reveal" use:reveal={0}>
      <div class="feature-icon"><ShieldCheck class="h-5 w-5" /></div>
      <h3>On-device key storage</h3>
      <p>
        Your OAuth session and DPoP private key are held in the
        platform's secure storage, not app-readable preferences: Apple's
        Keychain on iOS, and Android's EncryptedSharedPreferences backed
        by a hardware-backed MasterKey on Android. Signing out or
        uninstalling removes them. Full detail — including backup
        behaviour on each platform — is in the
        <a href="/privacy">Privacy Policy</a>.
      </p>
    </div>

    <div class="feature-card reveal" use:reveal={1}>
      <div class="feature-icon"><EyeOff class="h-5 w-5" /></div>
      <h3>No analytics, no tracking</h3>
      <p>
        Inkwell contains no analytics SDKs, no crash-reporting SDKs, no
        ad networks, and no proprietary telemetry. The developer doesn't
        collect usage data from the app. This website sets no cookies,
        runs no analytics, and embeds no third-party trackers — Vercel,
        as host, records standard server request logs for delivery and
        security, and that's the extent of it.
      </p>
    </div>

    <div class="feature-card reveal" use:reveal={1}>
      <div class="feature-icon"><ShieldCheck class="h-5 w-5" /></div>
      <h3>Verification</h3>
      <p>
        Publications and documents can claim a canonical web address.
        Inkwell checks that claim against the publication's
        <code>.well-known</code> endpoint and the canonical
        <code>&lt;link&gt;</code> tag on the published page, so a mismatch
        — a spoofed or stale <code>.well-known</code> response, or a
        canonical link pointing somewhere else — is surfaced to you
        instead of silently trusted. See
        <a href="/features">Features</a> for more on how this works.
      </p>
    </div>

    <div class="feature-card reveal" use:reveal={1}>
      <div class="feature-icon"><KeyRound class="h-5 w-5" /></div>
      <h3>Your PDS, your control</h3>
      <p>
        Inkwell is a client, not a service that hosts your content. Your
        writing lives in your own AT Protocol repository, on whichever
        PDS you choose or self-host. Inkwell reads and writes to it
        directly; there's no intermediary database of your content on
        the developer's infrastructure.
      </p>
    </div>
  </div>
</section>

<section class="site-container py-12">
  <div class="callout reveal" use:reveal={2}>
    <h2>Read the legal detail</h2>
    <p>
      The Privacy Policy and Terms of Service spell out exactly what's
      stored, where, and for how long, including the two narrow exceptions
      to "no data collection" — optional in-app feedback and this
      website's server logs.
    </p>
    <a href="/privacy">Privacy Policy <ArrowRight class="h-3 w-3" /></a>
    <a href="/terms" class="ml-4">Terms of Service <ArrowRight class="h-3 w-3" /></a>
  </div>
</section>
