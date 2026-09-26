//
//  LoginAccessibilityUITests.swift
//  InkwellUITests
//
//  Accessibility regression coverage for Inkwell's guaranteed-reachable,
//  unauthenticated launch surface (`LoginView`) plus the "About Inkwell"
//  sheet (`CreditsView`) it opens. These are the only screens a fresh CI
//  simulator can reach deterministically: there is no persisted Keychain
//  session, and the app deliberately has no fixture/mock-session mode (see
//  `../AGENTS.md`'s Testing mode section), so authenticated screens
//  (Reader/Discover/Writer) stay on the manual VoiceOver checklist
//  documented there instead of being faked here.
//

import XCTest

final class LoginAccessibilityUITests: XCTestCase {
    override func setUpWithError() throws {
        continueAfterFailure = false
    }

    private func launchApp(contentSize: String? = nil) -> XCUIApplication {
        let app = XCUIApplication()
        app.launchArguments += ["-testing"]
        if let contentSize {
            app.launchArguments += ["-UIPreferredContentSizeCategoryName", contentSize]
        }
        app.launch()
        return app
    }

    // MARK: - Non-empty accessible names

    /// Every button on the launch screen must announce something to
    /// VoiceOver -- an icon-only or unlabeled button is unusable with the
    /// screen reader on.
    func testButtonsHaveNonEmptyAccessibleNames() {
        let app = launchApp()
        let continueButton = app.buttons["Continue"]
        XCTAssertTrue(continueButton.waitForExistence(timeout: 10))

        let buttons = app.buttons.allElementsBoundByIndex
        XCTAssertFalse(buttons.isEmpty, "Expected at least one button on the login screen")
        for button in buttons {
            XCTAssertFalse(
                button.label.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty,
                "Button at \(button.frame) has no accessible name"
            )
        }
    }

    /// The handle field must expose a non-empty accessible name so
    /// VoiceOver has something to announce before the user has typed
    /// anything into it.
    func testHandleFieldHasNonEmptyAccessibleName() {
        let app = launchApp()
        let handleField = app.textFields.firstMatch
        XCTAssertTrue(handleField.waitForExistence(timeout: 10))
        XCTAssertFalse(
            handleField.label.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty,
            "Handle field has no accessible name for VoiceOver"
        )
    }

    // MARK: - Large Dynamic Type

    /// At the largest accessibility text size, the primary action (the
    /// disabled-until-valid "Continue" button) and the input it depends on
    /// must both still exist and be reachable by scrolling -- a Dynamic
    /// Type regression that clips them out of the hierarchy entirely
    /// (rather than merely requiring a scroll) would strand every
    /// low-vision user who needs the largest text sizes and can't complete
    /// sign-in as a result.
    func testLargeDynamicTypeKeepsCoreActionsReachable() {
        let app = launchApp(contentSize: "UICTContentSizeCategoryAccessibilityExtraExtraExtraLarge")

        let handleField = app.textFields.firstMatch
        XCTAssertTrue(handleField.waitForExistence(timeout: 10))

        let continueButton = app.buttons["Continue"]
        XCTAssertTrue(continueButton.waitForExistence(timeout: 10))
        // The screen is a ScrollView, so "exists" is the meaningful
        // assertion at this size, not "isHittable" -- the button can
        // legitimately need a scroll to reach without being clipped out of
        // the accessibility hierarchy altogether.
        app.swipeUp()
        XCTAssertTrue(continueButton.exists)

        let aboutButton = app.buttons["About Inkwell"]
        XCTAssertTrue(aboutButton.exists)
    }

    // MARK: - About Inkwell sheet

    /// "About Inkwell" must reach a dismissible sheet whose interactive
    /// controls all expose non-empty accessible names -- this is the only
    /// other screen a signed-out CI simulator can reach deterministically.
    func testAboutSheetControlsHaveNonEmptyAccessibleNames() {
        let app = launchApp()
        let aboutButton = app.buttons["About Inkwell"]
        XCTAssertTrue(aboutButton.waitForExistence(timeout: 10))
        aboutButton.tap()

        let doneButton = app.buttons["Done"]
        XCTAssertTrue(doneButton.waitForExistence(timeout: 10))

        for button in app.buttons.allElementsBoundByIndex {
            XCTAssertFalse(
                button.label.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty,
                "Button at \(button.frame) in the About sheet has no accessible name"
            )
        }

        doneButton.tap()
        XCTAssertTrue(app.buttons["About Inkwell"].waitForExistence(timeout: 10))
    }
}
