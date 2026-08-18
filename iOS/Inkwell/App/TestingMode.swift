//
//  TestingMode.swift
//  Inkwell
//
//  Launch-argument configuration for testing and screenshot capture.
//
//  Testing mode signs in with the real session and reads real records over
//  the network — the feed, publications, documents, comments and
//  verification state are all genuine. What it intercepts is every write:
//  the UI stays fully interactive, but the moment an action would send
//  something outward it stops at the network boundary and says so.
//
//  It deliberately does not substitute mock data for anything. The
//  fixtures this replaced hid two real bugs — a subscribed/unsubscribed
//  bell state that never once rendered correctly, and a feed card with no
//  author — because the only thing anyone ever looked at was the fake.
//
//  Nor does it fabricate a success. `createRecord` and friends hand back
//  references that callers go on to read, so returning a plausible-looking
//  fake would break the next step and violate the honest-stubs rule in
//  AGENTS.md. The write throws `LoginError.testingMode` and the notice
//  below explains why.
//
//  Enable with `-testing`. Deep-link a tab with `-tab-reader`,
//  `-tab-discover` or `-tab-writer`.
//

import Foundation
import Observation

enum TestingMode {
    /// Intercepts every outbound mutation while leaving reads untouched.
    /// Enforced at the XRPC write choke points in `LoginStateManager`
    /// (`createRecord`, `updateRecord`, `deleteRecord`, `uploadBlob`) and at
    /// `submitFeedback`, so a new caller cannot route around it by accident.
    static let isEnabled = CommandLine.arguments.contains("-testing")

    /// Suppresses the notification permission prompt, local notification
    /// delivery and the Ko-fi tip prompt. Not writes, but they steal focus
    /// mid-capture. Tied to the same flag so there's one thing to remember.
    static var suppressesInterruptions: Bool { isEnabled }

    /// Tab to open on launch, when specified.
    static var initialTab: InkwellTab? {
        if CommandLine.arguments.contains("-tab-discover") { return .discover }
        if CommandLine.arguments.contains("-tab-writer") { return .writer }
        if CommandLine.arguments.contains("-tab-reader") { return .reader }
        return nil
    }
}

/// Carries the "this didn't leave the device" notice up to the root view,
/// so the explanation isn't buried under a feature's own "Couldn't Publish"
/// alert title.
@MainActor
@Observable
final class TestingModeNotice {
    static let shared = TestingModeNotice()
    private init() {}

    /// The blocked operation, in words a person reads — "Publish document",
    /// "Upload image". Non-nil presents the alert.
    var blockedAction: String?

    var isPresented: Bool {
        get { blockedAction != nil }
        set { if !newValue { blockedAction = nil } }
    }

    func report(_ action: String) {
        blockedAction = action
    }
}
