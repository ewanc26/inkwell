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
        GeometryReader { proxy in
            ScrollView {
                VStack(spacing: 32) {
                    Spacer(minLength: 24)
                    header
                    formSection
                    oauthNote
                    Spacer(minLength: 24)
                }
                .padding(.horizontal, 24)
                .frame(minHeight: proxy.size.height)
                .frame(maxWidth: .infinity)
            }
            .scrollDismissesKeyboard(.interactively)
        }
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

    private func errorBanner(_ message: String) -> some View {
        HStack(alignment: .top, spacing: 8) {
            Image(systemName: "exclamationmark.triangle.fill")
                .foregroundStyle(.red)
            Text(message)
                .font(.footnote)
                .foregroundStyle(.red)
            Spacer(minLength: 0)
        }
        .padding(12)
        .background(
            RoundedRectangle(cornerRadius: 10, style: .continuous)
                .fill(Color.red.opacity(0.1))
        )
    }

    private var signInButton: some View {
        Button(action: submit) {
            ZStack {
                Text("Continue")
                    .opacity(isSigningIn ? 0 : 1)
                if isSigningIn {
                    ProgressView()
                        .tint(.white)
                }
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 14)
        }
        .buttonStyle(.borderedProminent)
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
