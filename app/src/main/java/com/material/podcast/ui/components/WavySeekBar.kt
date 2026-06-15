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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

/**
 * A Material-3-expressive style seek bar: the played portion is a sine wave that
 * animates while playing (flattening when paused); the remaining portion is a straight
 * line. Drag or tap anywhere to scrub.
 */
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
) {
    val transition = rememberInfiniteTransition(label = "wave")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart),
        label = "phase",
    )
    val amplitude by animateFloatAsState(
        targetValue = if (playing) 1f else 0f,
        animationSpec = tween(400),
        label = "amp",
    )

    val density = LocalDensity.current
    val waveAmpPx = with(density) { 5.dp.toPx() }
    val strokePx = with(density) { 3.5.dp.toPx() }
    val thumbR = with(density) { 7.dp.toPx() }

    var dragFrac by remember { mutableFloatStateOf(fraction) }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .pointerInput(Unit) {
                detectTapGestures { off -> onScrubFinished((off.x / size.width).coerceIn(0f, 1f)) }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { off ->
                        onScrubStart()
                        dragFrac = (off.x / size.width).coerceIn(0f, 1f)
                        onScrub(dragFrac)
                    },
                    onHorizontalDrag = { change, _ ->
                        dragFrac = (change.position.x / size.width).coerceIn(0f, 1f)
                        onScrub(dragFrac)
                    },
                    onDragEnd = { onScrubFinished(dragFrac) },
                    onDragCancel = { onScrubFinished(dragFrac) },
                )
            },
    ) {
        val centerY = size.height / 2f
        val thumbX = size.width * fraction.coerceIn(0f, 1f)

        if (thumbX > 1f) {
            val path = Path().apply {
                moveTo(0f, centerY)
                var x = 0f
                while (x <= thumbX) {
                    val y = centerY + amplitude * waveAmpPx * sin(((x / 26f) + phase).toDouble()).toFloat()
                    lineTo(x, y)
                    x += 3f
                }
            }
            drawPath(path, color = activeColor, style = Stroke(width = strokePx, cap = StrokeCap.Round))
        }
        drawLine(
            color = inactiveColor,
            start = Offset(thumbX, centerY),
            end = Offset(size.width, centerY),
            strokeWidth = strokePx,
            cap = StrokeCap.Round,
        )
        drawCircle(color = thumbColor, radius = thumbR, center = Offset(thumbX, centerY))
    }
}
