package com.cosmiclaboratory.axiom.ui.design.components

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cosmiclaboratory.axiom.R
import com.cosmiclaboratory.axiom.ui.design.AxiomError
import com.cosmiclaboratory.axiom.ui.design.isRetryable
import com.cosmiclaboratory.axiom.ui.theme.AxiomTheme
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** One empty-state component for every empty surface, so the voice stays consistent. */
@Composable
fun AxiomEmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    primaryLabel: String? = null,
    onPrimary: (() -> Unit)? = null,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null
) {
    val c = AxiomTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(AxiomTheme.space.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            // Decorative: the title and body already carry the meaning.
            Icon(icon, contentDescription = null, tint = c.inkFaint, modifier = Modifier.size(40.dp))
            Spacer(Modifier.height(AxiomTheme.space.base))
        }
        Text(title, style = AxiomTheme.type.uiTitle, color = c.ink, textAlign = TextAlign.Center)
        Spacer(Modifier.height(AxiomTheme.space.sm))
        Text(
            body,
            style = AxiomTheme.type.uiBody,
            color = c.inkMuted,
            textAlign = TextAlign.Center
        )
        if (primaryLabel != null && onPrimary != null) {
            Spacer(Modifier.height(AxiomTheme.space.xl))
            Button(
                onClick = onPrimary,
                colors = ButtonDefaults.buttonColors(
                    containerColor = c.accent,
                    contentColor = c.onAccent
                ),
                shape = AxiomTheme.shapes.sm
            ) { Text(primaryLabel, style = AxiomTheme.type.uiLabel) }
        }
        if (secondaryLabel != null && onSecondary != null) {
            Spacer(Modifier.height(AxiomTheme.space.xs))
            TextButton(onClick = onSecondary) {
                Text(secondaryLabel, style = AxiomTheme.type.uiLabel, color = c.accent)
            }
        }
    }
}

/**
 * The one place an [AxiomError] becomes words. Copy lives here rather than in
 * each ViewModel, so the same failure reads the same everywhere.
 */
@Composable
fun AxiomErrorSurface(
    error: AxiomError,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
    onPrimary: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null
) {
    val c = AxiomTheme.colors
    // Copy comes from strings.xml, which already held every one of these keys
    // while this function kept its own hardcoded English. Two sources of truth
    // for the same sentence is how half an app ends up untranslated.
    val (title, body, primaryLabel) = when (error) {
        AxiomError.NoAiKey -> Triple(
            stringResource(R.string.error_no_key_title),
            stringResource(R.string.error_no_key_body),
            stringResource(R.string.action_connect_ai)
        )
        is AxiomError.RateLimited -> Triple(
            stringResource(R.string.error_rate_limited_title),
            stringResource(R.string.error_rate_limited_body),
            null
        )
        is AxiomError.Network -> Triple(
            stringResource(R.string.error_network_title),
            stringResource(R.string.error_network_body),
            null
        )
        is AxiomError.Malformed -> Triple(
            stringResource(R.string.error_malformed_title),
            stringResource(R.string.error_malformed_body),
            null
        )
        is AxiomError.Storage -> Triple(
            stringResource(R.string.error_storage_title),
            stringResource(R.string.error_storage_body),
            null
        )
        is AxiomError.Permission -> Triple(
            stringResource(R.string.error_permission_title),
            stringResource(R.string.error_permission_body),
            stringResource(R.string.action_open_settings)
        )
        is AxiomError.Unknown -> Triple(
            stringResource(R.string.error_unknown_title),
            error.message,
            null
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(AxiomTheme.shapes.md)
            .background(c.criticalSoft)
            .border(1.dp, c.critical.copy(alpha = 0.3f), AxiomTheme.shapes.md)
            .padding(AxiomTheme.space.base)
            .semantics { liveRegion = LiveRegionMode.Polite }
    ) {
        Text(title, style = AxiomTheme.type.uiTitleSmall, color = c.ink)
        Spacer(Modifier.height(AxiomTheme.space.xs))
        Text(body, style = AxiomTheme.type.uiBodySmall, color = c.inkMuted)

        Spacer(Modifier.height(AxiomTheme.space.md))
        Row(horizontalArrangement = Arrangement.spacedBy(AxiomTheme.space.sm)) {
            if (error is AxiomError.RateLimited) {
                RetryCountdownButton(seconds = error.retryAfterSeconds, onRetry = onRetry)
            } else if (error.isRetryable && onRetry != null) {
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = c.accent, contentColor = c.onAccent
                    ),
                    shape = AxiomTheme.shapes.sm
                ) { Text("Try again", style = AxiomTheme.type.uiLabel) }
            }
            if (primaryLabel != null && onPrimary != null) {
                Button(
                    onClick = onPrimary,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = c.accent, contentColor = c.onAccent
                    ),
                    shape = AxiomTheme.shapes.sm
                ) { Text(primaryLabel, style = AxiomTheme.type.uiLabel) }
            }
            if (onDismiss != null) {
                TextButton(onClick = onDismiss) {
                    Text("Dismiss", style = AxiomTheme.type.uiLabel, color = c.inkMuted)
                }
            }
        }
    }
}

