package com.cosmiclaboratory.axiom.widget

import androidx.glance.color.ColorProvider
import com.cosmiclaboratory.axiom.ui.theme.OledDarkColors
import com.cosmiclaboratory.axiom.ui.theme.PaperLightColors

/**
 * Widget colours, fed from the SAME AxiomColors source as the app.
 *
 * The widgets previously hardcoded Color(0xFF1976D2) and Color(0xFFF5F5F5) — a
 * blue and a near-white belonging to no palette in the app, and effectively
 * invisible on a dark home screen. Every role below resolves day/night from the
 * system, so dark mode works and a palette change lands everywhere at once.
 *
 * These are used directly rather than through GlanceTheme's ColorProviders,
 * because widgets only need a handful of roles and naming them explicitly makes
 * the mapping obvious at each call site.
 */
object WidgetColors {
    val background = ColorProvider(day = PaperLightColors.canvas, night = OledDarkColors.canvas)
    val surface = ColorProvider(day = PaperLightColors.surface, night = OledDarkColors.surface)
    val ink = ColorProvider(day = PaperLightColors.ink, night = OledDarkColors.ink)
    val inkMuted = ColorProvider(day = PaperLightColors.inkMuted, night = OledDarkColors.inkMuted)
    val inkFaint = ColorProvider(day = PaperLightColors.inkFaint, night = OledDarkColors.inkFaint)
    val accent = ColorProvider(day = PaperLightColors.accent, night = OledDarkColors.accent)
    val onAccent = ColorProvider(day = PaperLightColors.onAccent, night = OledDarkColors.onAccent)
    val hairline = ColorProvider(day = PaperLightColors.hairline, night = OledDarkColors.hairline)
}
