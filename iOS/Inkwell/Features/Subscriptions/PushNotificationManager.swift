//
//  PushNotificationManager.swift
//  Inkwell
//
//  APNs registration and hand-off for the "Real-time notifications" opt-in
//  (issue #35). This only ever *adds* a push wake-up on top of the existing
//  polling path in NotificationManager — it never replaces it. If push
//  registration fails, the endpoint is unreachable, or the endpoint isn't
//  configured yet, nothing here changes behaviour: BackgroundRefreshManager
//  keeps polling exactly as it does today.
//
//  Mirrors shared KMP's PushSubscriptionRequest/PushPlatform/NotificationTopic
//  (shared/src/commonMain/kotlin/.../model/PushSubscriptionRequest.kt,
//  .../policy/NotificationTopic.kt) with local Codable types, the same
//  temporary-duplication pattern InkwellOAuthScopes.swift uses: the checked-in
//  InkwellShared.xcframework predates these types, so this collapses into a
//  thin SharedKMP.swift wrapper once the framework is next rebuilt.
//
//  The registration endpoint (inkwell.ewancroft.uk/api/push/register) is
//  currently an honest stub with no real DID authentication and no delivery
//  backend wired — see website/src/routes/api/push/register/+server.ts and
//  issue #35's follow-ups. A non-2xx response here is expected right now,
//  not a bug; it's logged and the app falls back to polling silently, which
//  is exactly the "fall back to polling when push is unavailable" behaviour
//  issue #35 asks for.
//

import Foundation
import UIKit
import UserNotifications
import OSLog

@MainActor
final class PushNotificationManager {
    static let shared = PushNotificationManager()
    private let logger = Logger(subsystem: "uk.ewancroft.Inkwell", category: "PushNotifications")

    private let defaults = UserDefaults.standard
    private let storagePrefix = "standardSite.account."
    private var activeAccountDID: String?

    /// The APNs device token, hex-encoded. Not secret — it identifies this
    /// install to APNs, not a credential — but kept in Keychain rather than
    /// UserDefaults because it should survive exactly as long as the OAuth
    /// session does, and the Keychain wrapper is already there.
    private let deviceTokenStore = KeychainStore<String>(
        service: "uk.ewancroft.Inkwell",
        account: "apnsDeviceToken"
    )

    private func key(_ name: String, for did: String) -> String {
        "\(storagePrefix)\(did).\(name)"
    }

    /// User-facing opt-in, surfaced in SettingsView next to the existing
    /// "New Document Notifications" toggle. Defaults to `false`: the
    /// registration endpoint doesn't do anything yet (see file header), so
    /// defaulting this on would silently promise a capability that isn't
    /// there. Flip the default once the server side is real.
    var realtimeNotificationsEnabled: Bool {
        get { activeAccountDID.flatMap { defaults.object(forKey: key("realtimeNotificationsEnabled", for: $0)) as? Bool } ?? false }
        set {
            guard let did = activeAccountDID else { return }
            defaults.set(newValue, forKey: key("realtimeNotificationsEnabled", for: did))
            if newValue {
                Task { await requestAuthorizationAndRegister(accountDID: did) }
            }
        }
    }

    /// The current poll action, same closure `BackgroundRefreshManager` uses
    /// — set once from `InkwellApp`'s launch `.task`, where the real
    /// `loginStateManager` instance is in scope. `InkwellAppDelegate` (a
    /// plain `UIApplicationDelegate`) has no reachable path to that
    /// instance otherwise.
    private var silentPushAction: (() async -> Void)?

    private init() {}

    /// Sets the closure a silent push will invoke. Call once from the app's
    /// launch `.task`, alongside `BackgroundRefreshManager.configure`.
    func configure(silentPushAction: @escaping () async -> Void) {
        self.silentPushAction = silentPushAction
    }

    func activate(accountDID did: String) {
        activeAccountDID = did
    }

    /// Re-asserts registration on every launch/foreground for an account
    /// that already opted in. `registerForRemoteNotifications()` never
    /// re-prompts once permission is granted — it just refreshes the token
    /// hand-off — so this is safe to call unconditionally here.
    func reregisterIfEnabled(accountDID did: String) {
        activate(accountDID: did)
        guard realtimeNotificationsEnabled, !TestingMode.suppressesInterruptions else { return }
        UIApplication.shared.registerForRemoteNotifications()
    }

    func deactivate() {
        activeAccountDID = nil
    }

    // MARK: - APNs registration

    private func requestAuthorizationAndRegister(accountDID did: String) async {
        if TestingMode.suppressesInterruptions { return }
        let center = UNUserNotificationCenter.current()
        do {
            let granted = try await center.requestAuthorization(options: [.alert, .badge, .sound])
            guard granted else { return }
        } catch {
            logger.error("[PushNotificationManager] authorization request failed: \(error.localizedDescription)")
            return
        }
        UIApplication.shared.registerForRemoteNotifications()
    }

    /// Called from `InkwellAppDelegate.application(_:didRegisterForRemoteNotificationsWithDeviceToken:)`.
    func didRegister(deviceToken: Data) {
        let token = deviceToken.map { String(format: "%02x", $0) }.joined()
        try? deviceTokenStore.write(token)
        guard let did = activeAccountDID, realtimeNotificationsEnabled else { return }
        Task { await sendRegistration(deviceToken: token, did: did) }
    }

    /// Called from `InkwellAppDelegate.application(_:didFailToRegisterForRemoteNotificationsWithError:)`.
    func didFailToRegister(error: Error) {
        logger.notice("[PushNotificationManager] APNs registration unavailable, staying on polling: \(error.localizedDescription)")
    }

    private func sendRegistration(deviceToken: String, did: String) async {
        let request = PushSubscriptionRequestPayload(
            platform: "ios",
            deviceToken: deviceToken,
            topics: ["subscribe", "recommend", "comment"],
            did: did
        )
        guard let url = URL(string: "https://inkwell.ewancroft.uk/api/push/register") else { return }
        var urlRequest = URLRequest(url: url)
        urlRequest.httpMethod = "POST"
        urlRequest.setValue("application/json", forHTTPHeaderField: "Content-Type")
        do {
            urlRequest.httpBody = try JSONEncoder().encode(request)
            let (_, response) = try await URLSession.shared.data(for: urlRequest)
            guard let http = response as? HTTPURLResponse, (200..<300).contains(http.statusCode) else {
                logger.notice("[PushNotificationManager] registration endpoint declined (not yet configured?) — polling remains the delivery path")
                return
            }
        } catch {
            logger.notice("[PushNotificationManager] registration request failed, polling remains the delivery path: \(error.localizedDescription)")
        }
    }

    // MARK: - Silent push hand-off

    /// Called from `InkwellAppDelegate.application(_:didReceiveRemoteNotification:fetchCompletionHandler:)`
    /// for a silent (`content-available`) push. Runs the same configured
    /// poll action `BackgroundRefreshManager` uses — a push wake-up is
    /// "go check now", not an independent source of truth. A no-op if
    /// `configure(silentPushAction:)` hasn't run yet (e.g. a push arrives
    /// before launch finishes configuring it).
    func handleSilentPush() async {
        await silentPushAction?()
    }
}

/// Local mirror of shared KMP's `PushSubscriptionRequest` — see file header.
private struct PushSubscriptionRequestPayload: Codable {
    let platform: String
    let deviceToken: String
    let topics: [String]
    let did: String
}
