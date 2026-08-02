package com.cosmiclaboratory.axiom.ui.design

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cosmiclaboratory.axiom.domain.model.Entry
import com.cosmiclaboratory.axiom.domain.model.EntryKind
import com.cosmiclaboratory.axiom.domain.model.Tag
import com.cosmiclaboratory.axiom.domain.streak.StreakCalculator
import com.cosmiclaboratory.axiom.ui.design.components.*
import com.cosmiclaboratory.axiom.ui.theme.AxiomTheme
import com.cosmiclaboratory.axiom.ui.theme.ThemeMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Debug-only gallery. Every component, every state, in one scroll.
 *
 * Lives in the debug source set so it can never ship. Previews render it in both
 * themes, which is how a component that only works on dark gets caught.
 */

private fun sampleEntry(
    id: Long = 1,
    title: String = "Slept badly again",
    mood: Int? = 2,
    kind: EntryKind = EntryKind.FREE_FORM,
    favorite: Boolean = false
) = Entry(
    id = id,
    title = title,
    content = "Woke up at 3am and couldn't get back down. Spent an hour thinking about the review.",
    kind = kind,
    createdAt = LocalDateTime.now().minusHours(3),
    updatedAt = LocalDateTime.now().minusHours(3),
    mood = mood,
    isFavorite = favorite,
    wordCount = 240
)

@Composable
fun ComponentGallery(modifier: Modifier = Modifier) {
    var mood by remember { mutableStateOf<Int?>(3) }
    var segment by remember { mutableIntStateOf(0) }
    var filters by remember { mutableStateOf(setOf("all")) }
    val c = AxiomTheme.colors

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(c.canvas)
            .padding(horizontal = AxiomTheme.space.screenH),
        contentPadding = PaddingValues(vertical = AxiomTheme.space.xl),
        verticalArrangement = Arrangement.spacedBy(AxiomTheme.space.base)
    ) {
        item { SectionHeader("Entry cards") }
        item { EntryCard(sampleEntry(), onClick = {}) }
        item { EntryCard(sampleEntry(id = 2, title = "", mood = 5, kind = EntryKind.PROMPTED), onClick = {}) }
        item { EntryCard(sampleEntry(id = 3, mood = null, kind = EntryKind.VOICE, favorite = true), onClick = {}) }
        item { EntryCardSkeleton() }

        item { SectionHeader("Card tones") }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(AxiomTheme.space.sm)) {
                CardTone.entries.forEach { tone ->
                    AxiomCard(tone = tone) {
                        androidx.compose.material3.Text(
                            tone.name, style = AxiomTheme.type.uiBody, color = c.ink
                        )
                    }
                }
            }
        }

        item { SectionHeader("Mood") }
        item { MoodPicker(value = mood, onChange = { mood = it }) }

        item { SectionHeader("Chips") }
        item {
            AxiomFilterChipRow(
                options = listOf(
                    FilterOption("all", "All"),
                    FilterOption("written", "Written"),
                    FilterOption("prompted", "Prompted"),
                    FilterOption("voice", "Voice"),
                    FilterOption("fav", "Favourites")
                ),
                selectedIds = filters,
                onToggle = { id -> filters = if (id in filters) filters - id else filters + id }
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(AxiomTheme.space.sm)) {
                // Deliberately extreme colours: proves contrastingTextColor works.
                TagChip(Tag(1, "work", 0xFFFFF176), selected = true)
                TagChip(Tag(2, "sleep", 0xFF1A237E), selected = true)
                TagChip(Tag(3, "family", 0xFF4CAF50))
            }
        }
        item { AxiomSegmented(options = listOf("Write", "Preview", "Focus"), selectedIndex = segment, onSelect = { segment = it }) }

        item { SectionHeader("Stats") }
        item { StreakStrip(StreakCalculator.Result(4, 11, listOf(true, true, false, true, true, true, false))) }
        item {
            ContributionGrid(
                dates = (0..40 step 2).map { LocalDate.now().minusDays(it.toLong()) }.toSet()
            )
        }
        item {
            PatternCard(title = "Mood, last 30 days", subtitle = "18 of 30 days recorded", onInfo = {}) {
                // Nulls are gaps, not zeros — the whole point of the component.
                Sparkline(
                    points = listOf(3f, 4f, null, null, 2f, 2f, 3f, null, 4f, 5f, 4f, null, 3f, 3f, 4f)
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(AxiomTheme.space.sm)) {
                StatTile("Entries", "214", Modifier.weight(1f), delta = 12f)
                StatTile("Words", "48k", Modifier.weight(1f), delta = -8f)
            }
        }

        item { SectionHeader("Errors — all five AiResult states") }
        item { AxiomErrorSurface(AxiomError.NoAiKey, onPrimary = {}) }
        item { AxiomErrorSurface(AxiomError.RateLimited(20), onRetry = {}) }
        item { AxiomErrorSurface(AxiomError.Network(), onRetry = {}, onDismiss = {}) }
        item { AxiomErrorSurface(AxiomError.Malformed(), onRetry = {}) }
        item { AxiomErrorSurface(AxiomError.Permission("RECORD_AUDIO"), onPrimary = {}) }

        item { SectionHeader("Feedback") }
        item { AxiomInlineNotice("Prompts need a connection.", actionLabel = "Connect", onAction = {}) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(AxiomTheme.space.xs)) {
                SaveStateIndicator(SaveState.Editing)
                SaveStateIndicator(SaveState.Saving)
                SaveStateIndicator(SaveState.Saved(LocalTime.of(15, 42)))
                SaveStateIndicator(SaveState.Failed("disk full"), onRetry = {})
            }
        }
        item { ThinkingIndicator() }

        item { SectionHeader("Empty state") }
        item {
            AxiomEmptyState(
                title = "No entries yet",
                body = "Write your first line — it doesn't have to be good.",
                icon = Icons.Outlined.Palette,
                primaryLabel = "New entry",
                onPrimary = {},
                secondaryLabel = "Use a prompt",
                onSecondary = {}
            )
        }

        item { SectionHeader("Controls") }
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(AxiomTheme.space.sm),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                AxiomIconButton(Icons.Filled.Search, "Search", {})
                AxiomIconButton(Icons.Filled.Add, "New entry", {}, enabled = false)
                AxiomFab(Icons.Filled.Add, "New entry", {})
                AxiomFab(Icons.Filled.Add, "New entry", {}, expanded = false)
            }
        }
        item { Spacer(Modifier.height(AxiomTheme.space.huge)) }
    }
}

@Preview(name = "Components — paper light", heightDp = 3000)
@Composable
private fun PreviewLight() = AxiomTheme(themeMode = ThemeMode.LIGHT) { ComponentGallery() }

@Preview(name = "Components — OLED", heightDp = 3000)
@Composable
private fun PreviewDark() = AxiomTheme(themeMode = ThemeMode.AMOLED) { ComponentGallery() }

@Preview(name = "Components — 200% font", heightDp = 4200, fontScale = 2f)
@Composable
private fun PreviewLargeType() = AxiomTheme(themeMode = ThemeMode.DARK) { ComponentGallery() }
