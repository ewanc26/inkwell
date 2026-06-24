//
//  StoreManager.swift
//  Inkwell
//
//  Manages non-consumable in-app purchases for the tip jar.
//

import StoreKit
import OSLog
import Observation

@MainActor
@Observable
final class StoreManager {
    private(set) var tipProducts: [Product] = []
    private(set) var purchasedProductIDs: Set<String> = []
    private(set) var isPurchasing = false

    private let logger = Logger(subsystem: "uk.ewancroft.Inkwell", category: "Store")
    private var transactionListener: Task<Void, Never>?

    /// Product IDs for tip tiers. Must match App Store Connect.
    static let tipProductIDs = [
        "uk.ewancroft.inkwell.tip.small",
        "uk.ewancroft.inkwell.tip.medium",
        "uk.ewancroft.inkwell.tip.large",
        "uk.ewancroft.inkwell.tip.generous"
    ]

    init() {
        // Use a nonisolated intermediate to satisfy deinit requirements
        let task = listenForTransactions()
        self.transactionListener = task
        Task { await loadProducts() }
    }

    // MARK: - Products

    func loadProducts() async {
        do {
            tipProducts = try await Product.products(for: Self.tipProductIDs)
                .sorted(by: { $0.price < $1.price })
        } catch {
            logger.error("[Store] failed to load products: \(error.localizedDescription)")
        }
    }

    // MARK: - Purchase

    func purchase(_ product: Product) async throws {
        isPurchasing = true
        defer { isPurchasing = false }

        let result = try await product.purchase()

        switch result {
        case .success(let verification):
            if let transaction = try? verification.payloadValue {
                purchasedProductIDs.insert(transaction.productID)
                logger.info("[Store] purchased: \(transaction.productID)")
                await transaction.finish()
            }
        case .userCancelled:
            break
        case .pending:
            break
        @unknown default:
            break
        }
    }

    // MARK: - Transactions

    private func listenForTransactions() -> Task<Void, Never> {
        Task.detached { [weak self] in
            for await verification in Transaction.updates {
                guard let self,
                      let transaction = try? verification.payloadValue,
                      transaction.productType == .nonConsumable else { continue }
                await self.handle(transaction: transaction)
                await transaction.finish()
            }
        }
    }

    private func handle(transaction: Transaction) {
        purchasedProductIDs.insert(transaction.productID)
    }

    /// Check if any tip has been purchased. Used to show a subtle
    /// "thank you" state rather than hiding the tip jar entirely.
    var hasTipped: Bool {
        !purchasedProductIDs.isEmpty
    }
}
