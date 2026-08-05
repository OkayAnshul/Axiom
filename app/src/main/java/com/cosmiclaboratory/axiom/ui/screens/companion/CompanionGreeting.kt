package com.cosmiclaboratory.axiom.ui.screens.companion

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Segment
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import com.cosmiclaboratory.axiom.ui.design.AxiomMotion
import com.cosmiclaboratory.axiom.ui.design.components.AxiomIconButton
import com.cosmiclaboratory.axiom.ui.theme.AxiomTheme

/**
 * The first thing you see, and the only place Axiom raises its voice.
 *
 * This replaces a top bar that read "Axiom" — an app announcing itself. A name
 * and a sentence about your day do the same navigational job (you know where you
 * are) while doing an entirely different emotional one.
 *
 * It is expanded while the conversation rests at the bottom, which is where it
 * sits every time the app opens, and recedes once you scroll back into history —
 * at that point you are reading, and a greeting about *now* would be in the way.
 * The shelf glyph is the single door to everywhere else in the app; it stays put
 * through the collapse so it never becomes a moving target.
 */
@Composable
fun CompanionGreeting(
    greeting: String,
    lead: String?,
    expanded: Boolean,
    entryCount: Int,
    onOpenShelf: () -> Unit,
    onOpenJournal: () -> Unit,
    modifier: Modifier = Modifier,
    /**
     * Null when there is nothing to clear. Clearing lives here, next to the
     * conversation it clears, rather than only behind the shelf — a thread you
     * want gone is usually one you are looking at, and hunting through a menu
     * for that is the wrong amount of friction.
     */
    onClearConversation: (() -> Unit)? = null
) {
    val c = AxiomTheme.colors
    val motion = AxiomTheme.motion
    // Read from the theme, not a fresh AxiomMotion(): this is the instance that
    // collapses to snap() when the platform has animations turned off.
    val sizeSpec = motion.finiteSpatial<IntSize>(AxiomMotion.Kind.Slow)

    Column(
        modifier
            .fillMaxWidth()
            .padding(
                start = AxiomTheme.space.screenH,
                end = AxiomTheme.space.sm,
                top = AxiomTheme.space.sm
            )
            .testTag("header:greeting")
    ) {
        /*
         * The greeting shrinks rather than swapping style.
         *
         * It used to flip between `type.greeting` (Fraunces) and `type.uiTitle`
         * (Figtree) on the same frame the collapse began, so the word changed
         * typeface mid-gesture. Interpolating the two is not an option either:
         * TextStyle.lerp snaps fontFamily at the halfway point, so a serif would
         * pop to a sans in the middle of the animation. Keeping one family and
         * animating only the size gives the greeting somewhere to go without
         * ever changing what it is.
         */
        val openness by animateFloatAsState(
            targetValue = if (expanded) 1f else 0f,
            animationSpec = motion.spatialSlow,
            label = "greetingOpenness"
        )
        val base = AxiomTheme.type.greeting

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = greeting,
                style = base.copy(
                    fontSize = lerp(COLLAPSED_GREETING_SIZE, base.fontSize, openness),
                    lineHeight = lerp(COLLAPSED_GREETING_LINE, base.lineHeight, openness)
                ),
                color = c.ink,
                modifier = Modifier.weight(1f)
            )
            onClearConversation?.let { clear ->
                AxiomIconButton(
                    icon = Icons.Outlined.DeleteSweep,
                    label = "Clear this conversation",
                    onClick = clear,
                    tint = c.inkFaint
                )
            }
            AxiomIconButton(
                icon = Icons.AutoMirrored.Outlined.Segment,
                label = "Everything else",
                onClick = onOpenShelf,
                tint = c.inkFaint
            )
        }

        AnimatedVisibility(
            visible = expanded && !lead.isNullOrBlank(),
            enter = fadeIn(motion.effectsStandard) + expandVertically(sizeSpec),
            exit = fadeOut(motion.effectsQuick) + shrinkVertically(sizeSpec)
        ) {
            Column {
                Spacer(Modifier.height(AxiomTheme.space.sm))
                Text(
                    text = lead.orEmpty(),
                    style = AxiomTheme.type.greetingLead,
                    color = c.inkMuted
                )

                // What you've built, made visible again. Removing the tab bar
                // took the journal out of sight, and out of sight is the whole
                // reason a journalling app goes unused.
                Spacer(Modifier.height(AxiomTheme.space.md))
                Text(
                    text = keptPhrase(entryCount),
                    style = AxiomTheme.type.uiBodySmall,
                    color = c.accent,
                    modifier = Modifier
                        .clip(AxiomTheme.shapes.sm)
                        .clickable(onClick = onOpenJournal)
                        .padding(vertical = AxiomTheme.space.xs)
                )
            }
        }

        Spacer(Modifier.height(AxiomTheme.space.md))
    }
}

/**
 * Where the greeting lands once you scroll back into history: present, legible,
 * and no longer the loudest thing on the screen.
 */
private val COLLAPSED_GREETING_SIZE = 19.sp
private val COLLAPSED_GREETING_LINE = 26.sp

/**
 * Counting, said as a person would.
 *
 * "12 entries" is inventory. "Twelve moments you've kept" is the same number
 * describing something worth having done — and the swipe hint rides along with
 * it, because a gesture nobody is told about does not exist.
 */
private fun keptPhrase(count: Int): String = when (count) {
    0 -> "Nothing kept yet — swipe right to start your story"
    1 -> "One moment you've kept · swipe right"
    else -> "$count moments you've kept · swipe right"
}
