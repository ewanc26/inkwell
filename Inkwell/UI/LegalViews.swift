//
//  LegalViews.swift
//  Inkwell
//
//  Privacy Policy and Terms of Service screens. The documents themselves
//  live as LocalizedStringKey constants rather than a bundled file, so
//  they render with SwiftUI's inline Markdown formatting (bold, links) and
//  stay localised without needing a web view or a static resource bundle.
//

import SwiftUI

enum LegalDocumentType {
    case privacyPolicy
    case termsOfService
    
    var title: String {
        switch self {
        case .privacyPolicy: return "Privacy Policy"
        case .termsOfService: return "Terms of Service"
        }
    }
    
    var content: LocalizedStringKey {
        switch self {
        case .privacyPolicy:
            return """
            **Version 1.1 — Effective Date: 13 August 2026**
            
            Inkwell is a decentralized client for the Standard.site ecosystem on the AT Protocol. We believe your data belongs to you. This policy applies to Inkwell for iOS (version 1.0, build 49, distributed via AltStore) and Inkwell for Android (version 1.0.0, distributed via a self-hosted F-Droid repository).
            
            **1. Data Collection & Usage**
            Inkwell is a localized client application. The developer (Ewan Croft) does not collect, store, or harvest personal data, analytics, or usage metrics on proprietary servers. Inkwell contains no ads, tracking, or analytics software. All content you read, write, or publish is communicated directly between your device and the relevant Personal Data Server (PDS) or the wider AT Protocol network.
            
            **2. Data Stored on Your Device**
            * **iOS:** Your OAuth session (access and refresh tokens) and the P-256 DPoP private key are stored in Apple's Keychain. Non-sensitive handle and PDS hints, notification state, and a local record of recently seen URIs are stored in UserDefaults.
            * **Android:** Your OAuth session is stored in EncryptedSharedPreferences, backed by a hardware-backed MasterKey.
            
            **3. Authentication**
            Inkwell uses OAuth 2.1 to sign in to your AT Protocol account securely via the system browser. Inkwell never sees or stores your account password or app password.
            
            **4. Notifications**
            On iOS, with your permission, Inkwell may show local notifications when a publication you follow publishes a new document. Notifications are generated on-device from background refresh; Inkwell does not use push notification services. Notification state and already-seen URIs are stored locally. The Android build declares the notification permission but does not yet send notifications.
            
            **5. Backup**
            The Android build allows Android's automatic cloud backup of app data, which may include your stored OAuth session. You can disable this in your device's backup settings or by signing out. iOS Keychain items are not synchronized to iCloud or included in ordinary device backups.

            **6. Third-Party Services**
            To function, Inkwell communicates with external services:
            * **Your PDS & AT Protocol:** Standard network infrastructure to fetch and publish your content.
            * **AT Protocol identity services:** Standard DNS and PLC directory lookups for identity resolution.
            * **Leaflet Search:** Used as a cross-platform search index for Standard.site records.
            * **Constellation (microcosm.blue):** Used to discover cross-repository backlinks and recommend counts.
            Queries to these public services are subject to their respective privacy and data retention policies.
            
            **7. Changes to this Policy**
            We may update this policy occasionally to reflect new features or legal requirements. Continued use of the app constitutes acceptance of these changes.
            
            **8. Contact**
            For privacy-related inquiries, please email contact@ewancroft.uk or create an issue on the GitHub repository for Inkwell.
            """
            
        case .termsOfService:
            return """
            **Version 1.1 — Effective Date: 13 August 2026**
            
            By downloading or using Inkwell (the "App"), you agree to these terms. Inkwell is provided for iOS (version 1.0, build 49) via AltStore and for Android (version 1.0.0) via a self-hosted F-Droid repository. It is not distributed through the Apple App Store or Google Play.
            
            Inkwell is free and open-source software licensed under the GNU Affero General Public License v3.0 (AGPL-3.0). No store-imposed end-user license agreement applies to the AltStore or F-Droid distribution channels.
            
            **1. User-Generated Content (UGC)**
            Inkwell acts as a portal to the AT Protocol network. You are solely responsible for the content you publish through it. You must not publish illegal, harmful, or abusive content, and you must comply with the terms of the Personal Data Server (PDS) you use and of the AT Protocol network. The developer does not host user content and does not act as a moderator of the network.
            
            **2. Decentralized Network Disclaimer**
            Because Inkwell connects to a decentralized network, the developer has no control over the content published by other users, nor over the availability of PDSs, search indexes, and other network infrastructure. You may encounter content you find objectionable, and the developer is not responsible for it.
            
            **3. "As Is" Basis**
            Inkwell is provided "as is", without warranty of any kind, express or implied, including merchantability and fitness for a particular purpose. The developer is not liable for any data loss, service interruptions, or issues arising from your PDS or the AT Protocol network.
            
            **4. Experimental Software**
            The Android build is an experimental prototype. Features, behavior, and performance may be incomplete or change without notice. Install and use at your own risk.
            """
        }
    }
}

struct LegalDocumentView: View {
    let documentType: LegalDocumentType
    
    var body: some View {
        ScrollView {
            VStack(alignment: .leading) {
                Text(documentType.content)
                    .font(.body)
                    .lineSpacing(4)
                    .padding()
            }
        }
        .navigationTitle(documentType.title)
        .navigationBarTitleDisplayMode(.inline)
    }
}

#Preview("Privacy Policy") {
    NavigationStack {
        LegalDocumentView(documentType: .privacyPolicy)
    }
}

#Preview("Terms of Service") {
    NavigationStack {
        LegalDocumentView(documentType: .termsOfService)
    }
}
