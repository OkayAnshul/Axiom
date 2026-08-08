package com.cosmiclaboratory.axiom.ui.design.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cosmiclaboratory.axiom.R
import com.cosmiclaboratory.axiom.domain.model.CompanionIdentity
import com.cosmiclaboratory.axiom.ui.design.AxiomSpacing
import com.cosmiclaboratory.axiom.ui.design.LocalCompanionName
import com.cosmiclaboratory.axiom.ui.theme.AxiomTheme

/** The two places you actually live in. Everything else is a room off them. */
enum class AxiomPlace { Conversation, Story }

/**
 * A two-place bar: talking, and reading back what came of it.
 *
 * The app previously had a three-tab bar and it was removed for a reason worth
 * restating, because this is deliberately not a return to it. That bar claimed
 * Journal *and Patterns* were peers of the conversation, permanently worth a
 * glance — and Patterns is not; it is something you visit. Two places are a
 * different claim, and a true one: the whole app is talking and keeping. The
 * shelf still holds everything else, so nothing here competes with the
 * conversation except the one thing that genuinely earns it.
 *
 * The selected pill slides between the two rather than cutting, which is what
 * makes it read as one bar with a position instead of two buttons that light up.
 *
 * The left tab is a name, not an activity: a tab is a place you go, and you go
 * to *someone*. It defaults to the app's own name and the user can change it in
 * settings, which is the point — naming a thing is how people start treating it
 * as a presence rather than a feature.
 */
@Composable
fun AxiomPlaceBar(
    current: AxiomPlace,
    onSelect: (AxiomPlace) -> Unit,
    modifier: Modifier = Modifier,
    /** What the user calls their companion; app-wide, so it defaults from context. */
    companionName: String = LocalCompanionName.current
) {
    val c = AxiomTheme.colors
    val selected = if (current == AxiomPlace.Conversation) 0f else 1f
    val slide by animateFloatAsState(
        targetValue = selected,
        animationSpec = AxiomTheme.motion.spatialExpressive,
        label = "placeBarSlide"
    )

    Column(
        modifier
            .fillMaxWidth()
            .background(c.canvas)
            .windowInsetsPadding(WindowInsets.navigationBars)
            // Tight to whatever sits above it — the bar is a foot to the
            // screen, not a separate slab floating below the content.
            .padding(
                start = AxiomTheme.space.screenH,
                end = AxiomTheme.space.screenH,
                top = AxiomTheme.space.xxs,
                bottom = AxiomTheme.space.xxs
            )
            .testTag("bar:places")
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth().height(TOUCH_HEIGHT)) {
            val half = maxWidth / 2

            // The pill is drawn first and positioned by the animation, so the
            // labels above it never move — only the ground under them does.
            //
            // It is deliberately shorter than the row that contains it: the bar
            // should look like a light foot to the screen, but a tab still has
            // to be as easy to hit as anything else. Shrinking what you see
            // without shrinking what you can press is the whole trick.
            //
            // CenterStart, not Center. The offset below is measured from the
            // left edge, so the pill has to start there — aligning it centred
            // parks a half-width pill at a quarter width in, and it straddles
            // both tabs instead of sitting under one.
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = half * slide)
                    .width(half)
                    .height(PILL_HEIGHT)
                    // Inset before the background, so it shrinks what is drawn
                    // and not what can be pressed — the targets are the Row's.
                    .padding(horizontal = AxiomTheme.space.xxs)
                    .clip(AxiomTheme.shapes.lg)
                    .background(c.accentSoft)
            )

            Row(Modifier.fillMaxWidth()) {
                PlaceTab(
                    label = CompanionIdentity.resolve(companionName),
                    icon = ImageVector.vectorResource(R.drawable.ic_axiom_spark),
                    selected = current == AxiomPlace.Conversation,
                    onClick = { onSelect(AxiomPlace.Conversation) },
                    modifier = Modifier.weight(1f)
                )
                PlaceTab(
                    label = "Your story",
                    icon = ImageVector.vectorResource(R.drawable.ic_axiom_story),
                    selected = current == AxiomPlace.Story,
                    onClick = { onSelect(AxiomPlace.Story) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun PlaceTab(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = AxiomTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) PRESSED_SCALE else 1f,
        animationSpec = AxiomTheme.motion.spatialQuick,
        label = "placeTabScale"
    )
    // Colour is an effect, so it settles without overshooting — an accent that
    // bounced past itself would flash.
    val tint by animateColorAsState(
        targetValue = if (selected) c.onAccentSoft else c.inkMuted,
        animationSpec = AxiomTheme.motion.effects(),
        label = "placeTabTint"
    )

    Row(
        modifier
            .fillMaxHeight()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(AxiomTheme.shapes.lg)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Tab,
                onClick = onClick
            )
            .testTag("place:$label"),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(ICON_SIZE))
        Spacer(Modifier.width(AxiomTheme.space.sm))
        Text(label, style = AxiomTheme.type.uiLabel, color = tint)
    }
}

/** What you can press. Never below [AxiomDimens.MinTouchTarget]. */
private val TOUCH_HEIGHT = 48.dp

/** What you can see. Lighter than the target it sits inside. */
private val PILL_HEIGHT = 38.dp

private val ICON_SIZE = 18.dp
private const val PRESSED_SCALE = 0.94f

/**
 * How much room the bar takes, not counting the navigation-bar inset it also
 * consumes. Published so anything that has to float clear of the bar — the
 * app-wide snackbar, most obviously — reserves the real number instead of
 * carrying a copy of it that drifts.
 *
 * Declared after the constants it is built from: top-level initializers run in
 * file order, and reading them from above would silently yield zero.
 */
val AxiomPlaceBarHeight: Dp = TOUCH_HEIGHT + AxiomSpacing().xxs * 2
