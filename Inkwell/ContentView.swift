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
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(role: .destructive, action: loginStateManager.signOut) {
                        Label("Sign Out", systemImage: "rectangle.portrait.and.arrow.right")
                    }
                }
            }
        }
    }
}

#Preview {
    ContentView()
        .environment(LoginStateManager())
}
