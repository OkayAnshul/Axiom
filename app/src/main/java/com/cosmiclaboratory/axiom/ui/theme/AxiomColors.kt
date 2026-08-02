package com.cosmiclaboratory.axiom.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * The semantic colour contract. This is the source of truth — the Material
 * [ColorScheme] is *derived* from it via [toMaterialColorScheme], never the
 * other way round, and no hex literal belongs outside this file.
 *
 * Roles are named for meaning ("ink", "hairline", "aiTint"), not for appearance,
 * so a palette swap never requires touching a call site.
 */
@Immutable
data class AxiomColors(
    // Surfaces, back to front
    val canvas: Color,
    val surface: Color,
    val surfaceRaised: Color,
    val surfaceSunken: Color,

    // Text and strokes
    val ink: Color,
    val inkMuted: Color,
    val inkFaint: Color,
    val hairline: Color,
    val outline: Color,

    // Primary interaction
    val accent: Color,
    val onAccent: Color,
    val accentSoft: Color,
    val onAccentSoft: Color,

    /**
     * Reserved for machine-authored content. Deliberately a different hue from
     * [accent]: on a private journal the user must never have to guess whether
     * they wrote something or a model did.
     */
    val aiTint: Color,
    val aiTintSoft: Color,
    val onAiTintSoft: Color,

    // Status
    val positive: Color,
    val positiveSoft: Color,
    val caution: Color,
    val cautionSoft: Color,
    val critical: Color,
    val criticalSoft: Color,
    val onCritical: Color,

    // Content accents
    val highlight: Color,
    val focusRing: Color,
    val scrim: Color,

    /** Mood ramp, 1 (rough) to 5 (great). Diverging, not a single-hue gradient. */
    val mood1: Color,
    val mood2: Color,
    val mood3: Color,
    val mood4: Color,
    val mood5: Color,

    val isDark: Boolean
) {
    /** Mood colour for a 1..5 value; [inkFaint] when unset. */
    fun mood(value: Int?): Color = when (value) {
        1 -> mood1
        2 -> mood2
        3 -> mood3
        4 -> mood4
        5 -> mood5
        else -> inkFaint
    }
}

/** Warm paper. A journal is paper; the light theme should read like it. */
val PaperLightColors = AxiomColors(
    canvas = Color(0xFFFAF7F0),
    surface = Color(0xFFFFFDF8),
    surfaceRaised = Color(0xFFFFFFFF),
    surfaceSunken = Color(0xFFF1ECE1),
    ink = Color(0xFF1E1B16),
    inkMuted = Color(0xFF4F4A40),
    inkFaint = Color(0xFF6F6759),
    hairline = Color(0xFFE3DCCD),
    outline = Color(0xFF8A8171),
    accent = Color(0xFF2F6F62),
    onAccent = Color(0xFFFFFFFF),
    accentSoft = Color(0xFFCFE7DF),
    onAccentSoft = Color(0xFF0C2B25),
    aiTint = Color(0xFF6A4FA3),
    aiTintSoft = Color(0xFFE9E2F6),
    onAiTintSoft = Color(0xFF2A1B4D),
    positive = Color(0xFF3B6E3F),
    positiveSoft = Color(0xFFD9EBD8),
    caution = Color(0xFF8A6212),
    cautionSoft = Color(0xFFF7E7C3),
    critical = Color(0xFFA32A22),
    criticalSoft = Color(0xFFF8DDD9),
    onCritical = Color(0xFFFFFFFF),
    highlight = Color(0xFFFBE9A7),
    focusRing = Color(0xFF2F6F62),
    scrim = Color(0x661E1B16),
    mood1 = Color(0xFFB4564B),
    mood2 = Color(0xFFBD7C39),
    mood3 = Color(0xFF948973),
    mood4 = Color(0xFF6B9560),
    mood5 = Color(0xFF3F8F6F),
    isDark = false
)

