@file:OptIn(ExperimentalMaterial3Api::class)

package com.material.podcast.ui.screens

import android.app.Activity
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.material.podcast.BuildConfig
import com.material.podcast.data.store.LibraryStore
import com.material.podcast.data.store.SettingsStore

@Composable
fun SettingsScreen(onOpenThemes: () -> Unit, onOpenEqualizer: () -> Unit = {}) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    var currentLang by remember { mutableStateOf(SettingsStore.getLanguage(context)) }
    var defaultSpeed by remember { mutableFloatStateOf(SettingsStore.getDefaultSpeed(context)) }
    var wifiOnly by remember { mutableStateOf(SettingsStore.isWifiOnlyDownload(context)) }
    fun tr(turkish: String, english: String) = if (currentLang == "tr") turkish else english

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            val ok = runCatching {
                context.contentResolver.openOutputStream(uri)?.use { it.write(LibraryStore.exportJson().toByteArray()) }
            }.isSuccess
            Toast.makeText(context, if (ok) tr("Yedek kaydedildi", "Backup saved") else tr("Yedekleme başarısız", "Backup failed"), Toast.LENGTH_SHORT).show()
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            val text = runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }.getOrNull()
            val ok = text != null && LibraryStore.importJson(text)
            Toast.makeText(context, if (ok) tr("Geri yüklendi", "Restored") else tr("Geri yükleme başarısız", "Restore failed"), Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        tr("Ayarlar", "Settings"),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = 32.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ── Appearance ───────────────────────────────────────────────────
            item {
                SettingsCard(tr("Görünüm", "Appearance")) {
                    ListItem(
                        headlineContent = { Text(tr("Renk teması", "Color theme")) },
                        supportingContent = { Text(tr("Paletini seç", "Choose your palette")) },
                        leadingContent = { Icon(Icons.Rounded.Palette, null) },
                        trailingContent = {
                            OutlinedButton(onClick = onOpenThemes) { Text(tr("Seç", "Choose")) }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    )
                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text(tr("Dil", "Language")) },
                        leadingContent = { Icon(Icons.Rounded.Language, null) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    )
                    Row(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)) {
                        listOf("tr" to "Türkçe", "en" to "English").forEachIndexed { i, (code, label) ->
                            if (i > 0) Spacer(Modifier.width(8.dp))
                            FilterChip(
                                selected = currentLang == code,
                                onClick = {
                                    if (code != currentLang) {
                                        currentLang = code
                                        SettingsStore.setLanguage(context, code)
                                        (context as? Activity)?.recreate()
                                    }
                                },
                                label = { Text(label) },
                                leadingIcon = if (currentLang == code) {
                                    { Icon(Icons.Rounded.Check, null) }
                                } else null,
                            )
                        }
                    }
                }
            }

            // ── Playback ─────────────────────────────────────────────────────
            item {
                SettingsCard(tr("Oynatma", "Playback")) {
                    ListItem(
                        headlineContent = { Text(tr("Varsayılan hız", "Default speed")) },
                        leadingContent = { Icon(Icons.Rounded.Speed, null) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    )
                    Row(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)) {
                        listOf(0.75f to "0.75×", 1f to "1×", 1.25f to "1.25×", 1.5f to "1.5×", 2f to "2×").forEach { (speed, label) ->
                            FilterChip(
                                selected = defaultSpeed == speed,
                                onClick = { defaultSpeed = speed; SettingsStore.setDefaultSpeed(context, speed) },
                                label = { Text(label) },
                                modifier = Modifier.padding(end = 6.dp),
                            )
                        }
                    }
                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text(tr("Ekolayzer", "Equalizer")) },
                        supportingContent = { Text(tr("Ses tonu profilleri", "Audio tone profiles")) },
                        leadingContent = { Icon(Icons.Rounded.GraphicEq, null) },
                        trailingContent = {
                            OutlinedButton(onClick = onOpenEqualizer) { Text(tr("Aç", "Open")) }
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    )
                }
            }

            // ── Background / battery ─────────────────────────────────────────
            item {
                SettingsCard(tr("Arka plan", "Background")) {
                    ListItem(
                        headlineContent = { Text(tr("Pil optimizasyonu", "Battery optimization")) },
                        supportingContent = {
                            Text(tr("Arka planda çalmanın kesilmemesi için kapatın",
                                "Disable so background playback isn't interrupted"))
                        },
                        leadingContent = { Icon(Icons.Rounded.BatteryChargingFull, null) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    )
                    Row(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)) {
                        FilledTonalButton(onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            runCatching { context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
                        }) { Text(tr("Yönet", "Manage")) }
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(onClick = {
                            SettingsStore.setOnboarded(context, false)
                            (context as? Activity)?.recreate()
                        }) {
                            Icon(Icons.Rounded.RestartAlt, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(tr("Kurulumu tekrar çalıştır", "Re-run setup"))
                        }
                    }
                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text(tr("Yalnızca Wi-Fi'de otomatik indir", "Auto-download on Wi-Fi only")) },
                        supportingContent = {
                            Text(tr("Yeni bölümleri sadece Wi-Fi varken otomatik indir",
                                "Auto-download new episodes only on Wi-Fi"))
                        },
                        leadingContent = { Icon(Icons.Rounded.Wifi, null) },
                        trailingContent = {
                            Switch(
                                checked = wifiOnly,
                                onCheckedChange = {
                                    wifiOnly = it
                                    SettingsStore.setWifiOnlyDownload(context, it)
                                },
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    )
                }
            }

            // ── Backup ───────────────────────────────────────────────────────
            item {
                SettingsCard(tr("Yedekleme", "Backup")) {
                    ListItem(
                        headlineContent = { Text(tr("Verilerini yedekle (JSON)", "Back up your data (JSON)")) },
                        supportingContent = {
                            Text(tr("Beğeniler, notlar, indirmeler, kategoriler ve devam noktaları",
                                "Likes, notes, downloads, categories and resume points"))
                        },
                        leadingContent = { Icon(Icons.Rounded.CloudUpload, null) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    )
                    Row(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)) {
                        FilledTonalButton(onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            exportLauncher.launch("echoes-backup.json")
                        }) {
                            Icon(Icons.Rounded.CloudUpload, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(tr("Dışa aktar", "Export"))
                        }
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            importLauncher.launch(arrayOf("application/json"))
                        }) {
                            Icon(Icons.Rounded.CloudDownload, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(tr("İçe aktar", "Import"))
                        }
                    }
                }
            }

            // ── About ────────────────────────────────────────────────────────
            item {
                SettingsCard(tr("Hakkında", "About")) {
                    ListItem(
                        headlineContent = { Text(tr("Sürüm", "Version")) },
                        trailingContent = {
                            Text(
                                BuildConfig.VERSION_NAME,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    )
                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text(tr("Açık kaynak", "Open source")) },
                        supportingContent = { Text("Kotlin · Jetpack Compose · Material 3") },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable () -> Unit) {
    Column {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 8.dp, bottom = 6.dp),
        )
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            content()
        }
    }
}
