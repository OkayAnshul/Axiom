package com.cosmiclaboratory.axiom.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cosmiclaboratory.axiom.data.repository.NotesRepository
import com.cosmiclaboratory.axiom.domain.model.Note
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotesViewModel @Inject constructor(
    private val notesRepository: NotesRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(NotesUiState())
    val uiState: StateFlow<NotesUiState> = _uiState.asStateFlow()
    
    init {
        loadNotes()
    }
    
    private fun loadNotes() {
        viewModelScope.launch {
            notesRepository.getAllNotes().collect { notes ->
                _uiState.value = _uiState.value.copy(
                    notes = notes,
                    isLoading = false
                )
            }
        }
    }
    
    fun createNote(title: String, content: String) {
        viewModelScope.launch {
            try {
                val note = Note(
                    title = title,
                    content = content,
                    markdown = content
                )
                notesRepository.insertNote(note)
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
                val note = Note(
                    title = title,
                    content = voiceText,
                    markdown = voiceText
                )
                notesRepository.insertNote(note)
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
            words.isEmpty() -> "Voice Note"
            words.size <= 5 -> text
            else -> words.take(5).joinToString(" ") + "..."
        }.take(50) // Limit title length
    }
    
    fun updateNote(note: Note) {
        viewModelScope.launch {
            try {
                notesRepository.updateNote(note)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to update note: ${e.message}"
                )
            }
        }
    }
    
    fun deleteNote(note: Note) {
        viewModelScope.launch {
            try {
                notesRepository.deleteNote(note)
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
                val searchResults = notesRepository.searchNotes(query)
                _uiState.value = _uiState.value.copy(
                    notes = searchResults,
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
    val notes: List<Note> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)