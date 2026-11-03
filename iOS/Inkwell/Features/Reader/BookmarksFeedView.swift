//
//  BookmarksFeedView.swift
//  Inkwell
//
//  The Reader's third feed: articles bookmarked on this device. Bookmark
//  state lives in ArticleStateStore, so this needs no network request of its
//  own; opening an entry pushes its AT-URI onto the enclosing NavigationStack,
//  which resolves it through the same RemoteDocumentView as a notification or
//  deep link.
//

import SwiftUI

struct BookmarksFeedView: View {
    @State private var articleState = ArticleStateStore.shared

    var body: some View {
        let bookmarks = articleState.bookmarkedArticles()

        if bookmarks.isEmpty {
            ContentUnavailableView {
                Label("No Bookmarks Yet", systemImage: "bookmark")
            } description: {
                Text("Open an article and tap Bookmark to keep it here.")
            }
        } else {
            List {
                ForEach(bookmarks) { bookmark in
                    NavigationLink(value: bookmark.articleID) {
                        bookmarkRow(bookmark)
                    }
                    .swipeActions(edge: .trailing, allowsFullSwipe: true) {
                        removeButton(for: bookmark, role: .destructive)
                    }
                    .contextMenu {
                        removeButton(for: bookmark, role: .destructive)
                    }
                }
            }
            .listStyle(.insetGrouped)
        }
    }

    private func bookmarkRow(_ bookmark: BookmarkedArticle) -> some View {
        VStack(alignment: .leading, spacing: 5) {
            Text(bookmark.title.isEmpty ? "Untitled" : bookmark.title)
                .font(.headline)
                .lineLimit(2)

            HStack(spacing: 10) {
                Text("Saved \(bookmark.savedAt.formatted(date: .abbreviated, time: .omitted))")
                if bookmark.isRead {
                    Label("Read", systemImage: "checkmark")
                        .labelStyle(.titleAndIcon)
                }
            }
            .font(.caption)
            .foregroundStyle(.secondary)
        }
        .padding(.vertical, 2)
        .accessibilityElement(children: .combine)
        .accessibilityHint("Opens this article")
    }

    /// One removal action shared by the swipe and the context menu, so both
    /// routes update the same store (and therefore the visible list).
    private func removeButton(for bookmark: BookmarkedArticle, role: ButtonRole) -> some View {
        Button(role: role) {
            articleState.setBookmarked(bookmark.articleID, title: bookmark.title, bookmarked: false)
        } label: {
            Label("Remove Bookmark", systemImage: "bookmark.slash")
        }
    }
}
