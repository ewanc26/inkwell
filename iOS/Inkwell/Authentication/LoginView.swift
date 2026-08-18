//
//  LoginView.swift
//  Inkwell
//
//  OAuth-based sign-in. The user enters their AT Protocol handle and taps
//  "Sign in with your PDS." The app opens the system browser for OAuth
//  authorization — no app password is ever seen or stored.
//

import SwiftUI

struct LoginView: View {
    @Environment(LoginStateManager.self) private var loginStateManager

    @State private var handle = ""
    @State private var isSigningIn = false

    private enum Field: Hashable {
        case handle
    }
    @FocusState private var focusedField: Field?

    private var canSubmit: Bool {
        !handle.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
            && !isSigningIn
    }

    var body: some View {
        ScrollView {
            VStack(spacing: 32) {
                Spacer(minLength: 24)
                header
                onboardingSection
                formSection
                oauthNote
                Spacer(minLength: 24)
            }
            .padding(.horizontal, 24)
            .frame(maxWidth: .infinity)
            // Fills the scroll view's own height so the content centres on
            // a tall screen — the modern equivalent of measuring the
            // container with a GeometryReader and pinning a minHeight.
            .containerRelativeFrame(.vertical)
        }
        .scrollBounceBehavior(.basedOnSize)
        .scrollDismissesKeyboard(.interactively)
        .background(Color(uiColor: .systemBackground))
    }

    // MARK: - Sections

    private var header: some View {
        VStack(spacing: 12) {
            ZStack {
                InkwellMark()
                    .frame(height: 48)
                    .foregroundStyle(.primary)
            }
            .accessibilityHidden(true)

            Text("Inkwell")
                .font(.largeTitle.weight(.bold))

            Text("Sign in with your AT Protocol account")
                .font(.subheadline)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
        }
    }

    private var onboardingSection: some View {
        VStack(alignment: .leading, spacing: 10) {
            Label {
                Text("Read long-form posts from any standard.site blog — directly from the AT Protocol network, no middleman.")
                    .font(.callout)
                    .foregroundStyle(.secondary)
                    .fixedSize(horizontal: false, vertical: true)
            } icon: {
                Image(systemName: "book")
                    .foregroundStyle(.blue)
                    .frame(width: 20)
            }

            Label {
                Text("Subscribe to publications, leave comments, and recommend posts. Your data stays in your PDS.")
                    .font(.callout)
                    .foregroundStyle(.secondary)
                    .fixedSize(horizontal: false, vertical: true)
            } icon: {
                Image(systemName: "bell")
                    .foregroundStyle(.orange)
                    .frame(width: 20)
            }

            Label {
                Text("Write and publish your own posts using the standard.site lexicon.")
                    .font(.callout)
                    .foregroundStyle(.secondary)
                    .fixedSize(horizontal: false, vertical: true)
            } icon: {
                Image(systemName: "square.and.pencil")
                    .foregroundStyle(.green)
                    .frame(width: 20)
            }
        }
        .padding(16)
        // A semantic grouped-content fill rather than a translucent
        // quaternary shape style, so the panel keeps its contrast in dark
        // mode and with Increase Contrast turned on.
        .background(
            Color(uiColor: .secondarySystemGroupedBackground),
            in: .rect(cornerRadius: 12, style: .continuous)
        )
    }

    private var formSection: some View {
        VStack(spacing: 16) {
            handleField

            if let errorMessage = loginStateManager.errorMessage {
                errorBanner(errorMessage)
            }

            signInButton
        }
        .frame(maxWidth: 400)
    }

    private var handleField: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text("Handle")
                .font(.caption)
                .foregroundStyle(.secondary)
            TextField("yourname.bsky.social", text: $handle)
                .textContentType(.username)
                .textInputAutocapitalization(.never)
                .autocorrectionDisabled()
                .keyboardType(.URL)
                .focused($focusedField, equals: .handle)
                .submitLabel(.go)
                .onSubmit(submit)
                .padding(12)
                .background(fieldBackground)
        }
    }

    private var fieldBackground: some View {
        RoundedRectangle(cornerRadius: 10, style: .continuous)
            .fill(Color(uiColor: .secondarySystemBackground))
    }

    /// Validation feedback under the field, the way a form states it: a
    /// red footnote with a symbol, not a tinted alert-shaped box that
    /// competes with the primary button right below it.
    private func errorBanner(_ message: String) -> some View {
        Label {
            Text(message)
        } icon: {
            Image(systemName: "exclamationmark.triangle.fill")
        }
        .font(.footnote)
        .foregroundStyle(.red)
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityLabel("Sign-in error: \(message)")
    }

    private var signInButton: some View {
        Button(action: submit) {
            ZStack {
                Text("Continue")
                    .opacity(isSigningIn ? 0 : 1)
                if isSigningIn {
                    ProgressView()
                }
            }
            .frame(maxWidth: .infinity)
        }
        .buttonStyle(.borderedProminent)
        // The system's own large control metrics, rather than padding the
        // label out to guess at them.
        .controlSize(.large)
        .disabled(!canSubmit)
    }

    private var oauthNote: some View {
        VStack(spacing: 8) {
            Text("Inkwell uses OAuth to sign in to your PDS securely.")
                .font(.caption)
                .foregroundStyle(.secondary)
            Text("Your browser will open so you can approve access — no app password needed.")
                .font(.caption)
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
        }
        .frame(maxWidth: 400)
    }

    // MARK: - Actions

    private func submit() {
        guard canSubmit else { return }
        focusedField = nil
        isSigningIn = true
        Task {
            _ = await loginStateManager.signIn(handle: handle)
            isSigningIn = false
        }
    }
}

#Preview {
    LoginView()
        .environment(LoginStateManager())
}
