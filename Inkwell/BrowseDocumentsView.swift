//
//  BrowseDocumentsView.swift
//  Inkwell
//
//  Created by Letta on 20/06/2026.
//

import SwiftUI
import ATProtoKit

struct BrowseDocumentsView: View {
    @Environment(LoginStateManager.self) private var loginStateManager
    
    @State private var publications: [PublicationEntry] = []
    @State private var documents: [DocumentEntry] = []
    @State private var isLoading = true
    @State private var errorMessage: String? = nil
    
    var body: some View {
        NavigationStack {
            Group {
                if isLoading {
                    ProgressView("Fetching standard.site documents...")
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else if let error = errorMessage {
                    ContentUnavailableView(
                        "Failed to Load Documents",
                        systemImage: "exclamationmark.triangle",
                        description: Text(error)
                    )
                } else if documents.isEmpty {
                    ContentUnavailableView(
                        "No Documents Found",
                        systemImage: "doc.text",
                        description: Text("Create publication or documents in standard.site to read them here.")
                    )
                } else {
                    List {
                        ForEach(publications) { publication in
                            Section {
                                let pubDocs = documents.filter { publication.contains($0.record) }
                                
                                ForEach(pubDocs) { document in
                                    NavigationLink {
                                        ReadView(
                                            document: document.record,
                                            publication: publication.record,
                                            documentURI: document.uri,
                                            authorDID: document.authorDID
                                        )
                                    } label: {
                                        DocumentRowView(document: document.record, publication: publication.record)
                                    }
                                    .listRowInsets(EdgeInsets(top: 8, leading: 12, bottom: 8, trailing: 12))
                                }
                            } header: {
                                PublicationHeaderView(publication: publication.record)
                            }
                        }
                        
                        // Loose documents not associated with any known publication
                        let looseDocs = documents.filter { document in
                            !publications.contains(where: { $0.contains(document.record) })
                        }
                        
                        if !looseDocs.isEmpty {
                            Section("Other Documents") {
                                ForEach(looseDocs) { document in
                                    NavigationLink {
                                        ReadView(
                                            document: document.record,
                                            publication: nil,
                                            documentURI: document.uri,
                                            authorDID: document.authorDID
                                        )
                                    } label: {
                                        DocumentRowView(document: document.record, publication: nil)
                                    }
                                }
                            }
                        }
                    }
                    .listStyle(.insetGrouped)
                    .refreshable {
                        await loadData()
                    }
                }
            }
            .navigationTitle("Reader")
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(role: .destructive, action: loginStateManager.signOut) {
                        Label("Sign Out", systemImage: "rectangle.portrait.and.arrow.right")
                    }
                }
                
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button {
                        Task {
                            await loadData()
                        }
                    } label: {
                        Image(systemName: "arrow.clockwise")
                    }
                }
            }
            .task {
                await loadData()
            }
        }
    }
    
    private func loadData() async {
        isLoading = true
        errorMessage = nil
        
        do {
            async let fetchedPubs = loginStateManager.fetchPublicationsWithURIs()
            async let fetchedDocs = loginStateManager.fetchDocumentsWithURIs()
            
            let (pubs, docs) = try await (fetchedPubs, fetchedDocs)
            
            // Sort documents by published date descending
            self.publications = pubs
            self.documents = docs.sorted { $0.record.publishedAt > $1.record.publishedAt }
        } catch {
            self.errorMessage = error.localizedDescription
        }
        
        self.isLoading = false
    }
}

struct PublicationHeaderView: View {
    @Environment(LoginStateManager.self) private var loginStateManager
    let publication: SiteStandardLexicon.PublicationRecord
    
    var body: some View {
        HStack(spacing: 12) {
            if let icon = publication.icon, let did = loginStateManager.currentDID {
                let urlString = "https://cdn.bsky.app/img/avatar/plain/\(did)/\(icon.reference.link)"
                AsyncImage(url: URL(string: urlString)) { image in
                    image
                        .resizable()
                        .scaledToFill()
                } placeholder: {
                    Color.gray.opacity(0.1)
                }
                .frame(width: 44, height: 44)
                .clipShape(RoundedRectangle(cornerRadius: 8))
                .overlay(
                    RoundedRectangle(cornerRadius: 8)
                        .stroke(Color.primary.opacity(0.1), lineWidth: 1)
                )
            } else {
                Image(systemName: "building.2.crop.left.right.fill")
                    .font(.title3)
                    .foregroundStyle(.secondary)
                    .frame(width: 44, height: 44)
                    .background(Color.primary.opacity(0.05))
                    .clipShape(RoundedRectangle(cornerRadius: 8))
            }
            
            VStack(alignment: .leading, spacing: 4) {
                Text(publication.name)
                    .font(.headline)
                    .foregroundStyle(.primary)
                
                if let desc = publication.description, !desc.isEmpty {
                    Text(desc)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .lineLimit(1)
                }
            }
        }
        .padding(.vertical, 6)
        .textCase(nil) // prevent system upper-casing header text
    }
}

struct DocumentRowView: View {
    @Environment(LoginStateManager.self) private var loginStateManager
    let document: SiteStandardLexicon.DocumentRecord
    let publication: SiteStandardLexicon.PublicationRecord?
    
    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            VStack(alignment: .leading, spacing: 6) {
                Text(document.title)
                    .font(.system(.body, design: .serif))
                    .fontWeight(.semibold)
                    .lineLimit(2)
                    .foregroundStyle(.primary)
                
                if let desc = document.description, !desc.isEmpty {
                    Text(desc)
                        .font(.system(.caption, design: .serif))
                        .foregroundStyle(.secondary)
                        .lineLimit(2)
                }
                
                Text(formatDate(document.publishedAt))
                    .font(.caption2)
                    .foregroundStyle(.secondary)
                    .padding(.top, 2)
            }
            
            Spacer()
            
            if let cover = document.coverImage, let did = loginStateManager.currentDID {
                let urlString = "https://cdn.bsky.app/img/feed_thumbnail/plain/\(did)/\(cover.reference.link)"
                AsyncImage(url: URL(string: urlString)) { image in
                    image
                        .resizable()
                        .scaledToFill()
                } placeholder: {
                    Color.gray.opacity(0.05)
                }
                .frame(width: 70, height: 70)
                .clipShape(RoundedRectangle(cornerRadius: 6))
            }
        }
        .padding(.vertical, 4)
    }
    
    private func formatDate(_ date: Date) -> String {
        let outputFormatter = DateFormatter()
        outputFormatter.dateStyle = .medium
        outputFormatter.timeStyle = .none
        return outputFormatter.string(from: date)
    }
}
