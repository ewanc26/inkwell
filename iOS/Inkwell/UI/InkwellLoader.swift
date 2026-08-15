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
/// loading continues.
///
/// Usage:
/// ```swift
/// InkwellLoader(message: "Loading your reader...")
/// ```
struct InkwellLoader: View {
    let message: String?

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
        .task {
            await runAnimationLoop()
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
                withAnimation(.spring(response: 0.5, dampingFraction: 0.65)) {
                    phase = .dropFalling
                }
                try? await Task.sleep(for: .milliseconds(500))

                withAnimation(.spring(response: 0.3, dampingFraction: 0.5)) {
                    phase = .dropBouncing
                }
                try? await Task.sleep(for: .milliseconds(300))

                withAnimation(.spring(response: 0.4, dampingFraction: 0.8)) {
                    phase = .settling
                }
                try? await Task.sleep(for: .milliseconds(600))
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
/// Replaces `ProgressView()` with a tiny animated ink drop.
struct InkwellInlineLoader: View {
    @State private var isAnimating = false

    var body: some View {
        Circle()
            .fill(.primary.opacity(0.3))
            .frame(width: 6, height: 6)
            .scaleEffect(isAnimating ? 1.3 : 0.7)
            .opacity(isAnimating ? 0.4 : 1.0)
            .animation(
                .easeInOut(duration: 0.8).repeatForever(autoreverses: true),
                value: isAnimating
            )
            .onAppear { isAnimating = true }
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
