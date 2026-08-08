package com.cosmiclaboratory.axiom.ui.design

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import com.cosmiclaboratory.axiom.ui.theme.AxiomTheme

/**
 * Letting a thing you tapped travel to the screen it opens.
 *
 * Passed through composition locals rather than as screen parameters, for two
 * reasons. Threading `SharedTransitionScope` and `AnimatedVisibilityScope` down
 * through every screen signature to reach one card would put navigation
 * machinery in the parameter list of components that are otherwise about
 * content. And both are genuinely ambient: they are properties of *where the
 * composition is*, which is what a composition local is for.
 *
 * Null when there is no enclosing [androidx.compose.animation.SharedTransitionLayout]
 * — previews, tests, and any screen reached outside the NavHost — and
 * [axiomSharedBounds] degrades to doing nothing rather than crashing.
 */
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }

/** The per-destination scope; changes on every navigation. */
val LocalNavAnimatedScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

/**
 * Marks this node as the same object as the node with [key] on the other screen.
 *
 * `sharedBounds` rather than `sharedElement`: the two ends are not the same
 * composable. A card in the timeline and a full entry in the reader have
 * different content and different sizes, and sharedBounds animates the container
 * between them while letting each side draw its own contents — which is the
 * honest description of what is happening.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.axiomSharedBounds(key: Any): Modifier {
    val shared = LocalSharedTransitionScope.current ?: return this
    val animated = LocalNavAnimatedScope.current ?: return this
    // Reduced motion means no travel: a bounds animation is the most spatial
    // thing in the app, and the whole point of the setting is to stop that.
    if (AxiomTheme.motion.isReduced) return this
    // Directly, not through Modifier.composed: this function is already
    // composable, and composed() opts the whole chain out of skipping for no
    // gain — on a modifier attached to every row of a scrolling list.
    return with(shared) {
        this@axiomSharedBounds.sharedBounds(
            sharedContentState = rememberSharedContentState(key = key),
            animatedVisibilityScope = animated
        )
    }
}

/** Stable keys, so the two ends cannot drift apart in a rename. */
object SharedKeys {
    fun entry(id: Long): String = "entry-$id"
}
