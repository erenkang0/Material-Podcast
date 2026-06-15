package com.material.podcast.ui.theme

import android.app.Activity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/* ------------------------------------------------------------------------------------------------
 *  Palettes — five calm, low-strain color stories, each with a matched light + dark scheme.
 *  Components only ever read `MaterialTheme.colorScheme.*`, so swapping the palette retints the
 *  entire app instantly (and, thanks to [animatedColorScheme], smoothly).
 * ---------------------------------------------------------------------------------------------- */

private val IndigoLight = lightColorScheme(
    primary = Color(0xFF4C5BD4), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE0E0FF), onPrimaryContainer = Color(0xFF000F5C),
    secondary = Color(0xFF5B5D72), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE0E1F9), onSecondaryContainer = Color(0xFF181A2C),
    tertiary = Color(0xFF76536D), onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD7F0), onTertiaryContainer = Color(0xFF2D1228),
    background = Color(0xFFFBF8FF), onBackground = Color(0xFF1B1B21),
    surface = Color(0xFFFBF8FF), onSurface = Color(0xFF1B1B21),
    surfaceVariant = Color(0xFFE3E1EC), onSurfaceVariant = Color(0xFF46464F),
    surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFF5F2FA),
    surfaceContainer = Color(0xFFEFEDF4), surfaceContainerHigh = Color(0xFFE9E7F0),
    surfaceContainerHighest = Color(0xFFE3E1EC),
    outline = Color(0xFF767680), outlineVariant = Color(0xFFC7C5D0),
)
private val IndigoDark = darkColorScheme(
    primary = Color(0xFFBFC2FF), onPrimary = Color(0xFF111B79),
    primaryContainer = Color(0xFF313694), onPrimaryContainer = Color(0xFFE0E0FF),
    secondary = Color(0xFFC4C5DD), onSecondary = Color(0xFF2D2F42),
    secondaryContainer = Color(0xFF434559), onSecondaryContainer = Color(0xFFE0E1F9),
    tertiary = Color(0xFFE5BAD6), onTertiary = Color(0xFF44263D),
    tertiaryContainer = Color(0xFF5C3B54), onTertiaryContainer = Color(0xFFFFD7F0),
    background = Color(0xFF121318), onBackground = Color(0xFFE4E1E9),
    surface = Color(0xFF121318), onSurface = Color(0xFFE4E1E9),
    surfaceVariant = Color(0xFF46464F), onSurfaceVariant = Color(0xFFC7C5D0),
    surfaceContainerLowest = Color(0xFF0D0E13), surfaceContainerLow = Color(0xFF1A1B21),
    surfaceContainer = Color(0xFF1E1F25), surfaceContainerHigh = Color(0xFF292A2F),
    surfaceContainerHighest = Color(0xFF34343A),
    outline = Color(0xFF91909A), outlineVariant = Color(0xFF46464F),
)

