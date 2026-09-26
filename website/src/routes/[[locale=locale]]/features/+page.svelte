<!--
  Features page — deeper dive on what Inkwell does than the landing
  page's compressed grid can hold. Read / Discover / Write / AT
  Protocol native / verification / accessibility, each with real
  elaboration rather than restated headlines.

  Copy comes from the message catalogue for `data.locale`; the
  <title>/description live in the root layout's route-metadata table.
-->

<script lang="ts">
  import { reveal } from "$lib/motion";
  import { localizeHref, messagesFor, rich } from "$lib/i18n";
  import UserCarousel from "$lib/components/UserCarousel.svelte";
  import type { PageData } from "./$types";
  import {
    BookOpen,
    Compass,
    PenLine,
    Globe,
    ShieldCheck,
    Palette,
  } from "@lucide/svelte";

  let { data }: { data: PageData } = $props();

  const locale = $derived(data.locale);
  const shared = $derived(messagesFor(locale));
  const m = $derived(shared.features);

  // Icon and reveal-stagger group per card, alongside the catalogue key
  // that supplies its heading and body.
  const CARDS = [
    { key: "read", icon: BookOpen, group: 0 },
    { key: "discover", icon: Compass, group: 0 },
    { key: "write", icon: PenLine, group: 0 },
    { key: "native", icon: Globe, group: 1 },
    { key: "verification", icon: ShieldCheck, group: 1 },
    { key: "accessibility", icon: Palette, group: 1 },
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

<UserCarousel users={data.users} messages={shared} {locale}>
  <a href={localizeHref("/#download", locale)} class="section-link">
    {m.getEither}
  </a>
</UserCarousel>

<section class="site-container py-12">
  <h2 class="section-title">{m.differ.heading}</h2>
  <p class="mb-6 max-w-[42rem] text-lg leading-relaxed text-pretty">
    {m.differ.body}
  </p>
  <a href={localizeHref("/#download", locale)} class="section-link">
    {m.getEither}
  </a>
</section>
