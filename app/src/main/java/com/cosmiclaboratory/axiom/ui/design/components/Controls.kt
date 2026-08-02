package com.cosmiclaboratory.axiom.ui.design.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.cosmiclaboratory.axiom.ui.design.AxiomDimens
import com.cosmiclaboratory.axiom.ui.theme.AxiomTheme

/**
 * An icon-only button that CANNOT be built without a label.
 *
 * This is the whole point of the component: `label` is non-nullable, and it
 * becomes both the contentDescription and a long-press tooltip. Apple's Journal
 * app is widely criticised for burying features behind unlabelled icons, and
 * Axiom had the same problem — the type system is a more reliable fix than a
 * style guide nobody re-reads.
 *
 * For genuinely decorative icons use [Icon] directly with a null description;
 * decoration is not interaction.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AxiomIconButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: androidx.compose.ui.graphics.Color = AxiomTheme.colors.ink
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(label) } },
        state = rememberTooltipState()
    ) {
        IconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier.size(AxiomDimens.MinTouchTarget)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (enabled) tint else AxiomTheme.colors.inkFaint
            )
        }
    }
}

/** Top-bar action. Same non-nullable-label contract as [AxiomIconButton]. */
@Composable
fun AxiomTopBarAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) = AxiomIconButton(icon, label, onClick, modifier, enabled)

/**
 * The primary action. Extended (icon + visible text) by default, collapsing to
 * icon-only on scroll — never a bare `+` with no explanation of what it does.
 */
@Composable
fun AxiomFab(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    expanded: Boolean = true
) {
    if (expanded) {
        ExtendedFloatingActionButton(
            onClick = onClick,
            modifier = modifier,
            containerColor = AxiomTheme.colors.accent,
            contentColor = AxiomTheme.colors.onAccent,
            shape = AxiomTheme.shapes.xl
        ) {
            Icon(icon, contentDescription = null)
            Spacer(Modifier.width(AxiomTheme.space.sm))
            Text(label, style = AxiomTheme.type.uiLabel)
        }
    } else {
        FloatingActionButton(
            onClick = onClick,
            modifier = modifier,
            containerColor = AxiomTheme.colors.accent,
            contentColor = AxiomTheme.colors.onAccent,
            shape = AxiomTheme.shapes.xl
        ) {
            // Collapsed: the icon carries the meaning, so it must be described.
            Icon(icon, contentDescription = label)
        }
    }
}

/**
 * Wraps decorative content so screen readers skip it entirely rather than
 * announcing a meaningless node.
 */
@Composable
fun Decorative(content: @Composable () -> Unit) {
    Box(Modifier.clearAndSetSemantics { }) { content() }
}
