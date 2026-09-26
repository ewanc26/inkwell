<!--
  Terms of Service / EULA page.
  AGPL-3.0 licence with user-conduct guidelines for the AT Protocol
  network. Inkwell is sideloaded via AltStore / F-Droid, so no
  store-imposed EULA applies. Prose layout identical to /privacy.

  The liability carve-outs in section 6 are not optional boilerplate:
  UK law (CRA 2015 s65, UCTA s2) makes a blanket "as is" disclaimer
  unenforceable without them, which would risk taking the whole clause
  down with it. Translations must keep them; the provenance notice states
  that the British English text prevails either way.

  Nothing on this page is written here. The body is generated from
  legal/terms.md (British English) and legal/terms.fr.md by
  `node tools/legal/render.mjs` into $lib/legal/documents.ts, which also
  regenerates the in-app copy both apps render from the shared KMP
  module's LegalDocuments.kt. Edit the sources, not this file.

  The <title>/description live in the root layout's route-metadata table.
-->

<script lang="ts">
  import LegalDocument from "$lib/components/LegalDocument.svelte";
  import type { PageData } from "./$types";

  let { data }: { data: PageData } = $props();
</script>

<LegalDocument doc="terms" locale={data.locale} />
