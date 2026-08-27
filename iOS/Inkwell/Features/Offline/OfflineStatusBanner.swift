//
//  OfflineStatusBanner.swift
//  Inkwell
//

import SwiftUI

struct OfflineStatusBanner: View {
    @ScaledMetric(relativeTo: .body) private var verticalPadding = 8.0

    var body: some View {
        Label {
            Text("You’re offline. Search and new content may be unavailable.")
        } icon: {
            Image(systemName: "wifi.slash")
                .accessibilityHidden(true)
        }
        .font(.footnote)
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 16)
        .padding(.vertical, verticalPadding)
        .background(Color(uiColor: .secondarySystemBackground))
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("Offline. Search and new content may be unavailable.")
    }
}
