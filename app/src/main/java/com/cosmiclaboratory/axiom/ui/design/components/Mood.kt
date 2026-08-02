package com.cosmiclaboratory.axiom.ui.design.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cosmiclaboratory.axiom.ui.design.AxiomDimens
import com.cosmiclaboratory.axiom.ui.theme.AxiomTheme

/** Human labels for the 1..5 ramp. Words, not just faces — faces are ambiguous. */
val MoodLabels = listOf("Rough", "Low", "Okay", "Good", "Great")

fun moodLabel(value: Int?): String =
    value?.let { MoodLabels.getOrNull(it - 1) } ?: "Not set"

/** A single mood indicator. Decorative unless a [contentDescription] is given. */
@Composable
fun MoodDot(
    mood: Int?,
    modifier: Modifier = Modifier,
    size: Dp = 12.dp,
    color: Color? = null
) {
    val fill = color ?: AxiomTheme.colors.mood(mood)
    Box(
        modifier
            .size(size)
            .clip(AxiomTheme.shapes.full)
            .background(fill)
    )
}

/**
 * Five one-tap targets. Labelled, 56dp, with stateDescription so TalkBack
 * announces "Good, 4 of 5" rather than an anonymous selected dot.
 *
 * Sub-minute capture is the whole design goal here (Daylio's lesson): one tap
 * records a mood, with no navigation and no confirmation step.
 */
@Composable
fun MoodPicker(
    value: Int?,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val haptics = LocalHapticFeedback.current
    val target = if (compact) 44.dp else AxiomDimens.MoodTargetSize

    Row(
        modifier = modifier.selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(AxiomTheme.space.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        (1..5).forEach { level ->
            val selected = value == level
            val scale by animateFloatAsState(
                targetValue = if (selected) 1f else 0.86f,
                animationSpec = AxiomTheme.motion.spatialExpressive,
                label = "moodScale"
            )
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .selectable(
                        selected = selected,
                        role = Role.RadioButton,
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onChange(level)
                        }
                    )
                    .semantics {
                        stateDescription =
                            "${MoodLabels[level - 1]}, $level of 5" +
                                if (selected) ", selected" else ""
                    }
                    .padding(vertical = AxiomTheme.space.xs)
            ) {
                Box(
                    Modifier
                        .size(target)
                        .scale(scale)
                        .clip(AxiomTheme.shapes.full)
                        .background(
                            if (selected) AxiomTheme.colors.mood(level)
                            else AxiomTheme.colors.mood(level).copy(alpha = 0.22f)
                        )
                        .then(
                            if (selected) Modifier.border(
                                2.dp, AxiomTheme.colors.ink.copy(alpha = 0.35f), AxiomTheme.shapes.full
                            ) else Modifier
                        )
                )
                if (!compact) {
                    Spacer(Modifier.height(AxiomTheme.space.xs))
                    Text(
                        text = MoodLabels[level - 1],
                        style = AxiomTheme.type.uiLabelSmall,
                        color = if (selected) AxiomTheme.colors.ink else AxiomTheme.colors.inkFaint
                    )
                }
            }
        }
    }
}
