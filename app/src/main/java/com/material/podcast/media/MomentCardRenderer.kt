package com.material.podcast.media

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.material.podcast.nfc.MomentShareController
import java.io.File
import java.io.FileOutputStream
import kotlin.math.sin
import kotlin.random.Random

/**
 * Renders a shareable square "Moment Card" — a pulled quote from a podcast moment over an
 * artwork-derived gradient, with a waveform motif, timestamp, and wordmark. The shared message
 * also carries an `echoes://moment?…` deep link so a tap drops the recipient straight into
 * playback at that exact second (handled by [MomentShareController]).
 *
 * Pure-Canvas rendering (no network), so generation is instant and never fails on a bad image.
 */
object MomentCardRenderer {

    private const val SIZE = 1080

    /** Render the card to an in-memory bitmap (used for the live preview). */
    fun renderBitmap(
        payload: MomentShareController.MomentPayload,
        seedColor: Int,
    ): Bitmap? {
        return try {
            val bitmap = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawBackground(canvas, seedColor)
            drawWaveform(canvas, seedColor, payload.guid)
            drawText(canvas, payload)
            drawWordmark(canvas)
            bitmap
        } catch (_: Exception) {
            null
        }
    }

    /** Persist a rendered [bitmap] to a shareable cache file. */
    fun save(context: Context, bitmap: Bitmap): File? {
        return try {
            val dir = File(context.cacheDir, "cards").apply { mkdirs() }
            val file = File(dir, "moment_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            file
        } catch (_: Exception) {
            null
        }
    }

    /** Share the rendered [file] together with the playable deep link. */
    fun share(context: Context, file: File, payload: MomentShareController.MomentPayload) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val link = MomentShareController.buildUri(payload)
        val caption = buildString {
            append(payload.podcastTitle.ifBlank { payload.title })
            append("\n")
            append(link)
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, caption)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Anı kartını paylaş"))
    }

    // ---- Drawing helpers ---------------------------------------------------

    private fun drawBackground(canvas: Canvas, seedColor: Int) {
        val seed = if (seedColor != 0) seedColor else Color.parseColor("#4C5BD4")
        // Deep, rich diagonal gradient: a dark-shifted seed → near-black.
        val top = shift(seed, 0.55f)
        val bottom = shift(seed, 0.16f)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, SIZE.toFloat(), SIZE.toFloat(),
                top, bottom, Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, 0f, SIZE.toFloat(), SIZE.toFloat(), paint)

        // Soft radial vignette for depth.
        val glow = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = android.graphics.RadialGradient(
                SIZE * 0.3f, SIZE * 0.28f, SIZE * 0.7f,
                withAlpha(shift(seed, 0.9f), 70), Color.TRANSPARENT, Shader.TileMode.CLAMP,
            )
        }
        canvas.drawRect(0f, 0f, SIZE.toFloat(), SIZE.toFloat(), glow)
    }

    private fun drawWaveform(canvas: Canvas, seedColor: Int, seedKey: String) {
        val accent = withAlpha(if (seedColor != 0) shift(seedColor, 1.25f) else Color.WHITE, 90)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accent
            style = Paint.Style.STROKE
            strokeWidth = 5f
            strokeCap = Paint.Cap.ROUND
        }
        // A deterministic pseudo-waveform so the same moment always looks the same.
        val rnd = Random(seedKey.hashCode().toLong())
        val baseY = SIZE * 0.40f
        val bars = 56
        val gap = SIZE * 0.78f / bars
        val startX = SIZE * 0.11f
        for (i in 0 until bars) {
            val env = sin((i.toFloat() / bars) * Math.PI).toFloat()
            val h = (28f + rnd.nextFloat() * 150f) * (0.35f + env)
            val x = startX + i * gap
            canvas.drawLine(x, baseY - h / 2f, x, baseY + h / 2f, paint)
        }
    }

    private fun drawText(canvas: Canvas, payload: MomentShareController.MomentPayload) {
        val margin = SIZE * 0.11f
        val width = (SIZE - 2 * margin).toInt()

        // The pulled quote (falls back to the episode title when no transcript line is available).
        val quoteRaw = payload.quote.ifBlank { payload.title }.trim()
        val quote = if (quoteRaw.length > 220) quoteRaw.take(217) + "…" else quoteRaw
        val quotePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            textSize = if (quote.length > 120) 58f else 72f
        }
        val layout = StaticLayout.Builder
            .obtain("“$quote”", 0, quote.length + 2, quotePaint, width)
            .setLineSpacing(8f, 1f)
            .build()
        canvas.save()
        canvas.translate(margin, SIZE * 0.50f)
        layout.draw(canvas)
        canvas.restore()

        // Podcast + episode + timestamp footer block.
        val secs = (payload.startMs / 1000).toInt()
        val ts = "%d:%02d".format(secs / 60, secs % 60)
        val metaPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = withAlpha(Color.WHITE, 220)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            textSize = 40f
        }
        val title = payload.podcastTitle.ifBlank { payload.title }
        val titleClamped = if (title.length > 40) title.take(39) + "…" else title
        canvas.drawText(titleClamped, margin, SIZE * 0.86f, metaPaint)

        val subPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = withAlpha(Color.WHITE, 160)
            typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            textSize = 34f
        }
        canvas.drawText("$ts itibarıyla", margin, SIZE * 0.86f + 50f, subPaint)
    }

    private fun drawWordmark(canvas: Canvas) {
        val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = withAlpha(Color.WHITE, 180)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            textSize = 36f
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("Echoes", SIZE - SIZE * 0.11f, SIZE * 0.135f, paint)
    }

    // ---- Color utilities ---------------------------------------------------

    /** Scale a color's brightness by [factor] (in HSV value space), keeping hue/saturation. */
    private fun shift(color: Int, factor: Float): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[2] = (hsv[2] * factor).coerceIn(0f, 1f)
        return Color.HSVToColor(hsv)
    }

    private fun withAlpha(color: Int, alpha: Int): Int =
        Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
}
