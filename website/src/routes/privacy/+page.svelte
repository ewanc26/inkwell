<!--
  Privacy Policy page.
  Legal text rendered as prose; the content is substantive enough
  to warrant its own route rather than a modal.  Readable at
  any device width.
-->

<script lang="ts">
  import { SITE } from "$lib/config";
</script>

<svelte:head>
  <title>Privacy Policy — {SITE.title}</title>
</svelte:head>

<article class="site-container prose py-16 animate-in">
  <h1>Privacy Policy</h1>

  <p>
    <strong>Version 1.1 &mdash; Effective Date: <time datetime="2026-08-13">13 August 2026</time></strong>
  </p>

  <p>
    Inkwell is a decentralized client for the Standard.site ecosystem on the
    AT Protocol. We believe your data belongs to you. This policy applies to
    Inkwell for iOS (version 1.0, build 49, distributed via AltStore) and
    Inkwell for Android (version 1.2.0, distributed via a self-hosted F-Droid
    repository).
  </p>

  <h2>1. Data Collection &amp; Usage</h2>
  <p>
    Inkwell is a localized client application. The developer (Ewan Croft) does
    not collect, store, or harvest personal data, analytics, or usage metrics
    on proprietary servers. Inkwell contains no ads, tracking, or analytics
    software. All content you read, write, or publish is communicated directly
    between your device and the relevant Personal Data Server (PDS) or the
    wider AT Protocol network.
  </p>

  <h2>2. Data Stored on Your Device</h2>
  <ul>
    <li>
      <strong>iOS:</strong> Your OAuth session (access and refresh tokens) and
      the P-256 DPoP private key are stored in Apple&rsquo;s Keychain. Non-sensitive
      handle and PDS hints, notification state, and a local record of recently
      seen URIs are stored in UserDefaults.
    </li>
    <li>
      <strong>Android:</strong> Your OAuth session is stored in
      EncryptedSharedPreferences, backed by a hardware-backed MasterKey.
    </li>
  </ul>

  <h2>3. Authentication</h2>
  <p>
    Inkwell uses OAuth 2.1 to sign in to your AT Protocol account securely via
    the system browser. Inkwell never sees or stores your account password or
    app password.
  </p>

  <h2>4. Notifications</h2>
  <p>
    On iOS, with your permission, Inkwell may show local notifications when a
    publication you follow publishes a new document. Notifications are
    generated on-device from background refresh; Inkwell does not use push
    notification services. Notification state and already-seen URIs are stored
    locally. The Android build declares the notification permission but does
    not yet send notifications.
  </p>

  <h2>5. Backup</h2>
  <p>
    The Android build allows Android&rsquo;s automatic cloud backup of app data,
    which may include your stored OAuth session. You can disable this in your
    device&rsquo;s backup settings or by signing out. iOS Keychain items are not
    synchronized to iCloud or included in ordinary device backups.
  </p>

  <h2>6. Third-Party Services</h2>
  <p>To function, Inkwell communicates with external services:</p>
  <ul>
    <li><strong>Your PDS &amp; AT Protocol:</strong> Standard network infrastructure to fetch and publish your content.</li>
    <li><strong>AT Protocol identity services:</strong> Standard DNS and PLC directory lookups for identity resolution.</li>
    <li><strong>Leaflet Search:</strong> Used as a cross-platform search index for Standard.site records.</li>
    <li><strong>Constellation (microcosm.blue):</strong> Used to discover cross-repository backlinks and recommend counts.</li>
  </ul>
  <p>Queries to these public services are subject to their respective privacy and data retention policies.</p>

  <h2>7. Changes to this Policy</h2>
  <p>
    We may update this policy occasionally to reflect new features or legal
    requirements. Continued use of the app constitutes acceptance of these changes.
  </p>

  <h2>8. Contact</h2>
  <p>
    For privacy-related inquiries, please email
    <a href="mailto:contact@ewancroft.uk">contact@ewancroft.uk</a>
    or create an issue on the
    <a href="https://github.com/ewanc26/inkwell">Inkwell</a> GitHub
    repository.
  </p>

  <h2>9. AI-assisted contributions</h2>
  <p>
    AI tools may be used when contributing to Inkwell. Contributors should
    add <code>Co-authored-by:</code> trailers crediting AI agents when they
    materially contributed, so attribution stays honest and accurate.
  </p>
</article>
