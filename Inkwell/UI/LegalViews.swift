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
            **Effective Date: 21 June 2026**
            
            Inkwell is a decentralized client for the Standard.site ecosystem on the AT Protocol. We believe your data belongs to you.
            
            **1. Data Collection & Usage**
            Inkwell is a localized client application. The developer (Ewan Croft) does not collect, store, or harvest any personal data, analytics, or usage metrics on proprietary servers. All data you read, write, or publish is communicated directly between your device and your authenticated Personal Data Server (PDS) or the wider AT Protocol network.
            
            **2. Authentication**
            Inkwell uses OAuth 2.1 to sign in to your AT Protocol account securely via the system browser. Your OAuth tokens are stored securely on your device using Apple's native Keychain. Inkwell never sees or stores your account password or app password.

            **3. Third-Party Services**
            To function, Inkwell communicates with external services:
            * **Your PDS & AT Protocol:** Standard network infrastructure to fetch and publish your content.
            * **AT Protocol identity services:** Standard DNS and PLC directory lookups for identity resolution.
            * **Leaflet Search:** Used as a cross-platform search index for Standard.site records.
            Queries to these public services are subject to their respective privacy and data retention policies.
            
            **4. Changes to this Policy**
            We may update this policy occasionally to reflect new features or App Store requirements. Continued use of the app constitutes acceptance of these changes.
            
            **5. Contact**
            For privacy-related inquiries, please email contact@ewancroft.uk or create an issue on the GitHub repository for Inkwell.
            """
            
        case .termsOfService:
            return """
            **Standard Terms of Use & EULA**
            
            By downloading or using Inkwell, you agree to these terms. Inkwell is licensed to you under the terms of the AGPL 3.0 License, alongside Apple's standard App Store End User License Agreement.
            
            **1. User-Generated Content (UGC)**
            Inkwell acts as a portal to the AT Protocol network. You are solely responsible for the content you publish. 
            
            In alignment with Apple App Store guidelines:
            * You must not publish illegal, highly objectionable, or abusive content.
            * Inkwell provides native tools to mute or block users and report objectionable content to the network's moderation services.
            * There is zero tolerance for abusive users. We reserve the right to filter or hide content locally within the client that violates App Store guidelines.
            
            **2. Decentralized Network Disclaimer**
            Because Inkwell connects to a decentralized network, the developer has no control over the content published by other users. You may encounter content you find objectionable. Please use the built-in blocking and filtering tools to curate your experience.
            
            **3. "As Is" Basis**
            Inkwell is provided "as is", without warranty of any kind, express or implied. The developer is not liable for any data loss, service interruptions, or issues arising from your PDS or the AT Protocol network.
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
