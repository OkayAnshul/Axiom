package com.cosmiclaboratory.axiom.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = AxiomBlue80,
    onPrimary = Color.Black,
    secondary = AxiomAmber80,
    onSecondary = Color.Black,
    tertiary = AxiomGray80,
    onTertiary = Color.Black,
    background = AxiomBlue20,  // Pure black (#000000)
    onBackground = AxiomGray90,
    surface = Color(0xFF0A0A0A),  // Very subtle elevation from pure black
    onSurface = AxiomGray90,
    surfaceVariant = Color(0xFF121212),  // Slightly elevated for input fields
    onSurfaceVariant = AxiomGray80,
    outline = Color(0xFF404040),  // Enhanced borders for precise separation
    outlineVariant = Color(0xFF2A2A2A),  // Secondary borders and dividers
    inverseOnSurface = Color.Black,
    inverseSurface = AxiomGray90,
    inversePrimary = AxiomBlue40
)

private val LightColorScheme = lightColorScheme(
    primary = AxiomBlue60,
    onPrimary = Color.White,
    secondary = AxiomAmber60,
    onSecondary = Color.White,
    tertiary = AxiomGray60,
    onTertiary = Color.White,
    background = AxiomGray90,
    onBackground = AxiomGray20,
    surface = Color.White,
    onSurface = AxiomGray20,
    surfaceVariant = AxiomGray80,
    onSurfaceVariant = AxiomGray40,
    outline = AxiomGray60,
    inverseOnSurface = AxiomGray90,
    inverseSurface = AxiomGray20,
    inversePrimary = AxiomBlue80,
    // Additional colors for better note-taking experience
    error = AxiomRed,
    onError = Color.White,
    errorContainer = Color(0xFFFFEDEA),
    onErrorContainer = Color(0xFF410E0B)
)

@Composable
fun AxiomTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}