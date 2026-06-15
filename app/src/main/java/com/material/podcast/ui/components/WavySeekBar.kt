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
    val activeStrokePx = with(density) { 3.5.dp.toPx() }
    val inactiveStrokePx = with(density) { 2.dp.toPx() }
    val thumbR = with(density) { 7.dp.toPx() }
    val dotR = with(density) { 3.dp.toPx() }

    var dragFrac by remember { mutableFloatStateOf(fraction) }
    var lastSnappedFrac by remember { mutableFloatStateOf(-1f) }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .pointerInput(Unit) {
                detectTapGestures { off -> onScrubFinished((off.x / size.width).coerceIn(0f, 1f)) }
            }
            .pointerInput(momentFractions) {
                detectHorizontalDragGestures(
                    onDragStart = { off ->
                        lastSnappedFrac = -1f
                        onScrubStart()
                        dragFrac = (off.x / size.width).coerceIn(0f, 1f)
                        onScrub(dragFrac)
                    },
                    onHorizontalDrag = { change, _ ->
                        val raw = (change.position.x / size.width).coerceIn(0f, 1f)
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
        val thumbX = size.width * fraction.coerceIn(0f, 1f)

        // Active (played) portion
        if (thumbX > 0f) {
            drawLine(
                color = activeColor,
                start = Offset(0f, centerY),
                end = Offset(thumbX, centerY),
                strokeWidth = activeStrokePx,
                cap = StrokeCap.Round,
            )
        }
        // Inactive (remaining) portion — thinner and dimmer
        drawLine(
            color = inactiveColor,
            start = Offset(thumbX, centerY),
            end = Offset(size.width, centerY),
            strokeWidth = inactiveStrokePx,
            cap = StrokeCap.Round,
        )
        // Thumb
        drawCircle(color = thumbColor, radius = thumbR, center = Offset(thumbX, centerY))

        // Moment marker dots
        for (mf in momentFractions) {
            val mx = size.width * mf.coerceIn(0f, 1f)
            drawCircle(color = thumbColor, radius = dotR, center = Offset(mx, centerY - thumbR - dotR))
        }
    }
}
