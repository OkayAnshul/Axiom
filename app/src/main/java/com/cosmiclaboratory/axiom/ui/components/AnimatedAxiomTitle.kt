package com.cosmiclaboratory.axiom.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.delay

/**
 * Animated Axiom title component with typewriter effect
 *
 * @param modifier Modifier for the component
 * @param textStyle Style for the text (default is headlineMedium)
 * @param startDelay Delay before animation starts in milliseconds
 * @param typewriterSpeed Speed of typewriter effect in milliseconds per character
 * @param showAnimation Whether to show the typewriter animation or just display the title
 */
@Composable
fun AnimatedAxiomTitle(
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.headlineMedium,
    startDelay: Long = 0L,
    typewriterSpeed: Long = 150L,
    showAnimation: Boolean = true
) {
    var displayedText by remember { mutableStateOf("") }
    val fullText = "Axiom.md"

    // Gradient animation for .md extension
    val infiniteTransition = rememberInfiniteTransition(label = "gradient")
    val gradientOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gradientOffset"
    )

    // Typewriter effect
    LaunchedEffect(showAnimation) {
        if (showAnimation) {
            delay(startDelay)
            fullText.forEachIndexed { index, _ ->
                displayedText = fullText.substring(0, index + 1)
                delay(typewriterSpeed)
            }
        } else {
            displayedText = fullText
        }
    }

    Row(modifier = modifier) {
        val axiomPart = displayedText.takeWhile { it != '.' }
        val mdPart = if (displayedText.contains('.')) displayedText.substringAfter('.', "") else ""
        val hasDot = displayedText.contains('.')

        // "Axiom" part - regular text
        if (axiomPart.isNotEmpty()) {
            Text(
                text = axiomPart,
                style = textStyle.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // "." and "md" part - with gradient animation
        if (hasDot) {
            val gradientColors = listOf(
                Color(0xFF4A90E2), // AxiomBlue
                Color(0xFF64B5F6), // Lighter blue
                Color(0xFFFFB74D), // Amber
                Color(0xFF4A90E2)  // Back to blue
            )

            val gradientBrush = Brush.linearGradient(
                colors = gradientColors,
                start = Offset(gradientOffset, gradientOffset),
                end = Offset(gradientOffset + 500f, gradientOffset + 500f)
            )

            Text(
                text = ".${mdPart}",
                style = textStyle.copy(
                    fontWeight = FontWeight.ExtraBold,
                    brush = gradientBrush
                )
            )
        }
    }
}
