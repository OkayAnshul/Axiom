package com.cosmiclaboratory.axiom.ui.design

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Immutable

/**
 * Motion tokens over core [spring].
 *
 * We do NOT use M3's MotionScheme: it only exists in material3 1.5.0-alpha, and
 * we are on 1.4.0 stable. That is no real loss — MotionScheme is a thin wrapper
 * over the same springs, and a custom design language wants its own motion
 * identity rather than Material's defaults.
 *
 * Springs rather than tweens because spring physics are interruptible: a card
 * grabbed mid-animation continues from its current velocity instead of jumping.
 * Durations below are approximate settling times, not configured values.
 *
 * The split matters: SPATIAL things (position, size, bounds) may overshoot,
 * because real objects have momentum. EFFECTS (alpha, colour) must never
 * overshoot — 110% opacity is meaningless and reads as a flicker.
 */
@Immutable
data class AxiomMotion(
    /** ~150ms. Presses, toggles, chip selection. */
    val spatialQuick: SpringSpec<Float> = spring(
        dampingRatio = 0.90f,
        stiffness = 1400f
    ),
    /** ~250ms. List item enter, card expand, chip reflow. The default. */
    val spatialStandard: SpringSpec<Float> = spring(
        dampingRatio = 0.85f,
        stiffness = 700f
    ),
    /** ~450ms with slight overshoot. Shared elements, sheets, mood selection. */
    val spatialExpressive: SpringSpec<Float> = spring(
        dampingRatio = 0.70f,
        stiffness = 380f
    ),
    /** ~600ms, critically damped. Page-level containers; never bounces. */
    val spatialSlow: SpringSpec<Float> = spring(
        dampingRatio = 1.0f,
        stiffness = 220f
    ),
    /** Fades and colour, fast. */
    val effectsQuick: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 1600f
    ),
    /** Fades and colour, standard. */
    val effectsStandard: SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 800f
    ),
    /** True when the platform has animations disabled; see [reduced]. */
    val isReduced: Boolean = false
) {
    /**
     * Typed spring for non-Float animations (Dp, IntOffset, Color, Rect).
     * [spring] is generic but the visibility threshold differs per type, so let
     * Compose supply it rather than hardcoding one.
     */
    fun <T> spatial(kind: Kind = Kind.Standard): AnimationSpec<T> =
        if (isReduced) snap() else when (kind) {
            Kind.Quick -> spring(dampingRatio = 0.90f, stiffness = 1400f)
            Kind.Standard -> spring(dampingRatio = 0.85f, stiffness = 700f)
            Kind.Expressive -> spring(dampingRatio = 0.70f, stiffness = 380f)
            Kind.Slow -> spring(dampingRatio = 1.0f, stiffness = 220f)
        }

    fun <T> finiteSpatial(kind: Kind = Kind.Standard): FiniteAnimationSpec<T> =
        if (isReduced) snap() else when (kind) {
            Kind.Quick -> spring(dampingRatio = 0.90f, stiffness = 1400f)
            Kind.Standard -> spring(dampingRatio = 0.85f, stiffness = 700f)
            Kind.Expressive -> spring(dampingRatio = 0.70f, stiffness = 380f)
            Kind.Slow -> spring(dampingRatio = 1.0f, stiffness = 220f)
        }

    enum class Kind { Quick, Standard, Expressive, Slow }

    companion object {
        /**
         * Every spatial spec collapses to [snap]; only fades survive. Provided
         * once by AxiomTheme when the platform animator scale is 0, so no screen
         * ever has to branch on the accessibility setting itself.
         */
        val reduced = AxiomMotion(
            spatialQuick = snapSpring(),
            spatialStandard = snapSpring(),
            spatialExpressive = snapSpring(),
            spatialSlow = snapSpring(),
            effectsQuick = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 3000f),
            effectsStandard = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 3000f),
            isReduced = true
        )

        /** Stiff enough to be visually instant while remaining a spring. */
        private fun snapSpring(): SpringSpec<Float> =
            spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 10_000f)
    }
}
