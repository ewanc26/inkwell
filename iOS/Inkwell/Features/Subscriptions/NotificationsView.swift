//
//  NotificationsView.swift
//  Inkwell
//
//  The in-app notification history -- lets the user review documents they
//  were notified about (in case a banner was missed or dismissed) and
//  clear the list. Distinct from the unread *count* (the Reader tab
//  badge), which clears just by viewing the tab; the list itself persists
//  until explicitly cleared here. Mirrors Android's NotificationsDialog.
//

import SwiftUI

struct NotificationsView: View {
    @State private var notificationManager = NotificationManager.shared
    @Environment(\.dismiss) private var dismiss
    let onOpenDocument: (String) -> Void

    var body: some View {
        NavigationStack {
            Group {
                if notificationManager.notifications.isEmpty {
                    ContentUnavailableView(
                        "No Notifications Yet",
                        systemImage: "bell",
                        description: Text("You'll see new documents from your subscriptions here.")
                    )
                } else {
                    List(notificationManager.notifications) { notification in
                        Button {
                            onOpenDocument(notification.documentURI)
                            dismiss()
                        } label: {
                            VStack(alignment: .leading, spacing: 4) {
                                Text(notification.documentTitle)
                                    .font(.body)
                                    .foregroundStyle(.primary)
                                Text(
                                    [notification.publicationName, relativeTime(notification.date)]
                                        .compactMap { $0 }
                                        .joined(separator: " • ")
                                )
                                .font(.caption)
                                .foregroundStyle(.secondary)
                            }
                        }
                    }
                    .listStyle(.plain)
                }
            }
            .navigationTitle("Notifications")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close") { dismiss() }
                }
                if !notificationManager.notifications.isEmpty {
                    ToolbarItem(placement: .primaryAction) {
                        Button("Clear All") { notificationManager.clearAll() }
                    }
                }
            }
        }
    }

    /// A small self-contained relative-time formatter -- this is the only
    /// place in the app that needs one, so it isn't worth a shared utility.
    private func relativeTime(_ date: Date) -> String {
        let formatter = RelativeDateTimeFormatter()
        formatter.unitsStyle = .abbreviated
        return formatter.localizedString(for: date, relativeTo: Date())
    }
}

#Preview {
    NotificationsView(onOpenDocument: { _ in })
}
