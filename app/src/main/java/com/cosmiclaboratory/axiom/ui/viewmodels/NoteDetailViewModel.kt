package com.cosmiclaboratory.axiom.ui.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cosmiclaboratory.axiom.data.repository.NotesRepository
import com.cosmiclaboratory.axiom.domain.model.Note
import com.cosmiclaboratory.axiom.ui.navigation.AxiomScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

@HiltViewModel
class NoteDetailViewModel @Inject constructor(
    private val notesRepository: NotesRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    private val noteId: Long? = savedStateHandle.get<Long>(AxiomScreen.NoteDetail.NOTE_ID_ARG)
        ?.takeIf { it != -1L }
    
    private val _uiState = MutableStateFlow(NoteDetailUiState())
    val uiState: StateFlow<NoteDetailUiState> = _uiState.asStateFlow()
    
    private var autoSaveJob: Job? = null
    private var currentNote: Note? = null
    
    init {
        if (noteId != null) {
            loadNote(noteId)
        } else {
            // Creating new note
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                isNewNote = true
            )
        }
    }
    
    private fun loadNote(id: Long) {
        viewModelScope.launch {
            try {
                val note = notesRepository.getNoteById(id)
                if (note != null) {
                    currentNote = note
                    _uiState.value = _uiState.value.copy(
                        title = note.title,
                        content = note.content,
                        isLoading = false,
                        isNewNote = false,
                        note = note
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Note not found"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to load note: ${e.message}"
                )
            }
        }
    }
    
    fun updateTitle(newTitle: String) {
        _uiState.value = _uiState.value.copy(title = newTitle)
        scheduleAutoSave()
    }
    
    fun updateContent(newContent: String) {
        _uiState.value = _uiState.value.copy(content = newContent)
        scheduleAutoSave()
    }
    
    private fun scheduleAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(1000) // Wait 1 second after user stops typing
            saveNote()
        }
    }
    
    fun saveNote() {
        val state = _uiState.value
        if (state.title.isBlank() && state.content.isBlank()) {
            return // Don't save empty notes
        }
        
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isSaving = true)
                
                if (state.isNewNote) {
                    // Create new note
                    val newNote = Note(
                        title = state.title,
                        content = state.content,
                        markdown = state.content // TODO: Convert to markdown
                    )
                    val id = notesRepository.insertNote(newNote)
                    currentNote = newNote.copy(id = id)
                    _uiState.value = _uiState.value.copy(
                        isNewNote = false,
                        isSaving = false,
                        note = currentNote
                    )
                } else {
                    // Update existing note
                    currentNote?.let { note ->
                        val updatedNote = note.copy(
                            title = state.title,
                            content = state.content,
                            markdown = state.content, // TODO: Convert to markdown
                            updatedAt = LocalDateTime.now()
                        )
                        notesRepository.updateNote(updatedNote)
                        currentNote = updatedNote
                    }
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        note = currentNote
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    errorMessage = "Failed to save note: ${e.message}"
                )
            }
        }
    }
    
    fun deleteNote() {
        currentNote?.let { note ->
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
    }
    
    fun clearErrorMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
    
    override fun onCleared() {
        super.onCleared()
        autoSaveJob?.cancel()
        // Save final state if needed
        val state = _uiState.value
        if ((state.title.isNotBlank() || state.content.isNotBlank()) && !state.isSaving) {
            saveNote()
        }
    }
}

data class NoteDetailUiState(
    val title: String = "",
    val content: String = "",
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isNewNote: Boolean = true,
    val errorMessage: String? = null,
    val note: Note? = null
)