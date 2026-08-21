Inkwell is a decentralised client for the Standard.site ecosystem on the AT Protocol. Your data belongs to you. This policy applies to Inkwell for iOS ({{IOS_VERSION}}, distributed via AltStore), Inkwell for Android ({{ANDROID_VERSION}}, distributed via a self-hosted F-Droid repository), and the website at inkwell.ewancroft.uk.

## 1. Who is responsible for your data

Inkwell is developed and published by Ewan Croft, an individual developer based in the United Kingdom, acting as the data controller for the limited processing described in this policy. This policy is written to meet the UK GDPR and the Data Protection Act 2018.

Contact: [contact@ewancroft.uk](mailto:contact@ewancroft.uk). There is no separately appointed Data Protection Officer; Inkwell is not required to appoint one.

Inkwell is a client, not a service. For the content you read and publish on the AT Protocol network, **your Personal Data Server (PDS) operator is the controller**, not Inkwell. If you self-host your PDS, you are the controller of your own repository.

## 2. Data collection & usage

Inkwell is a local client application. The developer does not collect, store, or harvest personal data, analytics, or usage metrics on proprietary servers. Inkwell contains no ads, no tracking, no analytics SDKs, and no crash-reporting SDKs. All content you read, write, or publish is communicated directly between your device and the relevant PDS or the wider AT Protocol network.

There are two narrow exceptions, described in sections 7 and 8: the optional in-app feedback feature, and the server logs of this website.

## 3. Data stored on your device

- **iOS:** Your OAuth session (access and refresh tokens) and the P-256 DPoP private key are stored in Apple’s Keychain, with the `kSecAttrAccessibleAfterFirstUnlock` protection class. Your handle, PDS hints, notification state, reader preferences, and a local record of recently seen record URIs are stored in UserDefaults.
- **Android:** Your OAuth session is stored in EncryptedSharedPreferences, backed by a hardware-backed MasterKey. Notification state, already-seen record URIs, and preferences are stored in ordinary app-private SharedPreferences.

This data never leaves your device except as part of a device backup (see section 6). Uninstalling the app, or signing out, removes it.

## 4. Authentication

Inkwell uses OAuth 2.1 with DPoP to sign in to your AT Protocol account via the system browser. Inkwell never sees or stores your account password or an app password. Your tokens are held only on your device and sent only to your own PDS.

## 5. Notifications

On both iOS and Android, with your permission, Inkwell may show local notifications when a publication you subscribe to publishes a new document. Notifications are generated on-device by periodic background polling — background app refresh on iOS, WorkManager on Android. Inkwell does not use Apple Push Notification service, Firebase Cloud Messaging, or any other push provider, and no notification data is sent to the developer. Notification state and already-seen URIs are stored locally and pruned automatically.

## 6. Backup

The Android build permits Android’s automatic cloud backup of app data, which may include your encrypted OAuth session. You can disable this in your device’s backup settings, or sign out before backing up.

On iOS, Keychain items are not synchronised to iCloud Keychain, because Inkwell does not mark them as synchronisable. However, because they use the `AfterFirstUnlock` protection class rather than a device-only class, they *can* be included in an encrypted iCloud or Finder backup and restored to a replacement device. Sign out before taking a backup if you do not want that.

## 7. In-app feedback (optional)

