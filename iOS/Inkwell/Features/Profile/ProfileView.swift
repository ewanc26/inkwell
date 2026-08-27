//
//  ProfileView.swift
//  Inkwell
//

import SwiftUI

private struct ProfileReportTarget: Identifiable {
    let did: String

    var id: String { did }
}

struct ProfileView: View {
    let did: String

    @State private var viewModel = ProfileViewModel()
    @State private var reportTarget: ProfileReportTarget?
    @State private var reportMessage: String?

    var body: some View {
        Group {
            if viewModel.isLoading && viewModel.profile == nil {
                ProgressView("Loading profile…")
                    .controlSize(.large)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if let profile = viewModel.profile {
                profileContent(profile)
            } else if let errorMessage = viewModel.errorMessage {
                ContentUnavailableView {
                    Label("Profile Unavailable", systemImage: "person.crop.circle.badge.exclamationmark")
                } description: {
                    Text(errorMessage)
                } actions: {
                    Button("Try Again") {
                        Task { await viewModel.load(did: did) }
                    }
                    .buttonStyle(.borderedProminent)
                }
            } else {
                ProgressView("Loading profile…")
                    .controlSize(.large)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
        }
        .navigationTitle("Profile")
        .navigationBarTitleDisplayMode(.inline)
        .task(id: did) {
            await viewModel.load(did: did)
        }
        .sheet(item: $reportTarget) { target in
            ReportSheet(
                subject: target.did,
                recordCID: nil,
                onSubmit: {
                    reportMessage = "Report submitted."
                },
                onError: { message in
                    reportMessage = "Report failed: \(message)"
                }
            )
        }
        .alert("Report", isPresented: Binding(
            get: { reportMessage != nil },
            set: { if !$0 { reportMessage = nil } }
        )) {
            Button("OK", role: .cancel) {}
        } message: {
            Text(reportMessage ?? "")
        }
    }

    private func profileContent(_ profile: BSkyActorProfile) -> some View {
        List {
            Section {
                ProfileHeader(profile: profile)
                    .listRowInsets(EdgeInsets(top: 12, leading: 20, bottom: 12, trailing: 20))
            }

            if let description = profile.description?.trimmingCharacters(in: .whitespacesAndNewlines),
               !description.isEmpty {
                Section("About") {
                    Text(description)
                        .font(.body)
                        .fixedSize(horizontal: false, vertical: true)
                }
            }

            Section("Account") {
                LabeledContent("DID") {
                    Text(profile.did)
                        .font(.caption.monospaced())
                        .multilineTextAlignment(.trailing)
                        .textSelection(.enabled)
                }

                if let blueskyURL = URL(string: "https://bsky.app/profile/\(profile.handle)") {
                    Link(destination: blueskyURL) {
                        Label("Open in Bluesky", systemImage: "arrow.up.right.square")
                    }
                }
            }

            Section {
                Button(role: .destructive) {
                    reportTarget = ProfileReportTarget(did: profile.did)
                } label: {
                    Label("Report Account", systemImage: "exclamationmark.bubble")
                }
            } footer: {
                Text("Reports are sent to your account's moderation service.")
            }
        }
        .listStyle(.insetGrouped)
        .refreshable {
            await viewModel.load(did: did)
        }
    }
}

private struct ProfileHeader: View {
    let profile: BSkyActorProfile

    @ScaledMetric(relativeTo: .title2) private var avatarSize = 88.0

    private var displayName: String {
        guard let trimmed = profile.displayName?.trimmingCharacters(in: .whitespacesAndNewlines),
              !trimmed.isEmpty else {
            return profile.handle
        }
        return trimmed
    }

    private var accessibilityLabel: String {
        var parts = [displayName, "@\(profile.handle)"]
        if let followersCount = profile.followersCount {
            parts.append("\(followersCount) followers")
        }
        if let followsCount = profile.followsCount {
            parts.append("Following \(followsCount) accounts")
        }
        if let postsCount = profile.postsCount {
            parts.append("\(postsCount) posts")
        }
        return parts.joined(separator: ". ")
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            if let bannerURL = profile.banner.flatMap(URL.init(string:)) {
                AsyncImage(url: bannerURL) { phase in
                    switch phase {
                    case .success(let image):
                        image
                            .resizable()
                            .scaledToFill()
                            .frame(maxWidth: .infinity)
                            .frame(height: 140)
                            .clipped()
                            .accessibilityHidden(true)
                    case .failure, .empty:
                        EmptyView()
                    @unknown default:
                        EmptyView()
                    }
                }
            }

            HStack(alignment: .center, spacing: 14) {
                ProfileAvatar(urlString: profile.avatar, size: avatarSize)

                VStack(alignment: .leading, spacing: 3) {
                    Text(displayName)
                        .font(.title2.weight(.semibold))
                        .fixedSize(horizontal: false, vertical: true)

                    Text("@\(profile.handle)")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                        .textSelection(.enabled)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }

            ProfileStatistics(profile: profile)
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(accessibilityLabel)
    }
}

private struct ProfileAvatar: View {
    let urlString: String?
    let size: CGFloat

    var body: some View {
        Group {
            if let url = urlString.flatMap(URL.init(string:)) {
                AsyncImage(url: url) { phase in
                    switch phase {
                    case .success(let image):
                        image.resizable().scaledToFill()
                    case .failure, .empty:
                        Image(systemName: "person.crop.circle.fill")
                            .resizable()
                            .foregroundStyle(.secondary)
                    @unknown default:
                        Image(systemName: "person.crop.circle.fill")
                            .resizable()
                            .foregroundStyle(.secondary)
                    }
                }
            } else {
                Image(systemName: "person.crop.circle.fill")
                    .resizable()
                    .foregroundStyle(.secondary)
            }
        }
        .frame(width: size, height: size)
        .clipShape(.circle)
        .accessibilityHidden(true)
    }
}

private struct ProfileStatistics: View {
    let profile: BSkyActorProfile

    var body: some View {
        ViewThatFits(in: .horizontal) {
            HStack(spacing: 24) {
                statistics
            }
            VStack(alignment: .leading, spacing: 10) {
                statistics
            }
        }
    }

    @ViewBuilder
    private var statistics: some View {
        ProfileStatistic(value: profile.followersCount, label: "Followers")
        ProfileStatistic(value: profile.followsCount, label: "Following")
        ProfileStatistic(value: profile.postsCount, label: "Posts")
    }
}

private struct ProfileStatistic: View {
    let value: Int?
    let label: String

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(value.map(String.init) ?? "—")
                .font(.headline)
            Text(label)
                .font(.caption)
                .foregroundStyle(.secondary)
        }
        .accessibilityElement(children: .combine)
    }
}
