package com.cosmiclaboratory.axiom.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.*
import com.cosmiclaboratory.axiom.R
import com.cosmiclaboratory.axiom.ui.components.AnimatedAxiomTitle
import kotlinx.coroutines.delay

/**
 * Splash Screen with Lottie animation and animated Axiom.md title
 *
 * Features:
 * - Cosmic/space-themed Lottie animation (if available)
 * - Typewriter effect for "Axiom.md" title
 * - Smooth fade-in animations
 * - Auto-navigation after animation completes
 */
@Composable
fun SplashScreen(
    onSplashComplete: () -> Unit
) {
    val context = LocalContext.current
    var animationCompleted by remember { mutableStateOf(false) }

    // Check if Lottie animation resource exists
    val splashAnimationResId = remember {
        try {
            val resId = context.resources.getIdentifier("splash_animation", "raw", context.packageName)
            if (resId != 0) resId else null
        } catch (e: Exception) {
            null
        }
    }

    // Fade-in animation for the entire splash screen
    val alpha by animateFloatAsState(
        targetValue = if (animationCompleted) 0f else 1f,
        animationSpec = tween(durationMillis = 500),
        label = "splashFade"
    )

    // Lottie animation composition
    val compositionResult by rememberLottieComposition(
        splashAnimationResId?.let { LottieCompositionSpec.RawRes(it) } ?: LottieCompositionSpec.RawRes(0)
    )
    val composition = if (splashAnimationResId != null) compositionResult else null

    val lottieProgress by animateLottieCompositionAsState(
        composition = composition,
        iterations = 1,
        speed = 1f
    )

    // Navigation delay - wait for animations to complete
    LaunchedEffect(Unit) {
        delay(3000) // Total splash duration: 3 seconds
        animationCompleted = true
        delay(500) // Wait for fade-out
        onSplashComplete()
    }

    // Pulsing animation for fallback (when no Lottie animation is present)
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .alpha(alpha),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Lottie animation or fallback
            if (splashAnimationResId != null && composition != null) {
                LottieAnimation(
                    composition = composition,
                    progress = { lottieProgress },
                    modifier = Modifier
                        .size(250.dp)
                        .padding(bottom = 32.dp)
                )
            } else {
                // Fallback: Simple pulsing cosmic circle
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .scale(pulseScale)
                        .padding(bottom = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // You can add a custom composable graphic here
                    // For now, we'll rely on the animated title
                }
            }

            // Animated "Axiom.md" title with typewriter effect
            AnimatedAxiomTitle(
                textStyle = MaterialTheme.typography.displayMedium,
                startDelay = 300L,
                typewriterSpeed = 150L,
                showAnimation = true
            )
        }
    }
}
