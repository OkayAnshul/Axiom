package com.cosmiclaboratory.axiom.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime
import kotlin.math.pow

/**
 * The adaptive palette renders a *blend* of two anchors for most of the day, and
 * interpolating between two AA-passing colours does not guarantee an AA-passing
 * midpoint. Previews catch that only if a human looks at the right one, so the
 * invariant is asserted here instead — at every quarter hour, in every mode.
 */
class AxiomLightTest {

    // ---- WCAG 2.1 relative luminance and contrast ---------------------------

    private fun channel(v: Float): Double {
        val c = v.toDouble()
        return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
    }

    private fun luminance(color: Color): Double =
        0.2126 * channel(color.red) + 0.7152 * channel(color.green) + 0.0722 * channel(color.blue)

    private fun contrast(a: Color, b: Color): Double {
        val la = luminance(a)
        val lb = luminance(b)
        val hi = maxOf(la, lb)
        val lo = minOf(la, lb)
        return (hi + 0.05) / (lo + 0.05)
    }

    /** Text pairings, at AA's 4.5:1. */
    private fun textPairs(c: AxiomColors) = listOf(
        Triple("ink on canvas", c.ink, c.canvas),
        Triple("ink on surface", c.ink, c.surface),
        Triple("inkMuted on canvas", c.inkMuted, c.canvas),
        Triple("inkMuted on surface", c.inkMuted, c.surface),
        Triple("inkFaint on canvas", c.inkFaint, c.canvas),
        Triple("inkFaint on surface", c.inkFaint, c.surface),
        Triple("inkFaint on surfaceSunken", c.inkFaint, c.surfaceSunken),
        Triple("accent on canvas", c.accent, c.canvas),
        Triple("accent on surface", c.accent, c.surface),
        Triple("aiTint on canvas", c.aiTint, c.canvas),
        Triple("onAccent on accent", c.onAccent, c.accent),
        Triple("onAccentSoft on accentSoft", c.onAccentSoft, c.accentSoft),
        Triple("onAiTintSoft on aiTintSoft", c.onAiTintSoft, c.aiTintSoft),
        Triple("positive on canvas", c.positive, c.canvas),
        Triple("caution on canvas", c.caution, c.canvas),
        Triple("critical on canvas", c.critical, c.canvas)
    )

    /** Non-text boundaries, at AA's 3:1. */
    private fun uiPairs(c: AxiomColors) = listOf(
        Triple("outline on canvas", c.outline, c.canvas),
        Triple("outline on surface", c.outline, c.surface)
    )

    private fun assertAccessible(label: String, c: AxiomColors) {
        textPairs(c).forEach { (name, fg, bg) ->
            val ratio = contrast(fg, bg)
            assertTrue(
                "$label: $name is %.2f:1, below AA 4.5:1".format(ratio),
                ratio >= 4.5
            )
        }
        uiPairs(c).forEach { (name, fg, bg) ->
            val ratio = contrast(fg, bg)
            assertTrue(
                "$label: $name is %.2f:1, below AA 3:1".format(ratio),
                ratio >= 3.0
            )
        }
    }

    private fun everyQuarterHour(): List<LocalTime> =
        (0 until 24 * 4).map { LocalTime.of(it / 4, (it % 4) * 15) }

    // ---- the anchors themselves ---------------------------------------------

    @Test
    fun `every anchor palette meets AA`() {
        assertAccessible("Morning", MorningPaperColors)
        assertAccessible("Day", DayPaperColors)
        assertAccessible("Dusk", DuskInkColors)
        assertAccessible("Night", NightInkColors)
        assertAccessible("MidnightOled", MidnightOledColors)
    }

    // ---- and every blend the clock can actually produce ----------------------

    @Test
    fun `every blended palette meets AA at every quarter hour`() {
        everyQuarterHour().forEach { time ->
            listOf(ThemeMode.LIGHT, ThemeMode.DARK).forEach { mode ->
                val palette = AxiomLight.paletteFor(
                    mode = mode,
                    systemDark = false,
                    adaptive = true,
                    time = time
                )
                assertAccessible("$mode at $time", palette)
            }
        }
    }

    // ---- family isolation ----------------------------------------------------

    @Test
    fun `light mode never drifts into a dark palette`() {
        everyQuarterHour().forEach { time ->
            val palette = AxiomLight.paletteFor(ThemeMode.LIGHT, false, true, time)
            assertTrue("LIGHT went dark at $time", !palette.isDark)
        }
    }

    @Test
    fun `dark mode never drifts into a light palette`() {
        everyQuarterHour().forEach { time ->
            val palette = AxiomLight.paletteFor(ThemeMode.DARK, false, true, time)
            assertTrue("DARK went light at $time", palette.isDark)
        }
    }

    @Test
    fun `amoled is true black at every hour`() {
        everyQuarterHour().forEach { time ->
            val palette = AxiomLight.paletteFor(ThemeMode.AMOLED, false, true, time)
            assertEquals("AMOLED drifted at $time", MidnightOledColors, palette)
        }
    }

    @Test
    fun `system mode follows the OS, then blends inside it`() {
        val noon = LocalTime.of(12, 0)
        assertTrue(AxiomLight.paletteFor(ThemeMode.SYSTEM, systemDark = true, true, noon).isDark)
        assertTrue(!AxiomLight.paletteFor(ThemeMode.SYSTEM, systemDark = false, true, noon).isDark)
    }

    // ---- the opt-out ---------------------------------------------------------

    @Test
    fun `adaptive off pins one palette for the whole day`() {
        val light = everyQuarterHour().map {
            AxiomLight.paletteFor(ThemeMode.LIGHT, false, adaptive = false, time = it)
        }.distinct()
        val dark = everyQuarterHour().map {
            AxiomLight.paletteFor(ThemeMode.DARK, false, adaptive = false, time = it)
        }.distinct()
        assertEquals(listOf(MorningPaperColors), light)
        assertEquals(listOf(DuskInkColors), dark)
    }

    // ---- the ramps -----------------------------------------------------------

    @Test
    fun `light blend is warm at breakfast, neutral mid-afternoon, warm again by evening`() {
        assertEquals(0f, AxiomLight.lightBlend(8 * 60), 0.001f)
        assertEquals(1f, AxiomLight.lightBlend(14 * 60), 0.001f)
        assertEquals(0f, AxiomLight.lightBlend(20 * 60), 0.001f)
        // Genuinely partway between, not snapped to an endpoint.
        val lateMorning = AxiomLight.lightBlend(11 * 60)
        assertTrue("11:00 should be mid-blend, was $lateMorning", lateMorning > 0.4f && lateMorning < 0.6f)
    }

    @Test
    fun `dark blend sinks to night overnight and lifts by day`() {
        assertEquals(0f, AxiomLight.darkBlend(12 * 60), 0.001f)
        assertEquals(1f, AxiomLight.darkBlend(0), 0.001f)
        assertEquals(1f, AxiomLight.darkBlend(23 * 60), 0.001f)
    }

    @Test
    fun `blend endpoints return the anchors themselves`() {
        assertEquals(MorningPaperColors, lerp(MorningPaperColors, DayPaperColors, 0f))
        assertEquals(DayPaperColors, lerp(MorningPaperColors, DayPaperColors, 1f))
    }
}
