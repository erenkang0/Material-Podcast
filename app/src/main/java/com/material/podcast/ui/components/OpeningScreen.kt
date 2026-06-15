package com.material.podcast.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A short, professional cold-start animation that brings the app's equalizer logo to life:
 * the branded tile springs in, its five bars rise in a stagger, the "Echoes" wordmark fades
 * up, then the whole thing fades out. Starts on the same dark background as the system splash
 * for a seamless hand-off.
 */
@Composable
fun OpeningScreen(onDone: () -> Unit) {
    val containerAlpha = remember { Animatable(0f) }
    val tileScale = remember { Animatable(0.82f) }
    val wordmarkAlpha = remember { Animatable(0f) }
    // Relative bar heights, echoing the launcher icon's wave.
    val barTargets = listOf(0.40f, 0.66f, 1f, 0.56f, 0.46f)
    val bars = remember { barTargets.map { Animatable(0.18f) } }

    LaunchedEffect(Unit) {
        launch { containerAlpha.animateTo(1f, tween(260)) }
        launch { tileScale.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow)) }
        bars.forEachIndexed { i, bar ->
            launch {
                delay(80L + i * 70L)
                bar.animateTo(barTargets[i], spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow))
            }
        }
        launch { delay(260); wordmarkAlpha.animateTo(1f, tween(360)) }
        delay(1150)
        containerAlpha.animateTo(0f, tween(340))
        onDone()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121318))
            .graphicsLayer { alpha = containerAlpha.value },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(104.dp)
                    .graphicsLayer { scaleX = tileScale.value; scaleY = tileScale.value }
                    .clip(RoundedCornerShape(30.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF8B5CF6), Color(0xFF5B53E8), Color(0xFF4338CA)),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    modifier = Modifier.height(50.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    bars.forEach { bar ->
                        Box(
                            modifier = Modifier
                                .width(7.dp)
                                .fillMaxHeight(bar.value)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.White),
                        )
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Text(
                text = "Echoes",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                modifier = Modifier.graphicsLayer { alpha = wordmarkAlpha.value },
            )
        }
    }
}
