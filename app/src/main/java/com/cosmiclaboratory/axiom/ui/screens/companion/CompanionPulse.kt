package com.cosmiclaboratory.axiom.ui.screens.companion

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.airbnb.lottie.compose.rememberLottieDynamicProperties
import com.airbnb.lottie.compose.rememberLottieDynamicProperty
import com.airbnb.lottie.LottieProperty
import com.cosmiclaboratory.axiom.R
import com.cosmiclaboratory.axiom.ui.design.components.AxiomBottomSheet
import com.cosmiclaboratory.axiom.ui.theme.AxiomTheme

/**
 * The one thing on this screen that is visibly alive.
 *
 * A conversation that quietly becomes a journal entry is the app's whole
 * premise and the least visible thing it does — nothing on screen suggests
 * anything happens after you stop typing. This is the handle for that: it
 * breathes while you are here, and tapping it explains where what you said
 * ends up.
 *
 * Two things Lottie does not do on its own, both wired by hand below:
 *
 *  - **Reduced motion.** Every other animation in the app runs through
 *    [com.cosmiclaboratory.axiom.ui.design.AxiomMotion], which collapses to
 *    instant when the platform animator scale is zero. Lottie has never heard of
 *    that setting, so when motion is reduced this draws a static mark instead
 *    and no composition is played at all.
 *  - **The palette.** The artwork is authored in one colour and re-tinted from
 *    `aiTint` at runtime, so it follows the adaptive day/night palette like
 *    everything else. Baked-in colours would have made this the one element that
 *    stays indigo-on-cream at midnight. The tint is read once per palette
 *    change, never per frame — see the warning in AxiomLight.
 */
@Composable
fun CompanionPulse(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = PULSE_SIZE
) {
    val c = AxiomTheme.colors
    val reduced = AxiomTheme.motion.isReduced

    val tapTarget = modifier
        .size(size)
        .clip(AxiomTheme.shapes.full)
        .clickable(
            role = Role.Button,
            onClickLabel = "What happens to this conversation",
            onClick = onClick
        )
        .testTag("action:companion_pulse")

    if (reduced) {
        // The same mark, holding still: a filled core inside a single ring.
        Canvas(tapTarget) {
            val r = this.size.minDimension / 2f
            drawCircle(color = c.aiTint, radius = r * 0.27f)
            drawCircle(
                color = c.aiTint.copy(alpha = 0.4f),
                radius = r * 0.72f,
                style = Stroke(width = r * 0.075f)
            )
        }
        return
    }

    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.companion_pulse)
    )
    // "**" globs every layer and group, so the whole mark takes the tint without
    // the keypaths having to track the artwork's internal names.
    val tint = c.aiTint.toArgb()
    val dynamicProperties = rememberLottieDynamicProperties(
        rememberLottieDynamicProperty(
            property = LottieProperty.COLOR,
            value = tint,
            keyPath = arrayOf("**")
        ),
        rememberLottieDynamicProperty(
            property = LottieProperty.STROKE_COLOR,
            value = tint,
            keyPath = arrayOf("**")
        )
    )

    LottieAnimation(
        composition = composition,
        iterations = LottieConstants.IterateForever,
        dynamicProperties = dynamicProperties,
        modifier = tapTarget
    )
}

/**
 * What the pulse is for, in the app's own voice.
 *
 * The copy is deliberately the same as the disclosure sheet's — the honest
 * account of what leaves the device already existed, and saying it two
 * different ways is how an app ends up with two different reputations.
 */
@Composable
fun ConversationExplainerSheet(
    onDismiss: () -> Unit,
    onOpenStory: () -> Unit
) {
    val c = AxiomTheme.colors
    AxiomBottomSheet(title = "What happens to this", onDismiss = onDismiss) {
        Explainer(
            heading = "Talk, and it gets written down",
            body = "When you're done, I read the conversation back and write it " +
                "up as an entry in your story — dated to the day you had it, " +
                "kept under \"Talks\". You never have to press save."
        )
        Spacer(Modifier.height(AxiomTheme.space.lg))
        Explainer(
            heading = "It doesn't have to be about the prompt",
            body = "The question at the top is a way in, not the subject. Tell " +
                "me about your day, something that annoyed you, something you " +
                "don't want to forget. Anything you'd put in a journal belongs here."
        )
        Spacer(Modifier.height(AxiomTheme.space.lg))
        Explainer(
            heading = "You can say it out loud",
            body = "The microphone types for you. Hands-free goes further — I " +
                "read my replies aloud and start listening again when I'm done, " +
                "so you can journal with the phone face down."
        )
        Spacer(Modifier.height(AxiomTheme.space.lg))
        Text(
            text = "Read what's been kept",
            style = AxiomTheme.type.uiLabel,
            color = c.accent,
            modifier = Modifier
                .clip(AxiomTheme.shapes.sm)
                .clickable {
                    onDismiss()
                    onOpenStory()
                }
                .padding(vertical = AxiomTheme.space.sm, horizontal = AxiomTheme.space.xs)
        )
    }
}

@Composable
private fun Explainer(heading: String, body: String) {
    val c = AxiomTheme.colors
    Row(Modifier.fillMaxWidth()) {
        Box(Modifier.padding(top = 2.dp)) {
            CompanionPulseGlyph()
        }
        Spacer(Modifier.width(AxiomTheme.space.base))
        Column(Modifier.weight(1f)) {
            Text(heading, style = AxiomTheme.type.uiTitleSmall, color = c.ink)
            Spacer(Modifier.height(AxiomTheme.space.xs))
            Text(body, style = AxiomTheme.type.readingBody, color = c.inkMuted)
        }
    }
}

/** The still version of the mark, used as a bullet so the sheet reads as its own. */
@Composable
private fun CompanionPulseGlyph() {
    val c = AxiomTheme.colors
    Canvas(Modifier.size(14.dp)) {
        val r = size.minDimension / 2f
        drawCircle(color = c.aiTint, radius = r * 0.34f)
        drawCircle(
            color = c.aiTint.copy(alpha = 0.35f),
            radius = r * 0.85f,
            style = Stroke(width = r * 0.14f)
        )
    }
}

private val PULSE_SIZE = 28.dp
