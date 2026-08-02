package com.cosmiclaboratory.axiom.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cosmiclaboratory.axiom.data.repository.JournalRepository
import com.cosmiclaboratory.axiom.domain.model.Entry
import com.cosmiclaboratory.axiom.domain.usecase.journal.SearchEntriesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class JournalHistoryUiState(
    val entries: List<Entry> = emptyList(),
    val query: String = "",
    val isSearching: Boolean = false
)

@HiltViewModel
class JournalHistoryViewModel @Inject constructor(
    private val journalRepo: JournalRepository,
    private val searchEntries: SearchEntriesUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(JournalHistoryUiState())
    val state: StateFlow<JournalHistoryUiState> = _state.asStateFlow()
    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            journalRepo.observeCompleted().collect { entries ->
                if (_state.value.query.isBlank()) {
                    _state.update { it.copy(entries = entries) }
                }
            }
        }
    }

    fun setQuery(q: String) {
        _state.update { it.copy(query = q) }
        searchJob?.cancel()
        if (q.isBlank()) {
            viewModelScope.launch {
                val entries = journalRepo.observeCompleted()
                entries.collect { _state.update { s -> s.copy(entries = it) } }
            }
            return
        }
        searchJob = viewModelScope.launch {
            delay(300)
            _state.update { it.copy(isSearching = true) }
            val results = searchEntries(q)
            _state.update { it.copy(entries = results, isSearching = false) }
        }
    }
}
