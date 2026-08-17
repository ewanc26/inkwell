//
//  SharedModelMappers.swift
//  Inkwell
//
//  Bridges between iOS native model types and the neutral shared KMP model types.
//  Networking and serialization remain native; only the data shapes are shared.
//

import Foundation
import ATProtoKit
import InkwellShared

// ── BlobRef ────────────────────────────────────────────────────────────────

extension ComAtprotoLexicon.Repository.UploadBlobOutput {
    func toShared() -> BlobRef {
        BlobRef(
            link: self.reference.link,
            size: Int32(self.size),
            type: self.type ?? "blob",
            mimeType: self.mimeType
        )
    }
}

extension BlobRef {
    func toiOS() -> ComAtprotoLexicon.Repository.UploadBlobOutput {
        ComAtprotoLexicon.Repository.UploadBlobOutput(
            type: self.type,
            reference: ComAtprotoLexicon.Repository.BlobReference(link: self.link),
            mimeType: self.mimeType ?? "",
            size: Int(self.size)
        )
    }
}

// ── StrongRef ──────────────────────────────────────────────────────────────

extension ComAtprotoLexicon.Repository.StrongReference {
    func toShared() -> StrongRef {
        StrongRef(
            uri: self.recordURI,
            cid: self.recordCID
        )
    }
}

extension StrongRef {
    func toiOS() -> ComAtprotoLexicon.Repository.StrongReference {
        ComAtprotoLexicon.Repository.StrongReference(
            recordURI: self.uri,
            cidHash: self.cid ?? ""
        )
    }
}

// ── Theme Types ────────────────────────────────────────────────────────────

extension SiteStandardLexicon.Theme.RGBColor {
    func toShared() -> RgbColor {
        RgbColor(
            type: Self.type,
            r: Int32(self.r),
            g: Int32(self.g),
            b: Int32(self.b)
        )
    }
}

extension RgbColor {
    func toiOS() -> SiteStandardLexicon.Theme.RGBColor {
        SiteStandardLexicon.Theme.RGBColor(
            r: Int(self.r),
            g: Int(self.g),
            b: Int(self.b)
        )
    }
}

extension SiteStandardLexicon.Theme.RGBAColor {
    func toShared() -> RgbaColor {
        RgbaColor(
            type: Self.type,
            r: Int32(self.r),
            g: Int32(self.g),
            b: Int32(self.b),
            a: Int32(self.a)
        )
    }
}

extension RgbaColor {
    func toiOS() -> SiteStandardLexicon.Theme.RGBAColor {
        SiteStandardLexicon.Theme.RGBAColor(
            r: Int(self.r),
            g: Int(self.g),
            b: Int(self.b),
            a: Int(self.a)
        )
    }
}

extension SiteStandardLexicon.Theme.ColorValue {
    func toShared() -> ColorValue {
        ColorValue(
            type: self.type ?? "",
            r: Int32(self.r),
            g: Int32(self.g),
            b: Int32(self.b),
            a: self.a.map { KotlinInt(value: Int32($0)) }
        )
    }
}

extension ColorValue {
    func toiOS() -> SiteStandardLexicon.Theme.ColorValue {
        SiteStandardLexicon.Theme.ColorValue(
            type: self.type,
            r: Int(self.r),
            g: Int(self.g),
            b: Int(self.b),
            a: self.a.map { Int($0.intValue) }
        )
    }
}

extension SiteStandardLexicon.Theme.LegacyPalette {
    func toShared() -> LegacyPalette {
        LegacyPalette(
            background: self.background,
            text: self.text,
            accent: self.accent,
            link: self.link,
            surfaceHover: self.surfaceHover
        )
    }
}

extension LegacyPalette {
    func toiOS() -> SiteStandardLexicon.Theme.LegacyPalette {
        SiteStandardLexicon.Theme.LegacyPalette(
            background: self.background,
            text: self.text,
            accent: self.accent,
            link: self.link,
            surfaceHover: self.surfaceHover
        )
    }
}

extension SiteStandardLexicon.Theme.BasicDefinition {
    func toShared() -> BasicTheme {
        BasicTheme(
            type: Self.type,
            background: self.background.toShared(),
            foreground: self.foreground.toShared(),
            accent: self.accent.toShared(),
            accentForeground: self.accentForeground.toShared()
        )
    }
}

extension BasicTheme {
    func toiOS() -> SiteStandardLexicon.Theme.BasicDefinition {
        SiteStandardLexicon.Theme.BasicDefinition(
            background: self.background.toiOS(),
            foreground: self.foreground.toiOS(),
            accent: self.accent.toiOS(),
            accentForeground: self.accentForeground.toiOS()
        )
    }
}

extension SiteStandardLexicon.Theme.PublicationTheme {
    func toShared() -> PublicationTheme {
        PublicationTheme(
            type: self.type ?? "",
            backgroundColor: self.backgroundColor?.toShared(),
            pageBackground: self.pageBackground?.toShared(),
            primary: self.primary?.toShared(),
            accentBackground: self.accentBackground?.toShared(),
            accentText: self.accentText?.toShared(),
            pageWidth: self.pageWidth.map { KotlinInt(value: Int32($0)) },
            showPageBackground: self.showPageBackground.map { KotlinBoolean(value: $0) },
            headingFont: self.headingFont,
            bodyFont: self.bodyFont,
            font: self.font,
            light: self.light?.toShared(),
            dark: self.dark?.toShared()
        )
    }
}

extension PublicationTheme {
    func toiOS() -> SiteStandardLexicon.Theme.PublicationTheme {
        SiteStandardLexicon.Theme.PublicationTheme(
            type: self.type,
            backgroundColor: self.backgroundColor?.toiOS(),
            pageBackground: self.pageBackground?.toiOS(),
            primary: self.primary?.toiOS(),
            accentBackground: self.accentBackground?.toiOS(),
            accentText: self.accentText?.toiOS(),
            pageWidth: self.pageWidth.map { Int($0.intValue) },
            showPageBackground: self.showPageBackground.map { $0.boolValue },
            headingFont: self.headingFont,
            bodyFont: self.bodyFont,
            font: self.font,
            light: self.light?.toiOS(),
            dark: self.dark?.toiOS()
        )
    }
}
