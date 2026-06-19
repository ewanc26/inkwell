//
//  LoginView.swift
//  Inkwell
//
//  Created by Ewan Croft on 19/06/2026.
//

import SwiftUI

struct LoginView: View {
    @Environment(LoginStateManager.self) private var loginStateManager

    @State private var handle = ""
    @State private var password = ""
    @State private var isSigningIn = false

    private enum Field: Hashable {
        case handle
        case password
    }
    @FocusState private var focusedField: Field?

    private var canSubmit: Bool {
        !handle.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
            && !password.isEmpty
            && !isSigningIn
    }

    var body: some View {
        GeometryReader { proxy in
            ScrollView {
                VStack(spacing: 32) {
                    Spacer(minLength: 24)
                    header
                    formSection
                    appPasswordNote
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
                Circle()
                    .fill(Color.accentColor.opacity(0.15))
                    .frame(width: 88, height: 88)
                Image(systemName: "drop.fill")
                    .font(.system(size: 36, weight: .medium))
                    .foregroundStyle(Color.accentColor)
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
            credentialFields

            if let errorMessage = loginStateManager.errorMessage {
                errorBanner(errorMessage)
            }

            signInButton
        }
        .frame(maxWidth: 400)
    }

    private var credentialFields: some View {
        VStack(spacing: 12) {
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
                    .submitLabel(.next)
                    .onSubmit { focusedField = .password }
                    .padding(12)
                    .background(fieldBackground)
            }

            VStack(alignment: .leading, spacing: 6) {
                Text("App password")
                    .font(.caption)
                    .foregroundStyle(.secondary)
                SecureField("••••••••••••", text: $password)
                    .textContentType(.password)
                    .focused($focusedField, equals: .password)
                    .submitLabel(.go)
                    .onSubmit(submit)
                    .padding(12)
                    .background(fieldBackground)
            }
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
                Text("Sign In")
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

    private var appPasswordNote: some View {
        Text("Use an app password, not your main account password.")
            .font(.caption)
            .foregroundStyle(.secondary)
            .multilineTextAlignment(.center)
            .frame(maxWidth: 400)
    }

    // MARK: - Actions

    private func submit() {
        guard canSubmit else { return }
        focusedField = nil
        isSigningIn = true
        Task {
            _ = await loginStateManager.signIn(handle: handle, password: password)
            isSigningIn = false
            if loginStateManager.isAuthenticated {
                password = ""
            }
        }
    }
}

#Preview {
    LoginView()
        .environment(LoginStateManager())
}
