<script lang="ts">
  import { SITE } from "$lib/config";
</script>

<svelte:head>
  <title>Privacy Policy — {SITE.title}</title>
</svelte:head>

<article class="container prose py-16 animate-in">
  <h1>Privacy Policy</h1>

  <p><strong>Effective Date: <time datetime="2026-06-21">21 June 2026</time></strong></p>

  <p>
    Inkwell is a decentralized client for the Standard.site ecosystem on the
    AT Protocol. We believe your data belongs to you.
  </p>

  <h2>1. Data Collection &amp; Usage</h2>
  <p>
    Inkwell is a localized client application. The developer (Ewan Croft) does
    not collect, store, or harvest any personal data, analytics, or usage
    metrics on proprietary servers. All data you read, write, or publish is
    communicated directly between your device and your authenticated Personal
    Data Server (PDS) or the wider AT Protocol network.
  </p>

  <h2>2. Authentication</h2>
  <p>
    Inkwell uses OAuth 2.1 to sign in to your AT Protocol account securely via
    the system browser. Your OAuth tokens are stored securely on your device
    using Apple&rsquo;s native Keychain. Inkwell never sees or stores your account
    password or app password.
  </p>

  <h2>3. Third-Party Services</h2>
  <p>To function, Inkwell communicates with external services:</p>
  <ul>
    <li><strong>Your PDS &amp; AT Protocol:</strong> Standard network infrastructure to fetch and publish your content.</li>
    <li><strong>AT Protocol identity services:</strong> Standard DNS and PLC directory lookups for identity resolution.</li>
    <li><strong>Leaflet Search:</strong> Used as a cross-platform search index for Standard.site records.</li>
  </ul>
  <p>Queries to these public services are subject to their respective privacy and data retention policies.</p>

  <h2>4. Changes to this Policy</h2>
  <p>
    We may update this policy occasionally to reflect new features or App Store
    requirements. Continued use of the app constitutes acceptance of these changes.
  </p>

  <h2>5. Contact</h2>
  <p>
    For privacy-related inquiries, please email
    <a href="mailto:contact@ewancroft.uk">contact@ewancroft.uk</a>
    or create an issue on the
    <a href="https://github.com/ewanc26/inkwell">GitHub repository</a>.
  </p>
</article>