/** True black, for OLED and for writing at night. */
val OledDarkColors = AxiomColors(
    canvas = Color(0xFF000000),
    surface = Color(0xFF0B0B0C),
    surfaceRaised = Color(0xFF141416),
    surfaceSunken = Color(0xFF050506),
    ink = Color(0xFFEDE8DE),
    inkMuted = Color(0xFFA9A296),
    inkFaint = Color(0xFF827C71),
    hairline = Color(0xFF232326),
    outline = Color(0xFF5D5D62),
    accent = Color(0xFF7BD3BE),
    onAccent = Color(0xFF00201A),
    accentSoft = Color(0xFF123830),
    onAccentSoft = Color(0xFFB8EEE0),
    aiTint = Color(0xFFB79BFF),
    aiTintSoft = Color(0xFF221A38),
    onAiTintSoft = Color(0xFFD9CBFF),
    positive = Color(0xFF7FC98A),
    positiveSoft = Color(0xFF12301A),
    caution = Color(0xFFE3B458),
    cautionSoft = Color(0xFF332608),
    critical = Color(0xFFFF8D82),
    criticalSoft = Color(0xFF3A1310),
    onCritical = Color(0xFF3A0906),
    highlight = Color(0xFF4A3D12),
    focusRing = Color(0xFF7BD3BE),
    scrim = Color(0x99000000),
    mood1 = Color(0xFFE08A80),
    mood2 = Color(0xFFE0AE76),
    mood3 = Color(0xFF9C958A),
    mood4 = Color(0xFF8FC383),
    mood5 = Color(0xFF6FD3B0),
    isDark = true
)

/**
 * Softer dark for people who find true black harsh. Exists so ThemeMode.DARK
 * and ThemeMode.AMOLED are genuinely different — previously both resolved to
 * the same scheme, which made the setting a lie.
 */
val SoftDarkColors = OledDarkColors.copy(
    canvas = Color(0xFF121214),
    surface = Color(0xFF1A1A1D),
    surfaceRaised = Color(0xFF232327),
    surfaceSunken = Color(0xFF0E0E10),
    hairline = Color(0xFF2E2E33)
)

/**
 * Projects the semantic palette onto Material's scheme so stock M3 components
 * inherit correctly.
 *
 * The `surfaceContainer*` ramp is not optional: M3 1.4's NavigationBar, Card,
 * ModalBottomSheet and SearchBar read those roles rather than `surface`, and
 * leaving them at their defaults is exactly how M3 components end up looking
 * foreign inside a custom design language.
 */
fun AxiomColors.toMaterialColorScheme(): ColorScheme {
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = accent,
        onPrimary = onAccent,
        primaryContainer = accentSoft,
        onPrimaryContainer = onAccentSoft,
        inversePrimary = accentSoft,

        // Secondary carries the AI identity, so anything tagged "AI" can simply
        // use the secondary role and stay correct across palettes.
        secondary = aiTint,
        onSecondary = if (isDark) Color(0xFF1B1035) else Color(0xFFFFFFFF),
        secondaryContainer = aiTintSoft,
        onSecondaryContainer = onAiTintSoft,

        tertiary = caution,
        onTertiary = if (isDark) Color(0xFF2A1D05) else Color(0xFFFFFFFF),
        tertiaryContainer = cautionSoft,
        onTertiaryContainer = if (isDark) Color(0xFFF6DFB0) else Color(0xFF3A2A08),

        error = critical,
        onError = onCritical,
        errorContainer = criticalSoft,
        onErrorContainer = if (isDark) Color(0xFFFFD9D4) else Color(0xFF4A0F0B),

        background = canvas,
        onBackground = ink,
        surface = surface,
        onSurface = ink,
        surfaceVariant = surfaceSunken,
        onSurfaceVariant = inkMuted,

        surfaceContainerLowest = canvas,
        surfaceContainerLow = surfaceSunken,
        surfaceContainer = surface,
        surfaceContainerHigh = surfaceRaised,
        surfaceContainerHighest = surfaceRaised,
        surfaceBright = if (isDark) surfaceRaised else surfaceRaised,
        surfaceDim = if (isDark) canvas else surfaceSunken,

        outline = outline,
        outlineVariant = hairline,
        scrim = scrim,

        inverseSurface = ink,
        inverseOnSurface = canvas,

        // Neutralises M3's tonal-elevation tint, which would otherwise lift
        // true black to grey the moment anything claims elevation.
        surfaceTint = surface
    )
}
