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

    /// Fades out when the app is ready. The splash matches the static
    /// `UILaunchScreen` (LaunchBackground color + centered mark) so the
    /// transition from OS launch screen → SwiftUI is invisible.
    @State private var splashOpacity: Double = 1.0

    var body: some Scene {
        WindowGroup {
            ZStack {
                ContentView()
                    .environment(loginStateManager)
                    .opacity(1.0 - splashOpacity)

                // Splash overlay — matches UILaunchScreen exactly
                if splashOpacity > 0 {
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
                // Hold the splash long enough for the system animation to
                // finish, then fade into the real UI with a soft spring.
                try? await Task.sleep(for: .milliseconds(300))
                withAnimation(.spring(response: 0.6, dampingFraction: 0.85)) {
                    splashOpacity = 0.0
                }
            }
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
