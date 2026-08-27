//
//  ConnectivityMonitor.swift
//  Inkwell
//

import Foundation
import Network
import Observation

/// Native connectivity state for presentation-only offline affordances.
/// Cached content remains owned by the shared KMP cache APIs.
@MainActor
@Observable
final class ConnectivityMonitor {
    private let pathMonitor = NWPathMonitor()
    private let monitorQueue = DispatchQueue(label: "uk.ewancroft.inkwell.connectivity")

    private(set) var isOnline = true

    init() {
        pathMonitor.pathUpdateHandler = { [weak self] path in
            let isOnline = path.status == .satisfied
            Task { @MainActor [weak self] in
                self?.isOnline = isOnline
            }
        }
        pathMonitor.start(queue: monitorQueue)
    }

    deinit {
        pathMonitor.cancel()
    }
}
