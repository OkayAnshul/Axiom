package com.cosmiclaboratory.axiom.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cosmiclaboratory.axiom.data.repository.JournalRepository
import com.cosmiclaboratory.axiom.domain.model.Entry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotesViewModel @Inject constructor(
    private val journalRepository: JournalRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(NotesUiState())
    val uiState: StateFlow<NotesUiState> = _uiState.asStateFlow()
    
    init {
        loadNotes()
    }
    
    private fun loadNotes() {
        viewModelScope.launch {
            journalRepository.observeAll().collect { notes ->
                _uiState.value = _uiState.value.copy(
                    entries = notes,
                    isLoading = false
                )
            }
        }
    }
    
    fun createNote(title: String, content: String) {
        viewModelScope.launch {
            try {
                val note = Entry(
                    title = title,
                    content = content,
                    markdown = content
                )
                journalRepository.upsert(note)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to create note: ${e.message}"
                )
            }
        }
    }
    
    fun createVoiceNote(voiceText: String) {
        viewModelScope.launch {
            try {
                // Generate a title from the first few words
                val title = generateTitleFromVoiceText(voiceText)
                val note = Entry(
                    title = title,
                    content = voiceText,
                    markdown = voiceText
                )
                journalRepository.upsert(note)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to create voice note: ${e.message}"
                )
            }
        }
    }
    
    private fun generateTitleFromVoiceText(text: String): String {
        val words = text.trim().split(" ")
        return when {
            words.isEmpty() -> "Voice Entry"
            words.size <= 5 -> text
            else -> words.take(5).joinToString(" ") + "..."
        }.take(50) // Limit title length
    }
    
    fun updateNote(note: Entry) {
        viewModelScope.launch {
            try {
                journalRepository.upsert(note)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to update note: ${e.message}"
                )
            }
        }
    }
    
    fun deleteNote(note: Entry) {
        viewModelScope.launch {
            try {
                journalRepository.delete(note)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to delete note: ${e.message}"
                )
            }
        }
    }
    
    fun clearErrorMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
    
    fun searchNotes(query: String) {
        if (query.isBlank()) {
            loadNotes()
            return
        }
        
        viewModelScope.launch {
            try {
                val searchResults = journalRepository.search(query)
                _uiState.value = _uiState.value.copy(
                    entries = searchResults,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Search failed: ${e.message}"
                )
            }
        }
    }
}

data class NotesUiState(
    val entries: List<Entry> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)