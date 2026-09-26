// ── British English message catalogue (source locale) ────────────
// This file is the source of truth for every piece of visible copy on
// the site, and its inferred type (`Messages`, exported from ../messages)
// is what every other locale is checked against. Adding a key here makes
// `pnpm check` fail until each translation supplies it — which is the
// point: a missing translation should be a build error, not a silent
// fallback to English inside an otherwise French page.
//
// Strings may carry the small inline markup subset documented in
// ../rich.ts (`code`, **bold**, [label](linkKey)). Placeholders written
// as `{name}` are filled by `fmt()` from ../index.ts.
//
// Deliberately NOT in here: protocol identifiers, NSIDs, URLs, version
// numbers, and brand names. Those live in $lib/config, in the LINKS table
// in ../rich.ts, or in the generated legal module, so no translation can
// rewrite something an external service consumes.

export const enGB = {
  // ── App shell ────────────────────────────────────────────────
  nav: {
    skipToContent: "Skip to content",
    primaryLabel: "Primary navigation",
    mainLabel: "Main navigation",
    /** Accessible name for the wordmark link back to the home page. */
    brandHome: "Inkwell, home",
    openMenu: "Open menu",
    closeMenu: "Close menu",
    links: {
      home: "Home",
      features: "Features",
      security: "Security",
      about: "About",
      download: "Get Inkwell",
      source: "Source",
    },
  },

  footer: {
    copyright:
      "© {year} Inkwell — a reader & writer for [Standard.site](standardSite) on the [AT Protocol](atproto)",
    navLabel: "Footer navigation",
    links: {
      privacy: "Privacy",
      terms: "Terms",
      github: "GitHub",
      kofi: "Ko-fi",
      sponsors: "GitHub Sponsors",
    },
    languageLabel: "Language",
  },

  // ── Per-route metadata ───────────────────────────────────────
  // Keyed by source-locale path. The layout owns every <title> and
  // description so they localise in one place rather than six.
  meta: {
    "/": {
      title: "Inkwell — Reader & Writer for Standard.site on AT Protocol",
      description:
        "A native reader and writer for the Standard.site publishing ecosystem on AT Protocol. Read, discover, and publish portable writing from your own PDS.",
    },
    "/features": {
      title: "Inkwell — Features",
      description:
        "What Inkwell actually does: reading Markpub, Leaflet, pckt, and Offprint content, discovering publications, writing and publishing to your own PDS, native AT Protocol OAuth, and built-in verification.",
    },
    "/security": {
      title: "Inkwell — Security",
      description:
        "How Inkwell handles authentication, key storage, and verification: OAuth 2.1 with DPoP-bound tokens, no app passwords, on-device Keychain/EncryptedSharedPreferences storage, and no analytics or tracking.",
    },
    "/about": {
      title: "Inkwell — About",
      description:
        "Why Inkwell exists: a native reader and writer for Standard.site on AT Protocol, built as free and open-source software instead of a web view or a Bluesky tab.",
    },
    "/privacy": {
      title: "Privacy Policy — Inkwell",
      description:
        "What Inkwell stores, where it stores it, and the two narrow exceptions to collecting nothing: optional in-app feedback and this website's server logs.",
    },
    "/terms": {
      title: "Terms of Service — Inkwell",
      description:
        "Inkwell's Terms of Service and EULA: AGPL-3.0 licensing, user conduct on the AT Protocol network, and the liability position under UK law.",
    },
  },

  // ── Shared: the Inkwell-user carousel (home + features) ──────
  users: {
    heading: "Users already using Inkwell",
    intro:
      "Inkwell users across the network. This carousel is generated from Constellation backlinks to the `uk.ewancroft.inkwell.user` lexicon record, resolving DIDs to Bluesky avatars and handle aliases via Slingshot.",
    avatarAlt: "{handle} avatar",
    empty:
      "No one has declared themselves an Inkwell user yet. Flip **Declare me as an Inkwell user** in the app's Settings and you'll show up here — the record lives in your own PDS, and the list is built entirely from public Constellation backlinks.",
  },

  // ── Landing page ─────────────────────────────────────────────
  home: {
    markLabel: "Inkwell",
    hero: {
      /** `{brand}` is replaced with the accent-coloured brand name. */
      title: "Read, discover, and publish {brand} writing",
      brand: "Standard.site",
      description:
        "Inkwell is a reader and writer for the Standard.site publishing ecosystem on AT Protocol. Available for iOS and Android. Your writing lives on your Personal Data Server — no silos, no lock-in.",
      metaLabel: "Product metadata",
      meta: ["AT Protocol", "OAuth 2.1", "iOS & Android"],
      cta: "Get Inkwell",
      viewSource: "View source",
      badgesLabel: "Project badges",
      badgeAlt: {
        ios: "Latest Inkwell release for iOS",
        android: "Latest Inkwell release for Android",
        licence: "AGPL-3.0",
        sponsor: "Sponsor",
      },
    },
    download: {
      heading: "Get Inkwell",
      intro:
        "Free direct installs are available now through AltStore Classic and F-Droid. £5 App Store and Google Play builds are planned; the store links below are placeholders until those listings go live.",
      ios: {
        heading: "iOS",
        body: "Install Inkwell free through its self-hosted AltStore Classic source. A £5 App Store build is planned as an optional mainstream install route.",
        addSource: "Add AltStore source",
        store: "App Store — planned £5",
        storeAria: "App Store listing placeholder — planned £5 build",
        note: "This source works with [AltStore Classic](altstore) — the free, worldwide sideloading AltStore (requires a computer for the first install, apps refresh every 7 days). The App Store button currently points at a placeholder listing URL and will be replaced when the paid listing exists.",
      },
      android: {
        heading: "Android",
        body: "Install the experimental Android build free from Inkwell's self-hosted F-Droid repository. A £5 Google Play build is planned as an optional mainstream install route.",
        addSource: "Add F-Droid repo",
        store: "Google Play — planned £5",
        storeAria: "Google Play listing placeholder — planned £5 build",
        note: "Or open [inkwell.ewancroft.uk/fdroid/repo](fdroidRepo) directly to browse it or scan its QR code. The Google Play button uses the expected package URL as a placeholder until the listing is live. Source on [GitHub](github).",
      },
    },
    screenshots: {
      heading: "See it in action",
      intro:
        "The same calm, three-tab workspace on iOS and Android — read, discover, and write, all from your own PDS.",
      platform: { ios: "iOS", android: "Android" },
      caption: {
        read: "Read",
        discover: "Discover",
        write: "Write",
        article: "Article",
      },
      alt: {
        read: "Inkwell reader on {platform} showing a published document",
        discover: "Inkwell discover on {platform} showing search results",
        write: "Inkwell writer on {platform} showing the compose screen",
        article: "A verified article open in Inkwell on {platform}",
      },
    },
    what: {
      heading: "What Inkwell does",
      read: {
        heading: "Read & Discover",
        body: "Browse publications and documents across the Standard.site ecosystem — Markpub, Leaflet, pckt, and Offprint content, rendered natively from their owning PDS. Search the public index and subscribe to the publications you follow.",
      },
      write: {
        heading: "Write",
        body: "Compose in Markdown and publish to your own PDS in your choice of format. Your writing stays a portable record in your repository — not locked into Inkwell.",
      },
      verified: {
        heading: "Native & verified",
        body: "OAuth sign-in with no app passwords, and every publication or document can be checked against its `.well-known` endpoint and canonical link before you trust it.",
      },
      link: "See all features",
    },
    secure: {
      heading: "Secure by design",
      body: "Inkwell uses OAuth 2.1 to sign in to your Personal Data Server. Your browser opens once to approve access — no password is ever seen or stored by the app. Tokens are DPoP-bound and held in the platform's secure store, and there's no analytics or tracking in the app or on this site.",
      link: "Read about security",
    },
    compare: {
      heading: "Why not just use the browser?",
      intro:
        "Standard.site content is on the open web, so you can always read it in a browser tab. Here's what a native client adds.",
      columnBrowser: "In a browser tab",
      columnApp: "In Inkwell",
      rows: [
        {
          label: "Sign-in",
          browser: "Sign in again at each publication you visit",
          app: "One OAuth sign-in to your PDS, then you're done",
        },
        {
          label: "Credentials",
          browser: "Held in ordinary browser session state",
          app: "Held in the Keychain or EncryptedSharedPreferences",
        },
        {
          label: "Following writers",
          browser: "Bookmark each site and check back manually",
          app: "One subscribed feed across every publication",
        },
        {
          label: "Authenticity",
          browser: "Judge it by the address bar",
          app: "Checked against `.well-known` and the canonical link",
        },
        {
          label: "Writing",
          browser: "A separate web editor, wherever you publish",
          app: "Compose and publish without leaving the app",
        },
      ],
    },
    faq: {
      heading: "Frequently asked",
      items: [
        {
          question: "Is Inkwell in the App Store or Play Store?",
          answer:
            "Not yet. Inkwell installs from its own self-hosted AltStore source (iOS) or F-Droid repository (Android). The [download section](download) includes the planned £5 App Store and Google Play routes too, but those buttons currently use placeholder listing URLs until the real store pages exist. AltStore Classic and F-Droid remain the free install routes.",
        },
        {
          question: "Do I need a Bluesky account?",
          answer:
            "No. Inkwell needs an AT Protocol account and a PDS, not specifically an account hosted by Bluesky. A Bluesky-hosted account works, but so does an account on another compatible PDS or one you self-host. You sign in with your AT Protocol handle through OAuth.",
        },
        {
          question: "Which publishing formats does Inkwell support?",
          answer:
            "Inkwell reads and publishes Markpub Markdown, Leaflet, pckt, and Offprint content. It renders those formats natively and keeps portable `textContent` available as a fallback when a client does not understand the richer body format. See [Features](features) for the format-by-format detail.",
        },
        {
          question: "Can I edit documents I already published?",
          answer:
            "Yes. Inkwell can open existing documents for editing and publish an updated revision back to your repository. If you choose to convert a document between formats, the editor reports content that cannot round-trip cleanly before you publish the conversion.",
        },
        {
          question: "What happens to my writing if Inkwell shuts down?",
          answer:
            "Nothing happens to it — your documents are AT Protocol records in your own repository on your PDS, not data stored by Inkwell. Any client that speaks the Standard.site record schemas can read or edit them, with or without Inkwell.",
        },
        {
          question:
            "Does Inkwell store my writing or account on its own servers?",
          answer:
            "No. Inkwell reads and writes your content directly on your PDS and has no intermediary database of your documents. Your OAuth session and DPoP key are stored on your device in the platform's secure storage; the app contains no analytics or tracking SDK. See [Security](security) and [Privacy](privacy) for the full detail.",
        },
        {
          question: "Do I need to know AT Protocol to use this?",
          answer:
            "No. You'll need an AT Protocol account and a PDS to sign in with — Inkwell doesn't create one for you — but from there it works like any reading and writing app. See [About](about) if you'd like the background.",
        },
        {
          question: "How do notifications work?",
          answer:
            "Inkwell does not send your subscriptions to a push-notification service. The app periodically checks the publications you follow and creates local notifications on your device when it finds new documents. You can turn the OS banners off without losing the in-app notification history.",
        },
        {
          question: "Can I use the same account on iOS and Android?",
          answer:
            "Yes. Sign in to the same AT Protocol account and both apps read the same repository-backed publications, documents, subscriptions, and recommendations from your PDS. Device-local preferences such as appearance, accessibility, and notification settings are configured separately on each device.",
        },
        {
          question: "What versions of iOS and Android are supported?",
          answer:
            "The iOS app supports iOS 18 and later. The Android app supports Android 8.0 (API 26) and later, and currently targets Android API 36.",
        },
        {
          question: "Is Android ready?",
          answer:
            "Yes, for normal use. Android now covers the core Inkwell experience: reading and discovery, publishing and editing across the supported Standard.site formats, comments and interactions, subscriptions and recommendations, verification, notifications, settings, and accessibility and customisation controls. It is still labelled experimental because its Android-specific automated coverage is comparatively thin and iOS remains the primary, more polished implementation.",
        },
        {
          question: "What does verification protect against?",
          answer:
            "A publication or document can claim a canonical web address, but a claim alone isn't proof. Inkwell checks that claim against the site's `.well-known` endpoint and its canonical `<link>` tag, surfacing a mismatch instead of silently trusting it. More detail on [Security](security).",
        },
      ],
    },
    availability: {
      heading: "Availability",
      cardHeading: "iOS primary, Android usable today",
      body: "Inkwell's iOS app remains the primary and more polished implementation, but Android now covers the core reading, discovery, writing, interaction, verification, notification, settings, and accessibility flows. Android is still labelled experimental while its platform-specific testing and the remaining edge cases catch up.",
    },
    finalCta: "Get Inkwell",
  },

  // ── Features page ────────────────────────────────────────────
  features: {
    title: "Features",
    description:
      "Inkwell is a native reader and writer for the Standard.site publishing ecosystem. Here's what that means in practice, on both iOS and Android.",
    cards: {
      read: {
        heading: "Read",
        body: "Inkwell renders content natively rather than loading a web view. It understands four Standard.site formats: Markpub Markdown (plain long-form writing), Leaflet's block and blob pages (rich layouts with embedded media), pckt's block arrays, and Offprint's block arrays — each converted and rendered in the app's own reading surface, with your system's Dynamic Type and light/dark theme respected throughout. Every document is fetched straight from its owning PDS, not a cached copy on a third-party server.",
      },
      discover: {
        heading: "Discover",
        body: "Search the cross-platform public index to find publications across the Standard.site ecosystem, then subscribe to the ones you want to follow. Background refresh polls subscriptions periodically and raises a local notification when one has a new document — on-device, with no push provider and no server-side notification infrastructure involved. A notification list keeps a history you can review or clear, and a Settings toggle turns the OS banner off without losing that list.",
      },
      write: {
        heading: "Write",
        body: "Compose in Markdown and publish directly to your own PDS in your choice of format — Markpub, Leaflet, pckt, or Offprint. Because the underlying record is a standard AT Protocol record in your repository, your writing isn't locked into Inkwell: any client that speaks the same lexicons can read, edit, or migrate it. That's the point of publishing on your own PDS instead of a platform's database.",
      },
      native: {
        heading: "AT Protocol native",
        body: "Signing in opens your system browser once, where you approve access directly with your PDS using OAuth 2.1 — the same flow as signing into a website with your identity provider. Inkwell never sees or stores your account password or an app password. Day to day, this means the browser handshake happens once at login; after that, Inkwell holds a short-lived, DPoP-bound access token and refreshes it automatically, so you stay signed in without repeating the approval step.",
      },
      verification: {
        heading: "Verification built in",
        body: "Publications and documents on Standard.site can claim a canonical web address, but a claim on its own isn't proof. Inkwell checks it: it fetches the publication's `.well-known` endpoint and compares it against the record, and it looks for a matching canonical `<link>` tag on the published page. This catches the case where a document or publication record points at a site it doesn't actually control — a spoofed or stale `.well-known` response, or a canonical link that doesn't match — and surfaces that mismatch to you rather than silently treating every claimed link as trustworthy.",
      },
      accessibility: {
        heading: "Accessibility & appearance",
        body: "Text size, bold text, an increase-contrast mode that snaps foreground to pure black or white against the current background, and underlined links are all free, unconditional settings — never gated behind anything. A separate accent colour, reading font, and appearance override let you make the reading surface your own. Inkwell has no ads, no paywalls, and no premium tier; a single dismissible prompt towards Ko-fi appears once, the first time you change a setting, and never again after that.",
      },
    },
    differ: {
      heading: "Where iOS and Android differ",
      body: "iOS is Inkwell's primary implementation and gets new features first. The Android app is still labelled experimental, but it has closed most of the gap: reading, discovery, writing across every Standard.site format, comments, verification, and background notifications are all implemented and usable today.",
    },
    getEither: "Get Inkwell for either platform",
  },

  // ── Security page ────────────────────────────────────────────
  security: {
    title: "Security",
    description:
      "Inkwell is a client for your own data. This page explains, plainly, how it authenticates, what it stores, and what it doesn't collect.",
    cards: {
      oauth: {
        heading: "OAuth 2.1, no app passwords",
        body: 'Signing in opens your system browser once, where you approve access directly with your PDS — the same kind of flow as "Sign in with" any identity provider. Inkwell never sees or stores your account password. There is no legacy app-password fallback: if a PDS only supports OAuth, so does Inkwell.',
      },
      dpop: {
        heading: "DPoP-bound tokens",
        body: "Inkwell requests DPoP (Demonstrating Proof-of-Possession) tokens rather than plain bearer tokens. Each request is signed with a private key that never leaves your device, so a stolen access token by itself isn't enough to impersonate a request — it also has to be replayed alongside a valid proof from that specific key. Sessions refresh automatically in the background; the token that's actually sent over the network is always short-lived.",
      },
      storage: {
        heading: "On-device key storage",
        body: "Your OAuth session and DPoP private key are held in the platform's secure storage, not app-readable preferences: Apple's Keychain on iOS, and Android's EncryptedSharedPreferences backed by a hardware-backed MasterKey on Android. Android excludes the OAuth session from backup and device transfer because the Keystore key cannot be restored safely. Signing out or uninstalling removes them. Full detail — including backup behaviour on each platform — is in the [Privacy Policy](privacy).",
      },
      analytics: {
        heading: "No analytics, no tracking",
        body: "Inkwell contains no analytics SDKs, no crash-reporting SDKs, no ad networks, and no proprietary telemetry. The developer doesn't collect usage data from the app. This website sets no cookies, runs no analytics, and embeds no third-party trackers — Vercel, as host, records standard server request logs for delivery and security, and that's the extent of it.",
      },
      verification: {
        heading: "Verification",
        body: "Publications and documents can claim a canonical web address. Inkwell checks that claim against the publication's `.well-known` endpoint and the canonical `<link>` tag on the published page, so a mismatch — a spoofed or stale `.well-known` response, or a canonical link pointing somewhere else — is surfaced to you instead of silently trusted. See [Features](features) for more on how this works.",
      },
      control: {
        heading: "Your PDS, your control",
        body: "Inkwell is a client, not a service that hosts your content. Your writing lives in your own AT Protocol repository, on whichever PDS you choose or self-host. Inkwell reads and writes to it directly; there's no intermediary database of your content on the developer's infrastructure.",
      },
    },
    legal: {
      heading: "Read the legal detail",
      body: "The Privacy Policy and Terms of Service spell out exactly what's stored, where, and for how long, including the two narrow exceptions to \"no data collection\" — optional in-app feedback and this website's server logs.",
      privacy: "Privacy Policy",
      terms: "Terms of Service",
    },
  },

  // ── About page ───────────────────────────────────────────────
  about: {
    title: "About",
    description:
      "Why a dedicated app exists for this, who builds it, and where things stand today.",
    protocol: {
      heading: "Standard.site and AT Protocol, briefly",
      atproto:
        "AT Protocol is the decentralized network behind Bluesky and a growing set of independent apps — your identity and data live in a repository you control, on a Personal Data Server (PDS) you or someone you trust operates, rather than inside one company's database. For the full picture, [atproto.com](atproto) covers the protocol itself.",
      standardSite:
        "Standard.site is a publishing ecosystem built on top of that network — a shared set of record schemas for long-form writing, so publications and documents you write in one client stay portable and readable in another. Markpub, Leaflet, pckt, and Offprint are all Standard.site publishing formats; Inkwell reads and writes all four. See [standard.site](standardSite) for the ecosystem itself.",
    },
    native: {
      heading: "Why a native app, not a web view",
      body: "Decentralized publishing deserves a first-class native client — not a browser tab wrapped in an app shell, and not a feature bolted onto a social app that wasn't designed for long-form writing. A native app can integrate properly with the platform's secure credential storage, respect system typography and accessibility settings, work with background refresh and local notifications, and feel like it belongs on your device rather than borrowed from the web. That's the premise Inkwell starts from.",
    },
    who: {
      heading: "Who builds it",
      body: "Inkwell is free and open-source software, licensed under the [AGPL-3.0](agpl) with an [App Store Distribution Exception](appStoreException). The exception is a narrow additional permission for app-store distribution; it does not remove the AGPL's source-availability or copyleft requirements. The source, issues, and releases are all on [GitHub](github). If Inkwell is useful to you, you can support its development through [Ko-fi](kofi) or [GitHub Sponsors](sponsors) — entirely optional, and unrelated to any feature gate.",
    },
    status: {
      heading: "Where things stand",
      today:
        "iOS is the primary implementation, built with SwiftUI. Android is a Jetpack Compose port that shares a common Kotlin Multiplatform core with iOS for record handling and verification logic. It's still labelled experimental, but reading, discovery, writing, comments, verification, and background notifications are all implemented and usable today — iOS simply gets new features first. Neither app is distributed through the App Store or Play Store yet; both install from Inkwell's own self-hosted AltStore source and F-Droid repository. See [Features](features) for what each platform actually does today.",
      plan: "The longer-term plan is to add the Apple App Store and Google Play as optional mainstream distribution routes for a flat £5 purchase. Those builds are intended to be the same open-source app, not a premium edition: AltStore Classic on iOS and F-Droid on Android will remain free alternatives, with the source continuing to be available under the AGPL-3.0.",
    },
    contributors: {
      heading: "Contributors",
      intro:
        "Inkwell is built in public. These GitHub accounts have commits attributed to them in the repository; the list updates automatically.",
      viewOnGitHub: "View on GitHub",
      attributedOne: "{count} commit attributed by GitHub",
      attributedOther: "{count} commits attributed by GitHub",
      unavailable:
        "Contributor data is temporarily unavailable. You can still [view the contributor graph on GitHub](githubContributors).",
    },
    openSource: {
      heading: "Open source",
      body: "Read the code, file an issue, or open a pull request. Contributions are welcome under the AGPL-3.0 with Inkwell's App Store Distribution Exception.",
      link: "View the repository",
    },
    support: {
      heading: "Support the project",
      body: "Inkwell has no ads, no paywalls, and no premium tier. If you'd like to support ongoing development, sponsorship is optional and appreciated.",
      kofi: "Ko-fi",
      sponsors: "GitHub Sponsors",
    },
  },

  // ── Legal pages ──────────────────────────────────────────────
  // The document bodies are generated into $lib/legal/documents.ts from
  // legal/*.md; these are only the headings and the provenance framing
  // around them, which is why they live in the catalogue like any other
  // UI copy.
  legal: {
    privacyHeading: "Privacy Policy",
    termsHeading: "Terms of Service & EULA",
    /** `{date}` is replaced with a <time> element, so it must appear once. */
    versionLine: "Version {version} — Effective Date: {date}",
    /** Used when a translation is stale and therefore has no live date. */
    versionLineUndated: "Translated from version {version}",
    provenanceLabel: "About this translation",
    /**
     * Shown on every translated legal page. Legal effect follows the
     * British English source, so the translation says so plainly rather
     * than implying it is independently binding.
     */
    translationNotice:
      "This is a translation of version {version} of the British English source, reviewed on {reviewedOn}. The [British English text](sourceDoc) is the authoritative version; where the two differ, the British English text prevails.",
    /**
     * Shown when the source has moved on since the translation was last
     * reviewed. It deliberately does not present the current effective
     * date over older wording.
     */
    staleNotice:
      "**This translation is out of date.** It was translated from version {version} of the British English source. Version {currentVersion} has been in force since {currentDate}, and its wording may differ from what you read below. Read the [British English text](sourceDoc) for the terms that currently apply.",
  },
};

export default enGB;
