//
//  InkwellApp.swift
//  Inkwell
//
//  Created by Ewan Croft on 19/06/2026.
//

import SwiftUI

@main
struct InkwellApp: App {
    @State private var loginStateManager = LoginStateManager()

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environment(loginStateManager)
        }
    }
}
