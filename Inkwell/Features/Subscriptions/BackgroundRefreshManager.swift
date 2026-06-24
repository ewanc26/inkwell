//
//  BackgroundRefreshManager.swift
//  Inkwell
//

#if os(iOS)
import BackgroundTasks
#endif
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

    func configure(refreshAction: @escaping () async -> Void) {
        self.refreshAction = refreshAction
    }

    #if os(iOS)
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

    func schedule() {
        let request = BGAppRefreshTaskRequest(identifier: Self.taskIdentifier)
        request.earliestBeginDate = Date(timeIntervalSinceNow: 15 * 60)
        try? BGTaskScheduler.shared.submit(request)
    }

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
    #else
    func register() {}
    func schedule() {}
    #endif
}

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
