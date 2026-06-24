//
//  SupportView.swift
//  Inkwell
//
//  Tip jar + alternate support methods. Uses StoreKit 2 ProductView
//  for in-app tips, plus links to ko-fi, GitHub Sponsors, and crypto.
//

import SwiftUI
import StoreKit
import UIKit

struct SupportView: View {
    @Environment(StoreManager.self) private var store

    var body: some View {
        NavigationStack {
            List {
                // MARK: - Tip Jar
                Section {
                    if store.tipProducts.isEmpty {
                        HStack {
                            Spacer()
                            ProgressView("Loading tips…")
                            Spacer()
                        }
                    } else {
                        ForEach(store.tipProducts) { product in
                            ProductView(id: product.id)
                                .productViewStyle(.compact)
                        }
                    }

                    if store.hasTipped {
                        HStack {
                            Spacer()
                            Label("Thank you for your support!", systemImage: "heart.fill")
                                .foregroundStyle(.pink)
                                .font(.subheadline.weight(.medium))
                            Spacer()
                        }
                        .listRowBackground(Color.pink.opacity(0.08))
                    }
                } header: {
                    Text("Tip Jar")
                } footer: {
                    Text("One-time tips to support Inkwell's development. Not a subscription — pay once, and thank you forever.")
                }

                // MARK: - Alternate Methods
                Section {
                    Link(destination: URL(string: "https://ko-fi.com/ewancroft")!) {
                        supportRow(
                            icon: "cup.and.saucer.fill",
                            title: "Ko-fi",
                            detail: "Buy me a tea"
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
                    Text("Other Ways to Support")
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
        .environment(StoreManager())
}
