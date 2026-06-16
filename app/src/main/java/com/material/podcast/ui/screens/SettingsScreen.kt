@file:OptIn(ExperimentalMaterial3Api::class)

package com.material.podcast.ui.screens

import android.app.Activity
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Podcasts
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.material.podcast.BuildConfig
import com.material.podcast.data.store.LibraryStore
import com.material.podcast.data.store.SettingsStore
import com.material.podcast.ui.theme.AppThemeColor

@Composable
fun SettingsScreen(onOpenThemes: () -> Unit, onOpenEqualizer: () -> Unit = {}) {
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    var currentLang by remember { mutableStateOf(SettingsStore.getLanguage(context)) }
    var defaultSpeed by remember { mutableFloatStateOf(SettingsStore.getDefaultSpeed(context)) }
    var wifiOnly by remember { mutableStateOf(SettingsStore.isWifiOnlyDownload(context)) }
    var searchQuery by remember { mutableStateOf("") }
    fun tr(turkish: String, english: String) = if (currentLang == "tr") turkish else english

    fun tapLight() = haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    fun tapStrong() = haptics.performHapticFeedback(HapticFeedbackType.LongPress)

    // Flat list of all settings items for search
    data class SettingsItem(val keys: List<String>, val section: String)
    val allItems = listOf(
        SettingsItem(listOf(tr("Renk teması", "Color theme"), tr("Paletini seç", "Choose your palette")), tr("Görünüm", "Appearance")),
        SettingsItem(listOf(tr("Dil", "Language"), "Türkçe", "English"), tr("Görünüm", "Appearance")),
        SettingsItem(listOf(tr("Varsayılan hız", "Default speed")), tr("Oynatma", "Playback")),
        SettingsItem(listOf(tr("Ekolayzer", "Equalizer"), tr("Ses tonu profilleri", "Audio tone profiles")), tr("Oynatma", "Playback")),
        SettingsItem(listOf(tr("Pil optimizasyonu", "Battery optimization")), tr("Arka plan", "Background")),
        SettingsItem(listOf(tr("Yalnızca Wi-Fi'de otomatik indir", "Auto-download on Wi-Fi only")), tr("Arka plan", "Background")),
        SettingsItem(listOf(tr("Verilerini yedekle", "Back up your data")), tr("Yedekleme", "Backup")),
        SettingsItem(listOf(tr("Sürüm", "Version"), "Echoes", BuildConfig.VERSION_NAME), tr("Hakkında", "About")),
    )

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
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp,
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = 40.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            // ── Hero header ──────────────────────────────────────────────────
            item {
                StaggeredReveal(index = 0) {
                    HeroHeader(
                        title = tr("Ayarlar", "Settings"),
                        subtitle = tr("Echoes'u kendine göre ayarla", "Tune Echoes to your taste"),
                    )
                }
            }

            // ── Search ───────────────────────────────────────────────────────
            item {
                StaggeredReveal(index = 1) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text(tr("Ayarlarda ara...", "Search settings...")) },
                        leadingIcon = { Icon(Icons.Rounded.Search, null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.extraLarge,
                    )
                }
            }

            if (searchQuery.isNotEmpty()) {
                // ── Filtered results ─────────────────────────────────────────
                val q = searchQuery.lowercase()
                val matches = allItems.filter { item ->
                    item.keys.any { it.lowercase().contains(q) } || item.section.lowercase().contains(q)
                }
                item {
                    if (matches.isEmpty()) {
                        Box(Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                            Text(
                                tr("Sonuç bulunamadı", "No results found"),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        ElevatedCard(Modifier.fillMaxWidth()) {
                            matches.forEachIndexed { idx, item ->
                                ListItem(
                                    headlineContent = { Text(item.keys.first()) },
                                    supportingContent = { Text(item.section, style = MaterialTheme.typography.labelSmall) },
                                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                )
                                if (idx < matches.lastIndex) HorizontalDivider()
                            }
                        }
                    }
                }
            } else {
                // ── Appearance ───────────────────────────────────────────────
                item {
                    StaggeredReveal(index = 2) {
                        SettingsCard(
                            title = tr("Görünüm", "Appearance"),
                            accent = MaterialTheme.colorScheme.primary,
                        ) {
                            SettingsRow(
                                icon = Icons.Rounded.Palette,
                                iconBg = MaterialTheme.colorScheme.primaryContainer,
                                iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                                title = tr("Renk teması", "Color theme"),
                                subtitle = tr("Paletini seç", "Choose your palette"),
                                trailing = {
                                    FilledTonalButton(onClick = { tapLight(); onOpenThemes() }) {
                                        Text(tr("Seç", "Choose"))
                                    }
                                },
                            )
                            // Theme color swatches row
                            Row(
                                Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                AppThemeColor.entries.forEach { theme ->
                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(CircleShape)
                                            .background(theme.swatch),
                                    )
                                }
                            }
                            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                            SettingsRow(
                                icon = Icons.Rounded.Language,
                                iconBg = MaterialTheme.colorScheme.primaryContainer,
                                iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                                title = tr("Dil", "Language"),
                                subtitle = tr("Uygulama dilini değiştir", "Change the app language"),
                            )
                            Row(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 16.dp)) {
                                listOf("tr" to "Türkçe", "en" to "English").forEachIndexed { i, (code, label) ->
                                    if (i > 0) Spacer(Modifier.width(8.dp))
                                    FilterChip(
                                        selected = currentLang == code,
                                        onClick = {
                                            if (code != currentLang) {
                                                tapLight()
                                                currentLang = code
                                                SettingsStore.setLanguage(context, code)
                                                (context as? Activity)?.recreate()
                                            }
                                        },
                                        label = { Text(label) },
                                        leadingIcon = if (currentLang == code) {
                                            { Icon(Icons.Rounded.Check, null, Modifier.size(FilterChipDefaults.IconSize)) }
                                        } else null,
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Playback ─────────────────────────────────────────────────
                item {
                    StaggeredReveal(index = 3) {
                        SettingsCard(
                            title = tr("Oynatma", "Playback"),
                            accent = MaterialTheme.colorScheme.secondary,
                        ) {
                            SettingsRow(
                                icon = Icons.Rounded.Speed,
                                iconBg = MaterialTheme.colorScheme.secondaryContainer,
                                iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                                title = tr("Varsayılan hız", "Default speed"),
                                subtitle = tr("Bölümler bu hızda başlar", "Episodes start at this speed"),
                                trailing = {
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.secondaryContainer)
                                            .padding(horizontal = 12.dp, vertical = 5.dp),
                                    ) {
                                        Text(
                                            text = if (defaultSpeed == 1f) "1.0×" else "${defaultSpeed}×",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                },
                            )
                            Row(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 16.dp)) {
                                listOf(0.75f to "0.75×", 1f to "1×", 1.25f to "1.25×", 1.5f to "1.5×", 2f to "2×").forEach { (speed, label) ->
                                    FilterChip(
                                        selected = defaultSpeed == speed,
                                        onClick = { tapLight(); defaultSpeed = speed; SettingsStore.setDefaultSpeed(context, speed) },
                                        label = { Text(label) },
                                        modifier = Modifier.padding(end = 6.dp),
                                    )
                                }
                            }
                            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                            SettingsRow(
                                icon = Icons.Rounded.GraphicEq,
                                iconBg = MaterialTheme.colorScheme.secondaryContainer,
                                iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                                title = tr("Ekolayzer", "Equalizer"),
                                subtitle = tr("Ses tonu profilleri", "Audio tone profiles"),
                                trailing = {
                                    FilledTonalButton(onClick = { tapLight(); onOpenEqualizer() }) {
                                        Icon(Icons.Rounded.Tune, null, Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(tr("Aç", "Open"))
                                    }
                                },
                            )
                        }
                    }
                }

                // ── Background / battery ─────────────────────────────────────
                item {
                    StaggeredReveal(index = 4) {
                        SettingsCard(
                            title = tr("Arka plan", "Background"),
                            accent = MaterialTheme.colorScheme.tertiary,
                        ) {
                            SettingsRow(
                                icon = Icons.Rounded.BatteryChargingFull,
                                iconBg = MaterialTheme.colorScheme.tertiaryContainer,
                                iconTint = MaterialTheme.colorScheme.onTertiaryContainer,
                                title = tr("Pil optimizasyonu", "Battery optimization"),
                                subtitle = tr("Arka planda çalmanın kesilmemesi için kapatın",
                                    "Disable so background playback isn't interrupted"),
                            )
                            Row(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp)) {
                                FilledTonalButton(onClick = {
                                    tapStrong()
                                    runCatching { context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
                                }) { Text(tr("Yönet", "Manage")) }
                                Spacer(Modifier.width(8.dp))
                                OutlinedButton(onClick = {
                                    tapStrong()
                                    SettingsStore.setOnboarded(context, false)
                                    (context as? Activity)?.recreate()
                                }) {
                                    Icon(Icons.Rounded.RestartAlt, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(tr("Kurulumu tekrar çalıştır", "Re-run setup"))
                                }
                            }
                            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                            SettingsRow(
                                icon = Icons.Rounded.Wifi,
                                iconBg = MaterialTheme.colorScheme.tertiaryContainer,
                                iconTint = MaterialTheme.colorScheme.onTertiaryContainer,
                                title = tr("Yalnızca Wi-Fi'de otomatik indir", "Auto-download on Wi-Fi only"),
                                subtitle = tr("Yeni bölümleri sadece Wi-Fi varken otomatik indir",
                                    "Auto-download new episodes only on Wi-Fi"),
                                trailing = {
                                    Switch(
                                        checked = wifiOnly,
                                        onCheckedChange = {
                                            tapLight()
                                            wifiOnly = it
                                            SettingsStore.setWifiOnlyDownload(context, it)
                                        },
                                    )
                                },
                            )
                        }
                    }
                }

                // ── Backup ───────────────────────────────────────────────────
                item {
                    StaggeredReveal(index = 5) {
                        SettingsCard(
                            title = tr("Yedekleme", "Backup"),
                            accent = MaterialTheme.colorScheme.primary,
                        ) {
                            SettingsRow(
                                icon = Icons.Rounded.CloudUpload,
                                iconBg = MaterialTheme.colorScheme.primaryContainer,
                                iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                                title = tr("Verilerini yedekle (JSON)", "Back up your data (JSON)"),
                                subtitle = tr("Beğeniler, notlar, indirmeler, kategoriler ve devam noktaları",
                                    "Likes, notes, downloads, categories and resume points"),
                            )
                            Row(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 16.dp)) {
                                FilledTonalButton(onClick = {
                                    tapStrong()
                                    exportLauncher.launch("echoes-backup.json")
                                }) {
                                    Icon(Icons.Rounded.CloudUpload, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(tr("Dışa aktar", "Export"))
                                }
                                Spacer(Modifier.width(8.dp))
                                OutlinedButton(onClick = {
                                    tapStrong()
                                    importLauncher.launch(arrayOf("application/json"))
                                }) {
                                    Icon(Icons.Rounded.CloudDownload, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(tr("İçe aktar", "Import"))
                                }
                            }
                        }
                    }
                }

                // ── About card ───────────────────────────────────────────────
                item {
                    StaggeredReveal(index = 6) {
                        AboutCard(
                            aboutLabel = tr("Hakkında", "About"),
                        )
                    }
                }
            }
        }
    }
}

/* ───────────────────────────── Components ───────────────────────────── */

@Composable
private fun HeroHeader(title: String, subtitle: String) {
    val cs = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(cs.primaryContainer, cs.tertiaryContainer),
                    ),
                )
                .padding(24.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(cs.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.Podcasts,
                        contentDescription = null,
                        tint = cs.onPrimary,
                        modifier = Modifier.size(30.dp),
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = cs.onPrimaryContainer,
                    )
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = cs.onPrimaryContainer.copy(alpha = 0.8f),
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    accent: Color,
    content: @Composable () -> Unit,
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 12.dp, bottom = 8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(accent),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                title.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = accent,
                fontWeight = FontWeight.Bold,
            )
        }
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            Spacer(Modifier.height(4.dp))
            content()
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun SettingsRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    subtitle: String? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.SemiBold) },
        supportingContent = subtitle?.let { { Text(it) } },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = iconTint, modifier = Modifier.size(22.dp))
            }
        },
        trailingContent = trailing,
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

