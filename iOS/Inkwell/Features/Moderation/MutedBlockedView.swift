//
//  MutedBlockedView.swift
//  Inkwell
//

import SwiftUI

struct MutedBlockedView: View {
    @State private var viewModel: MutedBlockedViewModel
    @State private var selectedTab: ModerationTab = .muted

    init(loginStateManager: LoginStateManager) {
        _viewModel = State(initialValue: MutedBlockedViewModel(loginStateManager: loginStateManager))
    }

    var body: some View {
        VStack(spacing: 0) {
            Picker("Moderation List", selection: $selectedTab) {
                Text("Muted (\(viewModel.mutedActors.count))").tag(ModerationTab.muted)
                Text("Blocked (\(viewModel.blockedActors.count))").tag(ModerationTab.blocked)
            }
            .pickerStyle(.segmented)
            .padding()

            if let errorMessage = viewModel.errorMessage {
                Text(errorMessage)
                    .font(.footnote)
                    .foregroundStyle(.red)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.horizontal)
                    .padding(.bottom, 8)
                    .accessibilityLabel("Error: \(errorMessage)")
            }

            if viewModel.isLoading {
                Spacer()
                ProgressView("Loading moderation settings…")
                Spacer()
            } else {
                switch selectedTab {
                case .muted:
                    mutedList
                case .blocked:
                    blockedList
                }
            }
        }
        .navigationTitle("Muted & Blocked")
        .navigationBarTitleDisplayMode(.inline)
        .task {
            await viewModel.load()
        }
    }

    @ViewBuilder
    private var mutedList: some View {
        if viewModel.mutedActors.isEmpty {
            ContentUnavailableView(
                "No Muted Accounts",
                systemImage: "speaker.slash",
                description: Text("Accounts you mute will appear here.")
            )
        } else {
            List(viewModel.mutedActors) { actor in
                actorRow(
                    actor: actor,
                    actionLabel: "Unmute",
                    isRemoving: viewModel.removingKeys.contains(actor.did)
                ) {
                    Task { await viewModel.unmute(did: actor.did) }
                }
            }
            .listStyle(.plain)
        }
    }

    @ViewBuilder
    private var blockedList: some View {
        if viewModel.blockedActors.isEmpty {
            ContentUnavailableView(
                "No Blocked Accounts",
                systemImage: "hand.raised",
                description: Text("Accounts you block will appear here.")
            )
        } else {
            List(viewModel.blockedActors) { entry in
                actorRow(
                    actor: entry.actor,
                    actionLabel: "Unblock",
                    isRemoving: viewModel.removingKeys.contains(entry.recordKey)
                ) {
                    Task { await viewModel.unblock(entry) }
                }
            }
            .listStyle(.plain)
        }
    }

    private func actorRow(
        actor: ModeratedActor,
        actionLabel: String,
        isRemoving: Bool,
        action: @escaping () -> Void
    ) -> some View {
        HStack(spacing: 12) {
            VStack(alignment: .leading, spacing: 2) {
                Text(actor.displayName ?? actor.handle)
                    .font(.body)
                Text("@\(actor.handle)")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }

            Spacer()

            if isRemoving {
                ProgressView()
                    .controlSize(.small)
                    .accessibilityLabel("\(actionLabel) in progress")
            } else {
                Button(actionLabel, action: action)
                    .buttonStyle(.borderless)
                    .accessibilityLabel("\(actionLabel) @\(actor.handle)")
            }
        }
        .padding(.vertical, 4)
    }
}

private enum ModerationTab: Hashable {
    case muted
    case blocked
}
