//
//  NotificationDelegate.swift
//  Inkwell
//
//  Handles delivered/tapped local notifications. Without this, iOS never
//  shows a banner for a notification fired while the app is in the
//  foreground (the OS default is to suppress it), and tapping a delivered
//  notification does nothing beyond the default "open the app" behaviour
//  -- the documentURI NotificationManager stores in each notification's
//  userInfo was otherwise never read.
//

import UserNotifications

final class NotificationDelegate: NSObject, UNUserNotificationCenterDelegate {
    static let shared = NotificationDelegate()

    private override init() {}

    /// Shows the notification banner/sound even while the app is active,
    /// matching what happens when the app is backgrounded.
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        completionHandler([.banner, .sound, .badge])
    }

    /// Stores the document until the authenticated reader navigation stack is
    /// mounted. The delegate can receive a response before SwiftUI observers
    /// exist during a cold launch.
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        defer { completionHandler() }
        guard let uri = response.notification.request.content.userInfo["documentURI"] as? String else { return }

        NotificationCenter.default.post(
            name: .inkwellOpenTab,
            object: nil,
            userInfo: [InkwellTabKey.tab: InkwellTab.reader.rawValue]
        )
        Task { @MainActor in
            NotificationNavigationCoordinator.shared.enqueue(documentURI: uri)
        }
    }
}
