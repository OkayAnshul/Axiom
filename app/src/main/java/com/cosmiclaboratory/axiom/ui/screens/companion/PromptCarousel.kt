package com.cosmiclaboratory.axiom.ui.screens.companion

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.cosmiclaboratory.axiom.ui.theme.AxiomTheme

/**
 * Today's prompt, as an offer rather than an assignment.
 *
 * A single fixed question reads like homework — the one thing you are supposed
 * to write about today, and if you don't feel like writing about that, the app
 * has nothing for you. Being able to flick to another one changes what the
 * screen is saying: here are some ways in, or ignore them and say anything.
 *
 * The gesture nests deliberately. [com.cosmiclaboratory.axiom.ui.design.components.swipeBetween]
 * is applied to the conversation root, the user's bubbles and the reply blocks,
 * all of which read horizontal drags to move between the conversation and the
 * journal. Pointer input reaches children before parents, so the pager claims
 * drags that start on the prompt and the outer navigation still gets everything
 * else — which is exactly what SwipeBetween's own documentation anticipated.
 */
@Composable
fun PromptCarousel(
    options: List<PromptOption>,
    index: Int,
    onIndexChange: (Int) -> Unit,
    onWriteAbout: (PromptOption) -> Unit,
    showHint: Boolean,
    onHintSeen: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (options.isEmpty()) return
    val c = AxiomTheme.colors
    val pagerState = rememberPagerState(initialPage = index) { options.size }

    // Settled, not mid-drag: rewriting the stored prompt on every frame of a
    // swipe would write a row per pixel and land on whatever the finger passed
    // over last.
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { settled ->
            if (settled != index) onIndexChange(settled)
            if (settled != 0) onHintSeen()
        }
    }

    Row(modifier.fillMaxWidth()) {
        // Same rule as a reply: this is the companion talking, not a card.
        Box(
            Modifier
                .width(2.dp)
                .heightIn(min = 24.dp)
                .clip(AxiomTheme.shapes.full)
                .background(c.aiTint.copy(alpha = 0.5f))
        )
        Spacer(Modifier.width(AxiomTheme.space.base))
        Column(Modifier.weight(1f)) {
            HorizontalPager(
                state = pagerState,
                // Room for the longest prompt in the bank without the block
                // resizing as you swipe past a short one.
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = PROMPT_MIN_HEIGHT)
                    .testTag("pager:prompts"),
                verticalAlignment = Alignment.Top,
                pageSpacing = AxiomTheme.space.base,
                contentPadding = PaddingValues(end = AxiomTheme.space.xxl)
            ) { page ->
                Text(
                    text = options[page].text,
                    style = AxiomTheme.type.readingBody,
                    color = c.ink,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(AxiomTheme.space.sm))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Write about this",
                    style = AxiomTheme.type.uiLabel,
                    color = c.accent,
                    modifier = Modifier
                        .clip(AxiomTheme.shapes.sm)
                        .clickable {
                            options.getOrNull(pagerState.currentPage)?.let(onWriteAbout)
                        }
                        .padding(
                            vertical = AxiomTheme.space.sm,
                            horizontal = AxiomTheme.space.xs
                        )
                )
                Spacer(Modifier.weight(1f))
                if (options.size > 1) {
                    PromptDots(count = options.size, current = pagerState.currentPage)
                }
            }

            // Said once, quietly, and only until the first swipe proves it was
            // understood. A hint that keeps reappearing is an instruction.
            AnimatedVisibility(
                visible = showHint,
                enter = fadeIn(AxiomTheme.motion.effectsStandard),
                exit = fadeOut(AxiomTheme.motion.effectsQuick)
            ) {
                Text(
                    text = "Swipe for another — or just say what's on your mind.",
                    style = AxiomTheme.type.uiBodySmall,
                    color = c.inkFaint,
                    modifier = Modifier.padding(top = AxiomTheme.space.xs)
                )
            }
        }
    }
}

@Composable
private fun PromptDots(count: Int, current: Int) {
    val c = AxiomTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.semantics {
            contentDescription = "Prompt ${current + 1} of $count"
        }
    ) {
        repeat(count) { i ->
            Box(
                Modifier
                    .padding(horizontal = 3.dp)
                    .size(if (i == current) 6.dp else 4.dp)
                    .clip(CircleShape)
                    .background(if (i == current) c.accent else c.hairline)
            )
        }
    }
}

/** Three lines of readingBody, so the block doesn't resize mid-swipe. */
private val PROMPT_MIN_HEIGHT = 92.dp
