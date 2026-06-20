//
//  InkwellMark.swift
//  Inkwell
//

import SwiftUI

/// The Inkwell wordmark on its own — a serif "I" with a single drop of ink
/// beneath it — drawn as pure vector geometry rather than an image asset.
///
/// Traces the same coordinates as the app icon's Icon Composer layers
/// (`Inkwell.icon/Assets/letter.svg` and `dot.svg`), so it stays in sync
/// with the real icon design without needing separate light/dark image
/// assets: the letter follows `foregroundStyle` like any other shape, and
/// only the ink drop carries a fixed brand colour.
struct InkwellMark: View {
    private let designSize = CGSize(width: 400, height: 952)
    private let dotColor = Color(red: 0, green: 152.0 / 255.0, blue: 0)

    var body: some View {
        Canvas { context, size in
            let scale = size.width / designSize.width

            func bar(x: CGFloat, y: CGFloat, width: CGFloat, height: CGFloat, cornerRadius: CGFloat) -> Path {
                Path(
                    roundedRect: CGRect(x: x * scale, y: y * scale, width: width * scale, height: height * scale),
                    cornerRadius: cornerRadius * scale
                )
            }

            // The letter: top bar, vertical stroke, bottom bar.
            context.fill(bar(x: 40, y: 40, width: 320, height: 80, cornerRadius: 16), with: .style(.foreground))
            context.fill(bar(x: 125, y: 120, width: 150, height: 640, cornerRadius: 0), with: .style(.foreground))
            context.fill(bar(x: 40, y: 760, width: 320, height: 80, cornerRadius: 16), with: .style(.foreground))

            // The ink drop.
            let dotDiameter = 64 * scale
            let dotRect = CGRect(
                x: 200 * scale - dotDiameter / 2,
                y: 880 * scale - dotDiameter / 2,
                width: dotDiameter,
                height: dotDiameter
            )
            context.fill(Path(ellipseIn: dotRect), with: .color(dotColor))
        }
        .aspectRatio(designSize.width / designSize.height, contentMode: .fit)
    }
}

#Preview {
    VStack(spacing: 24) {
        InkwellMark()
            .frame(width: 44, height: 44 * 952 / 400)
            .foregroundStyle(.primary)
    }
    .padding()
}

#Preview("Dark") {
    VStack(spacing: 24) {
        InkwellMark()
            .frame(width: 44, height: 44 * 952 / 400)
            .foregroundStyle(.primary)
    }
    .padding()
    .preferredColorScheme(.dark)
}
