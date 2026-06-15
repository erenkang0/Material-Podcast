@file:OptIn(ExperimentalMaterial3Api::class)

package com.material.podcast.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.material.podcast.data.store.SettingsStore
import com.material.podcast.media.PlaybackService
import com.material.podcast.ui.LocalPlayer
import com.material.podcast.ui.components.EqualizerBars
import kotlin.math.roundToInt

// Band gain curves per preset (low → high), normalized to roughly -1f..1f for display.
private val previewCurves = mapOf(
    0 to listOf(0f, 0f, 0f, 0f, 0f),
    1 to listOf(-0.3f, 0.3f, 0.7f, 0.45f, -0.15f),
    2 to listOf(1f, 0.55f, 0f, -0.15f, -0.3f),
    3 to listOf(-0.3f, -0.15f, 0f, 0.55f, 1f),
)
private val bandLabels = listOf("60Hz", "230Hz", "910Hz", "3.6kHz", "14kHz")

@Composable
fun EqualizerScreen(onBack: () -> Unit) {
    val player = LocalPlayer.current
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val selected = player.eqPreset

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ekolayzer", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Geri")
                    }
                },
            )
        },
    ) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Ses tonunu içeriğe göre ayarla. Değişiklik anında uygulanır ve kaydedilir.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))

            // ── Visualizer title row ──────────────────────────────────────────
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Ekolayzer",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Box(modifier = Modifier.height(20.dp).width(36.dp)) {
                    EqualizerBars(
                        active = selected != 0,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))

            ElevatedCard(Modifier.fillMaxWidth()) {
                BandVisualizer(
                    curve = previewCurves[selected] ?: previewCurves.getValue(0),
                    active = selected != 0,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .padding(20.dp),
                )
            }

            Spacer(Modifier.height(24.dp))
            Text("Profiller", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))

            // ── Preset cards grid ─────────────────────────────────────────────
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                itemsIndexed(PlaybackService.EQ_PRESETS) { index, label ->
                    val isSelected = selected == index
                    ElevatedCard(
                        onClick = {
                            player.applyEqPreset(index)
                            SettingsStore.setEqPreset(context, index)
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (isSelected) {
                                    Modifier.border(
                                        2.dp,
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.shapes.medium,
                                    )
                                } else Modifier,
                            ),
                    ) {
                        Box(Modifier.fillMaxWidth()) {
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                            ) {
                                Text(
                                    label,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurface,
                                )
                                Spacer(Modifier.height(8.dp))
                                // Mini 5-bar visualizer preview
                                MiniBandPreview(
                                    curve = previewCurves[index] ?: previewCurves.getValue(0),
                                    active = index != 0,
                                    isSelected = isSelected,
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                        .size(16.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniBandPreview(
    curve: List<Float>,
    active: Boolean,
    isSelected: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        curve.forEachIndexed { i, gain ->
            val target = (0.5f + gain * 0.45f).coerceIn(0.08f, 1f)
            val fill by animateFloatAsState(
                targetValue = if (active) target else 0.5f,
                animationSpec = spring(),
                label = "miniband$i",
            )
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight(fill / 3f + 0.1f)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant,
                    ),
            )
        }
    }
}

@Composable
private fun BandVisualizer(
    curve: List<Float>,
    active: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        curve.forEachIndexed { i, gain ->
            // Map -1..1 gain to a 0.1..1 bar fill so even flat bands are visible.
            val target = (0.5f + gain * 0.45f).coerceIn(0.08f, 1f)
            val fill by animateFloatAsState(
                targetValue = if (active) target else 0.5f,
                animationSpec = spring(),
                label = "band$i",
            )
            val dbValue = ((fill - 0.5f) * 20).roundToInt()
            Column(
                Modifier.weight(1f).fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                Box(
                    Modifier
                        .width(18.dp)
                        .fillMaxHeight(fill)
                        .clip(RoundedCornerShape(9.dp))
                        .background(
                            if (active) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant,
                        ),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    if (dbValue == 0) "0dB" else "${if (dbValue > 0) "+" else ""}${dbValue}dB",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (dbValue != 0) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    bandLabels.getOrElse(i) { "" },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
