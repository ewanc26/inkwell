//
//  WriterDraftBanner.swift
//  Inkwell
//
//  Draft recovery UI for the Writer: the "Draft restored" banner, the
//  remote-revision conflict alert, and the view hooks that drive restore and
//  debounced autosave in `WriterViewModel+Draft.swift`.
//

import SwiftUI

/// Form section announcing a restored draft, with a way to throw it away.
struct WriterDraftBanner: View {
    let viewModel: WriterViewModel

    var body: some View {
        Section {
            HStack(spacing: 8) {
                Image(systemName: "clock.arrow.circlepath")
                    .foregroundStyle(.blue)
                    .accessibilityHidden(true)
                Text("Draft restored")
                Spacer()
                Button("Discard", role: .destructive) {
                    viewModel.discardDraft()
                }
                .buttonStyle(.borderless)
                Button {
                    viewModel.dismissDraftBanner()
                } label: {
                    Image(systemName: "xmark")
                        .foregroundStyle(.secondary)
                }
                .buttonStyle(.borderless)
                .accessibilityLabel("Dismiss")
            }
        } footer: {
            Text("Inkwell autosaved this on your device. Discarding it can't be undone.")
        }
    }
}

private struct WriterDraftLifecycle: ViewModifier {
    let viewModel: WriterViewModel
    let accountDID: String?

    func body(content: Content) -> some View {
        content
            .task(id: accountDID) {
                await viewModel.restoreDraft(accountDid: accountDID)
            }
            .onChange(of: viewModel.autosaveSnapshot) { _, snapshot in
                viewModel.scheduleDraftAutosave(snapshot)
            }
            .alert(
                "Remote Document Changed",
                isPresented: Binding(
                    get: { viewModel.draftConflict },
                    // Only the buttons below may resolve the conflict.
                    set: { _ in }
                )
            ) {
                Button("Keep Draft") {
                    viewModel.resolveDraftConflict(keepDraft: true)
                }
                Button("Reload", role: .destructive) {
                    viewModel.resolveDraftConflict(keepDraft: false)
                }
            } message: {
                Text("This document changed elsewhere after your draft was saved. Keep your draft, replacing those changes when you update, or reload the published version and discard the draft?")
            }
    }
}

extension View {
    /// Restores `accountDID`'s draft, autosaves edits, and surfaces revision conflicts.
    func writerDraftLifecycle(viewModel: WriterViewModel, accountDID: String?) -> some View {
        modifier(WriterDraftLifecycle(viewModel: viewModel, accountDID: accountDID))
    }
}
