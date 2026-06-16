@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.material.podcast.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Brightness6
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.material.podcast.data.store.SettingsStore
import com.material.podcast.ui.theme.AppThemeColor
import com.material.podcast.ui.theme.DarkModeOption
import com.material.podcast.ui.theme.ThemeController

@Composable
fun ThemePickerSheet(
    controller: ThemeController,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState()

    var amberTaps by remember { mutableIntStateOf(0) }
    var eldenRingUnlocked by remember { mutableStateOf(SettingsStore.isEldenRingUnlocked(context)) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp),
        ) {
            SheetHeader(
                title = "Görünüm",
                subtitle = "Gözlerine rahat gelen bir renk seç",
            )

            Spacer(Modifier.height(20.dp))
            Text(
                "Tema Rengi",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val visibleThemes = AppThemeColor.entries.filter {
                    it != AppThemeColor.EldenRing || eldenRingUnlocked
                }
                visibleThemes.forEach { item ->
                    if (item == AppThemeColor.Amber) {
                        ColorSwatch(
                            item = item,
                            selected = controller.color == item,
                            onClick = {
                                controller.color = item
                                amberTaps++
                                if (amberTaps >= 6 && !eldenRingUnlocked) {
                                    eldenRingUnlocked = true
                                    SettingsStore.setEldenRingUnlocked(context)
                                }
                            },
                        )
                    } else if (item == AppThemeColor.EldenRing) {
                        AnimatedVisibility(
                            visible = eldenRingUnlocked,
                            enter = fadeIn(tween(600)) +
                                scaleIn(initialScale = 0.3f, animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessLow,
                                )) +
                                slideInVertically(tween(500)) { it / 2 },
                        ) {
                            EldenRingSwatch(
                                selected = controller.color == item,
                                onClick = { controller.color = item },
                            )
                        }
                    } else {
                        ColorSwatch(
                            item = item,
                            selected = controller.color == item,
                            onClick = { controller.color = item },
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Text(
                "Karanlık Mod",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val options = DarkModeOption.entries
                options.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = controller.darkMode == option,
                        onClick = { controller.darkMode = option },
                        shape = SegmentedButtonDefaults.itemShape(index, options.size),
                        icon = {
                            Icon(
                                imageVector = when (option) {
                                    DarkModeOption.System -> Icons.Rounded.Brightness6
                                    DarkModeOption.Light -> Icons.Rounded.LightMode
                                    DarkModeOption.Dark -> Icons.Rounded.DarkMode
                                },
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                    ) {
                        Text(option.label)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Text(
                text = "Echoes · v1.0.0 · Claude ile yapıldı",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun SheetHeader(title: String, subtitle: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Palette,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.size(14.dp))
        Column {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ColorSwatch(
    item: AppThemeColor,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.12f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "swatch",
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .clip(CircleShape)
                .background(item.swatch)
                .then(
                    if (selected) {
                        Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                    } else {
                        Modifier.border(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant,
                            CircleShape,
                        )
                    }
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = "Seçili",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            item.label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

// Special swatch for the Elden Ring easter egg — rune-gold radial, pulsing crimson glow
@Composable
private fun EldenRingSwatch(
    selected: Boolean,
    onClick: () -> Unit,
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.14f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "eldenSwatch",
    )
    // Pulsing glow alpha — breathes slowly like the Erdtree
    val pulse = rememberInfiniteTransition(label = "eldenPulse")
    val glowAlpha by pulse.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glowAlpha",
    )

    // Deep void → blood-black core → gold ring → bright gold crown
    val goldGradient = Brush.radialGradient(
        0f   to Color(0xFFF5D87E),  // crown: pale gold
        0.35f to Color(0xFFD4A843), // body: tarnished gold
        0.65f to Color(0xFF6B3800), // shadow: deep amber-brown
        1f   to Color(0xFF1A0800),  // edge: void dark
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            // Outer crimson bloodflame ring — only visible and pulsing when selected
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            alpha = glowAlpha
                        }
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                0f to Color(0x00E05252),
                                0.5f to Color(0x44E05252),
                                1f to Color(0x00E05252),
                            )
                        ),
                )
            }
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .clip(CircleShape)
                    .background(brush = goldGradient)
                    .then(
                        if (selected) {
                            Modifier.border(
                                width = 2.5.dp,
                                brush = Brush.sweepGradient(
                                    listOf(
                                        Color(0xFFF5D87E),
                                        Color(0xFFE05252),
                                        Color(0xFFD4A843),
                                        Color(0xFFF5D87E),
                                    )
                                ),
                                shape = CircleShape,
                            )
                        } else {
                            Modifier.border(
                                width = 1.5.dp,
                                color = Color(0xFFD4A843).copy(alpha = 0.7f),
                                shape = CircleShape,
                            )
                        }
                    )
                    .clickable(onClick = onClick),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) {
                    Icon(
                        Icons.Rounded.Check,
                        contentDescription = "Seçili",
                        tint = Color(0xFF1A0800),
                        modifier = Modifier.size(26.dp),
                    )
                } else {
                    Icon(
                        Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = Color(0xFF1A0800).copy(alpha = 0.85f),
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "Elden Ring",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) Color(0xFFD4A843) else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