If you use the “Send Feedback” feature, Inkwell creates an `app.userinput.discussion` record **in your own repository**, pointing at Inkwell’s feedback space on [userinput.app](https://userinput.app), which is operated by the developer. That record is public: it contains the title and body you wrote, the time you wrote it, and is attributable to your DID and handle. It is visible to anyone on the AT Protocol network and may be mirrored by third-party indexes and relays outside the developer’s control.

- **Legal basis:** consent (UK GDPR Art. 6(1)(a)) — you choose to send it. The feature is entirely optional and the app is fully usable without it.
- **Retention:** the record lives in your repository until you delete it. The developer keeps feedback visible on the board for as long as it is useful, and does not copy it into any other system.
- **Deletion:** because the record is yours, you can delete it from your own repository at any time using any AT Protocol client. Deleting it removes it from the board. Copies held by third-party indexes are outside the developer’s control.

Do not put sensitive personal information in feedback.

## 8. This website

`inkwell.ewancroft.uk` is hosted on Vercel, which acts as a processor and records standard server request logs (including IP addresses) for security and delivery purposes. The site sets no cookies, runs no analytics, and embeds no third-party trackers, so no consent banner is required under the Privacy and Electronic Communications Regulations.

- **Legal basis:** legitimate interests (UK GDPR Art. 6(1)(f)) — operating and securing the site.
- **Transfers:** Vercel may process log data outside the UK, including in the United States, under the UK Addendum to the EU Standard Contractual Clauses.
- **Retention:** as set by Vercel’s own log retention, typically a matter of days.

## 9. Third-party services

To function, Inkwell contacts external services directly from your device. In each case your IP address is necessarily visible to that service. None of them report back to the developer.

- **Your PDS & the AT Protocol network:** to fetch and publish your content.
- **AT Protocol identity services:** DNS and `plc.directory` lookups to resolve handles and DIDs.
- **Leaflet Search (`leaflet-search-backend.fly.dev`):** a cross-platform search index for Standard.site records. Your search terms are sent to it.
- **Constellation (`constellation.microcosm.blue`):** to discover cross-repository backlinks and recommend counts.
- **Bluesky CDN (`cdn.bsky.app`):** to load images and cover art stored as blobs.
- **Bluesky public API (`public.api.bsky.app`):** to render embedded Bluesky posts, profiles, and lists that appear inside documents you read.
- **userinput.app:** only if you use the optional feedback feature (section 7).
- **Ko-fi and GitHub Sponsors:** only if you tap a support link, which opens them in your browser. Any payment is handled entirely by them under their own policies; the developer never receives your payment details through Inkwell.

Queries to these services are subject to their own privacy and data retention policies. The legal basis for these contacts is performance of the service you have asked for (UK GDPR Art. 6(1)(b)) or, for the optional ones, your consent.

## 10. Your rights

Under the UK GDPR you have the right to access, rectify, erase, restrict, object to, and port your personal data, and to withdraw consent at any time without affecting processing already carried out.

In practice, because the developer holds almost nothing: data on your device is under your sole control and is removed by signing out or uninstalling; data in your AT Protocol repository is under your control and your PDS operator’s, and can be edited or deleted with any client; and feedback records can be deleted by you as described in section 7. For anything else, email [contact@ewancroft.uk](mailto:contact@ewancroft.uk) and you will receive a response within one month.

If you are unhappy with how your data has been handled, you can complain to the Information Commissioner’s Office at [ico.org.uk/make-a-complaint](https://ico.org.uk/make-a-complaint/), by calling 0303 123 1113, or by writing to Information Commissioner’s Office, Wycliffe House, Water Lane, Wilmslow, Cheshire SK9 5AF. You may also seek a judicial remedy.

## 11. Automated decision-making

Inkwell does not carry out profiling or automated decision-making that produces legal or similarly significant effects.

## 12. Children

Inkwell is not directed at children and is not intended for anyone under 13. The developer does not knowingly process the personal data of children under 13.

## 13. Changes to this policy

This policy may be updated to reflect new features or legal requirements. The version number and effective date at the top always reflect the current version, and the full history is in the public Git repository. Where a change materially affects your rights, it will be surfaced in the app or in the release notes rather than made silently.

## 14. AI-assisted contributions

AI tools may be used when contributing to Inkwell. Contributors should add `Co-authored-by:` trailers crediting AI agents when they materially contributed, so attribution stays honest and accurate. This has no bearing on the processing of your personal data.

## 15. Contact

For privacy-related enquiries, email [contact@ewancroft.uk](mailto:contact@ewancroft.uk) or open an issue on the [Inkwell](https://github.com/ewanc26/inkwell) GitHub repository.
