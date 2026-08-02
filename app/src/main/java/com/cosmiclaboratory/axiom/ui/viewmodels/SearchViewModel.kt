package com.cosmiclaboratory.axiom.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cosmiclaboratory.axiom.data.repository.NotesRepository
import com.cosmiclaboratory.axiom.domain.model.Entry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val notesRepository: NotesRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()
    
    private var searchJob: Job? = null
    private val searchHistory = mutableListOf<String>()
    
    fun updateQuery(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
        
        if (query.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                searchResults = emptyList(),
                isSearching = false,
                showHistory = true
            )
        } else {
            scheduleSearch(query)
        }
    }
    
    private fun scheduleSearch(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300) // Debounce search
            performSearch(query)
        }
    }
    
    fun performSearch(query: String) {
        if (query.isBlank()) return
        
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isSearching = true,
                    showHistory = false
                )
                
                val results = notesRepository.searchNotes(query.trim())
                
                // Add to search history if not already present
                if (query.trim().isNotEmpty() && !searchHistory.contains(query.trim())) {
                    searchHistory.add(0, query.trim())
                    if (searchHistory.size > 10) {
                        searchHistory.removeAt(searchHistory.size - 1)
                    }
                }
                
                _uiState.value = _uiState.value.copy(
                    searchResults = results,
                    isSearching = false,
                    hasSearched = true,
                    searchHistory = searchHistory.toList()
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSearching = false,
                    errorMessage = "Search failed: ${e.message}"
                )
            }
        }
    }
    
    fun clearSearch() {
        searchJob?.cancel()
        _uiState.value = SearchUiState()
    }
    
    fun selectHistoryItem(query: String) {
        _uiState.value = _uiState.value.copy(query = query)
        performSearch(query)
    }
    
    fun clearErrorMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
    
    override fun onCleared() {
        super.onCleared()
        searchJob?.cancel()
    }
}

data class SearchUiState(
    val query: String = "",
    val searchResults: List<Entry> = emptyList(),
    val searchHistory: List<String> = emptyList(),
    val isSearching: Boolean = false,
    val hasSearched: Boolean = false,
    val showHistory: Boolean = true,
    val errorMessage: String? = null
)