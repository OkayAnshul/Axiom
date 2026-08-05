package com.cosmiclaboratory.axiom.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp as lerpColor

/**
 * The semantic colour contract. This is the source of truth — the Material
 * [ColorScheme] is *derived* from it via [toMaterialColorScheme], never the
 * other way round, and no hex literal belongs outside this file.
 *
 * Roles are named for meaning ("ink", "hairline", "aiTint"), not for appearance,
 * so a palette swap never requires touching a call site.
 *
 * "Ember and paper": warm ember for anything the user touches, dusty indigo for
 * anything a model wrote. There are five anchor palettes rather than one per
 * theme, because the light in a room changes over a day and so should this one —
 * see [AxiomLight] for how adjacent anchors blend.
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
     * they wrote something or a model did. Ember versus indigo separates further
     * than the old teal versus violet did, including for red-green deficiency.
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

/*
 * Every value below is checked against WCAG AA — 4.5:1 for text roles, 3.0:1 for
 * `outline` as a non-text boundary — on canvas, surface AND surfaceSunken, and
 * so are the 50% blends between adjacent anchors, since the adaptive palette
 * spends most of the day rendering a midpoint rather than an anchor.
 * TokenGallery renders the same checks live; keep them green when retuning.
 */

/** Morning: soft cream and gold sunlight. Anchored at 08:00. */
val MorningPaperColors = AxiomColors(
    canvas = Color(0xFFFCF8F1),
    surface = Color(0xFFFFFCF7),
    surfaceRaised = Color(0xFFFFFFFF),
    surfaceSunken = Color(0xFFF3EDE1),
    ink = Color(0xFF211D17),
    inkMuted = Color(0xFF544C41),
    inkFaint = Color(0xFF736A5D),
    hairline = Color(0xFFE8E0D1),
    outline = Color(0xFF8E8574),
    accent = Color(0xFF9C5926),
    onAccent = Color(0xFFFFFFFF),
    accentSoft = Color(0xFFF7E4D2),
    onAccentSoft = Color(0xFF4A2410),
    aiTint = Color(0xFF4E5A8C),
    aiTintSoft = Color(0xFFE4E6F2),
    onAiTintSoft = Color(0xFF242B4A),
    positive = Color(0xFF3F6B41),
    positiveSoft = Color(0xFFDCEAD9),
    caution = Color(0xFF8A6212),
    cautionSoft = Color(0xFFF7E7C3),
    critical = Color(0xFFA32A22),
    criticalSoft = Color(0xFFF8DDD9),
    onCritical = Color(0xFFFFFFFF),
    highlight = Color(0xFFFBEAB4),
    focusRing = Color(0xFF9C5926),
    scrim = Color(0x66211D17),
    mood1 = Color(0xFFB0574A),
    mood2 = Color(0xFFBE7C3E),
    mood3 = Color(0xFF96897A),
    mood4 = Color(0xFF6E9068),
    mood5 = Color(0xFF43907A),
    isDark = false
)

/** Afternoon: neutral warm-white with a cool hint. Anchored at 14:00. */
val DayPaperColors = AxiomColors(
    canvas = Color(0xFFF8F8F7),
    surface = Color(0xFFFDFDFC),
    surfaceRaised = Color(0xFFFFFFFF),
    surfaceSunken = Color(0xFFEDEEEE),
    ink = Color(0xFF1B1D1E),
    inkMuted = Color(0xFF4A4E50),
    inkFaint = Color(0xFF686C70),
    hairline = Color(0xFFE1E4E5),
    outline = Color(0xFF848A8E),
    accent = Color(0xFF8A5230),
    onAccent = Color(0xFFFFFFFF),
    accentSoft = Color(0xFFF0E2D8),
    onAccentSoft = Color(0xFF40200F),
    aiTint = Color(0xFF47548A),
    aiTintSoft = Color(0xFFE2E5F0),
    onAiTintSoft = Color(0xFF212847),
    positive = Color(0xFF3A6640),
    positiveSoft = Color(0xFFD8E8D7),
    caution = Color(0xFF7F5C14),
    cautionSoft = Color(0xFFF4E5C4),
    critical = Color(0xFF9C2A22),
    criticalSoft = Color(0xFFF6DBD7),
    onCritical = Color(0xFFFFFFFF),
    highlight = Color(0xFFF7E7AE),
    focusRing = Color(0xFF8A5230),
    scrim = Color(0x661B1D1E),
    mood1 = Color(0xFFAB564B),
    mood2 = Color(0xFFB87A3F),
    mood3 = Color(0xFF8E8A82),
    mood4 = Color(0xFF6A8E68),
    mood5 = Color(0xFF42897A),
    isDark = false
)

/** Evening: deep navy, warm grey, amber. A rainy evening. Anchored at 19:00. */
val DuskInkColors = AxiomColors(
    canvas = Color(0xFF131620),
    surface = Color(0xFF1A1E29),
    surfaceRaised = Color(0xFF232834),
    surfaceSunken = Color(0xFF0E1119),
    ink = Color(0xFFE9E4DA),
    inkMuted = Color(0xFFA8A79F),
    inkFaint = Color(0xFF84868D),
    hairline = Color(0xFF2A2F3B),
    outline = Color(0xFF676C76),
    accent = Color(0xFFE0A05C),
    onAccent = Color(0xFF2A1705),
    accentSoft = Color(0xFF3A2A18),
    onAccentSoft = Color(0xFFF5D8B4),
    aiTint = Color(0xFF9AA6E0),
    aiTintSoft = Color(0xFF1F2436),
    onAiTintSoft = Color(0xFFC9D0F2),
    positive = Color(0xFF7FC98A),
    positiveSoft = Color(0xFF16301E),
    caution = Color(0xFFE3B458),
    cautionSoft = Color(0xFF33290C),
    critical = Color(0xFFFF8D82),
    criticalSoft = Color(0xFF3A1614),
    onCritical = Color(0xFF3A0906),
    highlight = Color(0xFF453A18),
    focusRing = Color(0xFFE0A05C),
    scrim = Color(0x99070910),
    mood1 = Color(0xFFE39084),
    mood2 = Color(0xFFE3AE74),
    mood3 = Color(0xFF9E968A),
    mood4 = Color(0xFF93C288),
    mood5 = Color(0xFF6FD0B2),
    isDark = true
)

