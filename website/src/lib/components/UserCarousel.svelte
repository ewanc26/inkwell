<!--
  The "Users already using Inkwell" carousel, shared by the landing page
  and /features. It was duplicated in both routes; a second copy would
  have meant a second place for the empty state to go missing.
-->

<script lang="ts">
  import type { Snippet } from "svelte";
  import { fmt, rich, type Messages } from "$lib/i18n";
  import type { InkwellUser } from "$lib/inkwellUser";

  let {
    users,
    messages,
    children,
  }: {
    users: InkwellUser[];
    messages: Messages;
    /** Optional trailing call to action, rendered under the carousel. */
    children?: Snippet;
  } = $props();

  const m = $derived(messages.users);
</script>

<section class="site-container section">
  <h2 class="section-title">{m.heading}</h2>
  <p class="mb-6 max-w-[42rem] text-lg leading-relaxed text-pretty">
    {@html rich(m.intro)}
  </p>

  {#if users.length > 0}
    <div class="user-carousel">
      {#each users as user (user.handle)}
        <div class="user-card reveal">
          {#if user.avatar}
            <img
              class="user-avatar"
              src={user.avatar}
              alt={fmt(m.avatarAlt, { handle: user.handle })}
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
    <div class="user-empty">{@html rich(m.empty)}</div>
  {/if}

  {@render children?.()}
</section>
