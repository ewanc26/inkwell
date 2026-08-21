//
//  SettingsView.swift
//  Inkwell
//
//  The app's actual settings surface: account, notifications, legal, and
//  about/support, in one place. Previously scattered -- Sign Out lived
//  bare in AccountMenu, notifications had no on/off switch anywhere in
//  the app (only the OS permission prompt), and Legal/About/Support were
//  each their own separate sheet with no common home.
//

import SwiftUI

struct SettingsView: View {
    @Environment(LoginStateManager.self) private var loginStateManager
    @Environment(\.dismiss) private var dismiss

    @State private var notificationManager = NotificationManager.shared
    @State private var notificationsEnabled = NotificationManager.shared.notificationsEnabled
    @State private var isConfirmingSignOut = false
    @State private var legalDocument: LegalDocumentType?
    @State private var showAbout = false

    var body: some View {
        NavigationStack {
            Form {
                Section("Account") {
                    if let handle = loginStateManager.currentHandle {
                        LabeledContent("Handle", value: "@\(handle)")
                    }
                    Button("Sign Out", role: .destructive) {
                        isConfirmingSignOut = true
                    }
                }

                Section {
                    Toggle("New Document Notifications", isOn: $notificationsEnabled)
                        .onChange(of: notificationsEnabled) { _, newValue in
                            notificationManager.notificationsEnabled = newValue
                        }
                    Button("Open System Notification Settings") {
                        if let url = URL(string: UIApplication.openSettingsURLString) {
                            UIApplication.shared.open(url)
                        }
                    }
                } header: {
                    Text("Notifications")
                } footer: {
                    Text("Inkwell polls your subscriptions in the background and notifies you about new documents. Turning this off keeps the in-app notification list working but stops system banners. The system permission (above) controls whether banners can appear at all.")
                }

                Section("Legal") {
                    Button("Privacy Policy") { legalDocument = .privacyPolicy }
                    Button("Terms of Service") { legalDocument = .termsOfService }
                }

                Section("About") {
                    Button("About Inkwell") { showAbout = true }
                }

                Section {
                    LabeledContent("Version", value: appVersionString)
                }
            }
            .navigationTitle("Settings")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close") { dismiss() }
                }
            }
            .sheet(item: $legalDocument) { documentType in
                NavigationStack {
                    LegalDocumentView(documentType: documentType)
                }
            }
            .sheet(isPresented: $showAbout) {
                CreditsView()
            }
            .confirmationDialog(
                "Sign out of Inkwell?",
                isPresented: $isConfirmingSignOut,
                titleVisibility: .visible
            ) {
                Button("Sign Out", role: .destructive, action: loginStateManager.signOut)
                Button("Cancel", role: .cancel) {}
            } message: {
                Text("Your publications and subscriptions stay in your PDS. You can sign back in at any time.")
            }
        }
    }

    private var appVersionString: String {
        let version = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "?"
        let build = Bundle.main.infoDictionary?["CFBundleVersion"] as? String ?? "?"
        return "\(version) (\(build))"
    }
}

#Preview {
    SettingsView()
        .environment(LoginStateManager())
}
