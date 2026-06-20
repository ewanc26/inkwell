//
//  SiteStandardTheme.swift
//  Inkwell
//
//  Created by Ewan Croft on 20/06/2026.
//

import Foundation
import ATProtoKit

extension SiteStandardLexicon.Theme {

    /// A simplified theme definition for a publication.
    ///
    /// Lets a publication keep a consistent visual identity (background/foreground/accent
    /// colours) across different reading apps, rather than each one falling back to its own
    /// default styling.
    ///
    /// - SeeAlso: This is based on the [`site.standard.theme.basic`][docs] lexicon.
    ///
    /// [docs]: https://standard.site/docs/lexicons/theme/
    nonisolated public struct BasicDefinition: Sendable, Codable, Equatable, Hashable {

        /// The identifier of the lexicon.
        ///
        /// - Warning: The value must not change.
        public static let type: String = "site.standard.theme.basic"

        /// The colour used for content backgrounds.
        public let background: RGBColor

        /// The colour used for content text.
        public let foreground: RGBColor

        /// The colour used for links and button backgrounds.
        public let accent: RGBColor

        /// The colour used for button text.
        public let accentForeground: RGBColor

        public init(background: RGBColor, foreground: RGBColor, accent: RGBColor, accentForeground: RGBColor) {
            self.background = background
            self.foreground = foreground
            self.accent = accent
            self.accentForeground = accentForeground
        }

        public init(from decoder: any Decoder) throws {
            let container = try decoder.container(keyedBy: CodingKeys.self)

            self.background = try container.decode(RGBColor.self, forKey: .background)
            self.foreground = try container.decode(RGBColor.self, forKey: .foreground)
            self.accent = try container.decode(RGBColor.self, forKey: .accent)
            self.accentForeground = try container.decode(RGBColor.self, forKey: .accentForeground)
        }

        public func encode(to encoder: any Encoder) throws {
            var container = encoder.container(keyedBy: CodingKeys.self)

            try container.encode(BasicDefinition.type, forKey: .type)
            try container.encode(self.background, forKey: .background)
            try container.encode(self.foreground, forKey: .foreground)
            try container.encode(self.accent, forKey: .accent)
            try container.encode(self.accentForeground, forKey: .accentForeground)
        }

        enum CodingKeys: String, CodingKey {
            case type = "$type"
            case background
            case foreground
            case accent
            case accentForeground
        }
    }

    /// An opaque RGB colour, used by ``BasicDefinition``.
    ///
    /// - SeeAlso: This is based on the [`site.standard.theme.color#rgb`][docs] definition.
    ///
    /// [docs]: https://standard.site/docs/lexicons/theme/
    nonisolated public struct RGBColor: Sendable, Codable, Equatable, Hashable {

        /// The identifier of the lexicon.
        ///
        /// - Warning: The value must not change.
        public static let type: String = "site.standard.theme.color#rgb"

        /// The red channel value. Range: 0-255.
        public let r: Int

        /// The green channel value. Range: 0-255.
        public let g: Int

        /// The blue channel value. Range: 0-255.
        public let b: Int

        public init(r: Int, g: Int, b: Int) {
            self.r = r
            self.g = g
            self.b = b
        }

        public init(from decoder: any Decoder) throws {
            let container = try decoder.container(keyedBy: CodingKeys.self)

            self.r = try container.decode(Int.self, forKey: .r)
            self.g = try container.decode(Int.self, forKey: .g)
            self.b = try container.decode(Int.self, forKey: .b)
        }

        public func encode(to encoder: any Encoder) throws {
            var container = encoder.container(keyedBy: CodingKeys.self)

            try container.encode(RGBColor.type, forKey: .type)
            try container.encode(self.r, forKey: .r)
            try container.encode(self.g, forKey: .g)
            try container.encode(self.b, forKey: .b)
        }

        enum CodingKeys: String, CodingKey {
            case type = "$type"
            case r
            case g
            case b
        }
    }

    /// A translucent RGBA colour.
    ///
    /// Defined by the `site.standard.theme.color` lexicon alongside ``RGBColor``, though
    /// ``BasicDefinition``'s four colour properties are all currently typed as ``RGBColor``
    /// rather than this per the docs — included here for completeness/forward compatibility.
    ///
    /// - SeeAlso: This is based on the [`site.standard.theme.color#rgba`][docs] definition.
    ///
    /// [docs]: https://standard.site/docs/lexicons/theme/
    nonisolated public struct RGBAColor: Sendable, Codable, Equatable, Hashable {

        /// The identifier of the lexicon.
        ///
        /// - Warning: The value must not change.
        public static let type: String = "site.standard.theme.color#rgba"

        /// The red channel value. Range: 0-255.
        public let r: Int

        /// The green channel value. Range: 0-255.
        public let g: Int

        /// The blue channel value. Range: 0-255.
        public let b: Int

        /// The alpha (opacity) value, where 0 is transparent and 100 is opaque. Range: 0-100.
        public let a: Int

        public init(r: Int, g: Int, b: Int, a: Int) {
            self.r = r
            self.g = g
            self.b = b
            self.a = a
        }

        public init(from decoder: any Decoder) throws {
            let container = try decoder.container(keyedBy: CodingKeys.self)

            self.r = try container.decode(Int.self, forKey: .r)
            self.g = try container.decode(Int.self, forKey: .g)
            self.b = try container.decode(Int.self, forKey: .b)
            self.a = try container.decode(Int.self, forKey: .a)
        }

        public func encode(to encoder: any Encoder) throws {
            var container = encoder.container(keyedBy: CodingKeys.self)

            try container.encode(RGBAColor.type, forKey: .type)
            try container.encode(self.r, forKey: .r)
            try container.encode(self.g, forKey: .g)
            try container.encode(self.b, forKey: .b)
            try container.encode(self.a, forKey: .a)
        }

        enum CodingKeys: String, CodingKey {
            case type = "$type"
            case r
            case g
            case b
            case a
        }
    }
}

#if canImport(SwiftUI)
import SwiftUI

extension SiteStandardLexicon.Theme.RGBColor {

    /// A SwiftUI `Color` representation of this value, for direct use when rendering a
    /// publication's theme.
    public var color: Color {
        Color(red: Double(r) / 255, green: Double(g) / 255, blue: Double(b) / 255)
    }
}

extension SiteStandardLexicon.Theme.RGBAColor {

    /// A SwiftUI `Color` representation of this value, for direct use when rendering a
    /// publication's theme.
    public var color: Color {
        Color(red: Double(r) / 255, green: Double(g) / 255, blue: Double(b) / 255, opacity: Double(a) / 100)
    }
}
#endif
