//
//  AccountToolbar.swift
//  Inkwell
//
//  The account control shared by every top-level tab.
//
//  Sign Out used to sit unguarded in each tab's leading toolbar slot —
//  one stray tap from ending the session — next to a separate "info"
//  button, and Write additionally put a greeting and avatar in the
//  principal slot. iOS puts account-level actions behind the account's
//  avatar at the trailing edge, so that's where they live now: one
//  control, one place, on every tab.
//

import SwiftUI

/// The signed-in account's Bluesky avatar. Falls back to a generic person
/// symbol while loading or when the account has no avatar set.
struct AccountAvatar: View {
    let url: URL?
    var size: CGFloat = 26

    var body: some View {
        AsyncImage(url: url) { phase in
            if let image = phase.image {
                image
                    .resizable()
                    .scaledToFill()
            } else {
                Image(systemName: "person.crop.circle.fill")
                    .resizable()
                    .scaledToFit()
                    .foregroundStyle(.secondary)
            }
        }
        .frame(width: size, height: size)
        .clipShape(.circle)
    }
}

/// Trailing toolbar menu holding the account-level actions every tab
/// shares: who you're signed in as, About, and Sign Out.
///
/// Signing out is confirmed rather than immediate — it discards the
/// Keychain session, and the old bare toolbar button made that a
/// single mis-tap away.
struct AccountMenu: View {
    @Environment(LoginStateManager.self) private var loginStateManager

    /// Bound to the presenting tab so the About sheet comes up over that
    /// tab's own navigation stack rather than being owned here.
    @Binding var showAbout: Bool

    @State private var isConfirmingSignOut = false
    @State private var showSettings = false

    var body: some View {
        Menu {
            if let handle = loginStateManager.currentHandle {
                Section(loginStateManager.displayName ?? "Signed in") {
                    Text("@\(handle)")
                }
            }

            Button("Settings", systemImage: "gearshape") {
                showSettings = true
            }

            Button("About Inkwell", systemImage: "info.circle") {
                showAbout = true
            }

            Section {
                Button(role: .destructive) {
                    isConfirmingSignOut = true
                } label: {
                    Label("Sign Out", systemImage: "rectangle.portrait.and.arrow.right")
                }
            }
        } label: {
            AccountAvatar(url: loginStateManager.avatarURL)
        }
        .accessibilityLabel("Account")
        .sheet(isPresented: $showSettings) {
            SettingsView()
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

extension View {
    /// Adds the shared account menu to a tab's trailing navigation bar and
    /// wires up the About sheet it presents. For tabs that have trailing
    /// items of their own, place `AccountMenu` yourself so it can be
    /// grouped correctly, and use `aboutSheet(isPresented:)` alone.
    func accountToolbar(showAbout: Binding<Bool>) -> some View {
        self
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    AccountMenu(showAbout: showAbout)
                }
            }
            .aboutSheet(isPresented: showAbout)
    }

    /// Presents the About screen.
    func aboutSheet(isPresented: Binding<Bool>) -> some View {
        sheet(isPresented: isPresented) {
            CreditsView()
        }
    }
}