private val EmeraldLight = lightColorScheme(
    primary = Color(0xFF00696C), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF9CF1F0), onPrimaryContainer = Color(0xFF002020),
    secondary = Color(0xFF4A6363), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCCE8E7), onSecondaryContainer = Color(0xFF051F1F),
    tertiary = Color(0xFF4B607C), onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD3E4FF), onTertiaryContainer = Color(0xFF04305B),
    background = Color(0xFFF4FBFA), onBackground = Color(0xFF161D1D),
    surface = Color(0xFFF4FBFA), onSurface = Color(0xFF161D1D),
    surfaceVariant = Color(0xFFDAE5E3), onSurfaceVariant = Color(0xFF3F4948),
    surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFEEF5F4),
    surfaceContainer = Color(0xFFE8EFEE), surfaceContainerHigh = Color(0xFFE2E9E8),
    surfaceContainerHighest = Color(0xFFDCE4E3),
    outline = Color(0xFF6F7978), outlineVariant = Color(0xFFBEC9C7),
)
private val EmeraldDark = darkColorScheme(
    primary = Color(0xFF80D5D4), onPrimary = Color(0xFF003738),
    primaryContainer = Color(0xFF004F50), onPrimaryContainer = Color(0xFF9CF1F0),
    secondary = Color(0xFFB0CCCB), onSecondary = Color(0xFF1B3535),
    secondaryContainer = Color(0xFF324B4B), onSecondaryContainer = Color(0xFFCCE8E7),
    tertiary = Color(0xFFA6C8FF), onTertiary = Color(0xFF1B3146),
    tertiaryContainer = Color(0xFF324863), onTertiaryContainer = Color(0xFFD3E4FF),
    background = Color(0xFF0E1514), onBackground = Color(0xFFDDE4E3),
    surface = Color(0xFF0E1514), onSurface = Color(0xFFDDE4E3),
    surfaceVariant = Color(0xFF3F4948), onSurfaceVariant = Color(0xFFBEC9C7),
    surfaceContainerLowest = Color(0xFF090F0F), surfaceContainerLow = Color(0xFF161D1D),
    surfaceContainer = Color(0xFF1A2120), surfaceContainerHigh = Color(0xFF242B2A),
    surfaceContainerHighest = Color(0xFF2F3635),
    outline = Color(0xFF889392), outlineVariant = Color(0xFF3F4948),
)

private val RoseLight = lightColorScheme(
    primary = Color(0xFF9C4146), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDAD9), onPrimaryContainer = Color(0xFF410008),
    secondary = Color(0xFF775656), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDAD9), onSecondaryContainer = Color(0xFF2C1516),
    tertiary = Color(0xFF755A2F), onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDEA8), onTertiaryContainer = Color(0xFF281900),
    background = Color(0xFFFFF8F7), onBackground = Color(0xFF231919),
    surface = Color(0xFFFFF8F7), onSurface = Color(0xFF231919),
    surfaceVariant = Color(0xFFF4DDDC), onSurfaceVariant = Color(0xFF524342),
    surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFFEF0EF),
    surfaceContainer = Color(0xFFFCEAE8), surfaceContainerHigh = Color(0xFFF7E4E3),
    surfaceContainerHighest = Color(0xFFF1DEDD),
    outline = Color(0xFF857372), outlineVariant = Color(0xFFD8C2C0),
)
private val RoseDark = darkColorScheme(
    primary = Color(0xFFFFB3B3), onPrimary = Color(0xFF5F1318),
    primaryContainer = Color(0xFF7E2A2E), onPrimaryContainer = Color(0xFFFFDAD9),
    secondary = Color(0xFFE6BDBC), onSecondary = Color(0xFF44292A),
    secondaryContainer = Color(0xFF5D3F3F), onSecondaryContainer = Color(0xFFFFDAD9),
    tertiary = Color(0xFFE5C18D), onTertiary = Color(0xFF402D04),
    tertiaryContainer = Color(0xFF5A4225), onTertiaryContainer = Color(0xFFFFDEA8),
    background = Color(0xFF1A1111), onBackground = Color(0xFFF0DEDD),
    surface = Color(0xFF1A1111), onSurface = Color(0xFFF0DEDD),
    surfaceVariant = Color(0xFF524342), onSurfaceVariant = Color(0xFFD7C1C0),
    surfaceContainerLowest = Color(0xFF140C0C), surfaceContainerLow = Color(0xFF221919),
    surfaceContainer = Color(0xFF271D1D), surfaceContainerHigh = Color(0xFF322827),
    surfaceContainerHighest = Color(0xFF3D3231),
    outline = Color(0xFFA08C8B), outlineVariant = Color(0xFF524342),
)