/** Retry disabled behind a live countdown — pointless retries just re-trip the limit. */
@Composable
private fun RetryCountdownButton(seconds: Int, onRetry: (() -> Unit)?) {
    var remaining by remember(seconds) { mutableIntStateOf(seconds) }
    LaunchedEffect(seconds) {
        while (remaining > 0) {
            delay(1000)
            remaining -= 1
        }
    }
    Button(
        onClick = { onRetry?.invoke() },
        enabled = remaining <= 0 && onRetry != null,
        colors = ButtonDefaults.buttonColors(
            containerColor = AxiomTheme.colors.accent,
            contentColor = AxiomTheme.colors.onAccent
        ),
        shape = AxiomTheme.shapes.sm
    ) {
        Text(
            text = if (remaining > 0) "Try again in ${remaining}s" else "Try again",
            style = AxiomTheme.type.uiNumeric
        )
    }
}

/** Lightweight non-blocking notice, for things that are not failures. */
@Composable
fun AxiomInlineNotice(
    text: String,
    modifier: Modifier = Modifier,
    tone: CardTone = CardTone.Caution,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val c = AxiomTheme.colors
    val bg = when (tone) {
        CardTone.Accent -> c.accentSoft
        CardTone.Ai -> c.aiTintSoft
        CardTone.Caution -> c.cautionSoft
        CardTone.Neutral -> c.surfaceSunken
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(AxiomTheme.shapes.sm)
            .background(bg)
            .padding(horizontal = AxiomTheme.space.md, vertical = AxiomTheme.space.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, style = AxiomTheme.type.uiBodySmall, color = c.ink, modifier = Modifier.weight(1f))
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(actionLabel, style = AxiomTheme.type.uiLabel, color = c.accent)
            }
        }
    }
}

/** Autosave state. Always visible — silent saving is what makes users distrust an editor. */
sealed interface SaveState {
    data object Idle : SaveState
    data object Editing : SaveState
    data object Saving : SaveState
    data class Saved(val at: LocalTime) : SaveState
    data class Failed(val reason: String) : SaveState
}

@Composable
fun SaveStateIndicator(
    state: SaveState,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null
) {
    val c = AxiomTheme.colors
    val text = when (state) {
        SaveState.Idle -> ""
        SaveState.Editing -> "Editing…"
        SaveState.Saving -> "Saving…"
        is SaveState.Saved -> "Saved ${state.at.format(DateTimeFormatter.ofPattern("HH:mm"))}"
        is SaveState.Failed -> "Save failed"
    }
    if (text.isEmpty()) return

    Row(
        modifier = modifier.semantics {
            stateDescription = text
            if (state is SaveState.Failed) liveRegion = LiveRegionMode.Assertive
        },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = AxiomTheme.type.uiMeta,
            color = if (state is SaveState.Failed) c.critical else c.inkFaint
        )
        if (state is SaveState.Failed && onRetry != null) {
            TextButton(onClick = onRetry, contentPadding = PaddingValues(horizontal = 6.dp)) {
                Text("Retry", style = AxiomTheme.type.uiMeta, color = c.accent)
            }
        }
    }
}

/**
 * Three dots, breathing, while the model is working. Announced politely, once.
 *
 * The old version pulsed on a 600ms tween, which reads as a loading spinner —
 * a machine telling you it is busy. On the breath cycle with a slight swell as
 * well as a fade, the same three dots read as someone thinking before they
 * answer. Waiting is part of the conversation, so it should look like it.
 */
@Composable
fun ThinkingIndicator(modifier: Modifier = Modifier) {
    val motion = AxiomTheme.motion
    val transition = rememberInfiniteTransition(label = "thinking")
    Row(
        modifier = modifier.semantics {
            liveRegion = LiveRegionMode.Polite
            stateDescription = "Thinking"
        },
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { i ->
            val phase by transition.animateFloat(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = motion.breathe,
                label = "dot$i"
            )
            // One shared breath, sampled a third of a cycle apart, so the dots
            // travel together rather than blinking in sequence.
            val shifted = ((phase + i / 3f) % 1f).let { t ->
                if (t <= 0.5f) t * 2f else (1f - t) * 2f
            }
            Box(
                Modifier
                    .size(6.dp)
                    .scale(0.82f + 0.18f * shifted)
                    .alpha(0.35f + 0.65f * shifted)
                    .clip(AxiomTheme.shapes.full)
                    .background(AxiomTheme.colors.aiTint)
            )
        }
    }
}
