package com.cosmiclaboratory.axiom.widget

import androidx.glance.color.ColorProvider
import com.cosmiclaboratory.axiom.ui.theme.NightInkColors
import com.cosmiclaboratory.axiom.ui.theme.MorningPaperColors

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
    val background = ColorProvider(day = MorningPaperColors.canvas, night = NightInkColors.canvas)
    val surface = ColorProvider(day = MorningPaperColors.surface, night = NightInkColors.surface)
    val ink = ColorProvider(day = MorningPaperColors.ink, night = NightInkColors.ink)
    val inkMuted = ColorProvider(day = MorningPaperColors.inkMuted, night = NightInkColors.inkMuted)
    val inkFaint = ColorProvider(day = MorningPaperColors.inkFaint, night = NightInkColors.inkFaint)
    val accent = ColorProvider(day = MorningPaperColors.accent, night = NightInkColors.accent)
    val onAccent = ColorProvider(day = MorningPaperColors.onAccent, night = NightInkColors.onAccent)
    val hairline = ColorProvider(day = MorningPaperColors.hairline, night = NightInkColors.hairline)
}
