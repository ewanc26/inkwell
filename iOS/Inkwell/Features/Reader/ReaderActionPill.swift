//
//  ReaderActionPill.swift
//  Inkwell
//

import SwiftUI

// MARK: - Action Pill

/// A small capsule button used for the Subscribe / Recommend actions.
/// Tinted from the resolved `ReaderTheme` so it stays in step with whatever
/// the publication actually set, while the capsule shape, fill transition,
/// and bounce give Inkwell's own chrome a bit more personality than a bare
/// toolbar icon would.
struct ReaderActionPill: View {
    let icon: String
    let label: String
    let isActive: Bool
    let isLoading: Bool
    let tint: Color
    let activeForeground: Color
    let action: () -> Void

    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        Button(action: action) {
            HStack(spacing: 6) {
                if isLoading {
                    ProgressView()
                        .scaleEffect(0.7)
                        .tint(isActive ? activeForeground : tint)
                } else if reduceMotion {
                    Image(systemName: icon)
                } else {
                    Image(systemName: icon)
                        .symbolEffect(.bounce, value: isActive)
                }
                Text(label)
                    .lineLimit(1)
                    .truncationMode(.tail)
            }
            .font(.subheadline.weight(.semibold))
            .foregroundStyle(isActive ? activeForeground : tint)
            .padding(.horizontal, 14)
            .padding(.vertical, 8)
            .background(
                Capsule()
                    .fill(isActive ? tint : tint.opacity(0.12))
            )
            .overlay(
                Capsule()
                    .strokeBorder(tint.opacity(isActive ? 0 : 0.35), lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
        .animation(reduceMotion ? nil : InkwellMotion.micro, value: isActive)
        .accessibilityLabel(label)
        .accessibilityHint(isActive ? "Tap to remove" : "Tap to add")
        .accessibilityAddTraits(isActive ? [.isButton, .isSelected] : .isButton)
    }
}
