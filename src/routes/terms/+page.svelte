<!--
  Terms of Service / EULA page.
  AGPL-3.0 + Apple App Store EULA legalese, with user-conduct
  guidelines for the AT Protocol network.  Prose layout
  identical to /privacy.
-->

<script lang="ts">
  import { SITE } from "$lib/config";
</script>

<svelte:head>
  <title>Terms of Service — {SITE.title}</title>
</svelte:head>

<article class="container prose py-16 animate-in">
  <h1>Terms of Service &amp; EULA</h1>

  <p>
    By downloading or using Inkwell, you agree to these terms. Inkwell is
    licensed to you under the terms of the AGPL 3.0 License, alongside
    Apple&rsquo;s standard App Store End User License Agreement.
  </p>

  <h2>1. User-Generated Content</h2>
  <p>
    Inkwell acts as a portal to the AT Protocol network. You are solely
    responsible for the content you publish.
  </p>
  <p>In alignment with Apple App Store guidelines:</p>
  <ul>
    <li>You must not publish illegal, highly objectionable, or abusive content.</li>
    <li>Inkwell provides native tools to mute or block users and report objectionable content to the network&rsquo;s moderation services.</li>
    <li>There is zero tolerance for abusive users. We reserve the right to filter or hide content locally within the client that violates App Store guidelines.</li>
  </ul>

  <h2>2. Decentralized Network Disclaimer</h2>
  <p>
    Because Inkwell connects to a decentralized network, the developer has no
    control over the content published by other users. You may encounter content
    you find objectionable. Please use the built-in blocking and filtering tools
    to curate your experience.
  </p>

  <h2>3. &ldquo;As Is&rdquo; Basis</h2>
  <p>
    Inkwell is provided &ldquo;as is,&rdquo; without warranty of any kind, express
    or implied. The developer is not liable for any data loss, service
    interruptions, or issues arising from your PDS or the AT Protocol network.
  </p>
</article>
