package com.cosmiclaboratory.axiom.ui.design.components

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import com.cosmiclaboratory.axiom.ui.theme.AxiomTheme

/**
 * The room, breathing.
 *
 * Two very soft pools of light — one warm from `accent`, one cool from `aiTint` —
 * drift across the canvas and swell on the breath cycle. At 3–6% alpha none of it
 * is legible as a shape; what you notice is that an empty screen does not feel
 * dead. That is the whole brief for this file.
 *
 * ## Cost
 *
 * Only local floats animate here. The theme palette is never touched, which
 * matters: [com.cosmiclaboratory.axiom.ui.theme.LocalAxiomColors] is a static
 * composition local, so anything that changed it per frame would recompose every
 * screen in the app. Drawing happens in [drawBehind] on a single root node, so
 * the cost is two gradient fills per frame against one surface — negligible next
 * to laying out a screen of text, and it does not scale with list length.
 *
 * Under reduced motion this collapses to a flat `canvas` fill.
 */
@Composable
fun AmbientCanvas(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val c = AxiomTheme.colors
    val motion = AxiomTheme.motion

    if (motion.isReduced) {
        Box(modifier.fillMaxSize().background(c.canvas), content = content)
        return
    }

    val transition = rememberInfiniteTransition(label = "ambient")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = motion.ambientDrift,
        label = "ambient-drift"
    )
    val breath by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = motion.breathe,
        label = "ambient-breath"
    )

    // Warm light is stronger in dark palettes, where there is room for it to
    // register; on paper it would read as a stain.
    val warmAlpha = if (c.isDark) 0.075f else 0.045f
    val coolAlpha = if (c.isDark) 0.055f else 0.030f

    Box(
        modifier
            .fillMaxSize()
            .background(c.canvas)
            .drawBehind {
                val w = size.width
                val h = size.height
                val breadth = maxOf(w, h)

                // The two pools travel in opposite directions so the composition
                // never settles into a single sweeping motion.
                val warmCenter = Offset(
                    x = w * (0.18f + 0.30f * drift),
                    y = h * (0.16f + 0.10f * breath)
                )
                val coolCenter = Offset(
                    x = w * (0.86f - 0.26f * drift),
                    y = h * (0.74f - 0.12f * breath)
                )
                val warmRadius = breadth * (0.62f + 0.10f * breath)
                val coolRadius = breadth * (0.58f - 0.08f * breath)

                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            c.accent.copy(alpha = warmAlpha),
                            c.accent.copy(alpha = 0f)
                        ),
                        center = warmCenter,
                        radius = warmRadius
                    )
                )
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            c.aiTint.copy(alpha = coolAlpha),
                            c.aiTint.copy(alpha = 0f)
                        ),
                        center = coolCenter,
                        radius = coolRadius
                    )
                )
            },
        content = content
    )
}