private val AmberLight = lightColorScheme(
    primary = Color(0xFF785A00), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFE08D), onPrimaryContainer = Color(0xFF261A00),
    secondary = Color(0xFF6A5D3F), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF4E0BB), onSecondaryContainer = Color(0xFF231B04),
    tertiary = Color(0xFF4A6547), onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFCCEBC4), onTertiaryContainer = Color(0xFF072109),
    background = Color(0xFFFFF9EE), onBackground = Color(0xFF1E1B13),
    surface = Color(0xFFFFF9EE), onSurface = Color(0xFF1E1B13),
    surfaceVariant = Color(0xFFEDE1CF), onSurfaceVariant = Color(0xFF4D4639),
    surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFFCF2E2),
    surfaceContainer = Color(0xFFF6ECDA), surfaceContainerHigh = Color(0xFFF0E6D5),
    surfaceContainerHighest = Color(0xFFEBE1CF),
    outline = Color(0xFF7F7767), outlineVariant = Color(0xFFD0C5B3),
)
private val AmberDark = darkColorScheme(
    primary = Color(0xFFECC24B), onPrimary = Color(0xFF3F2E00),
    primaryContainer = Color(0xFF5B4300), onPrimaryContainer = Color(0xFFFFE08D),
    secondary = Color(0xFFD7C4A1), onSecondary = Color(0xFF3A2F15),
    secondaryContainer = Color(0xFF514526), onSecondaryContainer = Color(0xFFF4E0BB),
    tertiary = Color(0xFFB0CFA9), onTertiary = Color(0xFF1D361C),
    tertiaryContainer = Color(0xFF334B31), onTertiaryContainer = Color(0xFFCCEBC4),
    background = Color(0xFF15130B), onBackground = Color(0xFFE9E2D4),
    surface = Color(0xFF15130B), onSurface = Color(0xFFE9E2D4),
    surfaceVariant = Color(0xFF4D4639), onSurfaceVariant = Color(0xFFD0C5B4),
    surfaceContainerLowest = Color(0xFF100E07), surfaceContainerLow = Color(0xFF1D1A12),
    surfaceContainer = Color(0xFF211E16), surfaceContainerHigh = Color(0xFF2C2920),
    surfaceContainerHighest = Color(0xFF37342A),
    outline = Color(0xFF998F80), outlineVariant = Color(0xFF4D4639),
)

private val LavenderLight = lightColorScheme(
    primary = Color(0xFF7B4DBC), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFF0DBFF), onPrimaryContainer = Color(0xFF2C0050),
    secondary = Color(0xFF65596C), onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFECDDF3), onSecondaryContainer = Color(0xFF20182A),
    tertiary = Color(0xFF815250), onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDAD7), onTertiaryContainer = Color(0xFF331110),
    background = Color(0xFFFFF7FE), onBackground = Color(0xFF1D1B1E),
    surface = Color(0xFFFFF7FE), onSurface = Color(0xFF1D1B1E),
    surfaceVariant = Color(0xFFE9DFEA), onSurfaceVariant = Color(0xFF4A454D),
    surfaceContainerLowest = Color(0xFFFFFFFF), surfaceContainerLow = Color(0xFFFAF1FA),
    surfaceContainer = Color(0xFFF4EBF4), surfaceContainerHigh = Color(0xFFEEE6EF),
    surfaceContainerHighest = Color(0xFFE8E0E9),
    outline = Color(0xFF7B757E), outlineVariant = Color(0xFFCCC4CE),
)
private val LavenderDark = darkColorScheme(
    primary = Color(0xFFDCB8FF), onPrimary = Color(0xFF471969),
    primaryContainer = Color(0xFF5F3A86), onPrimaryContainer = Color(0xFFF0DBFF),
    secondary = Color(0xFFCFC0D7), onSecondary = Color(0xFF352C3D),
    secondaryContainer = Color(0xFF4D4356), onSecondaryContainer = Color(0xFFECDDF3),
    tertiary = Color(0xFFF5B7B3), onTertiary = Color(0xFF4C2523),
    tertiaryContainer = Color(0xFF663B39), onTertiaryContainer = Color(0xFFFFDAD7),
    background = Color(0xFF161218), onBackground = Color(0xFFE7E1E6),
    surface = Color(0xFF161218), onSurface = Color(0xFFE7E1E6),
    surfaceVariant = Color(0xFF4A454D), onSurfaceVariant = Color(0xFFCDC4CE),
    surfaceContainerLowest = Color(0xFF100C13), surfaceContainerLow = Color(0xFF1E1A21),
    surfaceContainer = Color(0xFF221E25), surfaceContainerHigh = Color(0xFF2D2830),
    surfaceContainerHighest = Color(0xFF38323B),
    outline = Color(0xFF968E98), outlineVariant = Color(0xFF4A454D),
)

