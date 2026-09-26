<!--
  Universal/App Link hand-off page — https://inkwell.ewancroft.uk/open?uri=<AT-URI>.

  This is the page a browser actually renders. On a device with Inkwell
  installed and Associated Domains / App Links verified, the OS never
  gets this far: iOS's `onContinueUserActivity(NSUserActivityTypeBrowsingWeb)`
  and Android's autoVerify intent-filter both intercept the link before
  it reaches Safari/Chrome, then verify the claimed AT-URI against the
  document's own PDS record and discovery link (see
  `iOS/Inkwell/App/VerifiedDeepLinkResolver.swift` and
  `Android/.../deeplink/HttpsDeepLinkResolver.kt`) before ever routing it
  into the Reader. This page is only what devices *without* the app (or
  without it installed) see — a graceful fallback, never a verifier.
  Nothing here should be trusted as having confirmed the AT-URI actually
  belongs to whoever the link claims.
-->

<script lang="ts">
  import { onMount } from "svelte";
  import { page } from "$app/state";
  import { SITE, ALTSTORE_SOURCE_LINK, FDROID_REPO_LINK } from "$lib/config";
  import { Download, ExternalLink, BookOpen } from "@lucide/svelte";

  // Mirrors the document-AT-URI shape both platforms' HttpsDeepLinkPolicy
  // require — at://<did>/site.standard.document/<record key>. This is a
  // *shape* check only, purely to decide whether attempting the custom
  // scheme is worthwhile; it is not, and must never be treated as, a
  // verification that the URI genuinely belongs to this content.
  const DOCUMENT_AT_URI = /^at:\/\/did:[a-zA-Z0-9._:%-]+\/site\.standard\.document\/[^/?#]+$/;

  const rawUri = $derived(page.url.searchParams.get("uri"));
  const rawUrl = $derived(page.url.searchParams.get("url"));
  const candidateDocumentUri = $derived(
    rawUri && DOCUMENT_AT_URI.test(rawUri) ? rawUri : null,
  );
  const customSchemeHref = $derived(
    candidateDocumentUri
      ? `inkwell://document?uri=${encodeURIComponent(candidateDocumentUri)}`
      : null,
  );

  onMount(() => {
    // Best-effort only: if Inkwell is installed but the OS didn't hand this
    // specific navigation to the app (e.g. the user chose "Open in Safari"
    // explicitly, or App Link verification hasn't propagated yet), give the
    // custom scheme one more chance before the visitor sees the fallback
    // below. If nothing is registered for `inkwell://`, this is a no-op —
    // no error, no visible failure, the fallback UI just stays on screen.
    if (customSchemeHref) {
      window.location.href = customSchemeHref;
    }
  });
</script>

<svelte:head>
  <title>{SITE.title} — Open</title>
  <meta name="description" content="Open a Standard.site document in Inkwell, or install Inkwell first." />
  <meta name="robots" content="noindex" />
</svelte:head>

<section class="site-container page-hd">
  <h1 class="page-title">Opening in Inkwell&hellip;</h1>
  <p class="page-desc">
    {#if candidateDocumentUri}
      If Inkwell is installed, it should open this document automatically.
      Didn&rsquo;t work? Install Inkwell below, then try the link again —
      once installed, Inkwell verifies the document against its author's
      own PDS record before showing it.
    {:else if rawUrl}
      This link points at a Standard.site document outside Inkwell's own
      domain, so your browser can't hand it to the app automatically.
      Install Inkwell, then open
      <span class="font-mono text-sm break-all">{rawUrl}</span>
      from inside the app — sharing or pasting a Standard.site link into
      Inkwell resolves and verifies it the same way.
    {:else}
      This is Inkwell's hand-off page for Standard.site document links. On
      its own — without a document link — there's nothing for it to open.
    {/if}
  </p>
</section>

<section class="site-container py-12">
  <div class="feature-grid">
    <div class="feature-card">
      <div class="feature-icon"><Download class="h-5 w-5" /></div>
      <h3>Don&rsquo;t have Inkwell yet?</h3>
      <p>
        Free direct installs are available through AltStore Classic (iOS)
        and F-Droid (Android). There is no App Store or Google Play
        listing yet.
      </p>
      <div class="flex flex-wrap gap-3">
        <a href={ALTSTORE_SOURCE_LINK} class="btn btn-primary active-press">
          <Download class="h-4 w-4" />
          iOS — AltStore source
        </a>
        <a href={FDROID_REPO_LINK} class="btn btn-primary active-press">
          <Download class="h-4 w-4" />
          Android — F-Droid repo
        </a>
      </div>
    </div>

    <div class="feature-card">
      <div class="feature-icon"><BookOpen class="h-5 w-5" /></div>
      <h3>Already have it installed?</h3>
      <p>
        Go back and open the original link again — once App Links /
        Associated Domains verification has propagated for this device,
        Inkwell opens it directly without ever showing this page.
      </p>
      <a href="/" class="btn btn-outline active-press">
        <ExternalLink class="h-4 w-4" />
        Back to inkwell.ewancroft.uk
      </a>
    </div>
  </div>
</section>
