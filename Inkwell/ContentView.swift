//
//  ContentView.swift
//  Inkwell
//
//  Created by Ewan Croft on 19/06/2026.
//

import SwiftUI

struct ContentView: View {
    @Environment(LoginStateManager.self) private var loginStateManager

    var body: some View {
        if loginStateManager.isAuthenticated {
            authenticatedView
        } else {
            LoginView()
        }
    }

    private var authenticatedView: some View {
        NavigationStack {
            ContentUnavailableView(
                "Nothing here yet",
                systemImage: "doc.text",
                description: Text("Leaflet.pub document browsing and publishing are coming soon.")
            )
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(role: .destructive, action: loginStateManager.signOut) {
                        Label("Sign Out", systemImage: "rectangle.portrait.and.arrow.right")
                    }
                }
                ToolbarItem(placement: .principal) {
                    HStack(spacing: 8) {
                        avatarView
                        Text(timeZoneAwareGreeting)
                            .font(.headline)
                    }
                }
            }
        }
    }

    /// The account's Bluesky avatar, shown small next to the greeting. Falls
    /// back to a generic person icon while loading or if no avatar is set.
    /// Purely decorative — not a button, so it shouldn't read as tappable.
    private var avatarView: some View {
        AsyncImage(url: loginStateManager.avatarURL) { phase in
            if let image = phase.image {
                image
                    .resizable()
                    .scaledToFill()
            } else {
                Image(systemName: "person.crop.circle.fill")
                    .resizable()
                    .foregroundStyle(.secondary)
            }
        }
        .frame(width: 24, height: 24)
        .clipShape(Circle())
    }

    /// A greeting whose period of day (morning/afternoon/evening/night) is
    /// derived from the device's current local time, with the account's
    /// Bluesky display name appended when one is set. If no display name is
    /// available, the name is dropped entirely rather than falling back to
    /// the handle.
    private var timeZoneAwareGreeting: String {
        let hour = Calendar.current.component(.hour, from: Date())
        let period: String
        switch hour {
        case 5..<12:
            period = "morning"
        case 12..<17:
            period = "afternoon"
        case 17..<22:
            period = "evening"
        default:
            period = "night"
        }

        if let name = loginStateManager.displayName {
            return "Good \(period), \(name)"
        } else {
            return "Good \(period)"
        }
    }
}

#Preview {
    ContentView()
        .environment(LoginStateManager())
}
