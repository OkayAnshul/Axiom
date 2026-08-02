package com.cosmiclaboratory.axiom.ui.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.cosmiclaboratory.axiom.domain.model.Tag
import com.cosmiclaboratory.axiom.ui.theme.AxiomTheme

/**
 * Picks black or white text for an arbitrary background.
 *
 * Tag colours are user-chosen, so we cannot assume any particular contrast.
 * Deciding from measured luminance means a pale yellow tag gets dark text and a
 * deep navy one gets light text, automatically.
 */
fun contrastingTextColor(background: Color): Color =
    if (background.luminance() > 0.45f) Color(0xFF14120E) else Color(0xFFFFFFFF)

@Composable
fun TagChip(
    tag: Tag,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val bg = Color(tag.color.toULong())
    val fg = contrastingTextColor(bg)
    Row(
        modifier = modifier
            .clip(AxiomTheme.shapes.xs)
            .background(if (selected) bg else bg.copy(alpha = 0.18f))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = AxiomTheme.space.sm, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = tag.name,
            style = AxiomTheme.type.uiLabelSmall,
            color = if (selected) fg else AxiomTheme.colors.ink
        )
    }
}

data class FilterOption(val id: String, val label: String)

/**
 * Horizontally scrolling filter row. Labels are always text — never icon-only,
 * so the current filter is readable at a glance.
 */
@Composable
fun AxiomFilterChipRow(
    options: List<FilterOption>,
    selectedIds: Set<String>,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = AxiomTheme.space.screenH),
        horizontalArrangement = Arrangement.spacedBy(AxiomTheme.space.sm)
    ) {
        options.forEach { option ->
            val selected = option.id in selectedIds
            Text(
                text = option.label,
                style = AxiomTheme.type.uiLabel,
                color = if (selected) AxiomTheme.colors.onAccentSoft else AxiomTheme.colors.inkMuted,
                modifier = Modifier
                    .clip(AxiomTheme.shapes.full)
                    .background(
                        if (selected) AxiomTheme.colors.accentSoft else AxiomTheme.colors.surfaceSunken
                    )
                    .border(
                        1.dp,
                        if (selected) AxiomTheme.colors.accent.copy(alpha = 0.4f) else AxiomTheme.colors.hairline,
                        AxiomTheme.shapes.full
                    )
                    .clickable { onToggle(option.id) }
                    .padding(horizontal = AxiomTheme.space.md, vertical = AxiomTheme.space.sm)
                    .semantics {
                        stateDescription = if (selected) "Selected" else "Not selected"
                    }
            )
        }
    }
}

/** Segmented control for mutually exclusive modes (Write / Preview / Focus). */
@Composable
fun AxiomSegmented(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(AxiomTheme.shapes.full)
            .background(AxiomTheme.colors.surfaceSunken)
            .padding(3.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Text(
                text = label,
                style = AxiomTheme.type.uiLabelSmall,
                color = if (selected) AxiomTheme.colors.onAccent else AxiomTheme.colors.inkMuted,
                modifier = Modifier
                    .clip(AxiomTheme.shapes.full)
                    .background(if (selected) AxiomTheme.colors.accent else Color.Transparent)
                    .selectable(
                        selected = selected,
                        role = Role.Tab,
                        onClick = { onSelect(index) }
                    )
                    .padding(horizontal = AxiomTheme.space.md, vertical = 6.dp)
            )
        }
    }
}
