package com.cosmiclaboratory.axiom.ui.screens.today

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cosmiclaboratory.axiom.domain.model.Entry
import com.cosmiclaboratory.axiom.ui.design.components.*
import com.cosmiclaboratory.axiom.ui.theme.AxiomTheme
import com.cosmiclaboratory.axiom.ui.viewmodels.today.TodayViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * The ritual surface. Answers "what do I do now" in under three seconds.
 *
 * Ordering is deliberate: the cheapest meaningful action (one-tap mood) sits
 * above the most expensive (writing an entry), so a user with ten seconds still
 * leaves a trace. That trace is also the supervised label the pattern engine
 * needs — the fastest interaction is also the most valuable one.
 */
@Composable
fun TodayScreen(
    onNewEntry: () -> Unit,
    onVoiceEntry: () -> Unit,
    onOpenEntry: (Long) -> Unit,
    onWriteAboutPrompt: (Long?) -> Unit,
    onSeeAllEntries: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TodayViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val c = AxiomTheme.colors

    // Collapse the FAB once the user is reading rather than deciding.
    val fabExpanded by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }

    AxiomScaffold(
        title = "",
        screenTag = "screen:today",
        modifier = modifier,
        actions = {
            AxiomTopBarAction(Icons.Outlined.Settings, "Settings", onOpenSettings)
        },
        floatingActionButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AxiomIconButton(Icons.Filled.Mic, "New voice entry", onVoiceEntry, tint = c.inkMuted)
                Spacer(Modifier.width(AxiomTheme.space.xs))
                AxiomFab(Icons.Filled.Add, "New entry", onNewEntry, expanded = fabExpanded)
            }
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("list:today"),
            contentPadding = PaddingValues(
                start = AxiomTheme.space.screenH,
                end = AxiomTheme.space.screenH,
                bottom = 96.dp
            ),
            verticalArrangement = Arrangement.spacedBy(AxiomTheme.space.base)
        ) {
            item(key = "greeting") {
                Column {
                    Text(state.greeting, style = AxiomTheme.type.uiDisplay, color = c.ink)
                    Text(
                        text = remember {
                            LocalDate.now()
                                .format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.getDefault()))
                        },
                        style = AxiomTheme.type.uiMeta,
                        color = c.inkFaint
                    )
                }
            }

            item(key = "mood") {
                val mood = state.todayMood
                if (mood == null) {
                    MoodCheckInCard(onPick = viewModel::recordMood)
                } else {
                    MoodRecordedRow(mood = mood, onChange = viewModel::recordMood)
                }
            }

            state.draft?.let { draft ->
                item(key = "draft") {
                    ContinueDraftCard(entry = draft, onClick = { onOpenEntry(draft.id) })
                }
            }

            item(key = "prompt") {
                PromptCard(
                    prompt = state.todayPrompt,
                    isFromEntries = state.promptIsPersonalised,
                    onWrite = { onWriteAboutPrompt(state.todayQuestionId) },
                    onShuffle = viewModel::shufflePrompt
                )
            }

            item(key = "streak") {
                AxiomCard { StreakStrip(state.streak) }
            }

            item(key = "recent-header") {
                SectionHeader("Recent") {
                    TextButton(onClick = onSeeAllEntries) {
                        Text("See all", style = AxiomTheme.type.uiLabel, color = c.accent)
                    }
                }
            }

            when {
                state.isLoading -> items(List(3) { it }) { EntryCardSkeleton() }
                state.recent.isEmpty() -> item(key = "empty") {
                    AxiomEmptyState(
                        title = "Nothing written yet",
                        body = "Start with today's prompt — it doesn't have to be good.",
                        primaryLabel = "New entry",
                        onPrimary = onNewEntry
                    )
                }
                else -> items(state.recent, key = { it.id }) { entry ->
                    EntryCard(entry = entry, onClick = { onOpenEntry(entry.id) })
                }
            }
        }
    }
}

/** Inline mood capture — one tap, no navigation, no confirmation step. */
@Composable
private fun MoodCheckInCard(onPick: (Int) -> Unit) {
    AxiomCard(tone = CardTone.Accent) {
        Text("How's today going?", style = AxiomTheme.type.uiTitle, color = AxiomTheme.colors.ink)
        Spacer(Modifier.height(AxiomTheme.space.md))
        MoodPicker(value = null, onChange = onPick, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun MoodRecordedRow(mood: Int, onChange: (Int) -> Unit) {
    var editing by remember { mutableStateOf(false) }
    AxiomCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MoodDot(mood = mood, size = 16.dp)
            Spacer(Modifier.width(AxiomTheme.space.sm))
            Text(
                "Feeling ${moodLabel(mood).lowercase(Locale.getDefault())}",
                style = AxiomTheme.type.uiBody,
                color = AxiomTheme.colors.ink,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = { editing = !editing }) {
                Text(
                    if (editing) "Done" else "Change",
                    style = AxiomTheme.type.uiLabel,
                    color = AxiomTheme.colors.accent
                )
            }
        }
        AnimatedVisibility(visible = editing) {
            Column {
                Spacer(Modifier.height(AxiomTheme.space.sm))
                MoodPicker(value = mood, onChange = { onChange(it); editing = false }, compact = true)
            }
        }
    }
}

@Composable
private fun ContinueDraftCard(entry: Entry, onClick: () -> Unit) {
    AxiomCard(tone = CardTone.Caution, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.EditNote, contentDescription = null, tint = AxiomTheme.colors.caution)
            Spacer(Modifier.width(AxiomTheme.space.sm))
            Column(Modifier.weight(1f)) {
                Text("Unfinished entry", style = AxiomTheme.type.uiTitleSmall, color = AxiomTheme.colors.ink)
                Text(
                    entry.displayTitle.ifBlank { "Untitled" },
                    style = AxiomTheme.type.uiBodySmall,
                    color = AxiomTheme.colors.inkMuted,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * The prompt renders in reading type, not UI type — it is something to sit with,
 * not a control. Actions are labelled text, never bare icons.
 */
@Composable
private fun PromptCard(
    prompt: String?,
    isFromEntries: Boolean,
    onWrite: () -> Unit,
    onShuffle: () -> Unit
) {
    if (prompt.isNullOrBlank()) return
    val c = AxiomTheme.colors
    AxiomCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Outlined.AutoAwesome,
                contentDescription = null,
                tint = if (isFromEntries) c.aiTint else c.inkFaint,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(6.dp))
            // Attribution is the product: the user should always know whether a
            // prompt is curated or derived from their own writing.
            Text(
                text = if (isFromEntries) "From your entries" else "Curated",
                style = AxiomTheme.type.uiOverline,
                color = if (isFromEntries) c.aiTint else c.inkFaint
            )
        }
        Spacer(Modifier.height(AxiomTheme.space.sm))
        Text(prompt, style = AxiomTheme.type.promptDisplay, color = c.ink)
        Spacer(Modifier.height(AxiomTheme.space.base))
        Row(horizontalArrangement = Arrangement.spacedBy(AxiomTheme.space.sm)) {
            TextButton(onClick = onWrite) {
                Text("Write about this", style = AxiomTheme.type.uiLabel, color = c.accent)
            }
            TextButton(onClick = onShuffle) {
                Icon(
                    Icons.Outlined.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = c.inkMuted
                )
                Spacer(Modifier.width(4.dp))
                Text("Shuffle", style = AxiomTheme.type.uiLabel, color = c.inkMuted)
            }
        }
    }
}
