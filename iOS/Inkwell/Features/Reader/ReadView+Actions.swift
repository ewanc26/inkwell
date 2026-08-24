//
//  ReadView+Actions.swift
//  Inkwell
//

import SwiftUI
import ATProtoKit

extension ReadView {
    // MARK: - Comments

    func loadComments() async {
        guard let uri = documentURI ?? resolvedDocumentURI else { return }
        isLoadingComments = true
        defer { isLoadingComments = false }
        do {
            comments = try await loginStateManager.fetchComments(documentURI: uri)
        } catch {
            // Comments are best-effort — don't show errors inline
            print("[ReadView] loadComments failed: \(error)")
        }
    }

    func submitComment() async {
        guard let uri = documentURI ?? resolvedDocumentURI else { return }
        let text = newCommentText.trimmingCharacters(in: .whitespaces)
        guard !text.isEmpty else { return }

        isSubmittingComment = true
        defer { isSubmittingComment = false }

        do {
            try await loginStateManager.createComment(
                subject: uri,
                plaintext: text,
                replyTo: replyToComment?.uri,
                onPage: nil
            )
            newCommentText = ""
            replyToComment = nil
            await loadComments()  // refresh
        } catch {
            print("[ReadView] submitComment failed: \(error)")
        }
    }

    /// The resolved AT-URI of the current document, derived from the author
    /// DID + document path when an explicit documentURI isn't provided.
    var resolvedDocumentURI: String? {
        guard let did = authorDID ?? loginStateManager.currentDID else { return nil }
        // Reconstruct from the document record's identity
        return "at://\(did)/site.standard.document/\(document.path ?? "")"
    }

    // MARK: - Action Pills

    @ViewBuilder
    var actionPills: some View {
        if let publicationURI {
            ReaderActionPill(
                icon: isSubscribed ? "bell.fill" : "bell",
                label: isSubscribed ? "Subscribed" : "Subscribe",
                isActive: isSubscribed,
                isLoading: isTogglingSubscription,
                tint: accentColor,
                activeForeground: theme.accentForeground
            ) {
                Task { await toggleSubscription(publicationURI: publicationURI) }
            }
            .disabled(isTogglingSubscription)
        }

        if let documentURI {
            ReaderActionPill(
                icon: isRecommended ? "star.fill" : "star",
                label: isRecommended ? "Recommended" : "Recommend",
                isActive: isRecommended,
                isLoading: isSubmittingRecommend,
                tint: accentColor,
                activeForeground: theme.accentForeground
            ) {
                Task { await toggleRecommend(documentURI: documentURI) }
            }
            .disabled(isSubmittingRecommend)
        }

        if let articleID {
            let isBookmarked = articleState.isBookmarked(articleID)
            ReaderActionPill(
                icon: isBookmarked ? "bookmark.fill" : "bookmark",
                label: isBookmarked ? "Bookmarked" : "Bookmark",
                isActive: isBookmarked,
                isLoading: false,
                tint: accentColor,
                activeForeground: theme.accentForeground
            ) {
                toggleBookmark(articleID: articleID)
            }
        }
    }

    // MARK: - Action State

    func loadActionState() async {
        if let publicationURI {
            if let subs = try? await loginStateManager.fetchSubscriptions(),
               let match = subs.first(where: { $0.record.publication == publicationURI }) {
                isSubscribed = true
                subscriptionRecordKey = match.recordKey
            } else {
                isSubscribed = false
                subscriptionRecordKey = nil
            }
        }

        if let documentURI {
            if let recs = try? await loginStateManager.fetchRecommends(),
               let match = recs.first(where: { $0.record.document == documentURI }) {
                isRecommended = true
                recommendRecordKey = match.recordKey
            } else {
                isRecommended = false
                recommendRecordKey = nil
            }
        }
    }

