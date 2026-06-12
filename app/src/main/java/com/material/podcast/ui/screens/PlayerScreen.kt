package com.material.podcast.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.material.podcast.data.MockData
import com.material.podcast.ui.theme.*
import kotlin.math.sin

@Composable
fun PlayerScreen(onNavigateBack: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val podcast = MockData.featuredPodcasts.first()
    val primaryColor = Color(podcast.colorHex)
    val secondaryColor = Pink60

    var isPlaying by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0.3f) }
    var isLiked by remember { mutableStateOf(false) }

    // Pulsing glow animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulseScale"
    )

    // Waveform bar heights
    val barCount = 42
    val barAnimations = (0 until barCount).map { i ->
        infiniteTransition.animateFloat(
            initialValue = 0.15f,
            targetValue = if (isPlaying) 1f else 0.2f,
            animationSpec = infiniteRepeatable(
                tween(
                    durationMillis = if (isPlaying) (600 + (i * 37) % 700) else 1200,
                    easing = FastOutSlowInEasing
                ),
                RepeatMode.Reverse
            ),
            label = "bar$i"
        )
    }

    var swipeOffset by remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(primaryColor.copy(0.25f), Color(0xFF0D0714), Color(0xFF0D0714)))
            )
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = { if (swipeOffset > 120f) onNavigateBack(); swipeOffset = 0f },
                    onVerticalDrag = { _, delta -> swipeOffset = (swipeOffset + delta).coerceAtLeast(0f) }
                )
            }
    ) {
        // Blurred background orbs
        Box(
            modifier = Modifier
                .size(300.dp)
                .offset(x = (-60).dp, y = (-40).dp)
                .blur(80.dp)
                .background(primaryColor.copy(0.3f), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(250.dp)
                .align(Alignment.TopEnd)
                .offset(x = 60.dp, y = 20.dp)
                .blur(80.dp)
                .background(secondaryColor.copy(0.2f), CircleShape)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .graphicsLayer { translationY = swipeOffset * 0.3f },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Rounded.KeyboardArrowDown, null, tint = OnSurface, modifier = Modifier.size(32.dp))
                }
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("NOW PLAYING", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                    Text(podcast.title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = OnSurface)
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = {}) {
                    Icon(Icons.Rounded.MoreVert, null, tint = OnSurface)
                }
            }

            Spacer(Modifier.height(16.dp))

            // Album art
            Box(
                modifier = Modifier
                    .size(300.dp)
                    .graphicsLayer { scaleX = if (isPlaying) pulseScale else 1f; scaleY = if (isPlaying) pulseScale else 1f }
                    .shadow(32.dp, RoundedCornerShape(28.dp), ambientColor = primaryColor, spotColor = primaryColor)
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.linearGradient(listOf(primaryColor, secondaryColor, Coral60))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Podcasts, null, tint = Color.White.copy(0.9f), modifier = Modifier.size(100.dp))
                // Shimmer overlay
                Box(
                    modifier = Modifier.fillMaxSize()
                        .background(Brush.linearGradient(listOf(Color.White.copy(0.08f), Color.Transparent, Color.White.copy(0.05f))))
                )
            }

            Spacer(Modifier.height(24.dp))

            // Track info
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "The Fabric of Time",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = OnSurface
                    )
                    Text(podcast.author, style = MaterialTheme.typography.bodyMedium, color = OnSurfaceVariant)
                }
                val likeScale by animateFloatAsState(
                    targetValue = if (isLiked) 1.3f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMedium),
                    label = "likeScale"
                )
                IconButton(
                    onClick = {
                        isLiked = !isLiked
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    modifier = Modifier.graphicsLayer { scaleX = likeScale; scaleY = likeScale }
                ) {
                    Icon(
                        if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        null,
                        tint = if (isLiked) Pink60 else OnSurfaceVariant,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Glassmorphism controls panel
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color(0x88251D35))
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(listOf(GlassStroke, Color.Transparent, GlassStroke)),
                        shape = RoundedCornerShape(32.dp)
                    )
                    .padding(20.dp)
            ) {
                Column {
                    // Waveform visualizer
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                    ) {
                        val barWidth = (size.width / barCount) * 0.6f
                        val spacing = size.width / barCount
                        val playedBars = (progress * barCount).toInt()
                        val maxBarHeight = size.height * 0.9f

                        barAnimations.forEachIndexed { i, anim ->
                            val barHeight = maxBarHeight * anim.value.coerceIn(0.08f, 1f)
                            val x = i * spacing + spacing / 2f
                            val isPlayed = i < playedBars
                            val color = if (isPlayed)
                                lerp(primaryColor, secondaryColor, i.toFloat() / barCount)
                            else
                                OnSurfaceVariant.copy(alpha = 0.3f)

                            drawRoundRect(
                                color = color,
                                topLeft = Offset(x - barWidth / 2f, (size.height - barHeight) / 2f),
                                size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f)
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Progress slider
                    Slider(
                        value = progress,
                        onValueChange = { progress = it },
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = primaryColor,
                            inactiveTrackColor = SurfaceContainerHigh
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("14:42", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                        Text("45:22", style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariant)
                    }

                    Spacer(Modifier.height(16.dp))

                    // Controls row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) }) {
                            Icon(Icons.Rounded.SkipPrevious, null, tint = OnSurface, modifier = Modifier.size(32.dp))
                        }
                        IconButton(
                            onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Rounded.Replay10, null, tint = OnSurface, modifier = Modifier.size(28.dp))
                        }

                        // Play/Pause FAB
                        val playInteraction = remember { MutableInteractionSource() }
                        val playPressed by playInteraction.collectIsPressedAsState()
                        val playScale by animateFloatAsState(
                            targetValue = if (playPressed) 0.88f else 1f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                            label = "playScale"
                        )
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .graphicsLayer { scaleX = playScale; scaleY = playScale }
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(primaryColor, secondaryColor)))
                                .clickable(interactionSource = playInteraction, indication = null) {
                                    isPlaying = !isPlaying
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                null,
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        IconButton(
                            onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(Icons.Rounded.Forward10, null, tint = OnSurface, modifier = Modifier.size(28.dp))
                        }
                        IconButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) }) {
                            Icon(Icons.Rounded.SkipNext, null, tint = OnSurface, modifier = Modifier.size(32.dp))
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Bottom actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf(
                            Icons.Rounded.Download to "Download",
                            Icons.Rounded.Share to "Share",
                            Icons.Rounded.QueueMusic to "Queue",
                            Icons.Rounded.Speed to "Speed",
                            Icons.Rounded.Timer to "Sleep"
                        ).forEach { (icon, label) ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                IconButton(onClick = {}) {
                                    Icon(icon, null, tint = OnSurfaceVariant, modifier = Modifier.size(22.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
