@file:OptIn(ExperimentalMaterial3Api::class)

package com.material.podcast.ui.screens

import android.app.Activity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Speed
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.material.podcast.BuildConfig
import com.material.podcast.R
import com.material.podcast.data.store.SettingsStore

@Composable
fun SettingsScreen(onOpenThemes: () -> Unit) {
    val context = LocalContext.current
    var currentLang by remember { mutableStateOf(SettingsStore.getLanguage(context)) }
    var defaultSpeed by remember { mutableFloatStateOf(SettingsStore.getDefaultSpeed(context)) }

    Scaffold(
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.settings_title),
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
                top = innerPadding.calculateTopPadding(),
                bottom = 32.dp,
            ),
        ) {
            // ── Appearance ───────────────────────────────────────────────────
            item {
                SettingsSectionHeader(stringResource(R.string.settings_appearance))
            }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_color_theme)) },
                    supportingContent = { Text(stringResource(R.string.settings_color_theme_sub)) },
                    leadingContent = { Icon(Icons.Rounded.Palette, null) },
                    trailingContent = {
                        OutlinedButton(onClick = onOpenThemes) {
                            Text(if (currentLang == "tr") "Seç" else "Choose")
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }

            item { HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp)) }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_language)) },
                    leadingContent = { Icon(Icons.Rounded.Language, null) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }

            item {
                LanguageChips(
                    current = currentLang,
                    onSelect = { lang ->
                        if (lang != currentLang) {
                            currentLang = lang
                            SettingsStore.setLanguage(context, lang)
                            (context as? Activity)?.recreate()
                        }
                    },
                    modifier = Modifier.padding(start = 72.dp, end = 16.dp, bottom = 8.dp),
                )
            }

            // ── Playback ─────────────────────────────────────────────────────
            item {
                SettingsSectionHeader(stringResource(R.string.settings_playback))
            }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_default_speed)) },
                    leadingContent = { Icon(Icons.Rounded.Speed, null) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }

            item {
                SpeedChips(
                    current = defaultSpeed,
                    onSelect = { speed ->
                        defaultSpeed = speed
                        SettingsStore.setDefaultSpeed(context, speed)
                    },
                    modifier = Modifier.padding(start = 72.dp, end = 16.dp, bottom = 8.dp),
                )
            }

            // ── About ────────────────────────────────────────────────────────
            item {
                SettingsSectionHeader(stringResource(R.string.settings_about))
            }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_version)) },
                    trailingContent = {
                        Text(
                            BuildConfig.VERSION_NAME,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }

            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_open_source)) },
                    supportingContent = { Text(stringResource(R.string.settings_open_source_sub)) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }

            item {
                ListItem(
                    headlineContent = {
                        Text(
                            stringResource(R.string.settings_made_with),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 4.dp, end = 16.dp),
    )
}

@Composable
private fun LanguageChips(
    current: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val langs = listOf("tr" to stringResource(R.string.lang_turkish), "en" to stringResource(R.string.lang_english))
    Row(modifier = modifier) {
        langs.forEachIndexed { i, (code, label) ->
            if (i > 0) Spacer(Modifier.width(8.dp))
            FilterChip(
                selected = current == code,
                onClick = { onSelect(code) },
                label = { Text(label) },
                leadingIcon = if (current == code) {
                    { Icon(Icons.Rounded.Check, null) }
                } else null,
            )
        }
    }
}

@Composable
private fun SpeedChips(
    current: Float,
    onSelect: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val speeds = listOf(0.75f to "0.75×", 1f to "1×", 1.25f to "1.25×", 1.5f to "1.5×", 2f to "2×")
    Row(modifier = modifier.fillMaxWidth()) {
        speeds.forEach { (speed, label) ->
            FilterChip(
                selected = current == speed,
                onClick = { onSelect(speed) },
                label = { Text(label) },
                modifier = Modifier.padding(end = 6.dp),
            )
        }
    }
}
