package com.cosmiclaboratory.axiom.ui.screens.patterns

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
 * What I've noticed.
 *
 * This was a dashboard: a contribution grid, tiles reading "Entries / Per week /
 * Words", and a streak counter. All of that still exists and still computes — it
 * has simply stopped being the first thing you see. A companion that opens with
 * your numbers is grading you, and a journal is not a workout.
 *
 * What leads instead is [com.cosmiclaboratory.axiom.domain.patterns.PatternFinder]'s
 * findings, which are already written as finished sentences rather than as data.
 * Their confidence score, previously used only to sort, now decides whether the
 * observation is stated plainly or hedged — never rendered as a number, because
 * "0.62 confident" is not something a person says.
 *
 * Nothing here calls an API. These are deterministic statistics over the user's
 * own rows — the surface stays useful with no key and no network.
 */
@Composable
fun PatternsScreen(
    /** Back to the conversation — by arrow or by swiping left. */
    onBackToCompanion: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PatternsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val c = AxiomTheme.colors
    var infoText by remember { mutableStateOf<String?>(null) }
    var showNumbers by remember { mutableStateOf(false) }

    AxiomScaffold(
        title = "What I've noticed",
        screenTag = "screen:patterns",
        modifier = modifier.swipeBetween(onSwipeLeft = onBackToCompanion),
        navigationIcon = {
            AxiomIconButton(
                Icons.AutoMirrored.Filled.ArrowBack,
                "Back to the conversation",
                onBackToCompanion
            )
        }
    ) { padding ->
        if (!state.hasEnoughData && !state.isLoading) {
            AxiomEmptyState(
                title = "Nothing worth saying yet",
                body = "I'd rather stay quiet than guess. Write a couple more times and " +
                    "I'll start to see the shape of things.",
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
            // The whole screen, really. Absent rather than apologetic when there
            // is nothing solid to say.
            if (state.findings.isNotEmpty()) {
                item("noticed") {
                    Column(verticalArrangement = Arrangement.spacedBy(AxiomTheme.space.xl)) {
                        state.findings.take(4).forEach { finding ->
                            Column {
                                Text(
                                    finding.text,
                                    style = AxiomTheme.type.readingBody,
                                    color = c.ink
                                )
                                if (finding.confidence < HEDGE_BELOW) {
                                    Spacer(Modifier.height(AxiomTheme.space.xs))
                                    Text(
                                        // The hedge is its own line rather than a
                                        // prefix: several findings open on a proper
                                        // noun, and lower-casing "Riya" to graft on
                                        // "I'm not certain, but…" would read worse
                                        // than the uncertainty it was hiding.
                                        "I'm not certain about this one.",
                                        style = AxiomTheme.type.uiBodySmall,
                                        color = c.inkFaint
                                    )
                                }
                            }
                        }
                    }
                }
                item("noticed-provenance") {
                    Text(
                        "Worked out on this device, from what you've written. " +
                            "None of it was sent anywhere.",
                        style = AxiomTheme.type.uiBodySmall,
                        color = c.inkFaint
                    )
                }
            } else {
                item("noticed-empty") {
                    Text(
                        "Nothing I'd want to claim yet. I'd rather wait until I'm surer.",
                        style = AxiomTheme.type.readingBody,
                        color = c.inkMuted
                    )
                }
            }

            item("numbers-toggle") {
                NumbersDisclosure(
                    expanded = showNumbers,
                    onToggle = { showNumbers = !showNumbers }
                )
            }

            if (showNumbers) {
            item("range") {
                AxiomSegmented(
                    options = PatternRange.entries.map { it.label },
                    selectedIndex = state.range.ordinal,
                    onSelect = { viewModel.setRange(PatternRange.entries[it]) }
                )
            }

            item("streak") {
                PatternCard(
                    title = "Showing up",
                    subtitle = "${state.writtenDates.size} days written",
                    onInfo = {
                        infoText = "A run counts consecutive days with at least one " +
                            "entry. Today doesn't break it until midnight, so an unwritten " +
                            "today never resets you to zero. It's here rather than on the " +
                            "home screen on purpose — a number you can lose is a bad reason " +
                            "to write."
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
                PatternCard(title = "Altogether") {
                    Row(horizontalArrangement = Arrangement.spacedBy(AxiomTheme.space.sm)) {
                        StatTile("Longest run", "${state.streak.longest}", Modifier.weight(1f))
                        StatTile("Days written", "${state.writtenDates.size}", Modifier.weight(1f))
                    }
                }
            }
            } // end showNumbers
        }
    }

    infoText?.let { text ->
        AxiomBottomSheet(title = "How this is calculated", onDismiss = { infoText = null }) {
            Text(text, style = AxiomTheme.type.uiBody, color = AxiomTheme.colors.inkMuted)
        }
    }
}

/**
 * Below this, an observation is offered with an audible shrug.
 *
 * PatternFinder computes confidence as
 * `0.4 * min(samples/10, 1) + 0.6 * min(magnitude/2, 1)`, so this threshold sits
 * roughly where either the sample is thin or the effect is small. Better to say
 * "I'm not certain" too often than to sound sure about someone's Mondays.
 */
private const val HEDGE_BELOW = 0.6f

/**
 * The one way into the numbers.
 *
 * Deliberately a sentence rather than a tab or a chart icon: the counting still
 * exists for anyone who wants it, but reaching it should be a small decision you
 * make, not the default view of your own life.
 */
@Composable
private fun NumbersDisclosure(expanded: Boolean, onToggle: () -> Unit) {
    val c = AxiomTheme.colors
    Column(
        Modifier
            .fillMaxWidth()
            .clip(AxiomTheme.shapes.sm)
            .clickable(onClick = onToggle)
            .padding(vertical = AxiomTheme.space.md)
    ) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(c.hairline))
        Spacer(Modifier.height(AxiomTheme.space.md))
        Text(
            if (expanded) "hide the numbers" else "the numbers, if you'd like them",
            style = AxiomTheme.type.uiBodySmall,
            color = c.inkFaint
        )
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
