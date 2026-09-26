<!--
  Security page — the full explanation behind the landing page's
  "Secure by design" teaser. Plain-language OAuth/DPoP, on-device
  key storage, no app passwords, no analytics, and the verification
  model. Precise and calm, not marketing hype.

  Copy comes from the message catalogue for `data.locale`; the
  <title>/description live in the root layout's route-metadata table.
-->

<script lang="ts">
  import { reveal } from "$lib/motion";
  import { localizeHref, messagesFor, rich } from "$lib/i18n";
  import type { PageData } from "./$types";
  import {
    KeyRound,
    Lock,
    ShieldCheck,
    EyeOff,
    ArrowRight,
  } from "@lucide/svelte";

  let { data }: { data: PageData } = $props();

  const locale = $derived(data.locale);
  const m = $derived(messagesFor(locale).security);

  const CARDS = [
    { key: "oauth", icon: KeyRound, group: 0 },
    { key: "dpop", icon: Lock, group: 0 },
    { key: "storage", icon: ShieldCheck, group: 0 },
    { key: "analytics", icon: EyeOff, group: 1 },
    { key: "verification", icon: ShieldCheck, group: 1 },
    { key: "control", icon: KeyRound, group: 1 },
  ] as const;
</script>

<section class="site-container page-hd">
  <h1 class="page-title">{m.title}</h1>
  <p class="page-desc">{m.description}</p>
</section>

<section class="site-container py-12">
  <div class="feature-grid">
    {#each CARDS as card (card.key)}
      {@const copy = m.cards[card.key]}
      <div class="feature-card reveal" use:reveal={card.group}>
        <div class="feature-icon"><card.icon class="h-5 w-5" /></div>
        <h3>{copy.heading}</h3>
        <p>{@html rich(copy.body, locale)}</p>
      </div>
    {/each}
  </div>
</section>

<section class="site-container py-12">
  <div class="callout reveal" use:reveal={2}>
    <h2>{m.legal.heading}</h2>
    <p>{m.legal.body}</p>
    <a href={localizeHref("/privacy", locale)}>
      {m.legal.privacy} <ArrowRight class="h-3 w-3" />
    </a>
    <a href={localizeHref("/terms", locale)} class="ml-4">
      {m.legal.terms} <ArrowRight class="h-3 w-3" />
    </a>
  </div>
</section>
