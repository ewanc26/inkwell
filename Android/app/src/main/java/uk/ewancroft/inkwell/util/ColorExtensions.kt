/**
 * Colour conversion extensions from AT Protocol colour types to Compose Color.
 *
 * Note: the package declaration here is incorrect (com.example.inkwell.util
 * instead of uk.ewancroft.inkwell.util). This is a known artifact from the
 * initial template — fix if it causes build issues, but the import paths
 * in Theme.kt use the fully qualified name so it resolves correctly.
 */
package uk.ewancroft.inkwell.util

import androidx.compose.ui.graphics.Color
import uk.ewancroft.inkwell.data.model.atproto.ColorValue
import uk.ewancroft.inkwell.data.model.atproto.RgbColor

/**
 * Converts a Leaflet ColorValue to a Compose Color.
 * ColorValue.a is a percentage (0-100, defaults to 100 = fully opaque).
 */
fun ColorValue.toColor(): Color = Color(
    red = r / 255f,
    green = g / 255f,
    blue = b / 255f,
    alpha = (a ?: 100) / 100f
)

fun RgbColor.toColor(): Color = Color(
    red = r / 255f,
    green = g / 255f,
    blue = b / 255f,
)
