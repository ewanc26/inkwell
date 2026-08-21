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
import UIKit

struct SettingsView: View {
    @Environment(LoginStateManager.self) private var loginStateManager
    @Environment(\.dismiss) private var dismiss

    @State private var notificationManager = NotificationManager.shared
    @State private var notificationsEnabled = NotificationManager.shared.notificationsEnabled
    @State private var isConfirmingSignOut = false
    @State private var legalDocument: LegalDocumentType?
    @State private var showAbout = false

    @State private var customisation = CustomisationSettings.shared
    @State private var licenseKeyInput = ""
    @State private var licenseKeyError = false
    @State private var accentColor: Color?

    @State private var accessibility = AccessibilitySettings.shared

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

                Section {
                    VStack(alignment: .leading) {
                        Text("Text Size")
                        Slider(value: $accessibility.fontSizeScale, in: 0.8...1.5, step: 0.1) {
                            Text("Text Size")
                        } minimumValueLabel: {
                            Text("A").font(.caption)
                        } maximumValueLabel: {
                            Text("A").font(.title2)
                        }
                    }
                    Toggle("Bold Text", isOn: $accessibility.boldText)
                    Toggle("Increase Contrast", isOn: $accessibility.increaseContrast)
                    Toggle("Underline Links", isOn: $accessibility.underlineLinks)
                    Button("Reset to Defaults", role: .destructive) {
                        accessibility.resetToDefaults()
                    }
                } header: {
                    Text("Accessibility")
                } footer: {
                    Text("These apply on top of your device's own Text Size and accessibility settings, and are always free.")
                }

                if customisation.isUnlocked {
                    Section {
                        ColorPicker(
                            "Accent Color",
                            selection: Binding(
                                get: { accentColor ?? .accentColor },
                                set: { newValue in
                                    accentColor = newValue
                                    customisation.accentColorHex = newValue.toHexString()
                                }
                            )
                        )
                        Picker("Reading Font", selection: Binding(
                            get: { customisation.fontFamilyOverride ?? .sans },
                            set: { customisation.fontFamilyOverride = $0 }
                        )) {
                            Text("Sans-Serif").tag(ReaderTheme.FontFamily.sans)
                            Text("Serif").tag(ReaderTheme.FontFamily.serif)
                            Text("Rounded").tag(ReaderTheme.FontFamily.rounded)
                            Text("Monospaced").tag(ReaderTheme.FontFamily.monospaced)
                        }
                        Picker("Appearance", selection: Binding(
                            get: { customisation.appearanceOverride },
                            set: { customisation.appearanceOverride = $0 }
                        )) {
                            Text("System").tag(ColorScheme?.none)
                            Text("Light").tag(ColorScheme?.some(.light))
                            Text("Dark").tag(ColorScheme?.some(.dark))
                        }
                        Button("Reset to Defaults", role: .destructive) {
                            accentColor = nil
                            customisation.accentColorHex = nil
                            customisation.fontFamilyOverride = nil
                            customisation.appearanceOverride = nil
                        }
                    } header: {
                        Text("Customisation")
                    } footer: {
                        Text("Overrides apply everywhere, including publications that set their own theme.")
                    }
                } else {
                    Section {
                        TextField("License Key", text: $licenseKeyInput)
                            .textInputAutocapitalization(.never)
                            .autocorrectionDisabled()
                        Button("Unlock") {
                            if customisation.unlock(withKey: licenseKeyInput) {
                                licenseKeyError = false
                                licenseKeyInput = ""
                            } else {
                                licenseKeyError = true
                            }
                        }
                        .disabled(licenseKeyInput.isEmpty)
                        if licenseKeyError {
                            Text("That key didn't verify. Check it was copied in full.")
                                .foregroundStyle(.red)
                                .font(.caption)
                        }
                    } header: {
                        Text("Customisation")
                    } footer: {
                        Text("A one-off £5 unlocks accent colour, reading font, and light/dark overrides that apply everywhere you read, including publications with their own theme. Pay via Ko-fi or GitHub Sponsors (under About → Support) and mention you'd like the customisation unlock — you'll get a key back to paste in here.")
                    }
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
            .onAppear {
                accentColor = customisation.accentColorHex.flatMap(Color.init(hex:))
            }
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

private extension Color {
    init?(hex: String?) {
        guard var value = hex?.trimmingCharacters(in: .whitespacesAndNewlines), !value.isEmpty else {
            return nil
        }
        if value.hasPrefix("#") { value.removeFirst() }
        guard value.count == 6, let rgb = UInt64(value, radix: 16) else { return nil }
        self.init(
            red: Double((rgb >> 16) & 0xff) / 255,
            green: Double((rgb >> 8) & 0xff) / 255,
            blue: Double(rgb & 0xff) / 255
        )
    }

    func toHexString() -> String {
        let components = UIColor(self).cgColor.components ?? [0, 0, 0]
        let r = Int((components.count > 0 ? components[0] : 0) * 255)
        let g = Int((components.count > 1 ? components[1] : 0) * 255)
        let b = Int((components.count > 2 ? components[2] : 0) * 255)
        return String(format: "#%02X%02X%02X", r, g, b)
    }
}
