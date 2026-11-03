<!--
  Error boundary for every route. The root layout resolves the locale from
  the URL; with no prefixed locale registered, every missing route speaks
  British English. Metadata for this page comes from the layout's error
  branch; this component owns only the visible body.
-->

<script lang="ts">
  import { page } from "$app/state";
  import { fmt, localizeHref, messagesFor, resolveLocale } from "$lib/i18n";

  const resolved = $derived(resolveLocale(page.url.pathname));
  const m = $derived(messagesFor(resolved.locale));
  const copy = $derived(
    page.status === 404 ? m.error.notFound : m.error.server,
  );
</script>

<svelte:head>
  <meta name="robots" content="noindex" />
</svelte:head>

<section class="site-container page-hd">
  <p class="page-meta">{fmt(m.error.status, { status: page.status })}</p>
  <h1 class="page-title">{copy.title}</h1>
  <p class="page-desc">{copy.body}</p>

  <div class="mt-8 flex flex-wrap gap-4">
    <a
      class="btn btn-primary active-press"
      href={localizeHref("/", resolved.locale)}
    >
      {m.error.home}
    </a>
    <a
      class="btn btn-outline active-press"
      href={localizeHref("/features", resolved.locale)}
    >
      {m.error.features}
    </a>
  </div>
</section>
