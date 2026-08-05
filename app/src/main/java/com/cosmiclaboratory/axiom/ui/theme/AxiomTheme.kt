package com.cosmiclaboratory.axiom.ui.theme

import android.provider.Settings
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import com.cosmiclaboratory.axiom.ui.design.AxiomMotion
import com.cosmiclaboratory.axiom.ui.design.AxiomSpacing
import java.time.LocalTime

/**
 * User-selectable theme. Storage values match `UserPreferences.themeOverride`.
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

val LocalAxiomColors = staticCompositionLocalOf { NightInkColors }
val LocalAxiomTypography = staticCompositionLocalOf { AxiomTypography() }
val LocalAxiomSpacing = staticCompositionLocalOf { AxiomSpacing() }
val LocalAxiomShapes = staticCompositionLocalOf { AxiomShapes() }
val LocalAxiomMotion = staticCompositionLocalOf { AxiomMotion() }

/**
 * The *resolved* dark state, after the user's override has been applied.
 *
 * Components must read this rather than calling [isSystemInDarkTheme] directly —
 * that returns the OS setting and ignores the override, which is what used to
 * paint a light formatting toolbar on top of a forced-dark editor.
 */
val LocalAxiomDarkTheme = staticCompositionLocalOf { true }

/** Token accessors: `AxiomTheme.colors.inkMuted`, `AxiomTheme.space.base`, … */
object AxiomTheme {
    val colors: AxiomColors
        @Composable @ReadOnlyComposable get() = LocalAxiomColors.current
    val type: AxiomTypography
        @Composable @ReadOnlyComposable get() = LocalAxiomTypography.current
    val space: AxiomSpacing
        @Composable @ReadOnlyComposable get() = LocalAxiomSpacing.current
    val shapes: AxiomShapes
        @Composable @ReadOnlyComposable get() = LocalAxiomShapes.current
    val motion: AxiomMotion
        @Composable @ReadOnlyComposable get() = LocalAxiomMotion.current
}

@Composable
fun AxiomTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    /** In-app reading-size preference, 0.9f–1.4f. Multiplies with OS font scale. */
    readingScale: Float = 1f,
    /** Let the palette drift with the time of day. See [AxiomLight]. */
    adaptiveLight: Boolean = true,
    /**
     * Injected so previews and tests can pin an hour. Production never passes
     * this; the alternative is mocking the system clock to look at a colour.
     */
    clock: () -> LocalTime = { LocalTime.now() },
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val colors = rememberAdaptivePalette(
        mode = themeMode,
        systemDark = systemDark,
        adaptive = adaptiveLight,
        clock = clock
    )

    // Resolve reduced-motion once, here, so no screen has to check it.
    val context = LocalContext.current
    val reduceMotion = remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) == 0f
        }.getOrDefault(false)
    }

    val typography = remember(readingScale) {
        AxiomTypography().scaledForReading(readingScale)
    }
    val motion = if (reduceMotion) AxiomMotion.reduced else AxiomMotion()
    val shapes = remember { AxiomShapes() }

    CompositionLocalProvider(
        LocalAxiomColors provides colors,
        LocalAxiomTypography provides typography,
        LocalAxiomSpacing provides AxiomSpacing(),
        LocalAxiomShapes provides shapes,
        LocalAxiomMotion provides motion,
        LocalAxiomDarkTheme provides colors.isDark
    ) {
        MaterialTheme(
            colorScheme = colors.toMaterialColorScheme(),
            typography = typography.toMaterialTypography(),
            shapes = shapes.toMaterialShapes(),
            content = content
        )
    }
}
