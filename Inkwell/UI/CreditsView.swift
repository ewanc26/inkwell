//
//  CreditsView.swift
//  Inkwell
//
//  About / credits / support screen — version info, acknowledgements for
//  the protocols and services Inkwell builds on, and links to the source
//  and the developer's support pages. Surfaced as its own tab rather than
//  buried in a settings sheet, since right now it's the only place in the
//  app that says what Inkwell actually is and who made it.
//

import SwiftUI

struct CreditsView: View {
    @Environment(LoginStateManager.self) private var loginStateManager

    private var versionString: String {
        let shortVersion = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0"
        let buildNumber = Bundle.main.infoDictionary?["CFBundleVersion"] as? String ?? "1"
        return "Version \(shortVersion) (\(buildNumber))"
    }

    var body: some View {
        NavigationStack {
            List {
                Section {
                    VStack(spacing: 12) {
                        InkwellMark()
                            .frame(height: 56)
                            .foregroundStyle(.primary)
                        Text("Inkwell")
                            .font(.title2.weight(.bold))
                        Text(versionString)
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 12)
                    .listRowBackground(Color.clear)
                }

                Section {
                    Text("A native reader and writer for the Standard.site publishing ecosystem on AT Protocol — read, discover, and publish portable writing from your own PDS.")
                        .font(.callout)
                        .foregroundStyle(.secondary)
                } header: {
                    Text("About")
                }

                Section {
                    creditRow(
                        title: "ATProtoKit",
                        detail: "AT Protocol SDK by MasterJ93",
                        url: "https://github.com/MasterJ93/ATProtoKit"
                    )
                    creditRow(
                        title: "OAuthenticator",
                        detail: "OAuth 2.1 authentication (from ChimeHQ / germ-network)",
                        url: "https://github.com/germ-network/OAuthenticator"
                    )
                    creditRow(
                        title: "ATResolve",
                        detail: "AT Protocol identity resolution (from ChimeHQ / germ-network)",
                        url: "https://github.com/germ-network/ATResolve"
                    )
                    creditRow(
                        title: "Standard.site",
                        detail: "The publishing protocol Inkwell reads and writes",
                        url: "https://standard.site"
                    )
                    creditRow(
                        title: "pub search",
                        detail: "Cross-platform Standard.site search index",
                        url: "https://leaflet-search-backend.fly.dev"
                    )
                } header: {
                    Text("Built On")
                } footer: {
                    Text("Inkwell reads and writes Leaflet, Markpub, pckt, and Offprint content alongside the shared site.standard.* records.")
                }

                Section {
                    creditRow(
                        title: "Source on GitHub",
                        detail: "ewanc26/inkwell",
                        url: "https://github.com/ewanc26/inkwell"
                    )
                    creditRow(
                        title: "Ewan Croft",
                        detail: "Developer — support links on ewancroft.uk",
                        url: "https://ewancroft.uk"
                    )
                } header: {
                    Text("Support")
                }
                
                // MARK: - New Legal Section
                Section {
                    NavigationLink(destination: LegalDocumentView(documentType: .privacyPolicy)) {
                        Text("Privacy Policy")
                    }
                    NavigationLink(destination: LegalDocumentView(documentType: .termsOfService)) {
                        Text("Terms of Service")
                    }
                    Text("AGPL 3.0 License")
                        .foregroundStyle(.secondary)
                } header: {
                    Text("Legal")
                }

                Section {
                    Button(role: .destructive, action: loginStateManager.signOut) {
                        Label("Sign Out", systemImage: "rectangle.portrait.and.arrow.right")
                    }
                }
            }
            .navigationTitle("About")
            .navigationBarTitleDisplayMode(.inline)
        }
    }

    @ViewBuilder
    private func creditRow(title: String, detail: String, url: String) -> some View {
        if let url = URL(string: url) {
            Link(destination: url) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(title)
                        .foregroundStyle(.primary)
                        .lineLimit(2)
                    Text(detail)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .lineLimit(3)
                }
            }
        }
    }
}

#Preview {
    CreditsView()
        .environment(LoginStateManager())
}
