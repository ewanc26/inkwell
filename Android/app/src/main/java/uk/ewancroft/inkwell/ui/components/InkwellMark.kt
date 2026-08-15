/**
 * InkwellMark.kt
 *
 * The Inkwell wordmark drawn as pure vector geometry — a serif "I" letterform
 * (top bar + vertical stroke + bottom bar) with a single ink drop beneath it.
 *
 * Mirrors iOS's InkwellMark.swift exactly: same 400×952 design space, same
 * coordinate values, same fixed brand green for the ink dot. The letter colour
 * follows LocalContentColor so it adapts to light/dark theme, exactly like
 * SwiftUI's `.foregroundStyle(.primary)`.
 *
 * Usage:
 * ```kotlin
 * InkwellMark(modifier = Modifier.height(48.dp))  // width is derived from aspect ratio
 * ```
 */
package uk.ewancroft.inkwell.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.tooling.preview.Preview

// Canonical brand green: #139500 — matches iOS InkwellMark.swift dot colour
// (Display P3: 0.07611, 0.58470, 0.00000), ic_launcher_foreground.xml, and
// the inkwell-website tokens.css. Fixed regardless of light/dark mode, just
// like the real app icon.
private val InkwellDotColor = Color(0xFF139500)

// Design space from iOS InkwellMark.swift — 400 wide, 952 tall.
private const val DESIGN_WIDTH = 400f
private const val DESIGN_HEIGHT = 952f

/**
 * The Inkwell wordmark as a Compose Canvas element.
 *
 * Constrain the height via [modifier] and the aspect ratio fills in the width
 * automatically:
 * ```kotlin
 * InkwellMark(modifier = Modifier.height(48.dp))
 * ```
 *
 * @param modifier Layout modifier. Usually just a height constraint.
 * @param color    Letter colour. Defaults to [LocalContentColor] so it adapts
 *                 to light/dark mode like any other text or icon.
 */
@Composable
fun InkwellMark(
    modifier: Modifier = Modifier,
    color: Color = LocalContentColor.current,
) {
    Canvas(
        modifier = modifier.aspectRatio(DESIGN_WIDTH / DESIGN_HEIGHT),
    ) {
        drawMark(letterColor = color)
    }
}

private fun DrawScope.drawMark(letterColor: Color) {
    val scale = size.width / DESIGN_WIDTH

    // Rounded rectangle helper — corner radius of 0 falls back to a plain rect.
    fun bar(x: Float, y: Float, w: Float, h: Float, r: Float = 0f) {
        if (r > 0f) {
            drawRoundRect(
                color = letterColor,
                topLeft = Offset(x * scale, y * scale),
                size = Size(w * scale, h * scale),
                cornerRadius = CornerRadius(r * scale),
            )
        } else {
            drawRect(
                color = letterColor,
                topLeft = Offset(x * scale, y * scale),
                size = Size(w * scale, h * scale),
            )
        }
    }

    // The letter "I": top cap, vertical stroke, bottom cap.
    bar(x = 40f, y = 40f, w = 320f, h = 80f, r = 16f)   // top bar
    bar(x = 125f, y = 120f, w = 150f, h = 640f)           // vertical stroke
    bar(x = 40f, y = 760f, w = 320f, h = 80f, r = 16f)   // bottom bar

    // The ink drop — fixed brand green, never shifts with colour mode.
    drawCircle(
        color = InkwellDotColor,
        radius = 32f * scale,
        center = Offset(200f * scale, 880f * scale),
    )
}

@Preview(showBackground = true, backgroundColor = 0xFFFAFAF5)
@Composable
private fun InkwellMarkPreviewLight() {
    InkwellMark(
        modifier = Modifier.aspectRatio(DESIGN_WIDTH / DESIGN_HEIGHT),
        color = Color(0xFF1A1A2E),
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF1C1C24)
@Composable
private fun InkwellMarkPreviewDark() {
    InkwellMark(
        modifier = Modifier.aspectRatio(DESIGN_WIDTH / DESIGN_HEIGHT),
        color = Color(0xFFE8E6F0),
    )
}
