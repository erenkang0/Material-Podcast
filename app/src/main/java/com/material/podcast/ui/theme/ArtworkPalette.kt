package com.material.podcast.ui.theme

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult

/**
 * Pull a representative seed color out of a podcast cover. Returns an ARGB int, or null if
 * the image can't be decoded. Runs the network/decode through the app's shared Coil loader.
 */
suspend fun extractArtworkColor(context: Context, url: String): Int? {
    if (url.isBlank()) return null
    val request = ImageRequest.Builder(context)
        .data(url)
        .allowHardware(false) // Palette needs to read pixels off the bitmap
        .size(128)
        .build()
    val result = context.imageLoader.execute(request)
    val bitmap = (result as? SuccessResult)?.drawable
        ?.let { it as? BitmapDrawable }
        ?.bitmap
        ?: return null
    val palette = Palette.from(bitmap).generate()
    return palette.vibrantSwatch?.rgb
        ?: palette.dominantSwatch?.rgb
        ?: palette.mutedSwatch?.rgb
}

/** The trio of colors the Now Playing surface uses, derived from the cover seed. */
data class PlayerColors(val background: Color, val onBackground: Color, val accent: Color)

/**
 * Build the player surface colors from a [seed] color.
 * - Dark theme → a deep, calm version of the cover hue with white text.
 * - Light theme → a soft, low-saturation (never garish) tint of the hue with black text.
 */
fun playerColorsFromSeed(seed: Int, dark: Boolean): PlayerColors {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(seed, hsv)
    val hue = hsv[0]
    val sat = hsv[1]
    return if (dark) {
        val bg = android.graphics.Color.HSVToColor(
            floatArrayOf(hue, (sat * 0.6f).coerceIn(0.25f, 0.7f), 0.16f),
        )
        val accent = android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.55f, 0.85f))
        PlayerColors(Color(bg), Color.White, Color(accent))
    } else {
        val bg = android.graphics.Color.HSVToColor(
            floatArrayOf(hue, (sat * 0.35f).coerceIn(0.12f, 0.32f), 0.93f),
        )
        val accent = android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.65f, 0.45f))
        PlayerColors(Color(bg), Color.Black, Color(accent))
    }
}
