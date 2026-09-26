Inkwell est un client décentralisé pour l'écosystème Standard.site sur l'AT Protocol. Vos données vous appartiennent. La présente politique s'applique à Inkwell pour iOS ({{IOS_VERSION}}, distribué via AltStore), à Inkwell pour Android ({{ANDROID_VERSION}}, distribué via un dépôt F-Droid auto-hébergé) et au site web inkwell.ewancroft.uk.

## 1. Qui est responsable de vos données

Inkwell est développé et publié par Ewan Croft, développeur individuel établi au Royaume-Uni, agissant en qualité de responsable du traitement pour les traitements limités décrits dans la présente politique. Cette politique est rédigée pour satisfaire au RGPD britannique (UK GDPR) et au Data Protection Act 2018.

Contact : [contact@ewancroft.uk](mailto:contact@ewancroft.uk). Aucun délégué à la protection des données n'a été désigné séparément ; Inkwell n'est pas tenu d'en désigner un.

Inkwell est un client, pas un service. Pour le contenu que vous lisez et publiez sur le réseau AT Protocol, **c'est l'exploitant de votre Personal Data Server (PDS) qui est responsable du traitement**, et non Inkwell. Si vous hébergez vous-même votre PDS, vous êtes responsable du traitement pour votre propre dépôt.

## 2. Collecte et utilisation des données

Inkwell est une application cliente locale. Le développeur ne collecte, ne conserve et ne récolte aucune donnée personnelle, statistique ou mesure d'usage sur des serveurs propriétaires. Inkwell ne contient aucune publicité, aucun suivi, aucun SDK de statistiques et aucun SDK de rapport de plantage. Tout le contenu que vous lisez, écrivez ou publiez est échangé directement entre votre appareil et le PDS concerné ou le réseau AT Protocol au sens large.

Il existe deux exceptions étroites, décrites aux sections 7 et 8 : la fonctionnalité facultative de retour d'information dans l'application, et les journaux serveur de ce site web.

## 3. Données conservées sur votre appareil

