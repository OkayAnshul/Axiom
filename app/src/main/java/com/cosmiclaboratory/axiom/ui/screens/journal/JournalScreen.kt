package com.cosmiclaboratory.axiom.ui.screens.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cosmiclaboratory.axiom.ui.components.LocalSnackbarHostState
import com.cosmiclaboratory.axiom.ui.design.components.*
import com.cosmiclaboratory.axiom.ui.theme.AxiomTheme
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * The chronological timeline — the shape Apple Journal gets right and the one
 * that matches how people actually look for an entry ("it was around March").
 *
 * Sticky date headers rather than dates repeated on every card: the date is
 * structure, not per-item metadata.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalScreen(
    onOpenEntry: (Long) -> Unit,
    onNewEntry: () -> Unit,
    onSearch: () -> Unit,
    onCalendar: () -> Unit,
    /** Back to the conversation — by arrow or by swiping left. */
    onBackToCompanion: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: JournalViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val snackbar = LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()
    val c = AxiomTheme.colors

    val fabExpanded by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }

    AxiomScaffold(
        title = "Your story",
        screenTag = "screen:journal",
        // Swiping left is the mirror of swiping right out of the conversation;
        // the arrow is here because a gesture must never be the only way back.
        modifier = modifier.swipeBetween(onSwipeLeft = onBackToCompanion),
        navigationIcon = {
            AxiomIconButton(
                Icons.AutoMirrored.Filled.ArrowBack,
                "Back to the conversation",
                onBackToCompanion
            )
        },
        actions = {
            AxiomTopBarAction(Icons.Filled.Search, "Search", onSearch)
            AxiomTopBarAction(Icons.Outlined.CalendarMonth, "Calendar", onCalendar)
        },
        floatingActionButton = {
            AxiomFab(Icons.Filled.Add, "New entry", onNewEntry, expanded = fabExpanded)
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            AxiomFilterChipRow(
                options = JournalFilter.entries.map { FilterOption(it.id, it.label) },
                selectedIds = setOf(state.filter.id),
                onToggle = { id -> viewModel.setFilter(JournalFilter.fromId(id)) },
                modifier = Modifier.padding(vertical = AxiomTheme.space.sm)
            )

            when {
                state.isLoading -> LazyColumn(
                    contentPadding = PaddingValues(AxiomTheme.space.screenH),
                    verticalArrangement = Arrangement.spacedBy(AxiomTheme.space.listGap)
                ) {
                    items(List(6) { it }) { EntryCardSkeleton() }
                }

                state.days.isEmpty() -> EmptyForFilter(
                    filter = state.filter,
                    onNewEntry = onNewEntry,
                    onClearFilter = { viewModel.setFilter(JournalFilter.All) }
                )

                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("list:journal"),
                    contentPadding = PaddingValues(
                        start = AxiomTheme.space.screenH,
                        end = AxiomTheme.space.screenH,
                        bottom = 96.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(AxiomTheme.space.listGap)
                ) {
                    state.days.forEach { day ->
                        stickyHeader(key = "h-${day.date}") {
                            TimelineDateHeader(day.date, day.entries.size)
                        }
                        items(day.entries, key = { it.id }) { entry ->
                            EntryCard(
                                entry = entry,
                                onClick = { onOpenEntry(entry.id) },
                                onLongClick = {
                                    // Archive is reversible and always undoable —
                                    // never a silent destructive action.
                                    viewModel.setArchived(entry, !entry.isArchived)
                                    scope.launch {
                                        val undone = snackbar.showUndo(
                                            if (entry.isArchived) "Unarchived" else "Archived"
                                        )
                                        if (undone) viewModel.setArchived(entry, entry.isArchived)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/** "Today" / "Yesterday" / "March 2026" — relative where it helps, absolute where it doesn't. */
@Composable
private fun TimelineDateHeader(date: LocalDate, count: Int) {
    val c = AxiomTheme.colors
    val label = remember(date) {
        val today = LocalDate.now()
        when (date) {
            today -> "Today"
            today.minusDays(1) -> "Yesterday"
            else -> {
                val pattern = if (date.year == today.year) "EEEE d MMMM" else "d MMMM yyyy"
                date.format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()))
            }
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(c.canvas)
            .padding(top = AxiomTheme.space.base, bottom = AxiomTheme.space.xs)
    ) {
        Text(label, style = AxiomTheme.type.uiTitleSmall, color = c.ink, modifier = Modifier.weight(1f))
        Text("$count", style = AxiomTheme.type.uiNumeric, color = c.inkFaint)
    }
}

@Composable
private fun EmptyForFilter(
    filter: JournalFilter,
    onNewEntry: () -> Unit,
    onClearFilter: () -> Unit
) {
    // Empty-because-filtered and empty-because-new need different copy and
    // different actions; conflating them is how users conclude an app is broken.
    if (filter == JournalFilter.All) {
        AxiomEmptyState(
            title = "We'll write your story together",
            body = "Start anywhere. It doesn't have to be good — that's rather the point.",
            primaryLabel = "Write something",
            onPrimary = onNewEntry
        )
    } else {
        AxiomEmptyState(
            title = "Nothing under that",
            body = "Nothing you've written matches \"${filter.label}\" yet.",
            primaryLabel = "Show everything",
            onPrimary = onClearFilter
        )
    }
}
