//
//  ReportSheet.swift
//  Inkwell
//

import SwiftUI
import InkwellShared

struct ReportSheet: View {
    @Environment(LoginStateManager.self) private var loginStateManager
    @Environment(\.dismiss) private var dismiss

    let subject: String
    let recordCID: String?
    let onSubmit: () -> Void
    let onError: (String) -> Void

    @State private var selectedReason: ReportReasonType = .spam
    @State private var comment = ""
    @State private var isSubmitting = false

    private var reasons: [ReportReasonType] {
        [.spam, .violation, .misleading, .sexual, .rude, .other]
    }

    private var title: String {
        subject.hasPrefix("did:") ? "Report Account" : "Report Post"
    }

    var body: some View {
        NavigationStack {
            Form {
                Picker("Reason", selection: $selectedReason) {
                    ForEach(reasons, id: \.wireValue) { reason in
                        Text(reason.displayName).tag(reason)
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

    private func submit() async {
        isSubmitting = true
        defer { isSubmitting = false }

        do {
            try await loginStateManager.submitReport(
                subject: subject,
                recordCID: recordCID,
                reasonType: selectedReason,
                reason: comment
            )
            dismiss()
            onSubmit()
        } catch {
            onError(error.localizedDescription)
            dismiss()
        }
    }
}
