package com.cosmiclaboratory.axiom.ui.navigation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cosmiclaboratory.axiom.ui.design.components.AxiomBottomSheet
import com.cosmiclaboratory.axiom.ui.theme.AxiomTheme

/**
 * One door to everywhere that is not the conversation.
 *
 * This replaces a three-tab bottom bar. A permanent nav bar is a claim that
 * Journal and Patterns are peers of the conversation, competing for attention at
 * all times; they are not — they are things the two of you have made, which you
 * visit occasionally. Removing the bar gives the conversation the whole screen,
 * which is the point of the app.
 *
 * Switching between the two primary surfaces is not in here: that is
 * [com.cosmiclaboratory.axiom.ui.design.components.AxiomPlaceBar]'s job, and it
 * is always on screen. This sheet holds the rooms off them.
 *
 * Discoverability of the rest is preserved the way the old nav bar preserved it:
 * every row is labelled, and each carries a sentence saying what is behind it.
 * The failure mode of hidden navigation is unlabelled navigation, not absent
 * chrome.
 */
@Composable
fun ShelfSheet(
    onDismiss: () -> Unit,
    onOpenJournal: () -> Unit,
    onOpenPatterns: () -> Unit,
    onOpenTalks: () -> Unit,
    onOpenMemories: () -> Unit,
    onOpenSettings: () -> Unit,
    onDisclosure: () -> Unit,
    /** Null when there is no conversation to clear. */
    onClearConversation: (() -> Unit)? = null
) {
    AxiomBottomSheet(title = "Where would you like to go?", onDismiss = onDismiss) {
        // Dismiss first so the sheet is already leaving as the destination
        // arrives; navigating out from under a live sheet leaves it stranded.
        fun go(action: () -> Unit): () -> Unit = {
            onDismiss()
            action()
        }

        /*
         * A single clock drives the whole entrance.
         *
         * The alternative — an animation per row — needs a coroutine each and
         * makes the stagger a property of eight separate call sites. Here one
         * value sweeps 0→1 and each element reads a delayed window out of it,
         * so the wave is described in one place and costs one animation.
         *
         * A tween, not a spring, precisely because it is a clock: the stagger
         * offsets below are fractions of its progress, and a spring's progress
         * is not linear in time, so identical offsets would produce an uneven
         * wave. Springs stay where they belong — on the things being moved.
         */
        val reduced = AxiomTheme.motion.isReduced
        val reveal = remember { Animatable(if (reduced) 1f else 0f) }
        LaunchedEffect(Unit) {
            if (!reduced) {
                reveal.animateTo(1f, tween(REVEAL_MS, easing = FastOutSlowInEasing))
            }
        }

        // Journal is still listed: the bar is the fast path, but a door that
        // exists in only one place is a door people miss.
        ShelfRow(
            title = "Your story",
            body = "Everything we've written",
            progress = reveal.value.window(0),
            onClick = go(onOpenJournal)
        )
        ShelfRow(
            title = "Our talks",
            body = "Every conversation, by the day it happened",
            progress = reveal.value.window(1),
            onClick = go(onOpenTalks)
        )
        ShelfRow(
            title = "What I've noticed",
            body = "Gently, and never certain",
            progress = reveal.value.window(2),
            onClick = go(onOpenPatterns)
        )
        ShelfRow(
            title = "What I remember",
            body = "Yours to edit or forget",
            progress = reveal.value.window(3),
            onClick = go(onOpenMemories)
        )
        ShelfRow(
            title = "Your space",
            body = "Voice, appearance, and what leaves this phone",
            progress = reveal.value.window(4),
            onClick = go(onOpenSettings)
        )

        if (onClearConversation != null) {
            Spacer(Modifier.height(AxiomTheme.space.sm))
            Text(
                text = "Start the conversation over",
                style = AxiomTheme.type.uiBodySmall,
                color = AxiomTheme.colors.critical,
                modifier = Modifier
                    .rise(reveal.value.window(5))
                    .fillMaxWidth()
                    .clip(AxiomTheme.shapes.md)
                    .clickable(onClick = go(onClearConversation))
                    .padding(
                        horizontal = AxiomTheme.space.sm,
                        vertical = AxiomTheme.space.md
                    )
            )
        }

        Spacer(Modifier.height(AxiomTheme.space.base))
        // Against three cloud-subscription competitors this is the difference,
        // so it stays one tap from home rather than buried in settings.
        Text(
            text = "What gets sent?",
            style = AxiomTheme.type.uiLabelSmall,
            color = AxiomTheme.colors.inkFaint,
            modifier = Modifier
                .rise(reveal.value.window(6))
                .clip(AxiomTheme.shapes.sm)
                .clickable(onClick = go(onDisclosure))
                .padding(
                    horizontal = AxiomTheme.space.xs,
                    vertical = AxiomTheme.space.sm
                )
        )
    }
}

@Composable
private fun ShelfRow(
    title: String,
    body: String,
    progress: Float,
    onClick: () -> Unit
) {
    Column(
        Modifier
            .rise(progress)
            .fillMaxWidth()
            .clip(AxiomTheme.shapes.md)
            .clickable(onClick = onClick)
            .padding(
                horizontal = AxiomTheme.space.sm,
                vertical = AxiomTheme.space.md
            )
            .testTag("shelf:$title")
    ) {
        Text(title, style = AxiomTheme.type.uiTitle, color = AxiomTheme.colors.ink)
        Spacer(Modifier.height(AxiomTheme.space.xxs))
        Text(body, style = AxiomTheme.type.uiBodySmall, color = AxiomTheme.colors.inkMuted)
    }
}

/**
 * The slice of the shared reveal clock belonging to the [index]th element,
 * remapped to its own 0→1. Later elements start later and everything lands
 * before the clock runs out.
 */
private fun Float.window(index: Int): Float =
    ((this - index * STAGGER_STEP) / STAGGER_SPAN).coerceIn(0f, 1f)

/**
 * Fade and lift, applied in a graphics layer so the wave costs no relayout —
 * eight elements sliding by changing their padding would remeasure the sheet on
 * every frame of the entrance.
 */
private fun Modifier.rise(progress: Float): Modifier = graphicsLayer {
    alpha = progress
    translationY = (1f - progress) * RISE_PX
}

private const val REVEAL_MS = 520
private const val STAGGER_STEP = 0.07f
private const val STAGGER_SPAN = 0.50f
private const val PRESSED_SCALE = 0.96f
private const val RISE_PX = 34f
private val ART_HEIGHT: Dp = 38.dp
