package com.cosmiclaboratory.axiom.ui.screens.patterns

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cosmiclaboratory.axiom.ui.design.components.*
import com.cosmiclaboratory.axiom.ui.theme.AxiomTheme

/**
 * What the journal knows about you.
 *
 * Every module is a self-contained card so one failing computation cannot blank
 * the screen, and so the ML modules (mood forecast, findings, discovered themes,
 * semantic search) drop in later without touching this file.
 *
 * Nothing here calls an API. These are deterministic statistics over the user's
 * own rows — the surface stays useful with no key and no network.
 */
@Composable
fun PatternsScreen(
    modifier: Modifier = Modifier,
    viewModel: PatternsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val c = AxiomTheme.colors
    var infoText by remember { mutableStateOf<String?>(null) }

    AxiomScaffold(title = "Patterns", screenTag = "screen:patterns", modifier = modifier) { padding ->
        if (!state.hasEnoughData && !state.isLoading) {
            AxiomEmptyState(
                title = "Not enough to go on yet",
                body = "Patterns need a few entries. You have ${state.entryCount} of " +
                    "${PatternsUiState.MIN_ENTRIES} — write a couple more and come back.",
                modifier = Modifier.padding(padding)
            )
            return@AxiomScaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("list:patterns"),
            contentPadding = PaddingValues(
                start = AxiomTheme.space.screenH,
                end = AxiomTheme.space.screenH,
                bottom = AxiomTheme.space.huge
            ),
            verticalArrangement = Arrangement.spacedBy(AxiomTheme.space.base)
        ) {
            item("range") {
                AxiomSegmented(
                    options = PatternRange.entries.map { it.label },
                    selectedIndex = state.range.ordinal,
                    onSelect = { viewModel.setRange(PatternRange.entries[it]) }
                )
            }

            // The headline: what the companion has actually noticed, in the
            // same words it would use in conversation. Absent rather than
            // apologetic when there is nothing solid to say.
            if (state.findings.isNotEmpty()) {
                item("noticed") {
                    AxiomCard(tone = CardTone.Ai) {
                        Text("What I've noticed", style = AxiomTheme.type.uiOverline, color = c.aiTint)
                        Spacer(Modifier.height(AxiomTheme.space.sm))
                        Column(verticalArrangement = Arrangement.spacedBy(AxiomTheme.space.sm)) {
                            state.findings.take(4).forEach { finding ->
                                Text(
                                    finding.text,
                                    style = AxiomTheme.type.uiBody,
                                    color = c.ink
                                )
                            }
                        }
                        Spacer(Modifier.height(AxiomTheme.space.sm))
                        Text(
                            "Worked out on this device from what you've written. " +
                                "Nothing here was sent anywhere.",
                            style = AxiomTheme.type.uiBodySmall,
                            color = c.inkFaint
                        )
                    }
                }
            }

            item("streak") {
                PatternCard(
                    title = "Consistency",
                    subtitle = "${state.writtenDates.size} days written",
                    onInfo = {
                        infoText = "A streak counts consecutive days with at least one " +
                            "entry. Today doesn't break it until midnight, so an unwritten " +
                            "today never resets you to zero."
                    }
                ) {
                    StreakStrip(state.streak)
                    Spacer(Modifier.height(AxiomTheme.space.base))
                    ContributionGrid(dates = state.writtenDates, weeks = 12)
                }
            }

            item("mood") {
                PatternCard(
                    title = "Mood",
                    subtitle = "${state.moodDaysRecorded} of ${state.moodSeries.size} days recorded" +
                        if (state.inferredMoodCount > 0) " · ${state.inferredMoodCount} read from your writing" else "",
                    onInfo = {
                        infoText = "Each point is the average mood for that day. Some are moods " +
                            "you tapped; the rest are read from the words you wrote, and a mood " +
                            "you choose always overrides one that was inferred. Days with " +
                            "neither are left as gaps rather than plotted as zero — a day you " +
                            "didn't record isn't a bad day."
                    }
                ) {
                    if (state.moodDaysRecorded < 2) {
                        Text(
                            "A couple more days of writing and a trend will show up here.",
                            style = AxiomTheme.type.uiBodySmall,
                            color = c.inkFaint
                        )
                    } else {
                        Sparkline(points = state.moodSeries)
                    }

                    if (state.dominantEmotions.isNotEmpty()) {
                        Spacer(Modifier.height(AxiomTheme.space.base))
                        Text("Most often", style = AxiomTheme.type.uiOverline, color = c.inkFaint)
                        Spacer(Modifier.height(AxiomTheme.space.xs))
                        Row(horizontalArrangement = Arrangement.spacedBy(AxiomTheme.space.xs)) {
                            state.dominantEmotions.forEach { (emotion, count) ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(AxiomTheme.shapes.xs)
                                        .background(c.surfaceSunken)
                                        .padding(horizontal = AxiomTheme.space.sm, vertical = 4.dp)
                                ) {
                                    MoodDot(mood = emotion.valence, size = 8.dp)
                                    Spacer(Modifier.width(AxiomTheme.space.xs))
                                    Text(
                                        "${emotion.label} · $count",
                                        style = AxiomTheme.type.uiLabelSmall,
                                        color = c.inkMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item("rhythm") {
                PatternCard(
                    title = "Writing rhythm",
                    onInfo = {
                        infoText = "Counts are over the selected window. Time of day is " +
                            "taken from when each entry was created."
                    }
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(AxiomTheme.space.sm)) {
                        StatTile("Entries", "${state.entryCount}", Modifier.weight(1f))
                        StatTile("Per week", "${state.entriesPerWeek}", Modifier.weight(1f))
                        StatTile("Words", formatCount(state.totalWords), Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(AxiomTheme.space.base))
                    HourHistogram(state.hourHistogram)
                }
            }

            if (state.topThemes.isNotEmpty()) {
                item("themes") {
                    PatternCard(title = "Themes", subtitle = "Your most-used tags") {
                        Column(verticalArrangement = Arrangement.spacedBy(AxiomTheme.space.xs)) {
                            val max = state.topThemes.maxOf { it.second }.coerceAtLeast(1)
                            state.topThemes.forEach { (name, count) ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        name,
                                        style = AxiomTheme.type.uiBodySmall,
                                        color = c.ink,
                                        modifier = Modifier.width(96.dp)
                                    )
                                    Box(
                                        Modifier
                                            .weight(count.toFloat() / max)
                                            .height(8.dp)
                                            .clip(AxiomTheme.shapes.xs)
                                            .background(c.accent)
                                    )
                                    Spacer(Modifier.weight((max - count).toFloat() / max + 0.01f))
                                    Text(
                                        "$count",
                                        style = AxiomTheme.type.uiNumeric,
                                        color = c.inkFaint
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item("milestones") {
                PatternCard(title = "Milestones") {
                    Row(horizontalArrangement = Arrangement.spacedBy(AxiomTheme.space.sm)) {
                        StatTile("Longest streak", "${state.streak.longest}", Modifier.weight(1f))
                        StatTile("Days written", "${state.writtenDates.size}", Modifier.weight(1f))
                    }
                }
            }

            // Trend detection and discovered themes landed; what remains is
            // named honestly rather than left as a vague promise.
            item("coming") {
                AxiomCard(tone = CardTone.Ai) {
                    Text("Coming here", style = AxiomTheme.type.uiOverline, color = c.aiTint)
                    Spacer(Modifier.height(AxiomTheme.space.xs))
                    Text(
                        "Search that understands meaning rather than matching words, and " +
                            "mood forecasting — both computed on this device, no key required.",
                        style = AxiomTheme.type.uiBodySmall,
                        color = c.inkMuted
                    )
                    Spacer(Modifier.height(AxiomTheme.space.sm))
                    Text(
                        "Reading mood from your writing already works here without a key: " +
                            "it learns from the days you tapped a mood yourself.",
                        style = AxiomTheme.type.uiBodySmall,
                        color = c.inkFaint
                    )
                }
            }
        }
    }

    infoText?.let { text ->
        AxiomBottomSheet(title = "How this is calculated", onDismiss = { infoText = null }) {
            Text(text, style = AxiomTheme.type.uiBody, color = AxiomTheme.colors.inkMuted)
        }
    }
}

/** 24 bars, one per hour. Decorative — the card title carries the meaning. */
@Composable
private fun HourHistogram(counts: List<Int>) {
    val c = AxiomTheme.colors
    val max = counts.maxOrNull()?.coerceAtLeast(1) ?: 1
    Column {
        Row(
            Modifier.fillMaxWidth().height(48.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            counts.forEach { count ->
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight(if (count == 0) 0.04f else count.toFloat() / max)
                        .clip(AxiomTheme.shapes.xs)
                        .background(if (count == 0) c.surfaceSunken else c.accent)
                )
            }
        }
        Spacer(Modifier.height(AxiomTheme.space.xs))
        Row(Modifier.fillMaxWidth()) {
            listOf("00", "06", "12", "18", "24").forEach { label ->
                Text(
                    label,
                    style = AxiomTheme.type.uiLabelSmall,
                    color = c.inkFaint,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

private fun formatCount(n: Int): String = when {
    n >= 1_000_000 -> "${n / 100_000 / 10f}M"
    n >= 1_000 -> "${n / 100 / 10f}k"
    else -> "$n"
}
