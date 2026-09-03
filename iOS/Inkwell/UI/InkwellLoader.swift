//
//  InkwellLoader.swift
//  Inkwell
//
//  A branded loading indicator that replaces the generic ProgressView
//  spinner everywhere in the app. The ink drop descends, hits the
//  baseline with a bounce, and the letterform fades in — a tiny moment
//  of personality every time content loads.
//

import SwiftUI

// MARK: - Animated Loader

/// An animated Inkwell wordmark used as a loading indicator. The ink drop
/// falls and bounces, then the letterform fades in. Loops gently while
/// loading continues. When Reduce Motion is enabled, the mark remains static
/// in its settled state instead of running the loop.
///
/// Usage:
/// ```swift
/// InkwellLoader(message: "Loading your reader...")
/// ```
struct InkwellLoader: View {
    let message: String?

    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var phase: LoaderPhase = .dropFalling
    @State private var task: Task<Void, Never>?

    private enum LoaderPhase {
        case dropFalling
        case dropBouncing
        case settling
    }

    init(message: String? = nil) {
        self.message = message
    }

    var body: some View {
        VStack(spacing: 16) {
            // The mark
            InkwellMark()
                .frame(height: 48)
                .foregroundStyle(.primary)
                .opacity(phaseOpacity)
                .scaleEffect(phaseScale)

            // Optional label
            if let message {
                Text(message)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
        }
        .task(id: reduceMotion) {
            task?.cancel()
            if reduceMotion {
                phase = .settling
            } else {
                await runAnimationLoop()
            }
        }
        .onDisappear {
            task?.cancel()
        }
    }

    private var phaseOpacity: Double {
        switch phase {
        case .dropFalling:  return 0.4
        case .dropBouncing: return 0.7
        case .settling:     return 1.0
        }
    }

    private var phaseScale: Double {
        switch phase {
        case .dropFalling:  return 0.92
        case .dropBouncing: return 1.03
        case .settling:     return 1.0
        }
    }

    private func runAnimationLoop() async {
        task = Task {
            while !Task.isCancelled {
                withAnimation(InkwellMotion.celebrate) {
                    phase = .dropFalling
                }
                do {
                    try await Task.sleep(for: .milliseconds(500))
                } catch {
                    return
                }

                withAnimation(InkwellMotion.micro) {
                    phase = .dropBouncing
                }
                do {
                    try await Task.sleep(for: .milliseconds(300))
                } catch {
                    return
                }

                withAnimation(InkwellMotion.standard) {
                    phase = .settling
                }
                do {
                    try await Task.sleep(for: .milliseconds(600))
                } catch {
                    return
                }
            }
        }
        await task?.value
    }
}

// MARK: - Loading Overlay

/// A full-screen loading overlay with the animated ink drop, used for
/// initial app launch while restoring the session.
struct InkwellLoadingOverlay: View {
    let message: String

    var body: some View {
        ZStack {
            Color(.systemBackground)
                .ignoresSafeArea()

            VStack(spacing: 24) {
                InkwellLoader(message: nil)
                    .frame(height: 56)
                Text(message)
                    .font(.body)
                    .foregroundStyle(.secondary)
            }
        }
    }
}

// MARK: - Inline Loader (replaces ProgressView in HStack contexts)

/// A compact inline loader for use inside HStack or button contexts.
/// Replaces `ProgressView()` with a tiny animated ink drop. Reduce Motion
/// presents the same indicator as a static dot.
struct InkwellInlineLoader: View {
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var isAnimating = false

    var body: some View {
        Circle()
            .fill(.primary.opacity(0.3))
            .frame(width: 6, height: 6)
            .scaleEffect(reduceMotion ? 1.0 : (isAnimating ? 1.3 : 0.7))
            .opacity(reduceMotion ? 1.0 : (isAnimating ? 0.4 : 1.0))
            .animation(
                reduceMotion ? nil : .easeInOut(duration: 0.8).repeatForever(autoreverses: true),
                value: isAnimating
            )
            .onAppear { isAnimating = !reduceMotion }
            .onChange(of: reduceMotion) { _, shouldReduceMotion in
                isAnimating = !shouldReduceMotion
            }
    }
}

#Preview("Loader") {
    InkwellLoader(message: "Loading your reader...")
}

#Preview("Loading Overlay") {
    InkwellLoadingOverlay(message: "Restoring your session…")
}

#Preview("Inline Loader") {
    HStack(spacing: 8) {
        InkwellInlineLoader()
        Text("Loading…")
            .font(.caption)
            .foregroundStyle(.secondary)
    }
}
