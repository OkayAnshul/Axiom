package com.cosmiclaboratory.axiom.ui.design.components

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/**
 * Sideways movement between the conversation and the story.
 *
 * The two halves of Axiom sit beside each other rather than in a stack: swipe
 * right from the conversation to reach what you've written, swipe left from
 * your story to come back. Same gesture, opposite directions, so the pair is
 * learnable in one go.
 *
 * The drag is consumed as it is handled, which lets a child that carries this
 * modifier — a prompt card, say — claim the gesture and stop the page-level one
 * behind it from also firing.
 *
 * Vertical scrolling is untouched. A `LazyColumn` inside this sits nearer the
 * pointer and takes vertical drags during the main pass, so only the horizontal
 * movement it declines ever reaches here.
 */
fun Modifier.swipeBetween(
    onSwipeRight: (() -> Unit)? = null,
    onSwipeLeft: (() -> Unit)? = null
): Modifier = composed {
    val threshold = with(LocalDensity.current) { SWIPE_THRESHOLD_DP.dp.toPx() }
    pointerInput(onSwipeRight, onSwipeLeft) {
        var travelled = 0f
        detectHorizontalDragGestures(
            onDragStart = { travelled = 0f },
            onDragEnd = {
                when {
                    travelled >= threshold -> onSwipeRight?.invoke()
                    travelled <= -threshold -> onSwipeLeft?.invoke()
                }
                travelled = 0f
            },
            onDragCancel = { travelled = 0f }
        ) { change, dragAmount ->
            travelled += dragAmount
            change.consume()
        }
    }
}

/**
 * Far enough that resting a thumb never triggers it, short enough that a
 * deliberate flick always does.
 */
private const val SWIPE_THRESHOLD_DP = 72
