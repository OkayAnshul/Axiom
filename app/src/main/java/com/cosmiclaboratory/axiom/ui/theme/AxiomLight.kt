package com.cosmiclaboratory.axiom.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import kotlinx.coroutines.delay
import java.time.LocalTime

/**
 * The light in the room, over a day.
 *
 * Axiom has five anchor palettes rather than one per theme. Two belong to the
 * light family (morning cream, afternoon neutral) and two to the dark family
 * (dusk navy, night near-black); [MidnightOledColors] stands outside as a
 * deliberate true-black choice. Through the day the resolved palette drifts
 * between the two anchors of whichever family is active, so the app warms toward
 * evening the way a room does.
 *
 * A blend never crosses light into dark. Crossing families is what the theme
 * setting is for, and it keeps every rendered palette a midpoint between two
 * contrast-verified endpoints.
 *
 * ## Why this is quantised rather than animated
 *
 * [LocalAxiomColors] is a `staticCompositionLocalOf`, which does not track reads:
 * changing it recomposes the entire tree beneath the provider. That is the right
 * behaviour for a theme, but it means the palette must never be driven by
 * `animateColorAsState` or anything else that produces a new value per frame —
 * that would recompose every screen, sixty times a second, forever.
 *
 * So the clock is sampled on a [TICK_MINUTES]-minute tick and the blend fraction
 * is snapped to [BLEND_STEPS]. A whole-tree recomposition a few times an hour is
 * free. Because [AxiomColors] is a data class, an unchanged blend produces a
 * structurally equal instance and Compose skips the invalidation entirely.
 *
 * If a visibly smooth transition is ever wanted, crossfade a scrim over the
 * content. Do not animate these tokens.
 */
object AxiomLight {

    /** How often the clock is resampled. */
    const val TICK_MINUTES = 6L

    /** Blend fractions snap to this many steps, so tiny clock drift is inert. */
    private const val BLEND_STEPS = 32

    private const val MINUTES_PER_DAY = 24 * 60

    /**
     * Light family, 0 = [MorningPaperColors] to 1 = [DayPaperColors].
     *
     * Warm at breakfast, neutral through the working afternoon, warm again by
     * evening. The return trip is what stops a light theme feeling like an
     * office at 9pm.
     */
    internal fun lightBlend(minuteOfDay: Int): Float = ramp(
        minuteOfDay,
        listOf(
            0 to 0f,
            8 * 60 to 0f,
            14 * 60 to 1f,
            20 * 60 to 0f,
            MINUTES_PER_DAY to 0f
        )
    )

    /**
     * Dark family, 0 = [DuskInkColors] to 1 = [NightInkColors].
     *
     * Navy through the day and early evening, sinking to near-black overnight,
     * lifting again before dawn.
     */
    internal fun darkBlend(minuteOfDay: Int): Float = ramp(
        minuteOfDay,
        listOf(
            0 to 1f,
            3 * 60 to 1f,
            6 * 60 to 0f,
            17 * 60 to 0f,
            23 * 60 to 1f,
            MINUTES_PER_DAY to 1f
        )
    )

    /** Piecewise-linear interpolation over (minute, value) keypoints. */
    private fun ramp(minuteOfDay: Int, points: List<Pair<Int, Float>>): Float {
        val m = minuteOfDay.coerceIn(0, MINUTES_PER_DAY)
        for (i in 0 until points.lastIndex) {
            val (m0, v0) = points[i]
            val (m1, v1) = points[i + 1]
            if (m in m0..m1) {
                if (m1 == m0) return v1
                val t = (m - m0).toFloat() / (m1 - m0)
                return v0 + (v1 - v0) * t
            }
        }
        return points.last().second
    }

    private fun quantise(fraction: Float): Float =
        Math.round(fraction.coerceIn(0f, 1f) * BLEND_STEPS).toFloat() / BLEND_STEPS

    /**
     * The palette for a given theme choice and moment.
     *
     * [time] is a parameter rather than an internal `LocalTime.now()` so previews
     * and tests can pin an hour without mocking the system clock.
     */
    fun paletteFor(
        mode: ThemeMode,
        systemDark: Boolean,
        adaptive: Boolean,
        time: LocalTime
    ): AxiomColors {
        // True black is a deliberate choice about the panel, not about the hour.
        if (mode == ThemeMode.AMOLED) return MidnightOledColors

        val dark = when (mode) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
            ThemeMode.AMOLED -> true
            ThemeMode.SYSTEM -> systemDark
        }

        if (!adaptive) {
            // The previous fixed identities: warm paper by day, dusk by night.
            return if (dark) DuskInkColors else MorningPaperColors
        }

        val minuteOfDay = time.hour * 60 + time.minute
        return if (dark) {
            lerp(DuskInkColors, NightInkColors, quantise(darkBlend(minuteOfDay)))
        } else {
            lerp(MorningPaperColors, DayPaperColors, quantise(lightBlend(minuteOfDay)))
        }
    }

    /** The current [DayPhase], for copy that wants to name the time of day. */
    fun phaseAt(time: LocalTime): DayPhase = when (time.hour) {
        in 5..10 -> DayPhase.Morning
        in 11..16 -> DayPhase.Day
        in 17..20 -> DayPhase.Dusk
        else -> DayPhase.Night
    }
}

/** Named times of day, for copy and for previews. */
enum class DayPhase { Morning, Day, Dusk, Night }

/**
 * Resolves the palette and keeps it current, resampling the clock every
 * [AxiomLight.TICK_MINUTES] minutes. See [AxiomLight] for why this is a slow
 * tick rather than an animation.
 */
@Composable
fun rememberAdaptivePalette(
    mode: ThemeMode,
    systemDark: Boolean,
    adaptive: Boolean,
    clock: () -> LocalTime = { LocalTime.now() }
): AxiomColors {
    val now by produceState(initialValue = clock(), mode, adaptive) {
        while (true) {
            value = clock()
            delay(AxiomLight.TICK_MINUTES * 60_000L)
        }
    }
    return AxiomLight.paletteFor(mode, systemDark, adaptive, now)
}
