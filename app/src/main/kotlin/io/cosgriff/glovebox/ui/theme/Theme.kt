package io.cosgriff.glovebox.ui.theme

import io.cosgriff.glovebox.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.Font
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import io.cosgriff.glovebox.data.settings.ThemeMode

// Tokens from the Glovebox design canvas. Dynamic (wallpaper) color is intentionally off.

/** The canvas colours; also read by the home-screen widget, which has no MaterialTheme. */
internal val LightScheme = lightColorScheme(
    primary = Color(0xFFC2410C),
    onPrimary = Color(0xFFFFFFFF),
    inversePrimary = Color(0xFFF08A4B), // snackbar actions sit on a dark bar
    primaryContainer = Color(0xFFFBE9DF),
    onPrimaryContainer = Color(0xFF9A3412),
    secondary = Color(0xFF17191C),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFBE9DF),
    onSecondaryContainer = Color(0xFF9A3412),
    tertiary = Color(0xFF0F766E),
    tertiaryContainer = Color(0xFFDDEFEC),
    onTertiaryContainer = Color(0xFF0F766E),
    background = Color(0xFFF4F3EF),
    onBackground = Color(0xFF17191C),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF17191C),
    surfaceVariant = Color(0xFFE9E7E0),
    onSurfaceVariant = Color(0xFF5A5F66),
    surfaceContainer = Color(0xFFFFFFFF),
    // Named so nothing falls back to Material's lavender: dialogs and date pickers (High), switch tracks and
    // filled fields (Highest), sheets (Low).
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF4F3EF),
    surfaceContainerHigh = Color(0xFFFFFFFF),
    surfaceContainerHighest = Color(0xFFE9E7E0),
    outline = Color(0xFFB9B6AE),
    outlineVariant = Color(0xFFDEDCD5),
    error = Color(0xFFB42318),
    errorContainer = Color(0xFFFDE8E7),
)

internal val DarkScheme = darkColorScheme(
    primary = Color(0xFFF08A4B),
    onPrimary = Color(0xFF17191C),
    inversePrimary = Color(0xFFC2410C),
    primaryContainer = Color(0xFF3A2418),
    onPrimaryContainer = Color(0xFFFDBA74),
    secondary = Color(0xFFEDEBE6),
    onSecondary = Color(0xFF17191C),
    secondaryContainer = Color(0xFF3A2418),
    onSecondaryContainer = Color(0xFFFDBA74),
    tertiary = Color(0xFF5EEAD4),
    tertiaryContainer = Color(0xFF123B36),
    onTertiaryContainer = Color(0xFF5EEAD4),
    background = Color(0xFF121416),
    onBackground = Color(0xFFEDEBE6),
    surface = Color(0xFF1C1F23),
    onSurface = Color(0xFFEDEBE6),
    surfaceVariant = Color(0xFF24282D),
    onSurfaceVariant = Color(0xFFA5A9AF),
    surfaceContainer = Color(0xFF1C1F23),
    surfaceContainerLowest = Color(0xFF121416),
    surfaceContainerLow = Color(0xFF121416),
    surfaceContainerHigh = Color(0xFF24282D),
    surfaceContainerHighest = Color(0xFF2E3338),
    outline = Color(0xFF4A4F55),
    outlineVariant = Color(0xFF2E3338),
    error = Color(0xFFF97066),
    errorContainer = Color(0xFF3B1714),
)

/** IBM Plex Sans, bundled in `res/font`: all running text. */
val PlexSans = FontFamily(
    Font(R.font.ibm_plex_sans_regular, FontWeight.Normal),
    Font(R.font.ibm_plex_sans_medium, FontWeight.Medium),
    Font(R.font.ibm_plex_sans_semibold, FontWeight.SemiBold),
    Font(R.font.ibm_plex_sans_bold, FontWeight.Bold),
)

/** IBM Plex Mono: numbers that are read digit by digit or compared in a column (mileage, VINs, torque, dates in lists). */
val PlexMono = FontFamily(
    Font(R.font.ibm_plex_mono_regular, FontWeight.Normal),
    Font(R.font.ibm_plex_mono_medium, FontWeight.Medium),
    Font(R.font.ibm_plex_mono_semibold, FontWeight.SemiBold),
    // The design never sets mono heavier than semibold.
    Font(R.font.ibm_plex_mono_semibold, FontWeight.Bold),
)

/**
 * Material's type scale in Plex Sans. Sizes and line heights stay Material's, which the screens were
 * laid out against; titles and labels are a step heavier, as the canvas draws them.
 */
private val GloveboxTypography = Typography().run {
    copy(
        displayLarge = displayLarge.copy(fontFamily = PlexSans),
        displayMedium = displayMedium.copy(fontFamily = PlexSans),
        displaySmall = displaySmall.copy(fontFamily = PlexSans),
        headlineLarge = headlineLarge.copy(fontFamily = PlexSans),
        headlineMedium = headlineMedium.copy(fontFamily = PlexSans, fontWeight = FontWeight.SemiBold),
        headlineSmall = headlineSmall.copy(fontFamily = PlexSans, fontWeight = FontWeight.SemiBold),
        titleLarge = titleLarge.copy(fontFamily = PlexSans, fontWeight = FontWeight.SemiBold),
        titleMedium = titleMedium.copy(fontFamily = PlexSans),
        titleSmall = titleSmall.copy(fontFamily = PlexSans),
        bodyLarge = bodyLarge.copy(fontFamily = PlexSans),
        bodyMedium = bodyMedium.copy(fontFamily = PlexSans),
        bodySmall = bodySmall.copy(fontFamily = PlexSans),
        labelLarge = labelLarge.copy(fontFamily = PlexSans, fontWeight = FontWeight.SemiBold),
        labelMedium = labelMedium.copy(fontFamily = PlexSans),
        labelSmall = labelSmall.copy(fontFamily = PlexSans),
    )
}

/** Status colors that Material's scheme doesn't name. */
@Immutable
data class StatusColors(val overdue: Color, val soon: Color, val ok: Color)

val LocalStatusColors = staticCompositionLocalOf {
    StatusColors(overdue = Color(0xFFC2410C), soon = Color(0xFF3A3E44), ok = Color(0xFF0F766E))
}

@Composable
fun GloveboxTheme(mode: ThemeMode = ThemeMode.SYSTEM, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(colorScheme = if (dark) DarkScheme else LightScheme, typography = GloveboxTypography) {
        CompositionLocalProvider(LocalStatusColors provides statusColors(), content = content)
    }
}

@Composable
fun statusColors(): StatusColors = statusColors(dark = MaterialTheme.colorScheme.background == DarkScheme.background)

/** The same, outside composition: the home-screen widget picks by the phone's own light or dark. */
fun statusColors(dark: Boolean): StatusColors =
    if (dark) {
        StatusColors(overdue = Color(0xFFF59E6B), soon = Color(0xFFC4C1BA), ok = Color(0xFF5EEAD4))
    } else {
        StatusColors(overdue = Color(0xFFC2410C), soon = Color(0xFF3A3E44), ok = Color(0xFF0F766E))
    }
