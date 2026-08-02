package com.cosmiclaboratory.axiom.ui.screens.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cosmiclaboratory.axiom.data.repository.JournalRepository
import com.cosmiclaboratory.axiom.domain.model.Entry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class CalendarUiState(
    val isLoading: Boolean = true,
    val month: YearMonth = YearMonth.now(),
    /** Dates with at least one entry, for density marks. */
    val writtenDates: Set<LocalDate> = emptySet(),
    /** Dominant mood per date, for the day-cell colour. */
    val moodByDate: Map<LocalDate, Int> = emptyMap(),
    val selectedDate: LocalDate = LocalDate.now(),
    val selectedEntries: List<Entry> = emptyList()
)

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val entries: JournalRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CalendarUiState())
    val state: StateFlow<CalendarUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            // Reads straight from entries rather than daily_summaries: that table
            // has never been populated (rebuildFor has no callers), so relying on
            // it would render an empty calendar over a full journal.
            val dates = entries.entryDates().toSet()
            _state.update { it.copy(writtenDates = dates, isLoading = false) }
            loadMonthMoods(_state.value.month)
            selectDate(LocalDate.now())
        }
    }

    fun showMonth(month: YearMonth) {
        _state.update { it.copy(month = month) }
        viewModelScope.launch { loadMonthMoods(month) }
    }

    fun selectDate(date: LocalDate) {
        _state.update { it.copy(selectedDate = date) }
        viewModelScope.launch {
            _state.update { it.copy(selectedEntries = entries.forDay(date)) }
        }
    }

    private suspend fun loadMonthMoods(month: YearMonth) {
        val moods = mutableMapOf<LocalDate, Int>()
        (1..month.lengthOfMonth()).forEach { day ->
            val date = month.atDay(day)
            if (date in _state.value.writtenDates) {
                entries.forDay(date).firstNotNullOfOrNull { it.mood }?.let { moods[date] = it }
            }
        }
        _state.update { it.copy(moodByDate = it.moodByDate + moods) }
    }
}
