<!--
  About page — the "why this exists" story. Standard.site and AT
  Protocol explained just enough to orient a newcomer (full detail
  lives at standard.site / atproto.com), why a native client rather
  than a web view, who builds it, and honest availability status.

  Copy comes from the message catalogue for `data.locale`; the
  <title>/description live in the root layout's route-metadata table.
-->

<script lang="ts">
  import type { PageData } from "./$types";
  import { reveal } from "$lib/motion";
  import { fmt, localizeHref, messagesFor, rich } from "$lib/i18n";
  import { Heart, ArrowRight, ArrowUpRight } from "@lucide/svelte";

  let { data }: { data: PageData } = $props();

  const locale = $derived(data.locale);
  const m = $derived(messagesFor(locale).about);
</script>

<section class="site-container page-hd">
  <h1 class="page-title">{m.title}</h1>
  <p class="page-desc">{m.description}</p>
</section>

<section class="site-container prose py-12">
  <h2>{m.protocol.heading}</h2>
  <p>{@html rich(m.protocol.atproto, locale)}</p>
  <p>{@html rich(m.protocol.standardSite, locale)}</p>

  <h2>{m.native.heading}</h2>
  <p>{m.native.body}</p>

  <h2>{m.who.heading}</h2>
  <p>{@html rich(m.who.body, locale)}</p>

  <h2>{m.status.heading}</h2>
  <p>{@html rich(m.status.today, locale)}</p>
  <p>{m.status.plan}</p>
</section>

<section
  class="site-container contributors-section"
  aria-labelledby="contributors-heading"
>
  <div class="contributors-heading">
    <div>
      <h2 id="contributors-heading" class="section-title">
        {m.contributors.heading}
      </h2>
      <p>{m.contributors.intro}</p>
    </div>
    <a
      class="contributors-link"
      href="https://github.com/ewanc26/inkwell/graphs/contributors"
    >
      {m.contributors.viewOnGitHub}
      <ArrowUpRight class="h-3 w-3" aria-hidden="true" />
    </a>
  </div>

  {#if data.contributors.length > 0}
    <ul class="contributor-list">
      {#each data.contributors as contributor (contributor.login)}
        <li>
          <a class="contributor-row" href={contributor.profileUrl}>
            <img
              class="contributor-avatar"
              src={contributor.avatarUrl}
              alt=""
              width="48"
              height="48"
              loading="lazy"
              decoding="async"
              referrerpolicy="no-referrer"
            />
            <span class="contributor-copy">
              <span class="contributor-login">@{contributor.login}</span>
              <span class="contributor-meta">
                {fmt(
                  contributor.contributions === 1
                    ? m.contributors.attributedOne
                    : m.contributors.attributedOther,
                  { count: contributor.contributions },
                )}
              </span>
            </span>
            <span class="contributor-arrow">
              <ArrowUpRight class="h-4 w-4" aria-hidden="true" />
            </span>
          </a>
        </li>
      {/each}
    </ul>
  {:else}
    <p class="contributors-unavailable">
      {@html rich(m.contributors.unavailable, locale)}
    </p>
  {/if}
</section>

<section class="site-container py-12">
  <div class="callout reveal" use:reveal={0}>
    <h2>{m.openSource.heading}</h2>
    <p>{m.openSource.body}</p>
    <a href="https://github.com/ewanc26/inkwell">
      {m.openSource.link} <ArrowRight class="h-3 w-3" />
    </a>
  </div>
</section>

<section class="site-container pb-16">
  <div class="callout reveal" use:reveal={1}>
    <h2>
      <Heart class="mr-1 inline h-4 w-4" aria-hidden="true" />
      {m.support.heading}
    </h2>
    <p>{m.support.body}</p>
    <a href="https://ko-fi.com/ewancroft">
      {m.support.kofi} <ArrowRight class="h-3 w-3" />
    </a>
    <a href="https://github.com/sponsors/ewanc26" class="ml-4">
      {m.support.sponsors} <ArrowRight class="h-3 w-3" />
    </a>
  </div>
</section>

<style>
  .contributors-section {
    padding-block: var(--space-lg) var(--space-xl);
  }

  .contributors-heading {
    display: flex;
    align-items: flex-end;
    justify-content: space-between;
    gap: var(--space-lg);
    margin-bottom: var(--space-lg);
  }

  .contributors-heading .section-title {
    margin-bottom: var(--space-sm);
  }

  .contributors-heading p {
    max-width: var(--measure-copy);
    margin: 0;
    color: var(--color-text-700);
    line-height: 1.65;
    text-wrap: pretty;
  }

  .contributors-link {
    display: inline-flex;
    align-items: center;
    gap: var(--space-xs);
    flex-shrink: 0;
    min-height: var(--control-size);
    color: var(--color-primary-600);
    font-family: var(--font-mono);
    font-size: var(--text-xs);
    text-decoration: none;
  }

  .contributors-link:hover {
    color: var(--color-primary-700);
    text-decoration: underline;
    text-decoration-color: var(--color-primary-500);
    text-underline-offset: 4px;
  }

  .contributor-list {
    list-style: none;
    padding: var(--space-xs);
    margin: 0;
    display: flex;
    flex-direction: column;
    gap: var(--space-2xs);
    background: var(--surface-raised);
    border: 1px solid var(--surface-color);
    border-radius: var(--radius-md);
  }

  .contributor-row {
    display: grid;
    grid-template-columns: auto minmax(0, 1fr) auto;
    align-items: center;
    gap: var(--space-md);
    min-height: var(--control-size);
    padding: var(--space-3);
    border-radius: var(--radius-sm);
    color: var(--color-text-950);
    text-decoration: none;
    transition: background-color var(--duration-fast) var(--ease-out-quart);
  }

  .contributor-row:hover {
    background: var(--surface-sunken);
    color: var(--color-text-950);
    text-decoration: none;
  }

  .contributor-avatar {
    width: 3rem;
    height: 3rem;
    border-radius: 50%;
    object-fit: cover;
    background: var(--color-background-200);
  }

  .contributor-copy {
    min-width: 0;
    display: flex;
    flex-direction: column;
    gap: var(--space-2xs);
  }

  .contributor-login {
    overflow: hidden;
    font-size: var(--text-md);
    font-weight: 600;
    line-height: 1.4;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .contributor-meta {
    color: var(--color-text-600);
    font-family: var(--font-mono);
    font-size: var(--text-xs);
    line-height: 1.5;
  }

  .contributor-arrow {
    display: inline-flex;
    flex-shrink: 0;
    color: var(--color-primary-600);
  }

  .contributors-unavailable {
    max-width: 42rem;
    margin: 0;
    padding: var(--space-lg);
    border: 1px dashed var(--surface-color);
    border-radius: var(--radius-lg);
    color: var(--color-text-700);
    line-height: 1.65;
  }

  /* `:global` because the link arrives through `{@html rich(...)}` — the
     catalogue string owns it, so Svelte's compiler can't see it in the
     markup and would prune a plain descendant selector. Still scoped by
     the class, so it can't leak past this block. */
  .contributors-unavailable :global(a) {
    color: var(--color-primary-600);
  }

  @media (max-width: 700px) {
    .contributors-heading {
      align-items: flex-start;
      flex-direction: column;
      gap: var(--space-md);
    }

    .contributor-row {
      gap: var(--space-3);
    }

    .contributor-avatar {
      width: 2.75rem;
      height: 2.75rem;
    }
  }
</style>