- **iOS :** votre session OAuth (jetons d'accès et de rafraîchissement) et la clé privée DPoP P-256 sont conservées dans le Keychain d'Apple, avec la classe de protection `kSecAttrAccessibleAfterFirstUnlock`. Votre handle, les indications de PDS, l'état des notifications, vos préférences de lecture et un relevé local des URI d'enregistrements récemment vus sont conservés dans UserDefaults.
- **Android :** votre session OAuth (jetons d'accès et de rafraîchissement, ainsi que la clé privée DPoP) est chiffrée sur votre appareil avec AES-256-GCM, sous une clé non exportable qu'Inkwell génère dans l'Android Keystore, protégée matériellement sur les appareils dotés d'un élément sécurisé. Seuls le texte chiffré résultant, son nonce et une version de format sont écrits dans le stockage privé de l'application, si bien que le fichier conservé est illisible sans la clé, et que la clé ne quitte jamais le Keystore. La session est explicitement exclue de la sauvegarde Android et du transfert d'appareil, car sa clé de Keystore ne peut pas être restaurée avec elle. L'état des notifications, les URI d'enregistrements déjà vus et les préférences sont conservés dans des SharedPreferences ordinaires, privées à l'application.

Ces données ne quittent jamais votre appareil, sauf dans le cadre d'une sauvegarde d'appareil (voir la section 6). Désinstaller l'application, ou vous déconnecter, les supprime.

## 4. Authentification

Inkwell utilise OAuth 2.1 avec DPoP pour se connecter à votre compte AT Protocol via le navigateur du système. Inkwell ne voit ni ne conserve jamais le mot de passe de votre compte, ni un mot de passe d'application. Vos jetons ne sont détenus que sur votre appareil et ne sont envoyés qu'à votre propre PDS.

## 5. Notifications

Sur iOS comme sur Android, avec votre autorisation, Inkwell peut afficher des notifications locales lorsqu'une publication à laquelle vous êtes abonné publie un nouveau document. Les notifications sont générées sur l'appareil par une interrogation périodique en arrière-plan — l'actualisation d'application en arrière-plan sur iOS, WorkManager sur Android. Inkwell n'utilise ni Apple Push Notification service, ni Firebase Cloud Messaging, ni aucun autre fournisseur de push, et aucune donnée de notification n'est envoyée au développeur. L'état des notifications et les URI déjà vus sont conservés localement et purgés automatiquement.

## 6. Sauvegarde

La version Android autorise la sauvegarde automatique dans le nuage, par Android, des données d'application hors session. La session OAuth chiffrée est explicitement exclue à la fois de la sauvegarde dans le nuage et du transfert d'appareil, car sa clé de Keystore ne peut pas être restaurée de façon sûre.

Sur iOS, les éléments du Keychain ne sont pas synchronisés avec le Keychain iCloud, parce qu'Inkwell ne les marque pas comme synchronisables. Cependant, comme ils utilisent la classe de protection `AfterFirstUnlock` plutôt qu'une classe limitée à l'appareil, ils *peuvent* être inclus dans une sauvegarde chiffrée iCloud ou Finder et restaurés sur un appareil de remplacement. Déconnectez-vous avant d'effectuer une sauvegarde si vous ne le souhaitez pas.

## 7. Retour d'information dans l'application (facultatif)

Si vous utilisez la fonctionnalité « Envoyer un retour », Inkwell crée un enregistrement `app.userinput.discussion` **dans votre propre dépôt**, pointant vers l'espace de retour d'information d'Inkwell sur [userinput.app](https://userinput.app), exploité par le développeur. Cet enregistrement est public : il contient le titre et le corps que vous avez rédigés ainsi que l'heure de rédaction, et il est attribuable à votre DID et à votre handle. Il est visible par quiconque sur le réseau AT Protocol et peut être répliqué par des index et des relais tiers échappant au contrôle du développeur.

- **Base légale :** le consentement (UK GDPR art. 6(1)(a)) — vous choisissez de l'envoyer. La fonctionnalité est entièrement facultative et l'application est pleinement utilisable sans elle.
- **Conservation :** l'enregistrement vit dans votre dépôt jusqu'à ce que vous le supprimiez. Le développeur maintient le retour visible sur le tableau aussi longtemps qu'il est utile, et ne le copie dans aucun autre système.
- **Suppression :** comme l'enregistrement est le vôtre, vous pouvez le supprimer de votre propre dépôt à tout moment avec n'importe quel client AT Protocol. Le supprimer le retire du tableau. Les copies détenues par des index tiers échappent au contrôle du développeur.

Ne mettez pas d'informations personnelles sensibles dans un retour d'information.

## 8. Ce site web

`inkwell.ewancroft.uk` est hébergé sur Vercel, qui agit en qualité de sous-traitant et enregistre des journaux de requêtes serveur standard (y compris les adresses IP) à des fins de sécurité et de distribution. Le site ne dépose aucun cookie, n'exécute aucune statistique et n'intègre aucun traceur tiers : aucune bannière de consentement n'est donc requise au titre des Privacy and Electronic Communications Regulations.

- **Base légale :** les intérêts légitimes (UK GDPR art. 6(1)(f)) — exploiter et sécuriser le site.
- **Transferts :** Vercel peut traiter les données de journalisation en dehors du Royaume-Uni, y compris aux États-Unis, dans le cadre de l'addendum britannique aux clauses contractuelles types de l'UE.
- **Conservation :** selon la durée de conservation des journaux propre à Vercel, généralement de l'ordre de quelques jours.

## 9. Services tiers

Pour fonctionner, Inkwell contacte des services externes directement depuis votre appareil. Dans chaque cas, votre adresse IP est nécessairement visible par ce service. Aucun d'entre eux ne rend compte au développeur.

- **Votre PDS et le réseau AT Protocol :** pour récupérer et publier votre contenu.
- **Services d'identité AT Protocol :** requêtes DNS et `plc.directory` pour résoudre les handles et les DID.
- **Leaflet Search (`leaflet-search-backend.fly.dev`) :** un index de recherche multiplateforme pour les enregistrements Standard.site. Vos termes de recherche lui sont transmis.
- **Constellation (`constellation.microcosm.blue`) :** pour découvrir les rétroliens entre dépôts et les décomptes de recommandations.
- **CDN de Bluesky (`cdn.bsky.app`) :** uniquement pour les médias fournis par les réponses de l'AppView de Bluesky ; les blobs Standard.site sont récupérés depuis le PDS qui les héberge.
- **API publique de Bluesky (`public.api.bsky.app`) :** pour afficher les posts, profils et listes Bluesky intégrés qui apparaissent dans les documents que vous lisez.
- **userinput.app :** uniquement si vous utilisez la fonctionnalité facultative de retour d'information (section 7).
- **Ko-fi et GitHub Sponsors :** uniquement si vous touchez un lien de soutien, qui les ouvre dans votre navigateur. Tout paiement est traité entièrement par eux, selon leurs propres politiques ; le développeur ne reçoit jamais vos informations de paiement via Inkwell.

Les requêtes adressées à ces services sont soumises à leurs propres politiques de confidentialité et de conservation des données. La base légale de ces contacts est l'exécution du service que vous avez demandé (UK GDPR art. 6(1)(b)) ou, pour les services facultatifs, votre consentement.

## 10. Vos droits

En vertu du UK GDPR, vous disposez du droit d'accéder à vos données personnelles, de les rectifier, de les effacer, d'en limiter le traitement, de vous y opposer et de les porter, ainsi que du droit de retirer votre consentement à tout moment sans que cela n'affecte les traitements déjà effectués.

En pratique, comme le développeur ne détient presque rien : les données sur votre appareil relèvent de votre seul contrôle et sont supprimées par la déconnexion ou la désinstallation ; les données de votre dépôt AT Protocol relèvent de votre contrôle et de celui de l'exploitant de votre PDS, et peuvent être modifiées ou supprimées avec n'importe quel client ; et les enregistrements de retour d'information peuvent être supprimés par vous comme décrit à la section 7. Pour toute autre demande, écrivez à [contact@ewancroft.uk](mailto:contact@ewancroft.uk) et vous recevrez une réponse dans le délai d'un mois.

Si vous n'êtes pas satisfait de la manière dont vos données ont été traitées, vous pouvez porter plainte auprès de l'Information Commissioner's Office à l'adresse [ico.org.uk/make-a-complaint](https://ico.org.uk/make-a-complaint/), en appelant le 0303 123 1113, ou par courrier à Information Commissioner's Office, Wycliffe House, Water Lane, Wilmslow, Cheshire SK9 5AF. Vous pouvez également saisir la justice.

## 11. Décisions automatisées

Inkwell ne réalise aucun profilage ni aucune prise de décision automatisée produisant des effets juridiques ou des effets similairement significatifs.

## 12. Enfants

Inkwell ne s'adresse pas aux enfants et n'est pas destiné aux personnes de moins de 13 ans. Le développeur ne traite pas sciemment les données personnelles d'enfants de moins de 13 ans.

## 13. Modifications de la présente politique

La présente politique peut être mise à jour pour tenir compte de nouvelles fonctionnalités ou d'obligations légales. Le numéro de version et la date d'entrée en vigueur figurant en tête reflètent toujours la version en cours, et l'historique complet se trouve dans le dépôt Git public. Lorsqu'une modification affecte matériellement vos droits, elle est signalée dans l'application ou dans les notes de version, et non effectuée en silence.

## 14. Contributions assistées par IA

Des outils d'IA peuvent être utilisés pour contribuer à Inkwell. Les contributeurs devraient ajouter des mentions `Co-authored-by:` créditant les agents d'IA lorsqu'ils ont contribué de façon matérielle, afin que l'attribution reste honnête et exacte. Cela n'a aucune incidence sur le traitement de vos données personnelles.

## 15. Contact

Pour toute question relative à la confidentialité, écrivez à [contact@ewancroft.uk](mailto:contact@ewancroft.uk) ou ouvrez un ticket sur le dépôt GitHub d'[Inkwell](https://github.com/ewanc26/inkwell).
