@file:OptIn(ExperimentalMaterial3Api::class)

package com.material.podcast.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background

/**
 * First-run onboarding. Welcomes the user, then walks the important settings one at a time:
 * notification permission and disabling battery optimization (so background playback survives).
 */
@Composable
fun SetupScreen(onFinish: () -> Unit) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    var step by remember { mutableIntStateOf(0) }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { step = (step + 1).coerceAtMost(LAST_STEP) }

    fun next() {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        step = (step + 1).coerceAtMost(LAST_STEP)
    }

    fun requestNotifications() {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        } else {
            next()
        }
    }

    fun requestBattery() {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            runCatching {
                context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            }
        }
        next()
    }

    val batteryAlreadyOk = remember {
        val pm = context.getSystemService(PowerManager::class.java)
        pm?.isIgnoringBatteryOptimizations(context.packageName) == true
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(48.dp))

            // Step indicator dots
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(LAST_STEP + 1) { i ->
                    Box(
                        Modifier
                            .size(if (i == step) 22.dp else 8.dp, 8.dp)
                            .clip(CircleShape)
                            .background(
                                if (i <= step) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceContainerHighest,
                            ),
                    )
                }
            }

            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                AnimatedContent(
                    targetState = step,
                    transitionSpec = { (fadeIn() togetherWith fadeOut()) },
                    label = "setupStep",
                ) { s ->
                    when (s) {
                        0 -> StepContent(
                            icon = Icons.Rounded.GraphicEq,
                            title = "Echoes'e hoş geldiniz",
                            body = "Podcast'leri dinleyin, beğenin, favori anlarınızı not alın ve " +
                                "çevrimdışı indirin. Başlamadan önce birkaç önemli ayarı birlikte yapalım.",
                        )
                        1 -> StepContent(
                            icon = Icons.Rounded.Notifications,
                            title = "Bildirimler",
                            body = "Oynatma kontrollerini bildirim alanında göstermek ve arka planda " +
                                "çalmaya devam etmek için bildirim izni gerekir.",
                        )
                        2 -> StepContent(
                            icon = Icons.Rounded.BatteryChargingFull,
                            title = "Arka planda çalma",
                            body = if (batteryAlreadyOk) {
                                "Pil optimizasyonu bu uygulama için zaten kapalı. Harika!"
                            } else {
                                "Uygulamanın arka planda kapanmaması için pil optimizasyonunu " +
                                    "devre dışı bırakın. Bu, dinlerken çalmanın kesilmemesini sağlar."
                            },
                        )
                        else -> StepContent(
                            icon = Icons.Rounded.CheckCircle,
                            title = "Hazırsınız!",
                            body = "Her şey ayarlandı. Keyifli dinlemeler.",
                        )
                    }
                }
            }

            // Action buttons per step
            when (step) {
                0 -> PrimaryAction("Başla") { next() }
                1 -> {
                    PrimaryAction("İzin ver") { requestNotifications() }
                    TextButton(onClick = { next() }) { Text("Şimdilik atla") }
                }
                2 -> {
                    PrimaryAction(if (batteryAlreadyOk) "Devam et" else "Pil optimizasyonunu kapat") {
                        if (batteryAlreadyOk) next() else requestBattery()
                    }
                    if (!batteryAlreadyOk) TextButton(onClick = { next() }) { Text("Şimdilik atla") }
                }
                else -> PrimaryAction("Bitir") {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onFinish()
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

private const val LAST_STEP = 3

@Composable
private fun StepContent(icon: ImageVector, title: String, body: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon, null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(48.dp),
            )
        }
        Spacer(Modifier.height(28.dp))
        Text(
            title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PrimaryAction(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium)
    }
}