@Composable
private fun AboutCard(aboutLabel: String) {
    val cs = MaterialTheme.colorScheme
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 12.dp, bottom = 8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(cs.tertiary),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                aboutLabel.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = cs.tertiary,
                fontWeight = FontWeight.Bold,
            )
        }
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(cs.primary, cs.tertiary),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.Podcasts,
                        contentDescription = null,
                        tint = cs.onPrimary,
                        modifier = Modifier.size(36.dp),
                    )
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    "Echoes",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(cs.primaryContainer)
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                ) {
                    Text(
                        BuildConfig.VERSION_NAME,
                        style = MaterialTheme.typography.labelMedium,
                        color = cs.onPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Kotlin · Jetpack Compose · Material 3",
                        style = MaterialTheme.typography.labelMedium,
                        color = cs.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Favorite,
                        contentDescription = null,
                        tint = cs.primary,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Anthropic × Erenkang0",
                        style = MaterialTheme.typography.labelMedium,
                        color = cs.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun StaggeredReveal(index: Int, content: @Composable () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(Unit) { visible = true }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(300, delayMillis = index * 60)) +
            slideInVertically(
                animationSpec = tween(360, delayMillis = index * 60, easing = LinearOutSlowInEasing),
                initialOffsetY = { it / 6 },
            ),
    ) {
        content()
    }
}
