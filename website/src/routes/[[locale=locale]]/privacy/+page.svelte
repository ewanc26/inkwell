<!--
  Privacy Policy page.
  Legal text rendered as prose; the content is substantive enough to
  warrant its own route rather than a modal. Readable at any device width.

  Nothing on this page is written here. The body is generated from
  legal/privacy.md (British English) and legal/privacy.fr.md by
  `node tools/legal/render.mjs` into $lib/legal/documents.ts, which also
  regenerates the in-app copy both apps render from the shared KMP
  module's LegalDocuments.kt. The heading, version line, and translation
  provenance come from the message catalogue. Edit the sources, not this
  file, and never hand-edit $lib/legal/documents.ts.

  The <title>/description live in the root layout's route-metadata table.
-->

<script lang="ts">
  import LegalDocument from "$lib/components/LegalDocument.svelte";
  import type { PageData } from "./$types";

  let { data }: { data: PageData } = $props();
</script>

<LegalDocument doc="privacy" locale={data.locale} />
