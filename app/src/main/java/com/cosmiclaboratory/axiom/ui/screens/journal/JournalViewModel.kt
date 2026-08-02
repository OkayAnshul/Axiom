package com.cosmiclaboratory.axiom.ui.screens.journal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cosmiclaboratory.axiom.data.repository.JournalRepository
import com.cosmiclaboratory.axiom.domain.model.Entry
import com.cosmiclaboratory.axiom.domain.model.EntryKind
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/** Timeline filters. Ids are stable so chip selection survives config change. */
enum class JournalFilter(val id: String, val label: String) {
    All("all", "All"),
    Written("written", "Written"),
    Prompted("prompted", "Prompted"),
    Voice("voice", "Voice"),
    Favourites("fav", "Favourites"),
    Archive("archive", "Archive");

    companion object {
        fun fromId(id: String) = entries.firstOrNull { it.id == id } ?: All
    }
}

/** One day's entries, ready for a sticky header. */
data class JournalDay(val date: LocalDate, val entries: List<Entry>)

data class JournalUiState(
    val isLoading: Boolean = true,
    val filter: JournalFilter = JournalFilter.All,
    val days: List<JournalDay> = emptyList(),
    val totalCount: Int = 0
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class JournalViewModel @Inject constructor(
    private val entries: JournalRepository
) : ViewModel() {

    private val filter = MutableStateFlow(JournalFilter.All)

    /**
     * Grouping happens here rather than in the composable so the work is done
     * once per data change instead of once per recomposition.
     */
    val state: StateFlow<JournalUiState> =
        filter.flatMapLatest { f ->
            when (f) {
                JournalFilter.All -> entries.observeCompleted()
                JournalFilter.Written -> entries.observeByKind(EntryKind.FREE_FORM)
                JournalFilter.Prompted -> entries.observeByKind(EntryKind.PROMPTED)
                JournalFilter.Voice -> entries.observeByKind(EntryKind.VOICE)
                JournalFilter.Favourites -> entries.observeFavorites()
                JournalFilter.Archive -> entries.observeArchived()
            }.map { list -> f to list }
        }.map { (f, list) ->
            JournalUiState(
                isLoading = false,
                filter = f,
                days = list
                    .groupBy { it.createdAt.toLocalDate() }
                    .toSortedMap(reverseOrder())
                    .map { (date, dayEntries) -> JournalDay(date, dayEntries) },
                totalCount = list.size
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = JournalUiState()
        )

    fun setFilter(f: JournalFilter) {
        filter.value = f
    }

    fun toggleFavorite(entry: Entry) {
        viewModelScope.launch { entries.setFavorite(entry.id, !entry.isFavorite) }
    }

    /** Archive is reversible; the caller pairs this with an undo snackbar. */
    fun setArchived(entry: Entry, archived: Boolean) {
        viewModelScope.launch { entries.setArchived(entry.id, archived) }
    }
}
