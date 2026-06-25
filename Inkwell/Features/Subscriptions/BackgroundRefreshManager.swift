//
//  BackgroundRefreshManager.swift
//  Inkwell
//
//  iOS Background Task plumbing for polling subscribed publications in the
//  background. Registers the refresh task at launch via InkwellAppDelegate,
//  schedules a 15-minute-interval request each time the task runs, and
//  delegates the actual fetch to whatever `configure(refreshAction:)` was
//  given — currently NotificationManager's poll cycle.
//
//  This is a pull-based notification model, same as every other AT Protocol
//  app without a dedicated push relay: the OS wakes the app every ~15 min,
//  it checks for new documents, and schedules a local notification if any
//  are found.
//

import BackgroundTasks
import UIKit

enum InkwellIdentifiers {
    static let lexiconNamespace = "uk.ewancroft.inkwell"
    static let backgroundRefresh = "\(lexiconNamespace).refresh"
}

@MainActor
final class BackgroundRefreshManager {
    static let shared = BackgroundRefreshManager()
    static let taskIdentifier = InkwellIdentifiers.backgroundRefresh

    private var refreshAction: (() async -> Void)?

    private init() {}

    // MARK: - Configuration

    /// Sets the closure the background task will invoke on each wake.
    /// Call once from the app's launch `.task` before calling `schedule()`.
    func configure(refreshAction: @escaping () async -> Void) {
        self.refreshAction = refreshAction
    }

    /// Registers the BGTask handler with the system. Called from
    /// `InkwellAppDelegate.application(_:didFinishLaunchingWithOptions:)`.
    func register() {
        BGTaskScheduler.shared.register(
            forTaskWithIdentifier: Self.taskIdentifier,
            using: nil
        ) { task in
            Task { @MainActor in
                guard let refreshTask = task as? BGAppRefreshTask else {
                    task.setTaskCompleted(success: false)
                    return
                }
                self.handle(refreshTask)
            }
        }
    }

    /// Schedules the next background refresh for roughly 15 minutes from now.
    /// Called each time the task fires to keep the chain alive.
    func schedule() {
        let request = BGAppRefreshTaskRequest(identifier: Self.taskIdentifier)
        request.earliestBeginDate = Date(timeIntervalSinceNow: 15 * 60)
        try? BGTaskScheduler.shared.submit(request)
    }

    // MARK: - Task Handler

    private func handle(_ task: BGAppRefreshTask) {
        schedule()
        guard let refreshAction else {
            task.setTaskCompleted(success: false)
            return
        }

        let operation = Task { @MainActor in
            await refreshAction()
            guard !Task.isCancelled else { return }
            task.setTaskCompleted(success: true)
        }
        task.expirationHandler = {
            operation.cancel()
        }
    }
}

// MARK: - App Delegate

@MainActor
final class InkwellAppDelegate: NSObject, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        BackgroundRefreshManager.shared.register()
        configureNavigationBar()
        return true
    }

    /// Match IceCubesApp's navigation bar setup: translucent bar,
    /// no custom backgrounds. Let Liquid Glass handle the rest.
    private func configureNavigationBar() {
        let appearance = UINavigationBarAppearance()
        appearance.configureWithDefaultBackground()
        UINavigationBar.appearance().standardAppearance = appearance
        UINavigationBar.appearance().scrollEdgeAppearance = appearance
        UINavigationBar.appearance().isTranslucent = true
    }
}
