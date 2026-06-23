//
//  PollEmbedView.swift
//  Inkwell
//
//  Renders an interactive poll (pub.leaflet.blocks.poll). Fetches the
//  poll definition and vote records from the author's PDS, shows option
//  bars with relative counts, and supports casting a vote.
//

import SwiftUI
import ATProtoKit
import OSLog

// MARK: - Poll Models

/// A `pub.leaflet.poll.definition` record.
struct LeafletPollDefinition: ATRecordProtocol {
    static let type = "pub.leaflet.poll.definition"

    let name: String?
    let options: [PollOption]
    let endDate: Date?

    struct PollOption: Codable, Equatable, Hashable, Sendable {
        let text: String
    }
}

/// A `pub.leaflet.poll.vote` record.
struct LeafletPollVote: ATRecordProtocol {
    static let type = "pub.leaflet.poll.vote"

    let poll: ComAtprotoLexicon.Repository.StrongReference
    let option: [String]
}

// MARK: - Poll State

@MainActor
@Observable
final class PollState {
    private(set) var definition: LeafletPollDefinition?
    private(set) var voteCounts: [String: Int] = [:]
    private(set) var myVote: [String]?
    private(set) var isLoading = true
    private(set) var totalVotes = 0
    private var pollURI: String = ""

    private static let logger = Logger(subsystem: "uk.ewancroft.Inkwell", category: "Poll")

    func load(pollRef: ComAtprotoLexicon.Repository.StrongReference, loginStateManager: LoginStateManager) async {
        guard let did = loginStateManager.currentDID else { return }
        isLoading = true
        defer { isLoading = false }

        pollURI = pollRef.recordURI
        let recordURI = pollRef.recordURI

        do {
            // Fetch the poll definition from the author's PDS
            let (_, _, value) = try await loginStateManager.getRepositoryRecord(
                from: did, collection: "pub.leaflet.poll.definition",
                recordKey: Self.recordKey(from: recordURI)
            )
            definition = value?.getRecord(ofType: LeafletPollDefinition.self)
        } catch {
            Self.logger.error("[Poll] failed to load definition: \(error.localizedDescription)")
            return
        }

        // Fetch all votes for this poll
        do {
            let votes = try await loginStateManager.listAllRecords(
                from: did, collection: "pub.leaflet.poll.vote"
            )
            var counts: [String: Int] = [:]
            var myVoteOptions: [String]? = nil
            let myDID = loginStateManager.currentDID

            for record in votes {
                if let vote = record.value?.getRecord(ofType: LeafletPollVote.self),
                   vote.poll.recordURI == recordURI {
                    for opt in vote.option {
                        counts[opt, default: 0] += 1
                    }
                    if record.uri.contains(myDID ?? "") {
                        myVoteOptions = vote.option
                    }
                }
            }

            voteCounts = counts
            myVote = myVoteOptions
            totalVotes = counts.values.reduce(0, +)
        } catch {
            Self.logger.error("[Poll] failed to load votes: \(error.localizedDescription)")
        }
    }

    func castVote(option: String, loginStateManager: LoginStateManager) async {
        guard loginStateManager.currentDID != nil else { return }
        myVote = [option]

        // Optimistic local update
        voteCounts[option, default: 0] += 1
        totalVotes += 1

        do {
            let vote = LeafletPollVote(
                poll: ComAtprotoLexicon.Repository.StrongReference(
                    recordURI: pollURI,
                    cidHash: ""
                ),
                option: [option]
            )
            _ = try await loginStateManager.createRecord(
                collection: "pub.leaflet.poll.vote",
                record: UnknownType.record(vote)
            )
        } catch {
            Self.logger.error("[Poll] vote failed: \(error.localizedDescription)")
        }
    }

    func hasVotedFor(_ option: String) -> Bool {
        myVote?.contains(option) ?? false
    }

