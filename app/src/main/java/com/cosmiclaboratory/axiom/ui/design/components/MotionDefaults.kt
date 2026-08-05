package com.cosmiclaboratory.axiom.ui.design.components

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.cosmiclaboratory.axiom.ui.design.AxiomMotion
import com.cosmiclaboratory.axiom.ui.theme.AxiomTheme

/**
 * The house motion for anything that appears in, leaves, or moves within a list.
 *
 * Every list in the app was static: entries, memories, search results and
 * messages all appeared and vanished on a single frame. That is the most
 * noticeable roughness in an app whose lists change constantly — archiving an
 * entry made the ones below it jump up, and a deleted memory left a hole that
 * closed instantly, so an undoable action looked irreversible.
 *
 * Centralised rather than passed per call site so the lists cannot drift apart,
 * and built on the [AxiomMotion] tokens so it collapses to no motion at all when
 * the platform asks for that.
 *
 * Placement is spatial (things with position have momentum), fade is an effect
 * (opacity must never overshoot), and the fade out is quicker than the fade in:
 * a departing item should get out of the way, while an arriving one is worth
 * watching land.
 *
 * Pass `fade = false` where something else already owns the entrance — the
 * conversation animates only its newest turn on purpose, and a second fade on
 * top of that one would double it.
 */
@Composable
fun LazyItemScope.axiomItemMotion(fade: Boolean = true): Modifier {
    val motion = AxiomTheme.motion
    return Modifier.animateItem(
        fadeInSpec = if (fade) motion.effectsStandard else null,
        placementSpec = motion.finiteSpatial<IntOffset>(AxiomMotion.Kind.Standard),
        fadeOutSpec = if (fade) motion.effectsQuick else null
    )
}

/**
 * The house transition for anything that expands or collapses in place: a mood
 * picker opening, a day's transcript unfolding, a toolbar leaving focus mode.
 *
 * Six call sites were passing nothing at all and getting Compose's defaults,
 * which are tuned for Material rather than for this app — so the one thing the
 * design system exists to guarantee, that everything moves the same way, was
 * quietly not true wherever a spec was omitted.
 *
 * Size uses the slow, critically damped spring: a container that overshoots
 * pushes everything below it past its final position and back, which reads as a
 * glitch rather than as momentum.
 */
@Composable
fun axiomExpand(): EnterTransition {
    val motion = AxiomTheme.motion
    return fadeIn(motion.effectsStandard) +
        expandVertically(motion.finiteSpatial<IntSize>(AxiomMotion.Kind.Slow))
}

/** The counterpart to [axiomExpand]. Leaves faster than it arrives. */
@Composable
fun axiomCollapse(): ExitTransition {
    val motion = AxiomTheme.motion
    return fadeOut(motion.effectsQuick) +
        shrinkVertically(motion.finiteSpatial<IntSize>(AxiomMotion.Kind.Slow))
}
