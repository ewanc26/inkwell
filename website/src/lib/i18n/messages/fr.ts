// ── French message catalogue ─────────────────────────────────────
// Translated from the British English source in ./en-GB.ts, which stays
// authoritative: this file is annotated `Messages`, so removing or
// renaming a key there fails `pnpm check` here until the translation is
// brought back into line.
//
// Left untranslated on purpose: product and format names (Inkwell,
// Standard.site, Markpub, Leaflet, pckt, Offprint, AltStore, F-Droid,
// Ko-fi, Keychain, EncryptedSharedPreferences), protocol identifiers and
// NSIDs inside `code` spans, and the acronyms AT Protocol / PDS / OAuth /
// DPoP / AGPL, which are the terms a French-speaking reader will actually
// search for.

import type { Messages } from "./index";

export const fr: Messages = {
  // ── App shell ────────────────────────────────────────────────
  nav: {
    skipToContent: "Aller au contenu",
    primaryLabel: "Navigation principale",
    mainLabel: "Navigation du site",
    brandHome: "Inkwell, accueil",
    openMenu: "Ouvrir le menu",
    closeMenu: "Fermer le menu",
    links: {
      home: "Accueil",
      features: "Fonctionnalités",
      security: "Sécurité",
      about: "À propos",
      download: "Installer Inkwell",
      source: "Code source",
    },
  },

  footer: {
    copyright:
      "© {year} Inkwell — un lecteur et un éditeur pour [Standard.site](standardSite) sur l'[AT Protocol](atproto)",
    navLabel: "Navigation de pied de page",
    links: {
      privacy: "Confidentialité",
      terms: "Conditions",
      github: "GitHub",
      kofi: "Ko-fi",
      sponsors: "GitHub Sponsors",
    },
    languageLabel: "Langue",
  },

  // ── Per-route metadata ───────────────────────────────────────
  meta: {
    "/": {
      title: "Inkwell — lecteur et éditeur Standard.site sur AT Protocol",
      description:
        "Un lecteur et un éditeur natifs pour l'écosystème de publication Standard.site sur l'AT Protocol. Lisez, découvrez et publiez des écrits portables depuis votre propre PDS.",
    },
    "/features": {
      title: "Inkwell — Fonctionnalités",
      description:
        "Ce que fait réellement Inkwell : lire du contenu Markpub, Leaflet, pckt et Offprint, découvrir des publications, écrire et publier sur votre propre PDS, l'OAuth AT Protocol natif et la vérification intégrée.",
    },
    "/security": {
      title: "Inkwell — Sécurité",
      description:
        "Comment Inkwell gère l'authentification, le stockage des clés et la vérification : OAuth 2.1 avec jetons liés par DPoP, aucun mot de passe d'application, stockage local dans le Keychain ou l'Android Keystore, et aucun suivi ni statistique.",
    },
    "/about": {
      title: "Inkwell — À propos",
      description:
        "Pourquoi Inkwell existe : un lecteur et un éditeur natifs pour Standard.site sur l'AT Protocol, conçus comme un logiciel libre et open source plutôt qu'une vue web ou un onglet Bluesky.",
    },
    "/privacy": {
      title: "Politique de confidentialité — Inkwell",
      description:
        "Ce qu'Inkwell conserve, où il le conserve, et les deux seules exceptions à l'absence de collecte : le retour d'information facultatif dans l'application et les journaux serveur de ce site.",
    },
    "/terms": {
      title: "Conditions d'utilisation — Inkwell",
      description:
        "Les conditions d'utilisation et le CLUF d'Inkwell : licence AGPL-3.0, conduite des utilisateurs sur le réseau AT Protocol et régime de responsabilité en droit britannique.",
    },
  },

  // ── Shared: the Inkwell-user carousel (home + features) ──────
  users: {
    heading: "Ils utilisent déjà Inkwell",
    intro:
      "Les utilisateurs d'Inkwell à travers le réseau. Ce carrousel est généré à partir des rétroliens Constellation vers l'enregistrement de lexique `uk.ewancroft.inkwell.user`, en résolvant les DID vers les avatars Bluesky et les alias de handle via Slingshot.",
    avatarAlt: "Avatar de {handle}",
    empty:
      "Personne ne s'est encore déclaré utilisateur d'Inkwell. Activez **Me déclarer utilisateur d'Inkwell** dans les réglages de l'application et vous apparaîtrez ici — l'enregistrement vit dans votre propre PDS, et cette liste est construite uniquement à partir de rétroliens Constellation publics.",
  },

  // ── Landing page ─────────────────────────────────────────────
  home: {
    markLabel: "Inkwell",
    hero: {
      title: "Lire, découvrir et publier des écrits {brand}",
      brand: "Standard.site",
      description:
        "Inkwell est un lecteur et un éditeur pour l'écosystème de publication Standard.site sur l'AT Protocol. Disponible pour iOS et Android. Vos écrits vivent sur votre Personal Data Server — pas de silo, pas d'enfermement.",
      metaLabel: "Informations sur le produit",
      meta: ["AT Protocol", "OAuth 2.1", "iOS et Android"],
      cta: "Installer Inkwell",
      viewSource: "Voir le code source",
      badgesLabel: "Badges du projet",
      badgeAlt: {
        ios: "Dernière version d'Inkwell pour iOS",
        android: "Dernière version d'Inkwell pour Android",
        licence: "AGPL-3.0",
        sponsor: "Devenir sponsor",
      },
    },
    download: {
      heading: "Installer Inkwell",
      intro:
        "Les installations directes gratuites sont disponibles dès maintenant via AltStore Classic et F-Droid. Des versions App Store et Google Play à 5 £ sont prévues ; les liens vers les boutiques ci-dessous sont des espaces réservés jusqu'à la mise en ligne de ces fiches.",
      ios: {
        heading: "iOS",
        body: "Installez Inkwell gratuitement via sa source AltStore Classic auto-hébergée. Une version App Store à 5 £ est prévue comme voie d'installation grand public facultative.",
        addSource: "Ajouter la source AltStore",
        store: "App Store — 5 £ prévu",
        storeAria:
          "Espace réservé pour la fiche App Store — version à 5 £ prévue",
        note: "Cette source fonctionne avec [AltStore Classic](altstore) — la version gratuite et mondiale d'AltStore pour l'installation manuelle (un ordinateur est nécessaire pour la première installation, et les applications se renouvellent tous les 7 jours). Le bouton App Store pointe pour l'instant vers une URL de fiche fictive et sera remplacé lorsque la fiche payante existera.",
      },
      android: {
        heading: "Android",
        body: "Installez gratuitement la version Android expérimentale depuis le dépôt F-Droid auto-hébergé d'Inkwell. Une version Google Play à 5 £ est prévue comme voie d'installation grand public facultative.",
        addSource: "Ajouter le dépôt F-Droid",
        store: "Google Play — 5 £ prévu",
        storeAria:
          "Espace réservé pour la fiche Google Play — version à 5 £ prévue",
        note: "Vous pouvez aussi ouvrir directement [inkwell.ewancroft.uk/fdroid/repo](fdroidRepo) pour le parcourir ou scanner son QR code. Le bouton Google Play utilise l'URL de paquet attendue comme espace réservé jusqu'à la mise en ligne de la fiche. Code source sur [GitHub](github).",
      },
    },
    screenshots: {
      heading: "Inkwell en action",
      intro:
        "Le même espace de travail apaisé à trois onglets sur iOS et Android — lire, découvrir et écrire, entièrement depuis votre propre PDS.",
      platform: { ios: "iOS", android: "Android" },
      caption: {
        read: "Lire",
        discover: "Découvrir",
        write: "Écrire",
        article: "Article",
      },
      alt: {
        read: "Le lecteur d'Inkwell sur {platform} affichant un document publié",
        discover:
          "La découverte dans Inkwell sur {platform} affichant des résultats de recherche",
        write:
          "L'éditeur d'Inkwell sur {platform} affichant l'écran de rédaction",
        article: "Un article vérifié ouvert dans Inkwell sur {platform}",
      },
    },
    what: {
      heading: "Ce que fait Inkwell",
      read: {
        heading: "Lire et découvrir",
        body: "Parcourez les publications et les documents de tout l'écosystème Standard.site — contenus Markpub, Leaflet, pckt et Offprint, rendus nativement depuis le PDS qui les héberge. Cherchez dans l'index public et abonnez-vous aux publications que vous suivez.",
      },
      write: {
        heading: "Écrire",
        body: "Rédigez en Markdown et publiez sur votre propre PDS dans le format de votre choix. Vos écrits restent un enregistrement portable dans votre dépôt — ils ne sont pas enfermés dans Inkwell.",
      },
      verified: {
        heading: "Natif et vérifié",
        body: "Connexion OAuth sans mot de passe d'application, et chaque publication ou document peut être confronté à son point de terminaison `.well-known` et à son lien canonique avant que vous ne lui accordiez votre confiance.",
      },
      link: "Voir toutes les fonctionnalités",
    },
    secure: {
      heading: "Sûr par conception",
      body: "Inkwell utilise OAuth 2.1 pour se connecter à votre Personal Data Server. Votre navigateur s'ouvre une seule fois pour approuver l'accès — aucun mot de passe n'est jamais vu ni conservé par l'application. Les jetons sont liés par DPoP et conservés dans le stockage sécurisé de la plateforme, et il n'y a ni statistiques ni suivi, ni dans l'application ni sur ce site.",
      link: "En savoir plus sur la sécurité",
    },
    compare: {
      heading: "Pourquoi ne pas simplement utiliser le navigateur ?",
      intro:
        "Le contenu Standard.site se trouve sur le web ouvert : vous pouvez toujours le lire dans un onglet de navigateur. Voici ce qu'un client natif apporte en plus.",
      columnBrowser: "Dans un onglet de navigateur",
      columnApp: "Dans Inkwell",
      rows: [
        {
          label: "Connexion",
          browser: "Se reconnecter à chaque publication visitée",
          app: "Une seule connexion OAuth à votre PDS, et c'est réglé",
        },
        {
          label: "Identifiants",
          browser: "Conservés dans l'état de session ordinaire du navigateur",
          app: "Conservés dans le Keychain ou l'Android Keystore",
        },
        {
          label: "Suivre des auteurs",
          browser: "Mettre chaque site en favori et revenir voir à la main",
          app: "Un seul fil d'abonnements couvrant toutes les publications",
        },
        {
          label: "Authenticité",
          browser: "En juger d'après la barre d'adresse",
          app: "Confrontée au `.well-known` et au lien canonique",
        },
        {
          label: "Écriture",
          browser:
            "Un éditeur web distinct, propre à chaque lieu de publication",
          app: "Rédiger et publier sans quitter l'application",
        },
      ],
    },
    faq: {
      heading: "Questions fréquentes",
      items: [
        {
          question: "Inkwell est-il sur l'App Store ou le Play Store ?",
          answer:
            "Pas encore. Inkwell s'installe depuis sa propre source AltStore auto-hébergée (iOS) ou son dépôt F-Droid (Android). La [section d'installation](download) mentionne aussi les voies App Store et Google Play à 5 £ prévues, mais ces boutons utilisent pour l'instant des URL de fiches fictives, jusqu'à ce que les vraies pages existent. AltStore Classic et F-Droid restent les voies d'installation gratuites.",
        },
        {
          question: "Ai-je besoin d'un compte Bluesky ?",
          answer:
            "Non. Inkwell a besoin d'un compte AT Protocol et d'un PDS, pas spécifiquement d'un compte hébergé par Bluesky. Un compte hébergé par Bluesky fonctionne, mais un compte sur un autre PDS compatible ou que vous hébergez vous-même fonctionne tout autant. Vous vous connectez avec votre handle AT Protocol via OAuth.",
        },
        {
          question: "Quels formats de publication Inkwell prend-il en charge ?",
          answer:
            "Inkwell lit et publie du contenu Markpub Markdown, Leaflet, pckt et Offprint. Il rend ces formats nativement et conserve le `textContent` portable comme solution de repli lorsqu'un client ne comprend pas le format de corps le plus riche. Voir [Fonctionnalités](features) pour le détail format par format.",
        },
        {
          question: "Puis-je modifier des documents déjà publiés ?",
          answer:
            "Oui. Inkwell peut ouvrir des documents existants pour les modifier et publier une révision mise à jour dans votre dépôt. Si vous choisissez de convertir un document d'un format à un autre, l'éditeur signale le contenu qui ne peut pas être converti sans perte avant que vous ne publiiez la conversion.",
        },
        {
          question: "Qu'advient-il de mes écrits si Inkwell disparaît ?",
          answer:
            "Rien ne leur arrive — vos documents sont des enregistrements AT Protocol dans votre propre dépôt sur votre PDS, et non des données conservées par Inkwell. N'importe quel client qui comprend les schémas d'enregistrement Standard.site peut les lire ou les modifier, avec ou sans Inkwell.",
        },
        {
          question:
            "Inkwell conserve-t-il mes écrits ou mon compte sur ses propres serveurs ?",
          answer:
            "Non. Inkwell lit et écrit votre contenu directement sur votre PDS et ne dispose d'aucune base de données intermédiaire de vos documents. Votre session OAuth et votre clé DPoP sont conservées sur votre appareil, dans le stockage sécurisé de la plateforme ; l'application ne contient aucun SDK de statistiques ou de suivi. Voir [Sécurité](security) et [Confidentialité](privacy) pour le détail complet.",
        },
        {
          question: "Faut-il connaître l'AT Protocol pour s'en servir ?",
          answer:
            "Non. Il vous faudra un compte AT Protocol et un PDS pour vous connecter — Inkwell n'en crée pas pour vous — mais à partir de là, il fonctionne comme n'importe quelle application de lecture et d'écriture. Voir [À propos](about) si vous voulez le contexte.",
        },
        {
          question: "Comment fonctionnent les notifications ?",
          answer:
            "Inkwell n'envoie pas vos abonnements à un service de notifications push. L'application vérifie périodiquement les publications que vous suivez et crée des notifications locales sur votre appareil lorsqu'elle trouve de nouveaux documents. Vous pouvez désactiver les bannières du système sans perdre l'historique des notifications dans l'application.",
        },
        {
          question: "Puis-je utiliser le même compte sur iOS et Android ?",
          answer:
            "Oui. Connectez-vous au même compte AT Protocol et les deux applications liront les mêmes publications, documents, abonnements et recommandations adossés à votre dépôt, depuis votre PDS. Les préférences locales à l'appareil — apparence, accessibilité, réglages de notification — se configurent séparément sur chaque appareil.",
        },
        {
          question:
            "Quelles versions d'iOS et d'Android sont prises en charge ?",
          answer:
            "L'application iOS prend en charge iOS 18 et versions ultérieures. L'application Android prend en charge Android 8.0 (API 26) et versions ultérieures, et cible actuellement l'API Android 36.",
        },
        {
          question: "La version Android est-elle prête ?",
          answer:
            "Oui, pour un usage normal. Android couvre désormais l'essentiel de l'expérience Inkwell : lecture et découverte, publication et modification dans les formats Standard.site pris en charge, commentaires et interactions, abonnements et recommandations, vérification, notifications, réglages, accessibilité et personnalisation. Elle reste étiquetée expérimentale parce que sa couverture de tests automatisés propre à Android est comparativement mince et qu'iOS demeure l'implémentation principale, plus aboutie.",
        },
        {
          question: "Contre quoi la vérification protège-t-elle ?",
          answer:
            "Une publication ou un document peut revendiquer une adresse web canonique, mais une revendication seule n'est pas une preuve. Inkwell confronte cette revendication au point de terminaison `.well-known` du site et à sa balise `<link>` canonique, et fait apparaître toute discordance au lieu de l'accepter en silence. Plus de détails sur [Sécurité](security).",
        },
      ],
    },
    availability: {
      heading: "Disponibilité",
      cardHeading: "iOS en priorité, Android utilisable dès aujourd'hui",
      body: "L'application iOS d'Inkwell reste l'implémentation principale et la plus aboutie, mais Android couvre désormais les parcours essentiels : lecture, découverte, écriture, interactions, vérification, notifications, réglages et accessibilité. Android reste étiquetée expérimentale le temps que ses tests spécifiques à la plateforme et les cas limites restants rattrapent leur retard.",
    },
    finalCta: "Installer Inkwell",
  },

  // ── Features page ────────────────────────────────────────────
  features: {
    title: "Fonctionnalités",
    description:
      "Inkwell est un lecteur et un éditeur natifs pour l'écosystème de publication Standard.site. Voici ce que cela signifie en pratique, sur iOS comme sur Android.",
    cards: {
      read: {
        heading: "Lire",
        body: "Inkwell rend le contenu nativement au lieu de charger une vue web. Il comprend quatre formats Standard.site : le Markdown Markpub (écriture longue simple), les pages à blocs et à blobs de Leaflet (mises en page riches avec médias intégrés), les tableaux de blocs de pckt et ceux d'Offprint — chacun converti et rendu dans la surface de lecture propre à l'application, en respectant partout le Dynamic Type et le thème clair/sombre de votre système. Chaque document est récupéré directement depuis le PDS qui l'héberge, et non depuis une copie en cache sur un serveur tiers.",
      },
      discover: {
        heading: "Découvrir",
        body: "Cherchez dans l'index public multiplateforme pour trouver des publications dans tout l'écosystème Standard.site, puis abonnez-vous à celles que vous souhaitez suivre. L'actualisation en arrière-plan interroge périodiquement vos abonnements et déclenche une notification locale lorsque l'un d'eux publie un nouveau document — sur l'appareil, sans fournisseur de push ni infrastructure de notification côté serveur. Une liste de notifications conserve un historique que vous pouvez consulter ou effacer, et un réglage désactive la bannière du système sans perdre cette liste.",
      },
      write: {
        heading: "Écrire",
        body: "Rédigez en Markdown et publiez directement sur votre propre PDS dans le format de votre choix — Markpub, Leaflet, pckt ou Offprint. Comme l'enregistrement sous-jacent est un enregistrement AT Protocol standard dans votre dépôt, vos écrits ne sont pas enfermés dans Inkwell : n'importe quel client qui comprend les mêmes lexiques peut les lire, les modifier ou les migrer. C'est tout l'intérêt de publier sur son propre PDS plutôt que dans la base de données d'une plateforme.",
      },
      native: {
        heading: "Natif AT Protocol",
        body: "La connexion ouvre une seule fois le navigateur du système, où vous approuvez l'accès directement auprès de votre PDS avec OAuth 2.1 — le même parcours que pour se connecter à un site web via son fournisseur d'identité. Inkwell ne voit ni ne conserve jamais le mot de passe de votre compte, ni un mot de passe d'application. Au quotidien, cela signifie que la poignée de main dans le navigateur n'a lieu qu'à la connexion ; ensuite, Inkwell détient un jeton d'accès de courte durée lié par DPoP et le renouvelle automatiquement, de sorte que vous restez connecté sans répéter l'étape d'approbation.",
      },
      verification: {
        heading: "Vérification intégrée",
        body: "Les publications et les documents sur Standard.site peuvent revendiquer une adresse web canonique, mais une revendication seule n'est pas une preuve. Inkwell la vérifie : il récupère le point de terminaison `.well-known` de la publication et le compare à l'enregistrement, et il recherche une balise `<link>` canonique correspondante sur la page publiée. Cela détecte le cas où un enregistrement de document ou de publication pointe vers un site qu'il ne contrôle pas réellement — une réponse `.well-known` usurpée ou périmée, ou un lien canonique qui ne correspond pas — et vous signale cette discordance au lieu de traiter en silence chaque lien revendiqué comme digne de confiance.",
      },
      accessibility: {
        heading: "Accessibilité et apparence",
        body: "La taille du texte, le texte en gras, un mode d'augmentation du contraste qui bascule le premier plan en noir ou blanc pur selon l'arrière-plan courant, et les liens soulignés sont autant de réglages gratuits et inconditionnels — jamais conditionnés à quoi que ce soit. Une couleur d'accent, une police de lecture et une apparence distinctes vous permettent en outre de faire de la surface de lecture la vôtre. Inkwell n'a ni publicité, ni péage, ni offre premium ; une unique invitation à soutenir le projet sur Ko-fi, que l'on peut écarter, apparaît une seule fois, à la première modification d'un réglage, et plus jamais ensuite.",
      },
    },
    differ: {
      heading: "Où iOS et Android diffèrent",
      body: "iOS est l'implémentation principale d'Inkwell et reçoit les nouvelles fonctionnalités en premier. L'application Android reste étiquetée expérimentale, mais elle a comblé l'essentiel de l'écart : la lecture, la découverte, l'écriture dans tous les formats Standard.site, les commentaires, la vérification et les notifications en arrière-plan sont tous implémentés et utilisables aujourd'hui.",
    },
    getEither: "Installer Inkwell sur l'une ou l'autre plateforme",
  },

  // ── Security page ────────────────────────────────────────────
  security: {
    title: "Sécurité",
    description:
      "Inkwell est un client pour vos propres données. Cette page explique simplement comment il s'authentifie, ce qu'il conserve et ce qu'il ne collecte pas.",
    cards: {
      oauth: {
        heading: "OAuth 2.1, aucun mot de passe d'application",
        body: "La connexion ouvre une seule fois le navigateur du système, où vous approuvez l'accès directement auprès de votre PDS — le même genre de parcours que « Se connecter avec » n'importe quel fournisseur d'identité. Inkwell ne voit ni ne conserve jamais le mot de passe de votre compte. Il n'existe aucune solution de repli héritée par mot de passe d'application : si un PDS ne prend en charge qu'OAuth, Inkwell fait de même.",
      },
      dpop: {
        heading: "Jetons liés par DPoP",
        body: "Inkwell demande des jetons DPoP (Demonstrating Proof-of-Possession) plutôt que de simples jetons porteurs. Chaque requête est signée avec une clé privée qui ne quitte jamais votre appareil : un jeton d'accès volé ne suffit donc pas à usurper une requête — il doit aussi être rejoué avec une preuve valide issue de cette clé précise. Les sessions se renouvellent automatiquement en arrière-plan ; le jeton effectivement transmis sur le réseau est toujours de courte durée.",
      },
      storage: {
        heading: "Stockage des clés sur l'appareil",
        body: "Votre session OAuth et votre clé privée DPoP sont conservées dans le stockage sécurisé de la plateforme, et non dans des préférences lisibles par l'application : le Keychain d'Apple sur iOS, et sur Android une enveloppe chiffrée AES-256-GCM scellée par une clé non exportable qui réside dans l'Android Keystore, protégée matériellement lorsque l'appareil dispose d'un élément sécurisé. Android exclut la session OAuth de la sauvegarde et du transfert d'appareil, car la clé du Keystore ne peut pas être restaurée de façon sûre. La déconnexion ou la désinstallation les supprime. Le détail complet — y compris le comportement de sauvegarde sur chaque plateforme — figure dans la [politique de confidentialité](privacy).",
      },
      analytics: {
        heading: "Aucune statistique, aucun suivi",
        body: "Inkwell ne contient aucun SDK de statistiques, aucun SDK de rapport de plantage, aucune régie publicitaire et aucune télémétrie propriétaire. Le développeur ne collecte pas de données d'usage depuis l'application. Ce site ne dépose aucun cookie, n'exécute aucune statistique et n'intègre aucun traceur tiers — Vercel, en tant qu'hébergeur, enregistre des journaux de requêtes serveur standard pour la distribution et la sécurité, et cela s'arrête là.",
      },
      verification: {
        heading: "Vérification",
        body: "Les publications et les documents peuvent revendiquer une adresse web canonique. Inkwell confronte cette revendication au point de terminaison `.well-known` de la publication et à la balise `<link>` canonique de la page publiée : toute discordance — une réponse `.well-known` usurpée ou périmée, ou un lien canonique qui pointe ailleurs — vous est signalée au lieu d'être acceptée en silence. Voir [Fonctionnalités](features) pour en savoir plus sur son fonctionnement.",
      },
      control: {
        heading: "Votre PDS, votre contrôle",
        body: "Inkwell est un client, pas un service qui héberge votre contenu. Vos écrits vivent dans votre propre dépôt AT Protocol, sur le PDS que vous choisissez ou hébergez vous-même. Inkwell y lit et y écrit directement ; il n'existe aucune base de données intermédiaire de votre contenu sur l'infrastructure du développeur.",
      },
    },
    legal: {
      heading: "Lire le détail juridique",
      body: "La politique de confidentialité et les conditions d'utilisation précisent exactement ce qui est conservé, où et pendant combien de temps, y compris les deux seules exceptions à l'« absence de collecte de données » : le retour d'information facultatif dans l'application et les journaux serveur de ce site.",
      privacy: "Politique de confidentialité",
      terms: "Conditions d'utilisation",
    },
  },

  // ── About page ───────────────────────────────────────────────
  about: {
    title: "À propos",
    description:
      "Pourquoi une application dédiée existe pour cela, qui la développe et où en sont les choses aujourd'hui.",
    protocol: {
      heading: "Standard.site et l'AT Protocol, en bref",
      atproto:
        "L'AT Protocol est le réseau décentralisé qui sous-tend Bluesky et un ensemble croissant d'applications indépendantes — votre identité et vos données vivent dans un dépôt que vous contrôlez, sur un Personal Data Server (PDS) que vous ou une personne de confiance administrez, plutôt que dans la base de données d'une seule entreprise. Pour le tableau complet, [atproto.com](atproto) traite du protocole lui-même.",
      standardSite:
        "Standard.site est un écosystème de publication construit au-dessus de ce réseau — un ensemble partagé de schémas d'enregistrement pour l'écriture longue, afin que les publications et les documents rédigés dans un client restent portables et lisibles dans un autre. Markpub, Leaflet, pckt et Offprint sont tous des formats de publication Standard.site ; Inkwell lit et écrit les quatre. Voir [standard.site](standardSite) pour l'écosystème lui-même.",
    },
    native: {
      heading: "Pourquoi une application native, et non une vue web",
      body: "La publication décentralisée mérite un client natif de premier ordre — pas un onglet de navigateur enveloppé dans une coquille d'application, ni une fonctionnalité greffée sur une application sociale qui n'a pas été conçue pour l'écriture longue. Une application native peut s'intégrer correctement au stockage sécurisé d'identifiants de la plateforme, respecter la typographie et les réglages d'accessibilité du système, fonctionner avec l'actualisation en arrière-plan et les notifications locales, et donner le sentiment d'appartenir à votre appareil plutôt que d'être empruntée au web. C'est le postulat de départ d'Inkwell.",
    },
    who: {
      heading: "Qui le développe",
      body: "Inkwell est un logiciel libre et open source, sous licence [AGPL-3.0](agpl) assortie d'une [exception de distribution sur les boutiques d'applications](appStoreException). Cette exception est une permission supplémentaire étroite pour la distribution en boutique ; elle ne supprime ni l'obligation de mise à disposition du code ni le copyleft de l'AGPL. Le code, les tickets et les versions se trouvent tous sur [GitHub](github). Si Inkwell vous est utile, vous pouvez soutenir son développement via [Ko-fi](kofi) ou [GitHub Sponsors](sponsors) — c'est entièrement facultatif et sans rapport avec un quelconque verrou fonctionnel.",
    },
    status: {
      heading: "Où en sont les choses",
      today:
        "iOS est l'implémentation principale, écrite avec SwiftUI. Android est un portage Jetpack Compose qui partage avec iOS un cœur Kotlin Multiplatform commun pour le traitement des enregistrements et la logique de vérification. Elle reste étiquetée expérimentale, mais la lecture, la découverte, l'écriture, les commentaires, la vérification et les notifications en arrière-plan sont tous implémentés et utilisables aujourd'hui — iOS reçoit simplement les nouveautés en premier. Aucune des deux applications n'est encore distribuée via l'App Store ou le Play Store ; toutes deux s'installent depuis la source AltStore et le dépôt F-Droid auto-hébergés d'Inkwell. Voir [Fonctionnalités](features) pour ce que chaque plateforme fait réellement aujourd'hui.",
      plan: "Le plan à plus long terme est d'ajouter l'App Store d'Apple et Google Play comme voies de distribution grand public facultatives, pour un achat unique de 5 £. Ces versions sont censées être la même application open source, et non une édition premium : AltStore Classic sur iOS et F-Droid sur Android resteront des solutions gratuites, le code demeurant disponible sous AGPL-3.0.",
    },
    contributors: {
      heading: "Contributeurs",
      intro:
        "Inkwell est développé en public. Ces comptes GitHub ont des commits qui leur sont attribués dans le dépôt ; la liste se met à jour automatiquement.",
      viewOnGitHub: "Voir sur GitHub",
      attributedOne: "{count} commit attribué par GitHub",
      attributedOther: "{count} commits attribués par GitHub",
      unavailable:
        "Les données sur les contributeurs sont momentanément indisponibles. Vous pouvez tout de même [consulter le graphe des contributeurs sur GitHub](githubContributors).",
    },
    openSource: {
      heading: "Open source",
      body: "Lisez le code, ouvrez un ticket ou proposez une pull request. Les contributions sont bienvenues sous AGPL-3.0, avec l'exception de distribution sur les boutiques d'applications propre à Inkwell.",
      link: "Voir le dépôt",
    },
    support: {
      heading: "Soutenir le projet",
      body: "Inkwell n'a ni publicité, ni péage, ni offre premium. Si vous souhaitez soutenir le développement continu, le parrainage est facultatif et apprécié.",
      kofi: "Ko-fi",
      sponsors: "GitHub Sponsors",
    },
  },

  // ── Legal pages ──────────────────────────────────────────────
  legal: {
    privacyHeading: "Politique de confidentialité",
    termsHeading: "Conditions d'utilisation et CLUF",
    versionLine: "Version {version} — Date d'entrée en vigueur : {date}",
    versionLineUndated: "Traduit de la version {version}",
    provenanceLabel: "À propos de cette traduction",
    translationNotice:
      "Ceci est une traduction de la version {version} de la source en anglais britannique, revue le {reviewedOn}. Le [texte en anglais britannique](sourceDoc) est la version qui fait foi ; en cas de divergence, c'est le texte en anglais britannique qui prévaut.",
    staleNotice:
      "**Cette traduction n'est plus à jour.** Elle a été traduite de la version {version} de la source en anglais britannique. La version {currentVersion} est en vigueur depuis le {currentDate}, et sa formulation peut différer de ce que vous lisez ci-dessous. Reportez-vous au [texte en anglais britannique](sourceDoc) pour les conditions qui s'appliquent actuellement.",
  },
};

export default fr;
