package com.cosmiclaboratory.axiom.ui.screens.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.cosmiclaboratory.axiom.data.repository.JournalRepository
import com.cosmiclaboratory.axiom.domain.model.Entry
import com.cosmiclaboratory.axiom.ui.navigation.Search
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val results: List<Entry> = emptyList(),
    val isSearching: Boolean = false,
    val hasSearched: Boolean = false,
    val corpusSize: Int = 0,
    val recent: List<String> = emptyList()
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val entries: JournalRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            _state.update { it.copy(corpusSize = entries.count()) }
        }
        val initial = savedStateHandle.toRoute<Search>().initialQuery
        if (initial.isNotBlank()) setQuery(initial)
    }

    fun setQuery(value: String) {
        _state.update { it.copy(query = value) }
        searchJob?.cancel()
        if (value.trim().length < MIN_QUERY_LENGTH) {
            _state.update { it.copy(results = emptyList(), hasSearched = false, isSearching = false) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(DEBOUNCE_MS)
            _state.update { it.copy(isSearching = true) }
            // Raw text goes in — the repository sanitises. Passing a pre-built
            // FTS expression here would be double-processed.
            val results = runCatching { entries.search(value) }.getOrDefault(emptyList())
            _state.update {
                it.copy(results = results, isSearching = false, hasSearched = true)
            }
        }
    }

    fun commitToRecent() {
        val q = _state.value.query.trim()
        if (q.length < MIN_QUERY_LENGTH) return
        _state.update { it.copy(recent = (listOf(q) + it.recent).distinct().take(8)) }
    }

    fun clear() {
        searchJob?.cancel()
        _state.update { it.copy(query = "", results = emptyList(), hasSearched = false) }
    }

    private companion object {
        const val DEBOUNCE_MS = 250L
        const val MIN_QUERY_LENGTH = 2
    }
}