/** A selectable color story, with its matched light + dark schemes and a swatch for the picker. */
enum class AppThemeColor(
    val label: String,
    val swatch: Color,
    val light: ColorScheme,
    val dark: ColorScheme,
) {
    Indigo("Indigo", IndigoSeed, IndigoLight, IndigoDark),
    Emerald("Emerald", EmeraldSeed, EmeraldLight, EmeraldDark),
    Rose("Rose", RoseSeed, RoseLight, RoseDark),
    Amber("Amber", AmberSeed, AmberLight, AmberDark),
    Lavender("Lavender", LavenderSeed, LavenderLight, LavenderDark),
}

/** How the app resolves light vs dark. */
enum class DarkModeOption(val label: String) {
    System("Auto"), Light("Light"), Dark("Dark"),
}

/* ------------------------------------------------------------------------------------------------
 *  Theme controller — a tiny, observable holder hoisted in MainActivity and exposed app-wide so
 *  the picker (anywhere in the tree) can retint everything.
 * ---------------------------------------------------------------------------------------------- */

@Stable
class ThemeController(
    color: AppThemeColor = AppThemeColor.Indigo,
    darkMode: DarkModeOption = DarkModeOption.System,
) {
    var color by mutableStateOf(color)
    var darkMode by mutableStateOf(darkMode)
}

val LocalThemeController = staticCompositionLocalOf { ThemeController() }

@Composable
fun rememberThemeController(): ThemeController =
    rememberSaveable(
        saver = androidx.compose.runtime.saveable.Saver(
            save = { "${it.color.name}|${it.darkMode.name}" },
            restore = {
                val parts = it.split("|")
                ThemeController(
                    AppThemeColor.valueOf(parts[0]),
                    DarkModeOption.valueOf(parts[1]),
                )
            }
        )
    ) {
        ThemeController()
    }

/* ------------------------------------------------------------------------------------------------
 *  Animated scheme — every visible role cross-fades when the palette or light/dark mode changes,
 *  so theme switching feels like a smooth wash of color rather than a hard cut.
 * ---------------------------------------------------------------------------------------------- */

