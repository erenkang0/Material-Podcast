package com.material.podcast.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

private const val SNAP_THRESHOLD = 0.018f  // 1.8% of total duration

@Composable
fun WavySeekBar(
    fraction: Float,
    playing: Boolean,
    onScrubStart: () -> Unit,
    onScrub: (Float) -> Unit,
    onScrubFinished: (Float) -> Unit,
    activeColor: Color,
    inactiveColor: Color,
    thumbColor: Color,
    modifier: Modifier = Modifier,
    momentFractions: List<Float> = emptyList(),
    heat: List<Float> = emptyList(),
    heatColor: Color = Color(0xFFFF7043),
) {
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    // Material 3 Expressive style: the played portion is an animated squiggle, the
    // remaining portion a flat track, with a solid round handle.
    val activeStrokePx = with(density) { 4.dp.toPx() }
    val inactiveStrokePx = with(density) { 4.dp.toPx() }
    val thumbR = with(density) { 9.dp.toPx() }
    val dotR = with(density) { 3.dp.toPx() }
    val waveLengthPx = with(density) { 16.dp.toPx() }
    val targetAmplitudePx = with(density) { 3.dp.toPx() }

    var dragFrac by remember { mutableFloatStateOf(fraction) }
    var lastSnappedFrac by remember { mutableFloatStateOf(-1f) }

    // Continuously scroll the wave phase while playing; freeze when paused.
    val waveTransition = rememberInfiniteTransition(label = "wave")
    val phase by waveTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "wavePhase",
    )
    // Flatten the squiggle when paused so it reads as a calm line.
    val amplitude by animateFloatAsState(
        targetValue = if (playing) targetAmplitudePx else 0f,
        animationSpec = tween(450),
        label = "waveAmp",
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .pointerInput(Unit) {
                // Map a pointer x to a track fraction, accounting for the thumb-radius inset.
                fun fracAt(x: Float): Float {
                    val inset = thumbR
                    val w = (size.width - 2 * inset).coerceAtLeast(1f)
                    return ((x - inset) / w).coerceIn(0f, 1f)
                }
                detectTapGestures { off -> onScrubFinished(fracAt(off.x)) }
            }
            .pointerInput(momentFractions) {
                fun fracAt(x: Float): Float {
                    val inset = thumbR
                    val w = (size.width - 2 * inset).coerceAtLeast(1f)
                    return ((x - inset) / w).coerceIn(0f, 1f)
                }
                detectHorizontalDragGestures(
                    onDragStart = { off ->
                        lastSnappedFrac = -1f
                        onScrubStart()
                        dragFrac = fracAt(off.x)
                        onScrub(dragFrac)
                    },
                    onHorizontalDrag = { change, _ ->
                        val raw = fracAt(change.position.x)
                        // Magnetic snap to nearby moment marker
                        val snapped = momentFractions.firstOrNull { abs(it - raw) < SNAP_THRESHOLD }
                        if (snapped != null) {
                            if (snapped != lastSnappedFrac) {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                lastSnappedFrac = snapped
                            }
                            dragFrac = snapped
                        } else {
                            lastSnappedFrac = -1f
                            dragFrac = raw
                        }
                        onScrub(dragFrac)
                    },
                    onDragEnd = { onScrubFinished(dragFrac) },
                    onDragCancel = { onScrubFinished(dragFrac) },
                )
            },
    ) {
        val centerY = size.height / 2f
        // Inset the track so the thumb stays within bounds, like a Material 3 Slider.
        val startX = thumbR
        val endX = size.width - thumbR
        val trackWidth = (endX - startX).coerceAtLeast(0f)
        val thumbX = startX + trackWidth * fraction.coerceIn(0f, 1f)
        // Small gap on either side of the thumb, matching the M3 Slider look.
        val gap = thumbR * 0.6f

        // Personal replay heatmap — a soft warm glow under the track, hotter where the user has
        // rewound to re-listen. Drawn first so the wave and thumb sit on top of it.
        if (heat.isNotEmpty()) {
            val maxGlow = with(density) { 13.dp.toPx() }
            for (i in heat.indices) {
                val h = heat[i]
                if (h <= 0.04f) continue
                val bx = startX + trackWidth * ((i + 0.5f) / heat.size)
                // Two stacked circles: a wide faint halo plus a tighter brighter core.
                drawCircle(
                    color = heatColor,
                    radius = maxGlow * (0.5f + 0.5f * h),
                    center = Offset(bx, centerY),
                    alpha = 0.10f + 0.22f * h,
                )
                drawCircle(
                    color = heatColor,
                    radius = (maxGlow * 0.5f) * (0.4f + 0.6f * h),
                    center = Offset(bx, centerY),
                    alpha = 0.18f + 0.30f * h,
                )
            }
        }

        // Active (played) portion — an animated squiggle (M3 Expressive).
        val activeEnd = thumbX - gap
        if (activeEnd > startX) {
            if (amplitude < 0.5f) {
                // Effectively flat (paused) — draw a clean line.
                drawLine(
                    color = activeColor,
                    start = Offset(startX, centerY),
                    end = Offset(activeEnd, centerY),
                    strokeWidth = activeStrokePx,
                    cap = StrokeCap.Round,
                )
            } else {
                val path = Path().apply {
                    moveTo(startX, centerY)
                    var x = startX
                    val step = 2f
                    while (x <= activeEnd) {
                        val t = (x - startX) / waveLengthPx * (2f * PI).toFloat() + phase
                        lineTo(x, centerY + amplitude * sin(t))
                        x += step
                    }
                    lineTo(activeEnd, centerY + amplitude * sin((activeEnd - startX) / waveLengthPx * (2f * PI).toFloat() + phase))
                }
                drawPath(
                    path = path,
                    color = activeColor,
                    style = Stroke(width = activeStrokePx, cap = StrokeCap.Round),
                )
            }
        }
        // Inactive (remaining) portion
        if (endX > thumbX + gap) {
            drawLine(
                color = inactiveColor,
                start = Offset(thumbX + gap, centerY),
                end = Offset(endX, centerY),
                strokeWidth = inactiveStrokePx,
                cap = StrokeCap.Round,
            )
        }
        // Thumb — solid round, like the M3 Slider handle
        drawCircle(color = thumbColor, radius = thumbR, center = Offset(thumbX, centerY))

        // Moment marker dots
        for (mf in momentFractions) {
            val mx = startX + trackWidth * mf.coerceIn(0f, 1f)
            drawCircle(color = thumbColor, radius = dotR, center = Offset(mx, centerY - thumbR - dotR))
        }
    }
}
