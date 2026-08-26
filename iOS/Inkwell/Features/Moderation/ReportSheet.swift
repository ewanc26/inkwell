//
//  ReportSheet.swift
//  Inkwell
//

import SwiftUI
import ATProtoKit

struct ReportSheet: View {
    @Environment(LoginStateManager.self) private var loginStateManager
    @Environment(\.dismiss) private var dismiss

    let subjectDID: String?
    let subjectURI: String?
    let onSubmit: () -> Void
    let onError: (String) -> Void

    @State private var selectedReason: ComAtprotoLexicon.Moderation.ReasonTypeDefinition = .spam
    @State private var comment = ""
    @State private var isSubmitting = false

    private var reasons: [ComAtprotoLexicon.Moderation.ReasonTypeDefinition] {
        [.spam, .violation, .misleading, .sexual, .rude, .other]
    }

    private var title: String {
        subjectDID != nil ? "Report Account" : "Report Post"
    }

    var body: some View {
        NavigationStack {
            Form {
                Picker("Reason", selection: $selectedReason) {
                    ForEach(reasons, id: \.self) { reason in
                        Text(reasonLabel(reason)).tag(reason)
                    }
                }
                .pickerStyle(.menu)

                Section("Comment (optional)") {
                    TextField("Additional context", text: $comment, axis: .vertical)
                        .textFieldStyle(.roundedBorder)
                        .lineLimit(3...6)
                }
            }
            .navigationTitle(title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                        .disabled(isSubmitting)
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Report") {
                        Task { await submit() }
                    }
                    .disabled(isSubmitting)
                }
            }
        }
    }

    private func reasonLabel(_ reason: ComAtprotoLexicon.Moderation.ReasonTypeDefinition) -> String {
        switch reason {
        case .spam: return "Spam"
        case .violation: return "Violation"
        case .misleading: return "Misleading"
        case .sexual: return "Sexual content"
        case .rude: return "Rude or harassing"
        case .other: return "Other"
        case .appeal: return "Appeal"
        }
    }

    private func submit() async {
        isSubmitting = true
        defer { isSubmitting = false }

        do {
            if let did = subjectDID {
                try await loginStateManager.submitReportForAccount(
                    did: did,
                    reasonType: selectedReason,
                    reason: comment.isEmpty ? nil : comment
                )
            } else if let uri = subjectURI {
                try await loginStateManager.submitReportForRecord(
                    uri: uri,
                    reasonType: selectedReason,
                    reason: comment.isEmpty ? nil : comment
                )
            }
            dismiss()
            onSubmit()
        } catch {
            onError(error.localizedDescription)
            dismiss()
        }
    }
}
