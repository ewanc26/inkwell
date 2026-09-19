//
//  InAppLinkHandling.swift
//  Inkwell
//
//  A view modifier that routes SwiftUI `Link`/`openURL` calls within its
//  subtree through an in-app SFSafariViewController sheet, gated by
//  LinkPreferences.openLinksInApp -- falling back to the system browser
//  when the preference is off or the URL isn't http(s). Applied at the
//  container level (ReadView, DiscoverView) so it covers every content
//  link inside without touching each call site individually.
//

import SwiftUI
import SafariServices

struct SafariView: UIViewControllerRepresentable {
    let url: URL

    func makeUIViewController(context: Context) -> SFSafariViewController {
        SFSafariViewController(url: url)
    }

    func updateUIViewController(_ uiViewController: SFSafariViewController, context: Context) {}
}

private struct IdentifiableURL: Identifiable {
    let url: URL
    var id: URL { url }
}

private struct InAppLinkHandler: ViewModifier {
    @State private var presentedURL: IdentifiableURL?
    @State private var linkPreferences = LinkPreferences.shared

    func body(content: Content) -> some View {
        content
            .environment(\.openURL, OpenURLAction { url in
                guard Self.isAllowedContentURL(url) else {
                    return .discarded
                }
                guard linkPreferences.openLinksInApp, let scheme = url.scheme?.lowercased(),
                      scheme == "http" || scheme == "https" else {
                    return .systemAction
                }
                presentedURL = IdentifiableURL(url: url)
                return .handled
            })
            .sheet(item: $presentedURL) { item in
                SafariView(url: item.url)
            }
    }

    private static func isAllowedContentURL(_ url: URL) -> Bool {
        guard let scheme = url.scheme?.lowercased(),
              !url.absoluteString.unicodeScalars.contains(where: { $0.properties.isControl }),
              url.absoluteString == url.absoluteString.trimmingCharacters(in: .whitespacesAndNewlines),
              ["http", "https", "mailto", "tel"].contains(scheme) else { return false }
        if scheme == "http" || scheme == "https" { return url.host != nil }
        return true
    }
}

extension View {
    /// Opt a content view's `Link`s into the in-app browser preference.
    func inAppLinkHandling() -> some View {
        modifier(InAppLinkHandler())
    }
}
