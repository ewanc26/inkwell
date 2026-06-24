//
//  InkwellTheme.swift
//  Inkwell
//
//  Central design tokens for Inkwell's personality — motion, haptics,
//  and sensory details. Everything that makes the app feel like Inkwell
//  rather than a generic iOS app lives here.
//
//  Brand: Literary craftsmanship. Ink, paper, quiet focus, warmth.
//  Motion: Spring-based, organic, deliberate. Never robotic.
//  Haptics: Subtle, purposeful. Light taps for micro-interactions,
//          success pulses for achievements, error nudges for failures.
//

import SwiftUI
#if os(iOS)
import UIKit
#endif

// MARK: - Motion Personality

/// Central animation presets. Every animation in the app should use one
/// of these rather than ad-hoc values. The personality is "deliberate
/// craftsmanship" — springs are tuned for a fountain-pen feel: precise
/// but with a hint of organic bounce.
enum InkwellMotion {

    /// Quick micro-interactions — button presses, icon toggles, checkbox ticks.
    /// ~250ms, light bounce. Feels responsive without being jarring.
    static let micro: Animation = .spring(response: 0.25, dampingFraction: 0.75)

    /// Standard transitions — navigation pushes, sheet presents, content loads.
    /// ~400ms, medium bounce. The workhorse animation.
    static let standard: Animation = .spring(response: 0.4, dampingFraction: 0.8)

    /// Slow, contemplative reveals — greeting, wordmark appearance, empty states.
    /// ~700ms, soft bounce. Feels like ink spreading on paper.
    static let soft: Animation = .spring(response: 0.7, dampingFraction: 0.85)

    /// Celebration — publish success, subscription confirmation.
    /// ~500ms, extra bounce. A moment of joy.
    static let celebrate: Animation = .spring(response: 0.5, dampingFraction: 0.65)

    /// The ink-drop animation — a unique cubic bezier that mimics a drop
    /// falling, hitting a surface, and settling. Used for the loading
    /// indicator and pull-to-refresh.
    static let inkDrop: Animation = .timingCurve(0.34, 1.56, 0.64, 1.0, duration: 0.6)

    /// Stagger delay for cascading content reveals.
    static func stagger(index: Int, base: Double = 0.05) -> Double {
        Double(index) * base
    }
}

// MARK: - Haptic Vocabulary

/// Every haptic event in the app. Centralized so the "feel" is consistent
/// and tunable. Uses both UIKit feedback generators (for precise control)
/// and SwiftUI's `sensoryFeedback` (for declarative use).
enum InkwellHaptics {

    /// Light tap — button presses, cell selection, toggle switches.
    /// The most common haptic. Subtle, like a pen touching paper.
    @MainActor static func light() {
        #if os(iOS)
        UIImpactFeedbackGenerator(style: .light).impactOccurred()
        #endif
    }

    @MainActor static func medium() {
        #if os(iOS)
        UIImpactFeedbackGenerator(style: .medium).impactOccurred()
        #endif
    }

    @MainActor static func success() {
        #if os(iOS)
        UINotificationFeedbackGenerator().notificationOccurred(.success)
        #endif
    }

    @MainActor static func warning() {
        #if os(iOS)
        UINotificationFeedbackGenerator().notificationOccurred(.warning)
        #endif
    }

    @MainActor static func error() {
        #if os(iOS)
        UINotificationFeedbackGenerator().notificationOccurred(.error)
        #endif
    }

    @MainActor static func selection() {
        #if os(iOS)
        UISelectionFeedbackGenerator().selectionChanged()
        #endif
    }
}

// MARK: - Sensory Feedback View Modifiers

/// Conditionally applies a success haptic when `trigger` changes, but only
/// if `active` is true. Prevents haptics from firing during initial data
/// load (e.g., when `loadActionState()` sets initial subscribe/recommend
/// state from the server).
struct ConditionalHaptic<T: Equatable>: ViewModifier {
    let active: Bool
    let trigger: T

    func body(content: Content) -> some View {
        if active {
            content.sensoryFeedback(.success, trigger: trigger)
        } else {
            content
        }
    }
}

extension View {

    /// Adds a light haptic on tap — for buttons, toggles, and selectable rows.
    func inkwellTapHaptic() -> some View {
        self.onTapGesture {
            InkwellHaptics.light()
        }
    }

    /// Adds success haptic + a subtle scale bounce. Use on publish/subscribe
    /// confirmations.
    func inkwellCelebrate<V: Equatable>(value: V) -> some View {
        self
            .sensoryFeedback(.success, trigger: value)
            .animation(InkwellMotion.celebrate, value: value)
    }

    /// Staggers child view appearances with increasing delay.
    /// Wrap a ForEach or VStack content with this.
    func inkwellStaggerEntrance(isActive: Bool = true) -> some View {
        self.opacity(isActive ? 1 : 0)
            .animation(InkwellMotion.soft, value: isActive)
    }
}

// MARK: - Button Style

/// Inkwell's primary button style — a capsule with spring press animation
/// and light haptic feedback. Use for primary actions: Publish, Subscribe,
/// Sign In.
struct InkwellButtonStyle: ButtonStyle {
    var tint: Color = .accentColor

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.body.weight(.semibold))
            .foregroundStyle(.white)
            .padding(.horizontal, 24)
            .padding(.vertical, 12)
            .background(tint)
            .clipShape(Capsule())
            .scaleEffect(configuration.isPressed ? 0.96 : 1.0)
            .animation(InkwellMotion.micro, value: configuration.isPressed)
            .onChange(of: configuration.isPressed) { _, pressed in
                if pressed { InkwellHaptics.light() }
            }
    }
}
