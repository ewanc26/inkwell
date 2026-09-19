//
//  IframeBlockView.swift
//  Inkwell
//

import SwiftUI
import WebKit

// MARK: - Iframe Block View

/// Wraps a `WKWebView` for `pub.leaflet.blocks.iframe` embeds.
/// Blocks link-activated navigation so the iframe acts as a pure embed.
struct IframeBlockView: UIViewRepresentable {
    let url: URL
    let height: Double?
    let aspectRatio: String?
    let foregroundColor: Color

    func makeUIView(context: Context) -> WKWebView {
        let config = WKWebViewConfiguration()
        config.defaultWebpagePreferences.allowsContentJavaScript = false
        config.websiteDataStore = .nonPersistent()
        let webView = WKWebView(frame: .zero, configuration: config)
        webView.scrollView.isScrollEnabled = false
        webView.isOpaque = false
        webView.backgroundColor = .clear
        webView.navigationDelegate = context.coordinator
        webView.uiDelegate = context.coordinator
        webView.load(URLRequest(url: url))
        return webView
    }

    func updateUIView(_ webView: WKWebView, context: Context) {}

    func dismantleUIView(_ webView: WKWebView, coordinator: Coordinator) {
        webView.stopLoading()
        webView.navigationDelegate = nil
        webView.uiDelegate = nil
        webView.loadHTMLString("", baseURL: nil)
    }

    func makeCoordinator() -> Coordinator { Coordinator(allowedHost: url.host) }

    class Coordinator: NSObject, WKNavigationDelegate, WKUIDelegate {
        private let allowedHost: String?

        init(allowedHost: String?) {
            self.allowedHost = allowedHost
        }

        func webView(
            _ webView: WKWebView,
            decidePolicyFor navigationAction: WKNavigationAction,
            decisionHandler: @escaping (WKNavigationActionPolicy) -> Void
        ) {
            guard let candidate = navigationAction.request.url,
                  candidate.scheme == "https",
                  candidate.host == allowedHost else {
                decisionHandler(.cancel)
                return
            }
            if navigationAction.targetFrame?.isMainFrame == true {
                decisionHandler(.allow)
            } else {
                decisionHandler(.cancel)
            }
        }

        func webView(
            _ webView: WKWebView,
            createWebViewWith configuration: WKWebViewConfiguration,
            for navigationAction: WKNavigationAction,
            windowFeatures: WKWindowFeatures
        ) -> WKWebView? { nil }
    }
}
