//
//  FeedbackView.swift
//  Inkwell
//
//  "Send Feedback" sheet — posts a discussion to Inkwell's userinput.app
//  board (owned by ewancroft.uk). Opened from CreditsView.
//

import SwiftUI

struct FeedbackView: View {
    @Environment(LoginStateManager.self) private var loginStateManager
    @Environment(\.dismiss) private var dismiss

    @State private var title = ""
    @State private var bodyText = ""
    @State private var selectedTag: String?
    @State private var isSubmitting = false
    @State private var errorMessage: String?
    @State private var didSubmit = false

    private var canSubmit: Bool {
        !title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty && !isSubmitting
    }

    var body: some View {
        NavigationStack {
            Group {
                if didSubmit {
                    VStack(spacing: 12) {
                        Image(systemName: "checkmark.circle.fill")
                            .font(.system(size: 44))
                            .foregroundStyle(.green)
                        Text("Thanks!")
                            .font(.title2.weight(.bold))
                        Text("Your feedback was sent.")
                            .foregroundStyle(.secondary)
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else {
                    Form {
                        Section {
                            TextField("Title", text: $title)
                            TextField("Details (optional)", text: $bodyText, axis: .vertical)
                                .lineLimit(3...8)
                        } footer: {
                            Text("Sent to Inkwell's userinput.app board from your own account — bugs, questions, anything.")
                        }

                        Section {
                            ForEach(UserInputFeedback.tags, id: \.self) { tag in
                                Button {
                                    selectedTag = (selectedTag == tag) ? nil : tag
                                } label: {
                                    HStack {
                                        Text(tag)
                                            .foregroundStyle(.primary)
                                        Spacer()
                                        if selectedTag == tag {
                                            Image(systemName: "checkmark")
                                                .foregroundStyle(.tint)
                                        }
                                    }
                                }
                            }
                        } header: {
                            Text("Tag (optional)")
                        }

                        if let errorMessage {
                            Section {
                                Text(errorMessage)
                                    .foregroundStyle(.red)
                            }
                        }
                    }
                }
            }
            .navigationTitle("Send Feedback")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(didSubmit ? "Done" : "Cancel") { dismiss() }
                }
                if !didSubmit {
                    ToolbarItem(placement: .confirmationAction) {
                        if isSubmitting {
                            ProgressView()
                        } else {
                            Button("Send") { Task { await submit() } }
                                .disabled(!canSubmit)
                        }
                    }
                }
            }
        }
    }

    private func submit() async {
        isSubmitting = true
        errorMessage = nil
        do {
            try await loginStateManager.submitFeedback(
                title: title.trimmingCharacters(in: .whitespacesAndNewlines),
                body: bodyText,
                tag: selectedTag
            )
            isSubmitting = false
            didSubmit = true
        } catch {
            isSubmitting = false
            errorMessage = error.localizedDescription
        }
    }
}

#Preview {
    FeedbackView()
        .environment(LoginStateManager())
}
