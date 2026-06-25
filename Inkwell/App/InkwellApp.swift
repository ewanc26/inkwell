//
//  InkwellApp.swift
//  Inkwell
//
//  Created by Ewan Croft on 19/06/2026.
//

import SwiftUI

@main
struct InkwellApp: App {
    @UIApplicationDelegateAdaptor(InkwellAppDelegate.self) private var appDelegate
    @State private var loginStateManager = LoginStateManager()

    /// Controls whether the splash screen is in the view hierarchy.
    /// Uses a boolean (not opacity comparison) to ensure the overlay
    /// is fully REMOVED after fading out — an invisible Color with
    /// ignoresSafeArea still intercepts all touches at screen top.
    @State private var showSplash = true
    @State private var splashOpacity: Double = 1.0

    var body: some Scene {
        WindowGroup {
            ZStack {
                ContentView()
                    .environment(loginStateManager)

                // Splash overlay — matches UILaunchScreen exactly.
                // Removed from hierarchy after fade, not just hidden.
                if showSplash {
                    Color("LaunchBackground")
                        .ignoresSafeArea()
                        .overlay {
                            InkwellMark()
                                .frame(height: 48)
                                .foregroundStyle(.primary)
                        }
                        .opacity(splashOpacity)
                }
            }
            .task {
                // Hold briefly so the system launch screen → SwiftUI
                // transition isn't a flash, then fade out.
                try? await Task.sleep(for: .milliseconds(300))
                withAnimation(.spring(response: 0.6, dampingFraction: 0.85)) {
                    splashOpacity = 0.0
                }
                // Wait for animation to finish, then remove from hierarchy
                // so it doesn't block touches on nav bar / toolbar buttons.
                try? await Task.sleep(for: .milliseconds(700))
                showSplash = false
            }
            // MARK: - Launch Task
            .task {
                    // Cheap, synchronous-in-effect, so do it before resuming the session.
                    // Order doesn't matter between these two — they register disjoint
                    // sets of `$type` keys — but both must finish before anything tries
                    // to decode a site.standard.document's `content` field, since that's
                    // an open union that depends on the content-format registration to
                    // resolve to anything other than raw JSON.
                    await SiteStandardLexicon.registerRecordTypes()
                    await ContentFormatRegistration.registerRecordTypes()

                    // Attempt a silent session resume once per launch, before
                    // the user sees anything other than the loading state.
                    await loginStateManager.restoreSessionIfPossible()

                    BackgroundRefreshManager.shared.configure {
                        await NotificationManager.shared.pollForNewDocuments(
                            loginStateManager: loginStateManager
                        )
                    }
                    BackgroundRefreshManager.shared.schedule()
                }
        }
    }
}