/**
 * Night: warm near-black and dim amber. Anchored at 00:00.
 *
 * Not pure black — a journal read in the dark should feel like a lamp, not a
 * void. True black is available deliberately, as [MidnightOledColors].
 */
val NightInkColors = AxiomColors(
    canvas = Color(0xFF0D0C0B),
    surface = Color(0xFF151312),
    surfaceRaised = Color(0xFF1E1B19),
    surfaceSunken = Color(0xFF070606),
    ink = Color(0xFFEDE6D9),
    inkMuted = Color(0xFFA79E90),
    inkFaint = Color(0xFF857D71),
    hairline = Color(0xFF26231F),
    outline = Color(0xFF69635B),
    accent = Color(0xFFD99A5B),
    onAccent = Color(0xFF2A1705),
    accentSoft = Color(0xFF33261A),
    onAccentSoft = Color(0xFFF2D6B2),
    aiTint = Color(0xFF9BA4DC),
    aiTintSoft = Color(0xFF1D1E2A),
    onAiTintSoft = Color(0xFFC8CDEE),
    positive = Color(0xFF7BC486),
    positiveSoft = Color(0xFF142B18),
    caution = Color(0xFFDFB055),
    cautionSoft = Color(0xFF2E2409),
    critical = Color(0xFFFA8B80),
    criticalSoft = Color(0xFF331311),
    onCritical = Color(0xFF330805),
    highlight = Color(0xFF40360F),
    focusRing = Color(0xFFD99A5B),
    scrim = Color(0x99000000),
    mood1 = Color(0xFFE08D81),
    mood2 = Color(0xFFE0AB71),
    mood3 = Color(0xFF9C9488),
    mood4 = Color(0xFF90BF85),
    mood5 = Color(0xFF6CCDAF),
    isDark = true
)

/**
 * True black, for OLED panels where an unlit pixel costs nothing. Keeps Night's
 * warm ink and amber so the identity survives; only the substrate goes black.
 */
val MidnightOledColors = NightInkColors.copy(
    canvas = Color(0xFF000000),
    surface = Color(0xFF0A0908),
    surfaceRaised = Color(0xFF141210),
    surfaceSunken = Color(0xFF000000),
    hairline = Color(0xFF1E1B18)
)

/**
 * Blends two anchors. Used only *within* a family (morning↔day, dusk↔night), so
 * a blend never crosses light into dark and contrast degrades between two
 * verified endpoints rather than through an unverified middle.
 *
 * [isDark] snaps at the halfway point rather than blending, because it drives
 * branching (shadow versus hairline elevation) that has no meaningful midpoint.
 */
fun lerp(start: AxiomColors, stop: AxiomColors, fraction: Float): AxiomColors {
    if (fraction <= 0f) return start
    if (fraction >= 1f) return stop
    fun c(a: Color, b: Color) = lerpColor(a, b, fraction)
    return AxiomColors(
        canvas = c(start.canvas, stop.canvas),
        surface = c(start.surface, stop.surface),
        surfaceRaised = c(start.surfaceRaised, stop.surfaceRaised),
        surfaceSunken = c(start.surfaceSunken, stop.surfaceSunken),
        ink = c(start.ink, stop.ink),
        inkMuted = c(start.inkMuted, stop.inkMuted),
        inkFaint = c(start.inkFaint, stop.inkFaint),
        hairline = c(start.hairline, stop.hairline),
        outline = c(start.outline, stop.outline),
        accent = c(start.accent, stop.accent),
        onAccent = c(start.onAccent, stop.onAccent),
        accentSoft = c(start.accentSoft, stop.accentSoft),
        onAccentSoft = c(start.onAccentSoft, stop.onAccentSoft),
        aiTint = c(start.aiTint, stop.aiTint),
        aiTintSoft = c(start.aiTintSoft, stop.aiTintSoft),
        onAiTintSoft = c(start.onAiTintSoft, stop.onAiTintSoft),
        positive = c(start.positive, stop.positive),
        positiveSoft = c(start.positiveSoft, stop.positiveSoft),
        caution = c(start.caution, stop.caution),
        cautionSoft = c(start.cautionSoft, stop.cautionSoft),
        critical = c(start.critical, stop.critical),
        criticalSoft = c(start.criticalSoft, stop.criticalSoft),
        onCritical = c(start.onCritical, stop.onCritical),
        highlight = c(start.highlight, stop.highlight),
        focusRing = c(start.focusRing, stop.focusRing),
        scrim = c(start.scrim, stop.scrim),
        mood1 = c(start.mood1, stop.mood1),
        mood2 = c(start.mood2, stop.mood2),
        mood3 = c(start.mood3, stop.mood3),
        mood4 = c(start.mood4, stop.mood4),
        mood5 = c(start.mood5, stop.mood5),
        isDark = if (fraction < 0.5f) start.isDark else stop.isDark
    )
}

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
        onSecondary = if (isDark) onAiTintSoft else Color(0xFFFFFFFF),
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
        surfaceBright = surfaceRaised,
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
