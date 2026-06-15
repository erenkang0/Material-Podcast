package com.material.podcast.ui.components

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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import kotlin.math.abs

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
) {
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    // Styled to match the Material 3 Slider used in the speed control:
    // uniform 4dp track, rounded caps, and a solid round thumb.
    val activeStrokePx = with(density) { 4.dp.toPx() }
    val inactiveStrokePx = with(density) { 4.dp.toPx() }
    val thumbR = with(density) { 10.dp.toPx() }
    val dotR = with(density) { 3.dp.toPx() }

    var dragFrac by remember { mutableFloatStateOf(fraction) }
    var lastSnappedFrac by remember { mutableFloatStateOf(-1f) }

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

        // Active (played) portion
        if (thumbX - gap > startX) {
            drawLine(
                color = activeColor,
                start = Offset(startX, centerY),
                end = Offset(thumbX - gap, centerY),
                strokeWidth = activeStrokePx,
                cap = StrokeCap.Round,
            )
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