@Composable
private fun animatedColorScheme(target: ColorScheme, darkTheme: Boolean): ColorScheme {
    val spec = tween<Color>(durationMillis = 550)
    val primary by animateColorAsState(target.primary, spec, label = "primary")
    val onPrimary by animateColorAsState(target.onPrimary, spec, label = "onPrimary")
    val primaryContainer by animateColorAsState(target.primaryContainer, spec, label = "primaryContainer")
    val onPrimaryContainer by animateColorAsState(target.onPrimaryContainer, spec, label = "onPrimaryContainer")
    val secondary by animateColorAsState(target.secondary, spec, label = "secondary")
    val onSecondary by animateColorAsState(target.onSecondary, spec, label = "onSecondary")
    val secondaryContainer by animateColorAsState(target.secondaryContainer, spec, label = "secondaryContainer")
    val onSecondaryContainer by animateColorAsState(target.onSecondaryContainer, spec, label = "onSecondaryContainer")
    val tertiary by animateColorAsState(target.tertiary, spec, label = "tertiary")
    val onTertiary by animateColorAsState(target.onTertiary, spec, label = "onTertiary")
    val tertiaryContainer by animateColorAsState(target.tertiaryContainer, spec, label = "tertiaryContainer")
    val onTertiaryContainer by animateColorAsState(target.onTertiaryContainer, spec, label = "onTertiaryContainer")
    val background by animateColorAsState(target.background, spec, label = "background")
    val onBackground by animateColorAsState(target.onBackground, spec, label = "onBackground")
    val surface by animateColorAsState(target.surface, spec, label = "surface")
    val onSurface by animateColorAsState(target.onSurface, spec, label = "onSurface")
    val surfaceVariant by animateColorAsState(target.surfaceVariant, spec, label = "surfaceVariant")
    val onSurfaceVariant by animateColorAsState(target.onSurfaceVariant, spec, label = "onSurfaceVariant")
    val surfaceContainerLowest by animateColorAsState(target.surfaceContainerLowest, spec, label = "scLowest")
    val surfaceContainerLow by animateColorAsState(target.surfaceContainerLow, spec, label = "scLow")
    val surfaceContainer by animateColorAsState(target.surfaceContainer, spec, label = "sc")
    val surfaceContainerHigh by animateColorAsState(target.surfaceContainerHigh, spec, label = "scHigh")
    val surfaceContainerHighest by animateColorAsState(target.surfaceContainerHighest, spec, label = "scHighest")
    val outline by animateColorAsState(target.outline, spec, label = "outline")
    val outlineVariant by animateColorAsState(target.outlineVariant, spec, label = "outlineVariant")

    // Rebuilt through the stable scheme builders (no reliance on ColorScheme.copy). Roles we
    // don't animate fall back to sensible Material defaults, which our palettes already use.
    return if (darkTheme) {
        darkColorScheme(
            primary = primary, onPrimary = onPrimary,
            primaryContainer = primaryContainer, onPrimaryContainer = onPrimaryContainer,
            secondary = secondary, onSecondary = onSecondary,
            secondaryContainer = secondaryContainer, onSecondaryContainer = onSecondaryContainer,
            tertiary = tertiary, onTertiary = onTertiary,
            tertiaryContainer = tertiaryContainer, onTertiaryContainer = onTertiaryContainer,
            background = background, onBackground = onBackground,
            surface = surface, onSurface = onSurface,
            surfaceVariant = surfaceVariant, onSurfaceVariant = onSurfaceVariant,
            surfaceContainerLowest = surfaceContainerLowest, surfaceContainerLow = surfaceContainerLow,
            surfaceContainer = surfaceContainer, surfaceContainerHigh = surfaceContainerHigh,
            surfaceContainerHighest = surfaceContainerHighest,
            outline = outline, outlineVariant = outlineVariant,
        )
    } else {
        lightColorScheme(
            primary = primary, onPrimary = onPrimary,
            primaryContainer = primaryContainer, onPrimaryContainer = onPrimaryContainer,
            secondary = secondary, onSecondary = onSecondary,
            secondaryContainer = secondaryContainer, onSecondaryContainer = onSecondaryContainer,
            tertiary = tertiary, onTertiary = onTertiary,
            tertiaryContainer = tertiaryContainer, onTertiaryContainer = onTertiaryContainer,
            background = background, onBackground = onBackground,
            surface = surface, onSurface = onSurface,
            surfaceVariant = surfaceVariant, onSurfaceVariant = onSurfaceVariant,
            surfaceContainerLowest = surfaceContainerLowest, surfaceContainerLow = surfaceContainerLow,
            surfaceContainer = surfaceContainer, surfaceContainerHigh = surfaceContainerHigh,
            surfaceContainerHighest = surfaceContainerHighest,
            outline = outline, outlineVariant = outlineVariant,
        )
    }
}

@Composable
fun EchoesTheme(
    controller: ThemeController,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (controller.darkMode) {
        DarkModeOption.System -> systemDark
        DarkModeOption.Light -> false
        DarkModeOption.Dark -> true
    }
    // Apply the scheme directly (instant). Animating ~25 color roles at the root recomposed
    // the entire tree every frame during a switch, which caused visible jank.
    val colorScheme = if (darkTheme) controller.color.dark else controller.color.light

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val controllerCompat = WindowCompat.getInsetsController(window, view)
            // Keep system bar icons legible against whatever surface sits behind them.
            val lightIcons = colorScheme.background.luminance() > 0.5f
            controllerCompat.isAppearanceLightStatusBars = lightIcons
            controllerCompat.isAppearanceLightNavigationBars = lightIcons
        }
    }

    CompositionLocalProvider(LocalThemeController provides controller) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}
