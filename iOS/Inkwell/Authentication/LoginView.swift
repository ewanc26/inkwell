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
        VStack(alignment: .leading, spacing: 8) {
            Label {
                Text("Read long-form posts from the AT Protocol network.")
                    .font(.footnote)
                    .foregroundStyle(.secondary)
                    .fixedSize(horizontal: false, vertical: true)
            } icon: {
                Image(systemName: "book")
                    .foregroundStyle(.blue)
                    .frame(width: 16)
            }

            Label {
                Text("Subscribe, comment, and recommend — your data stays in your PDS.")
                    .font(.footnote)
                    .foregroundStyle(.secondary)
                    .fixedSize(horizontal: false, vertical: true)
            } icon: {
                Image(systemName: "bell")
                    .foregroundStyle(.orange)
                    .frame(width: 16)
            }

            Label {
                Text("Write and publish your own posts.")
                    .font(.footnote)
                    .foregroundStyle(.secondary)
                    .fixedSize(horizontal: false, vertical: true)
            } icon: {
                Image(systemName: "square.and.pencil")
                    .foregroundStyle(.green)
                    .frame(width: 16)
            }
        }
        .padding(12)
        // secondarySystemBackground, not secondarySystemGroupedBackground —
        // this panel sits directly on the screen's plain .systemBackground,
        // and secondarySystemGroupedBackground is white-on-white against
        // that in light mode (it's meant to sit on .systemGroupedBackground
        // instead). secondarySystemBackground is the correct pairing for
        // "elevated content directly on plain systemBackground" and keeps
        // contrast in both light and dark mode.
        .background(
            Color(uiColor: .secondarySystemBackground),
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
