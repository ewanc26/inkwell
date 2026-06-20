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

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environment(loginStateManager)
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
