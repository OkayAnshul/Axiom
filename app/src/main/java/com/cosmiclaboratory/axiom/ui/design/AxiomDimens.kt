package com.cosmiclaboratory.axiom.ui.design

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object AxiomDimens {
    /**
     * ~65 characters at 17sp Literata (average advance ≈ 0.48em). Typographic
     * research puts comfortable measure at 45–75 characters; past that the eye
     * loses the line return.
     *
     * On a phone this never clamps, which is correct. It earns its keep on
     * foldables, tablets and landscape, where full-width prose is unreadable.
     */
    val ReadingMaxWidth: Dp = 544.dp

    /** Wider ceiling for content that legitimately needs it — tables, code. */
    val ReadingMaxWidthWide: Dp = 640.dp

    /** Android's minimum accessible touch target. */
    val MinTouchTarget: Dp = 48.dp

    val FabSize: Dp = 56.dp
    val MoodTargetSize: Dp = 56.dp
    val HairlineWidth: Dp = 1.dp
}

/**
 * Constrains content to a comfortable reading measure and centres it.
 *
 * Must be applied to the *content*, not the scroll container, or the scrollbar
 * and any background end up inset too.
 */
@Composable
fun readingMeasureModifier(wide: Boolean = false): Modifier =
    Modifier.widthIn(max = if (wide) AxiomDimens.ReadingMaxWidthWide else AxiomDimens.ReadingMaxWidth)

/** Centres [content] inside a full-width row, clamped to the reading measure. */
@Composable
fun ReadingColumn(
    modifier: Modifier = Modifier,
    wide: Boolean = false,
    content: @Composable () -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = readingMeasureModifier(wide)
        ) { content() }
    }
}
