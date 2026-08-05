package com.cosmiclaboratory.axiom.ui.design.components

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cosmiclaboratory.axiom.ui.theme.AxiomTheme
import kotlin.math.PI
import kotlin.math.sin

/**
 * Listening, drawn as breathing.
 *
 * The previous treatment scaled a microphone icon on a 700ms loop and tinted it
 * `critical` — a red thing throbbing at you, which is the visual language of a
 * recording light or an error, not of someone paying attention. These bars rise
 * and fall on the shared breath cycle in warm accent instead.
 *
 * This is decoration over a state that is already announced in words by the
 * control that toggles it, so it carries a single content description and no
 * live region — a waveform narrated per frame would be unusable.
 */
@Composable
fun BreathingWaveform(
    modifier: Modifier = Modifier,
    barCount: Int = 5,
    maxHeight: Dp = 18.dp
) {
    val c = AxiomTheme.colors
    val motion = AxiomTheme.motion
    val transition = rememberInfiniteTransition(label = "waveform")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = motion.breathe,
        label = "waveform-phase"
    )

    Row(
        modifier = modifier
            .heightIn(min = maxHeight)
            .semantics { contentDescription = "Listening" },
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(barCount) { i ->
            // One breath sampled at staggered offsets, so the row undulates as a
            // single body of air rather than as independent meters.
            val offset = i.toFloat() / barCount
            val swell = if (motion.isReduced) {
                0.5f
            } else {
                (sin((phase + offset) * 2f * PI.toFloat()) + 1f) / 2f
            }
            val height = maxHeight * (0.28f + 0.72f * swell)
            Box(
                Modifier
                    .width(3.dp)
                    .height(height)
                    .clip(AxiomTheme.shapes.full)
                    .background(c.accent.copy(alpha = 0.45f + 0.4f * swell))
            )
        }
    }
}