    func toggleSubscription(publicationURI: String) async {
        guard !isTogglingSubscription else { return }
        isTogglingSubscription = true
        actionMessage = nil
        defer { isTogglingSubscription = false }

        do {
            if isSubscribed, let key = subscriptionRecordKey {
                try await loginStateManager.deleteSubscription(recordKey: key)
                isSubscribed = false
                subscriptionRecordKey = nil
            } else {
                if let publication {
                    // Best-effort: subscribing shouldn't be blocked on the
                    // verification round-trip, only informed by it.
                    _ = try? await SiteStandardLexicon.Verification.verify(
                        publicationURI: publicationURI,
                        publication: publication
                    )
                }
                let reference = try await loginStateManager.createSubscription(publicationURI: publicationURI)
                isSubscribed = true
                subscriptionRecordKey = parseAtUri(reference.recordURI)?.recordKey
                await NotificationManager.shared.requestPermission()
            }
        } catch {
            actionMessage = "Couldn't update subscription: \(error.localizedDescription)"
        }
    }

    func toggleRecommend(documentURI: String) async {
        guard !isSubmittingRecommend else { return }
        isSubmittingRecommend = true
        actionMessage = nil
        defer { isSubmittingRecommend = false }

        do {
            if isRecommended, let key = recommendRecordKey {
                try await loginStateManager.deleteRecommend(recordKey: key)
                isRecommended = false
                recommendRecordKey = nil
            } else {
                let reference = try await loginStateManager.createRecommend(documentURI: documentURI)
                isRecommended = true
                recommendRecordKey = parseAtUri(reference.recordURI)?.recordKey
            }
        } catch {
            actionMessage = "Couldn't update recommendation: \(error.localizedDescription)"
        }
    }

    // MARK: - Content Loader

    func loadContent() async {
        guard let contentUnknown = document.content else {
            self.markdownContent = document.textContent
            isLoading = false
            return
        }

        // Leaflet carries layout, alignment, and PDS blob references that
        // cannot survive a Markdown round-trip. Render it natively first.
        if let leaflet = contentUnknown.getRecord(ofType: LeafletContent.self) {
            if let inlinePages = leaflet.pages, !inlinePages.isEmpty {
                pages = inlinePages
                isLoading = false
                return
            }

            if let blobPages = leaflet.blobPages {
                do {
                    let data: Data
                    if let authorDID {
                        data = try await loginStateManager.downloadBlob(
                            cid: blobPages.reference.link,
                            fromDID: authorDID
                        )
                    } else {
                        data = try await loginStateManager.downloadBlob(cid: blobPages.reference.link)
                    }
                    pages = try JSONDecoder().decode([LeafletPage].self, from: data)
                    isLoading = false
                    return
                } catch {
                    errorMessage = "Failed to download this Leaflet: \(error.localizedDescription)"
                    isLoading = false
                    return
                }
            }
        }

        // Other supported formats convert cleanly to the shared Markdown
        // renderer.
        if let provider = ProviderRegistry.detectProvider(contentUnknown) {
            let result = provider.toMarkdown(contentUnknown)
            if !result.markdown.isEmpty {
                self.markdownContent = result.markdown
                self.isLoading = false
                return
            }
        }

        if markdownContent == nil && pages.isEmpty {
            markdownContent = document.textContent
        }

        self.isLoading = false
    }

    // MARK: - Local Read / Bookmark State

    func markAsReadIfNeeded() {
        guard let articleID else { return }
        articleState.markAsRead(articleID, title: document.title)
    }

    func toggleBookmark(articleID: String) {
        articleState.setBookmarked(
            articleID,
            title: document.title,
            bookmarked: !articleState.isBookmarked(articleID)
        )
    }

    func verifySource() async {
        guard let documentURI,
              document.canonicalURL(publication: publication) != nil else {
            return
        }
        do {
            try await SiteStandardLexicon.Verification.verify(
                documentURI: documentURI,
                document: document,
                publication: publication
            )
            isVerified = true
        } catch {
            isVerified = false
        }
    }
}
