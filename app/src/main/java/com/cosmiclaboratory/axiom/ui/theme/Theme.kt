package com.cosmiclaboratory.axiom.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = AxiomCyan,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF063B37),
    onPrimaryContainer = AxiomIvory,
    secondary = AxiomSun,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF352708),
    onSecondaryContainer = AxiomIvory,
    tertiary = AxiomSage,
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFF17351D),
    onTertiaryContainer = AxiomIvory,
    background = AxiomOledBlack,
    onBackground = AxiomIvory,
    surface = AxiomGraphite,
    onSurface = AxiomIvory,
    surfaceVariant = AxiomGraphiteRaised,
    onSurfaceVariant = AxiomMist,
    outline = Color(0xFF2A3337),
    outlineVariant = Color(0xFF182126),
    inverseOnSurface = Color.Black,
    inverseSurface = AxiomIvory,
    inversePrimary = Color(0xFF006A60),
    error = AxiomRose,
    onError = Color.Black,
    errorContainer = Color(0xFF4A111C),
    onErrorContainer = AxiomIvory
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF006A60),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFBDF4EA),
    onPrimaryContainer = Color(0xFF00201C),
    secondary = Color(0xFF755B00),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE08B),
    onSecondaryContainer = Color(0xFF241A00),
    tertiary = Color(0xFF386A42),
    onTertiary = Color.White,
    background = Color(0xFFF8F6F0),
    onBackground = Color(0xFF171C1F),
    surface = Color(0xFFFFFBF4),
    onSurface = Color(0xFF171C1F),
    surfaceVariant = Color(0xFFE2E8E6),
    onSurfaceVariant = Color(0xFF424B4A),
    outline = Color(0xFF727B7A),
    inverseOnSurface = Color(0xFFF8F6F0),
    inverseSurface = Color(0xFF2C3133),
    inversePrimary = AxiomCyan,
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410E0B)
)

/**
 * User-selectable theme. Storage values match [UserPreferences.themeOverride].
 *
 * AMOLED currently resolves to the same scheme as DARK because [DarkColorScheme] is
 * already built on AxiomOledBlack (#000000). It is kept as a distinct mode so a
 * future softer-dark variant can be added without another preferences migration.
 */
enum class ThemeMode(val storageValue: Int) {
    SYSTEM(-1),
    LIGHT(0),
    DARK(1),
    AMOLED(2);

    companion object {
        fun fromStorage(value: Int): ThemeMode =
            entries.firstOrNull { it.storageValue == value } ?: SYSTEM
    }
}

/**
 * The *resolved* dark state, after the user's override has been applied.
 *
 * Components must read this rather than calling [isSystemInDarkTheme] directly —
 * that returns the OS setting and ignores the override, which is what previously
 * painted a light formatting toolbar on top of a forced-dark editor.
 */
val LocalAxiomDarkTheme = staticCompositionLocalOf { true }

@Composable
fun AxiomTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.AMOLED -> true
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    CompositionLocalProvider(LocalAxiomDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
