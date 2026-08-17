package uk.ewancroft.inkwell.data.model.common

import uk.ewancroft.inkwell.data.model.atproto.BasicTheme
import uk.ewancroft.inkwell.data.model.atproto.ColorValue
import uk.ewancroft.inkwell.data.model.atproto.DocumentPreferences
import uk.ewancroft.inkwell.data.model.atproto.LegacyPalette
import uk.ewancroft.inkwell.data.model.atproto.PublicationPreferences
import uk.ewancroft.inkwell.data.model.atproto.PublicationTheme
import uk.ewancroft.inkwell.data.model.atproto.RgbColor
import uk.ewancroft.inkwell.shared.model.BasicTheme as SharedBasicTheme
import uk.ewancroft.inkwell.shared.model.BlobRef as SharedBlobRef
import uk.ewancroft.inkwell.shared.model.ColorValue as SharedColorValue
import uk.ewancroft.inkwell.shared.model.DocumentPreferences as SharedDocumentPreferences
import uk.ewancroft.inkwell.shared.model.LegacyPalette as SharedLegacyPalette
import uk.ewancroft.inkwell.shared.model.PublicationPreferences as SharedPublicationPreferences
import uk.ewancroft.inkwell.shared.model.PublicationTheme as SharedPublicationTheme
import uk.ewancroft.inkwell.shared.model.RgbColor as SharedRgbColor
import uk.ewancroft.inkwell.shared.model.StrongRef as SharedStrongRef

// ── BlobRef ────────────────────────────────────────────────────────────────

fun BlobRef.toShared(): SharedBlobRef = SharedBlobRef(
    link = link,
    size = size,
    type = type,
    mimeType = mimeType
)

fun SharedBlobRef.toAndroid(): BlobRef = BlobRef(
    link = link,
    size = size,
    type = type,
    mimeType = mimeType
)

// ── StrongRef ──────────────────────────────────────────────────────────────

fun StrongRef.toShared(): SharedStrongRef = SharedStrongRef(
    uri = uri,
    cid = cid
)

fun SharedStrongRef.toAndroid(): StrongRef = StrongRef(
    uri = uri,
    cid = cid
)

// ── Theme Types ────────────────────────────────────────────────────────────

fun RgbColor.toShared(): SharedRgbColor = SharedRgbColor(
    type = type,
    r = r,
    g = g,
    b = b
)

fun SharedRgbColor.toAndroid(): RgbColor = RgbColor(
    type = type,
    r = r,
    g = g,
    b = b
)

fun ColorValue.toShared(): SharedColorValue = SharedColorValue(
    type = type,
    r = r,
    g = g,
    b = b,
    a = a
)

fun SharedColorValue.toAndroid(): ColorValue = ColorValue(
    type = type,
    r = r,
    g = g,
    b = b,
    a = a
)

fun LegacyPalette.toShared(): SharedLegacyPalette = SharedLegacyPalette(
    background = background,
    text = text,
    accent = accent,
    link = link,
    surfaceHover = surfaceHover
)

fun SharedLegacyPalette.toAndroid(): LegacyPalette = LegacyPalette(
    background = background,
    text = text,
    accent = accent,
    link = link,
    surfaceHover = surfaceHover
)

fun BasicTheme.toShared(): SharedBasicTheme = SharedBasicTheme(
    type = type,
    background = background.toShared(),
    foreground = foreground.toShared(),
    accent = accent.toShared(),
    accentForeground = accentForeground.toShared()
)

fun SharedBasicTheme.toAndroid(): BasicTheme = BasicTheme(
    type = type,
    background = background.toAndroid(),
    foreground = foreground.toAndroid(),
    accent = accent.toAndroid(),
    accentForeground = accentForeground.toAndroid()
)

fun PublicationTheme.toShared(): SharedPublicationTheme = SharedPublicationTheme(
    type = type,
    backgroundColor = backgroundColor?.toShared(),
    pageBackground = pageBackground?.toShared(),
    primary = primary?.toShared(),
    accentBackground = accentBackground?.toShared(),
    accentText = accentText?.toShared(),
    pageWidth = pageWidth,
    showPageBackground = showPageBackground,
    headingFont = headingFont,
    bodyFont = bodyFont,
    font = font,
    light = light?.toShared(),
    dark = dark?.toShared()
)

fun SharedPublicationTheme.toAndroid(): PublicationTheme = PublicationTheme(
    type = type,
    backgroundColor = backgroundColor?.toAndroid(),
    pageBackground = pageBackground?.toAndroid(),
    primary = primary?.toAndroid(),
    accentBackground = accentBackground?.toAndroid(),
    accentText = accentText?.toAndroid(),
    pageWidth = pageWidth,
    showPageBackground = showPageBackground,
    headingFont = headingFont,
    bodyFont = bodyFont,
    font = font,
    light = light?.toAndroid(),
    dark = dark?.toAndroid()
)

// ── Preferences ────────────────────────────────────────────────────────────

fun PublicationPreferences.toShared(): SharedPublicationPreferences = SharedPublicationPreferences(
    showInDiscover = showInDiscover
)

fun SharedPublicationPreferences.toAndroid(): PublicationPreferences = PublicationPreferences(
    showInDiscover = showInDiscover
)

fun DocumentPreferences.toShared(): SharedDocumentPreferences = SharedDocumentPreferences(
    showComments = showComments,
    showMentions = showMentions,
    showRecommends = showRecommends,
    showPrevNext = showPrevNext,
    showInDiscover = showInDiscover
)

fun SharedDocumentPreferences.toAndroid(): DocumentPreferences = DocumentPreferences(
    showComments = showComments,
    showMentions = showMentions,
    showRecommends = showRecommends,
    showPrevNext = showPrevNext,
    showInDiscover = showInDiscover
)
