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
        let webView = WKWebView(frame: .zero, configuration: config)
        webView.scrollView.isScrollEnabled = false
        webView.isOpaque = false
        webView.backgroundColor = .clear
        webView.navigationDelegate = context.coordinator
        webView.load(URLRequest(url: url))
        return webView
    }

    func updateUIView(_ webView: WKWebView, context: Context) {}

    func makeCoordinator() -> Coordinator { Coordinator() }

    class Coordinator: NSObject, WKNavigationDelegate {
        func webView(
            _ webView: WKWebView,
            decidePolicyFor navigationAction: WKNavigationAction,
            decisionHandler: @escaping (WKNavigationActionPolicy) -> Void
        ) {
            if navigationAction.navigationType == .linkActivated {
                decisionHandler(.cancel)
            } else {
                decisionHandler(.allow)
            }
        }
    }
}
