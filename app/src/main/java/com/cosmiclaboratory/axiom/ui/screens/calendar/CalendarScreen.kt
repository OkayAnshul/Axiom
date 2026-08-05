package com.cosmiclaboratory.axiom.ui.screens.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cosmiclaboratory.axiom.ui.design.currentLocale
import com.cosmiclaboratory.axiom.ui.design.AxiomDimens
import com.cosmiclaboratory.axiom.ui.design.components.*
import com.cosmiclaboratory.axiom.ui.theme.AxiomTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

/**
 * Month grid over the journal.
 *
 * The forgiving part matters: selecting an empty past day offers to write an
 * entry *for that date*. Missing a day should not mean the day is closed —
 * that is what turns a streak from encouragement into pressure.
 */
@Composable
fun CalendarScreen(
    onBack: () -> Unit,
    onOpenEntry: (Long) -> Unit,
    onWriteForDate: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val c = AxiomTheme.colors

    AxiomScaffold(
        title = "Calendar",
        screenTag = "screen:calendar",
        modifier = modifier,
        navigationIcon = { AxiomIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Back", onBack) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag("list:calendar"),
            contentPadding = PaddingValues(AxiomTheme.space.screenH),
            verticalArrangement = Arrangement.spacedBy(AxiomTheme.space.base)
        ) {
            item("nav") {
                MonthHeader(
                    month = state.month,
                    onPrev = { viewModel.showMonth(state.month.minusMonths(1)) },
                    onNext = { viewModel.showMonth(state.month.plusMonths(1)) }
                )
            }

            item("grid") {
                MonthGrid(
                    month = state.month,
                    written = state.writtenDates,
                    moods = state.moodByDate,
                    selected = state.selectedDate,
                    onSelect = viewModel::selectDate
                )
            }

            item("selected-header") {
                SectionHeader(
                    state.selectedDate.format(
                        DateTimeFormatter.ofPattern("EEEE d MMMM", currentLocale())
                    )
                )
            }

            if (state.selectedEntries.isEmpty()) {
                item("empty") {
                    AxiomEmptyState(
                        title = "Nothing written",
                        body = if (state.selectedDate.isAfter(LocalDate.now())) {
                            "This day hasn't happened yet."
                        } else {
                            "You can still write about this day."
                        },
                        primaryLabel = if (state.selectedDate.isAfter(LocalDate.now())) null
                        else "Write an entry for ${state.selectedDate.dayOfMonth} " +
                            state.selectedDate.month.getDisplayName(
                                JavaTextStyle.SHORT, currentLocale()
                            ),
                        onPrimary = { onWriteForDate(state.selectedDate) }
                    )
                }
            } else {
                items(state.selectedEntries, key = { it.id }) { entry ->
                    EntryCard(
                        entry = entry,
                        onClick = { onOpenEntry(entry.id) },
                        modifier = axiomItemMotion()
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthHeader(month: YearMonth, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        AxiomIconButton(Icons.Filled.ChevronLeft, "Previous month", onPrev)
        Text(
            text = month.format(DateTimeFormatter.ofPattern("MMMM yyyy", currentLocale())),
            style = AxiomTheme.type.uiTitle,
            color = AxiomTheme.colors.ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
        AxiomIconButton(Icons.Filled.ChevronRight, "Next month", onNext)
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    written: Set<LocalDate>,
    moods: Map<LocalDate, Int>,
    selected: LocalDate,
    onSelect: (LocalDate) -> Unit
) {
    val c = AxiomTheme.colors
    val today = remember { LocalDate.now() }
    val firstDay = month.atDay(1)
    // Locale-correct week start: Monday in most of the world, Sunday in the US.
    val weekStart = java.time.temporal.WeekFields.of(currentLocale()).firstDayOfWeek
    val leadingBlanks = ((firstDay.dayOfWeek.value - weekStart.value) + 7) % 7
    val dayLabels = (0..6).map { weekStart.plus(it.toLong()) }

    Column(verticalArrangement = Arrangement.spacedBy(AxiomTheme.space.xs)) {
        Row {
            dayLabels.forEach { dow ->
                Text(
                    text = dow.getDisplayName(JavaTextStyle.NARROW, currentLocale()),
                    style = AxiomTheme.type.uiLabelSmall,
                    color = c.inkFaint,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        val cells = leadingBlanks + month.lengthOfMonth()
        val rows = (cells + 6) / 7
        repeat(rows) { row ->
            Row {
                repeat(7) { col ->
                    val dayIndex = row * 7 + col - leadingBlanks + 1
                    if (dayIndex < 1 || dayIndex > month.lengthOfMonth()) {
                        Box(Modifier.weight(1f).height(AxiomDimens.MinTouchTarget))
                    } else {
                        val date = month.atDay(dayIndex)
                        DayCell(
                            date = date,
                            hasEntry = date in written,
                            mood = moods[date],
                            isSelected = date == selected,
                            isToday = date == today,
                            isFuture = date.isAfter(today),
                            onClick = { onSelect(date) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    hasEntry: Boolean,
    mood: Int?,
    isSelected: Boolean,
    isToday: Boolean,
    isFuture: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val c = AxiomTheme.colors
    Column(
        modifier = modifier
            // Full 48dp target even though the visual dot is smaller.
            .height(AxiomDimens.MinTouchTarget)
            .clip(AxiomTheme.shapes.sm)
            .then(if (isSelected) Modifier.background(c.accentSoft) else Modifier)
            .then(
                if (isToday && !isSelected) Modifier.border(1.dp, c.accent, AxiomTheme.shapes.sm)
                else Modifier
            )
            .clickable(enabled = !isFuture, onClick = onClick)
            .semantics {
                stateDescription = buildString {
                    append(date.dayOfMonth)
                    if (hasEntry) append(", written") else append(", no entry")
                    if (mood != null) append(", mood ${moodLabel(mood)}")
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "${date.dayOfMonth}",
            style = AxiomTheme.type.uiNumeric.copy(fontSize = AxiomTheme.type.uiBodySmall.fontSize),
            color = when {
                isFuture -> c.inkFaint.copy(alpha = 0.5f)
                isSelected -> c.onAccentSoft
                else -> c.ink
            }
        )
        Spacer(Modifier.height(2.dp))
        Box(Modifier.height(6.dp)) {
            if (hasEntry) MoodDot(mood = mood, size = 6.dp, color = if (mood == null) c.accent else null)
        }
    }
}
