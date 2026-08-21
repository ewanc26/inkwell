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
    @Environment(\.dismiss) private var dismiss

    @State private var isConfirmingSignOut = false
    @State private var isShowingFeedback = false
    @State private var supporters: [BSkyActorProfile] = []
    @State private var hasLoadedSupporters = false

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
                        url: sharedSearchBackendUrl()
                    )
                } header: {
                    Text("Built On")
                } footer: {
                    Text("Inkwell reads and writes Leaflet, Markpub, pckt, and Offprint content alongside the shared site.standard.* records.")
                }

                if !supporters.isEmpty {
                    Section {
                        ForEach(supporters) { supporter in
                            if let url = URL(string: "https://bsky.app/profile/\(supporter.handle)") {
                                Link(destination: url) {
                                    HStack(spacing: 12) {
                                        AccountAvatar(url: supporter.avatar.flatMap(URL.init(string:)), size: 36)
                                        VStack(alignment: .leading, spacing: 2) {
                                            Text(supporter.displayName?.isEmpty == false ? supporter.displayName! : "@\(supporter.handle)")
                                                .foregroundStyle(.primary)
                                                .lineLimit(1)
                                            Text("@\(supporter.handle)")
                                                .font(.caption)
                                                .foregroundStyle(.secondary)
                                                .lineLimit(1)
                                        }
                                    }
                                }
                            }
                        }
                    } header: {
                        Text("Supporters")
                    } footer: {
                        Text("Everyone who's tipped Inkwell via Ko-fi or GitHub Sponsors, listed on Bluesky. Thank you.")
                    }
                }

                Section {
                    NavigationLink {
                        SupportView()
                    } label: {
                        Label("Support Inkwell", systemImage: "heart.fill")
                            .foregroundStyle(.pink)
                    }

                    Button {
                        isShowingFeedback = true
                    } label: {
                        Label("Send Feedback", systemImage: "exclamationmark.bubble")
                            .foregroundStyle(.primary)
                    }

                    creditRow(
                        title: "Source on GitHub",
                        detail: "ewanc26/inkwell",
                        url: "https://github.com/ewanc26/inkwell"
                    )
                    creditRow(
                        title: "Ewan Croft",
                        detail: "Developer",
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

                if loginStateManager.isAuthenticated {
                    Section {
                        Button(role: .destructive) {
                            isConfirmingSignOut = true
                        } label: {
                            Label("Sign Out", systemImage: "rectangle.portrait.and.arrow.right")
                        }
                    }
                }

                Section {
                    Text("Inkwell for Android is also available on GitHub.")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }
            .navigationTitle("About")
            .navigationBarTitleDisplayMode(.inline)
            // This is only ever presented as a sheet, and a sheet with no
            // visible way out relies on people knowing to swipe it down.
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("Done") { dismiss() }
                }
            }
            .confirmationDialog(
                "Sign out of Inkwell?",
                isPresented: $isConfirmingSignOut,
                titleVisibility: .visible
            ) {
                Button("Sign Out", role: .destructive) {
                    loginStateManager.signOut()
                    dismiss()
                }
                Button("Cancel", role: .cancel) {}
            } message: {
                Text("Your publications and subscriptions stay in your PDS. You can sign back in at any time.")
            }
            .sheet(isPresented: $isShowingFeedback) {
                FeedbackView()
            }
            .task {
                guard !hasLoadedSupporters else { return }
                hasLoadedSupporters = true
                supporters = await BSkyListFetcher.fetchListMembers(listUri: SupportersList.uri)
            }
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
