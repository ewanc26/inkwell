//
//  SupportView.swift
//  Inkwell
//
//  Tip jar + alternate support methods. Inkwell is free on both
//  platforms — these are external links (ko-fi, GitHub Sponsors,
//  crypto), not in-app purchases, since AltStore distribution has
//  no App Store billing to hook StoreKit into.
//

import SwiftUI
import UIKit

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

                // MARK: - Crypto
                Section {
                    cryptoRow(
                        currency: "Monero",
                        detail: "Preferred — the only genuinely private option",
                        address: "44yH2LpkSsrSmWQC3SVmrABw2MUhNjNCE365hG7Rr7veJYNPBD1f6dNgXNr2nc6ZcP3jEyj9vXnqmg7VBBPeS8uwMhJ4yXW"
                    )

                    cryptoRow(
                        currency: "Ethereum",
                        detail: nil,
                        address: "0x4B8c9d62ff89bc7199a197C55dac2abef1808B77"
                    )

                    cryptoRow(
                        currency: "Bitcoin",
                        detail: nil,
                        address: "bc1qp3l6e9pjc5jan7ulpd58av8wfdtyhrchj84clh"
                    )
                } header: {
                    Text("Cryptocurrency")
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

    private func cryptoRow(currency: String, detail: String?, address: String) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack(spacing: 12) {
                Image(systemName: "bitcoinsign.circle.fill")
                    .font(.title3)
                    .foregroundStyle(.orange)
                    .frame(width: 28)

                Text(currency)
                    .font(.body.weight(.medium))
                    .foregroundStyle(.primary)

                if let detail {
                    Text(detail)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }

                Spacer()

                Button {
                    UIPasteboard.general.string = address
                } label: {
                    Image(systemName: "doc.on.doc")
                        .font(.caption)
                }
                .buttonStyle(.borderless)
            }

            Text(address)
                .font(.caption2.monospaced())
                .foregroundStyle(.tertiary)
                .lineLimit(1)
                .truncationMode(.middle)
        }
    }
}

#Preview {
    SupportView()
}