    private static func recordKey(from uri: String) -> String {
        let parts = uri.split(separator: "/")
        return String(parts.last ?? "")
    }
}

// MARK: - Poll View

struct PollEmbedView: View {
    let pollRef: ComAtprotoLexicon.Repository.StrongReference
    var foregroundColor: Color = .primary
    var accentColor: Color = .blue

    @Environment(LoginStateManager.self) private var loginStateManager
    @State private var state = PollState()

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            // Title
            if let name = state.definition?.name {
                Text(name)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(foregroundColor)
            }

            if state.isLoading {
                HStack(spacing: 10) {
                    ProgressView().scaleEffect(0.8)
                    Text("Loading poll…")
                        .font(.subheadline)
                        .foregroundStyle(foregroundColor.opacity(0.5))
                }
            } else if let options = state.definition?.options, !options.isEmpty {
                ForEach(options, id: \.text) { option in
                    pollOptionRow(option)
                }
            }

            // Footer
            if state.totalVotes > 0 {
                HStack {
                    Text("\(state.totalVotes) vote\(state.totalVotes == 1 ? "" : "s")")
                        .font(.caption2)
                        .foregroundStyle(foregroundColor.opacity(0.4))
                    if let _ = state.myVote {
                        Text("· Voted")
                            .font(.caption2)
                            .foregroundStyle(accentColor)
                    }
                    Spacer()
                }
            }
        }
        .padding(14)
        .background(foregroundColor.opacity(0.04))
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .overlay(
            RoundedRectangle(cornerRadius: 12)
                .stroke(foregroundColor.opacity(0.12), lineWidth: 1)
        )
        .task {
            await state.load(pollRef: pollRef, loginStateManager: loginStateManager)
        }
    }

    @ViewBuilder
    private func pollOptionRow(_ option: LeafletPollDefinition.PollOption) -> some View {
        let count = state.voteCounts[option.text, default: 0]
        let fraction: CGFloat = state.totalVotes > 0
            ? CGFloat(count) / CGFloat(state.totalVotes)
            : 0
        let hasVoted = state.hasVotedFor(option.text)

        Button {
            Task {
                await state.castVote(option: option.text, loginStateManager: loginStateManager)
            }
        } label: {
            HStack(spacing: 10) {
                Text(option.text)
                    .font(.subheadline)
                    .foregroundStyle(foregroundColor)
                    .lineLimit(3)

                Spacer()

                if state.totalVotes > 0 {
                    Text("\(count)")
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(foregroundColor.opacity(0.5))
                        .frame(minWidth: 24, alignment: .trailing)
                }

                Image(systemName: hasVoted ? "checkmark.circle.fill" : "circle")
                    .foregroundStyle(hasVoted ? accentColor : foregroundColor.opacity(0.3))
                    .font(.title3)
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 10)
            .background(
                GeometryReader { geo in
                    ZStack(alignment: .leading) {
                        RoundedRectangle(cornerRadius: 8)
                            .fill(accentColor.opacity(hasVoted ? 0.15 : 0.0))
                        RoundedRectangle(cornerRadius: 8)
                            .fill(accentColor.opacity(0.08))
                            .frame(width: geo.size.width * fraction)
                    }
                }
            )
            .clipShape(RoundedRectangle(cornerRadius: 8))
            .overlay(
                RoundedRectangle(cornerRadius: 8)
                    .stroke(
                        hasVoted ? accentColor.opacity(0.4) : foregroundColor.opacity(0.1),
                        lineWidth: 1
                    )
            )
        }
        .buttonStyle(.plain)
    }
}

#Preview {
    PollEmbedView(
        pollRef: ComAtprotoLexicon.Repository.StrongReference(
            recordURI: "at://did:plc:example/pub.leaflet.poll.definition/abc123",
            cidHash: "bafyabc123"
        ),
        foregroundColor: .primary,
        accentColor: .blue
    )
    .environment(LoginStateManager())
    .padding()
}
