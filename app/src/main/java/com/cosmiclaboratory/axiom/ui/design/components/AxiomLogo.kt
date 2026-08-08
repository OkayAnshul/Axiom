package com.cosmiclaboratory.axiom.ui.design.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cosmiclaboratory.axiom.ui.theme.AxiomTheme

/**
 * The launcher mark, drawn in Compose so it can take the live palette.
 *
 * Deliberately not a `painterResource` of the launcher vector: that one is
 * authored against a fixed dark ground with baked-in cream paper, which is
 * correct for a home screen and wrong inside an app whose surface shifts from
 * cream to near-black across the day. Same shapes, same proportions, colours
 * read from the theme.
 *
 * The paths here are transcriptions of ic_launcher_foreground and have to be
 * edited alongside it — there is no shared source, because a vector drawable
 * cannot take a runtime palette and a Compose path cannot be a launcher icon.
 */
@Composable
fun AxiomLogo(
    modifier: Modifier = Modifier,
    size: Dp = LOGO_SIZE
) {
    val c = AxiomTheme.colors
    Canvas(
        modifier
            .size(size)
            .semantics { contentDescription = "Axiom" }
    ) {
        val u = this.size.minDimension / 108f

        drawPath(
            path = emberPath(u),
            brush = Brush.verticalGradient(
                colors = listOf(
                    c.accent.copy(alpha = 0.92f),
                    c.accent,
                    c.accent.copy(alpha = 0.78f)
                ),
                startY = 24f * u,
                endY = 66.5f * u
            )
        )

        // The page it burns on. Two leaves rather than one shape, so the valley
        // between them is the canvas showing through and stays a spine at any
        // size — the same reason the launcher vector splits them.
        drawPath(leftLeafPath(u), color = c.ink.copy(alpha = 0.5f))
        drawPath(rightLeafPath(u), color = c.ink.copy(alpha = 0.28f))
    }
}

/** The same teardrop as ic_launcher_foreground, in device pixels. */
private fun emberPath(u: Float): Path = Path().apply {
    moveTo(54f * u, 24f * u)
    cubicTo(54f * u, 24f * u, 69f * u, 41f * u, 69f * u, 51.5f * u)
    cubicTo(69f * u, 59.8f * u, 62.3f * u, 66.5f * u, 54f * u, 66.5f * u)
    cubicTo(45.7f * u, 66.5f * u, 39f * u, 59.8f * u, 39f * u, 51.5f * u)
    cubicTo(39f * u, 41f * u, 54f * u, 24f * u, 54f * u, 24f * u)
    close()
}

/** The open spread's left leaf, matching ic_launcher_foreground. */
private fun leftLeafPath(u: Float): Path = Path().apply {
    moveTo(26f * u, 69.5f * u)
    cubicTo(35f * u, 68.1f * u, 46f * u, 69.7f * u, 53f * u, 73.5f * u)
    lineTo(53f * u, 84f * u)
    cubicTo(46f * u, 80.2f * u, 35f * u, 78.6f * u, 26f * u, 80f * u)
    close()
}

/** The right leaf: the same curve mirrored about the spine at x = 54. */
private fun rightLeafPath(u: Float): Path = Path().apply {
    moveTo(82f * u, 69.5f * u)
    cubicTo(73f * u, 68.1f * u, 62f * u, 69.7f * u, 55f * u, 73.5f * u)
    lineTo(55f * u, 84f * u)
    cubicTo(62f * u, 80.2f * u, 73f * u, 78.6f * u, 82f * u, 80f * u)
    close()
}

private val LOGO_SIZE = 72.dp
