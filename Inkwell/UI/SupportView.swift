//
//  SupportView.swift
//  Inkwell
//
//  Tip jar + alternate support methods. Inkwell is free on both
//  platforms — these are external links (ko-fi, GitHub Sponsors),
//  not in-app purchases, since AltStore distribution has no App
//  Store billing to hook StoreKit into.
//

import SwiftUI

struct SupportView: View {
    var body: some View {
        NavigationStack {
            List {
                // MARK: - Support Methods
                Section {
                    Link(destination: URL(string: "https://ko-fi.com/ewancroft?amount=2.99")!) {
                        supportRow(
                            icon: "cup.and.saucer.fill",
                            title: "Ko-fi",
                            detail: "Buy me a tea — £2.99 suggested"
                        )
                    }

                    Link(destination: URL(string: "https://github.com/sponsors/ewanc26")!) {
                        supportRow(
                            icon: "heart.circle.fill",
                            title: "GitHub Sponsors",
                            detail: "Sponsor development work"
                        )
                    }
                } header: {
                    Text("Support Inkwell")
                }

                // MARK: - Non-Monetary
                Section {
                    ShareLink(item: URL(string: "https://inkwell.ewancroft.uk")!) {
                        supportRow(
                            icon: "square.and.arrow.up",
                            title: "Share Inkwell",
                            detail: "Word of mouth is the best support"
                        )
                    }

                    Link(destination: URL(string: "https://github.com/ewanc26/inkwell")!) {
                        supportRow(
                            icon: "curlybraces",
                            title: "Contribute",
                            detail: "Bugs, features, pull requests"
                        )
                    }
                } header: {
                    Text("Non-Monetary")
                }
            }
            .navigationTitle("Support Inkwell")
            .navigationBarTitleDisplayMode(.inline)
        }
    }

    // MARK: - Row Views

    private func supportRow(icon: String, title: String, detail: String) -> some View {
        HStack(spacing: 12) {
            Image(systemName: icon)
                .font(.title3)
                .foregroundStyle(.tint)
                .frame(width: 28)

            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.body.weight(.medium))
                    .foregroundStyle(.primary)
                Text(detail)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            Spacer()
            Image(systemName: "arrow.up.right")
                .font(.caption)
                .foregroundStyle(.tertiary)
        }
    }

}

#Preview {
    SupportView()
}
