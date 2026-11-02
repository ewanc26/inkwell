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

    /// The profile's pinned links, in display order, restricted to what the
    /// app can open externally. `profileLinkView` destinations are https by
    /// construction, but the filter keeps a misbehaving server from handing
    /// the row a scheme `Link` can't handle.
    private func profileLinks(_ profile: BSkyActorProfile) -> [BSkyProfileLink] {
        (profile.links ?? []).filter { link in
            guard let scheme = link.url.flatMap(URL.init(string:))?.scheme?.lowercased() else { return false }
            return scheme == "http" || scheme == "https"
        }
    }

    private func profileLinkTitle(_ link: BSkyProfileLink, host: String?) -> String {
        if let title = link.title?.trimmingCharacters(in: .whitespacesAndNewlines),
           !title.isEmpty {
            return title
        }
        return host ?? link.url ?? ""
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

            let links = profileLinks(profile)
            if !links.isEmpty {
                Section("Links") {
                    ForEach(Array(links.enumerated()), id: \.offset) { _, link in
                        if let destination = link.url.flatMap(URL.init(string:)) {
                            Link(destination: destination) {
                                HStack(spacing: 12) {
                                    if let iconURL = link.icon.flatMap(URL.init(string:)) {
                                        AsyncImage(url: iconURL) { phase in
                                            switch phase {
                                            case .success(let image):
                                                image.resizable().scaledToFill()
                                            case .failure, .empty:
                                                Image(systemName: "link")
                                                    .resizable()
                                                    .foregroundStyle(.secondary)
                                            @unknown default:
                                                Image(systemName: "link")
                                                    .resizable()
                                                    .foregroundStyle(.secondary)
                                            }
                                        }
                                        .frame(width: 28, height: 28)
                                        .clipShape(.rect(cornerRadius: 6))
                                        .accessibilityHidden(true)
                                    }
                                    VStack(alignment: .leading, spacing: 2) {
                                        Text(profileLinkTitle(link, host: destination.host))
                                            .foregroundStyle(.primary)
                                            .lineLimit(2)
                                        if let host = destination.host,
                                           host != profileLinkTitle(link, host: destination.host) {
                                            Text(host)
                                                .font(.caption)
                                                .foregroundStyle(.secondary)
                                                .lineLimit(1)
                                        }
                                    }
                                }
                            }
                        }
                    }
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

    private var accessibilityLabel: Text {
        var label = Text("\(displayName.bidiIsolated). @\(profile.handle.bidiIsolated)")
        if let pronouns = profile.pronouns?.trimmingCharacters(in: .whitespacesAndNewlines),
           !pronouns.isEmpty {
            label = label + Text(". \(pronouns.bidiIsolated)")
        }
        if profile.isVerifiedAccount {
            label = label + Text(". Verified account")
        }
        if let followersCount = profile.followersCount {
            label = label + Text(". ") + Text("^[\(followersCount) follower](inflect: true)")
        }
        if let followsCount = profile.followsCount {
            label = label + Text(". Following ^[\(followsCount) account](inflect: true)")
        }
        if let postsCount = profile.postsCount {
            label = label + Text(". ^[\(postsCount) post](inflect: true)")
        }
        return label
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
                    HStack(spacing: 4) {
                        Text(displayName)
                            .font(.title2.weight(.semibold))
                            .fixedSize(horizontal: false, vertical: true)
                        if profile.isVerifiedAccount {
                            Image(systemName: "checkmark.seal.fill")
                                .foregroundStyle(.blue)
                        }
                    }

                    Text("@\(profile.handle)")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                        .textSelection(.enabled)

                    if let pronouns = profile.pronouns?.trimmingCharacters(in: .whitespacesAndNewlines),
                       !pronouns.isEmpty {
                        Text(pronouns)
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                    }
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
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(accessibilityCountLabel)
    }

    private var accessibilityCountLabel: Text {
        guard let value else { return Text("—, \(label)") }
        switch label {
        case "Followers":
            return Text("^[\(value) follower](inflect: true)")
        case "Following":
            return Text("Following ^[\(value) account](inflect: true)")
        default:
            return Text("^[\(value) post](inflect: true)")
        }
    }
}
