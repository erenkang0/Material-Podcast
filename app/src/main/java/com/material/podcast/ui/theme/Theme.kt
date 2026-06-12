package com.material.podcast.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Violet60,
    onPrimary = Color.White,
    primaryContainer = Violet20,
    onPrimaryContainer = Violet80,
    secondary = Pink60,
    onSecondary = Color.White,
    secondaryContainer = Pink20,
    onSecondaryContainer = Pink80,
    tertiary = Coral60,
    onTertiary = Color.White,
    tertiaryContainer = Coral40.copy(alpha = 0.3f),
    onTertiaryContainer = Coral80,
    background = Surface,
    onBackground = OnSurface,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    surfaceContainer = SurfaceContainer,
    surfaceContainerHigh = SurfaceContainerHigh,
    outline = Color(0xFF6B5B8A),
    outlineVariant = Color(0xFF3D2F5A),
    error = Color(0xFFFF6B6B),
    onError = Color.White,
)

@Composable
fun MaterialPodcastTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
