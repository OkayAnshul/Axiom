package com.cosmiclaboratory.axiom.ui.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import com.cosmiclaboratory.axiom.data.repository.NotesRepository
import com.cosmiclaboratory.axiom.domain.model.Note
import com.cosmiclaboratory.axiom.ui.navigation.AxiomScreen
import com.cosmiclaboratory.axiom.utils.PlainTextToMarkdownConverter
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
    
    // Enhanced content update with cursor position tracking
    fun updateContentWithCursor(textFieldValue: TextFieldValue) {
        val newContent = textFieldValue.text
        val selection = textFieldValue.selection
        
        _uiState.value = _uiState.value.copy(
            content = newContent,
            cursorPosition = selection.start,
            selectionStart = selection.start,
            selectionEnd = selection.end,
            selectedText = if (selection.start != selection.end) {
                newContent.substring(selection.start, selection.end)
            } else ""
        )
        scheduleAutoSave()
    }
    
    // Update cursor position for template insertion
    fun updateCursorPosition(position: Int, selectionStart: Int = position, selectionEnd: Int = position) {
        val content = _uiState.value.content
        val clampedPosition = position.coerceIn(0, content.length)
        val clampedStart = selectionStart.coerceIn(0, content.length)
        val clampedEnd = selectionEnd.coerceIn(clampedStart, content.length)
        
        val selectedText = if (clampedStart != clampedEnd) {
            content.substring(clampedStart, clampedEnd)
        } else ""
        
        _uiState.value = _uiState.value.copy(
            cursorPosition = clampedPosition,
            selectionStart = clampedStart,
            selectionEnd = clampedEnd,
            selectedText = selectedText
        )
    }
    
    // Enhanced template insertion with smart selection handling and positioning
    fun insertTemplate(template: String, cursorOffset: Int = 0) {
        val currentState = _uiState.value
        val currentText = currentState.content
        val hasSelection = currentState.selectionStart != currentState.selectionEnd
        
        val newText: String
        val newCursorPosition: Int
        
        if (hasSelection) {
            // TEXT SELECTED: Wrap selected text with template
            val result = wrapSelectedTextWithTemplate(currentText, currentState, template)
            newText = result.text
            newCursorPosition = result.cursorPosition
        } else {
            // NO SELECTION: Insert template based on context
            val result = insertTemplateAtPosition(currentText, currentState.cursorPosition, template, cursorOffset)
            newText = result.text
            newCursorPosition = result.cursorPosition
        }
        
        _uiState.value = _uiState.value.copy(
            content = newText,
            cursorPosition = newCursorPosition,
            selectionStart = newCursorPosition,
            selectionEnd = newCursorPosition,
            selectedText = ""
        )
        scheduleAutoSave()
    }
    
    // Extract opening and closing markers from template for clean text wrapping
    private fun extractTemplateMarkers(template: String): Pair<String, String> {
        return when {
            // Bold: **bold text** -> ("**", "**")
            template.startsWith("**") && template.length > 4 && template.endsWith("**") -> "**" to "**"
            
            // Italic: *italic text* -> ("*", "*") (but not bold)
            template.startsWith("*") && !template.startsWith("**") && template.length > 2 && template.endsWith("*") -> "*" to "*"
            
            // Code: `code` -> ("`", "`")
            template.startsWith("`") && template.length > 2 && template.endsWith("`") -> "`" to "`"
            
            // Strikethrough: ~~text~~ -> ("~~", "~~")
            template.startsWith("~~") && template.length > 4 && template.endsWith("~~") -> "~~" to "~~"
            
            // Underline: __text__ -> ("__", "__")
            template.startsWith("__") && template.length > 4 && template.endsWith("__") -> "__" to "__"
            
            // Highlight: ==text== -> ("==", "==")
            template.startsWith("==") && template.length > 4 && template.endsWith("==") -> "==" to "=="
            
            // Link: [text](url) -> ("[", "]()")
            template.contains("[") && template.contains("](") -> {
                val linkStart = "["
                val urlPart = template.substring(template.indexOf("]("))
                linkStart to urlPart
            }
            
            // Image: ![text](url) -> ("![", "]()")
            template.startsWith("![") && template.contains("](") -> {
                val imageStart = "!["
                val urlPart = template.substring(template.indexOf("]("))
                imageStart to urlPart
            }
            
            // Headers: # text -> ("# ", "")
            template.startsWith("#") -> {
                val headerMarker = template.takeWhile { it == '#' } + " "
                headerMarker to ""
            }
            
            // Default: use template as-is for complex cases
            else -> template to ""
        }
    }
    
    // Helper function to wrap selected text with template
    private fun wrapSelectedTextWithTemplate(
        text: String, 
        state: NoteDetailUiState, 
        template: String
    ): TemplateInsertionResult {
        val selectedText = state.selectedText
        val beforeSelection = text.substring(0, state.selectionStart)
        val afterSelection = text.substring(state.selectionEnd)
        
        // Extract pure markers for clean wrapping
        val (openMarker, closeMarker) = extractTemplateMarkers(template)
        
        // Create clean wrapped text using only markers
        val wrappedText = when {
            // For symmetric markers (bold, italic, code, etc.)
            closeMarker.isNotEmpty() && openMarker != template -> {
                "$openMarker$selectedText$closeMarker"
            }
            
            // For asymmetric patterns (links, images)
            template.contains("[") && template.contains("](") -> {
                if (template.startsWith("![")) {
                    // Image: ![selectedText](image-url)
                    "![$selectedText](image-url)"
                } else {
                    // Link: [selectedText](url)
                    "[$selectedText](url)"
                }
            }
            
            // For headers: # selectedText
            template.startsWith("#") -> {
                "$openMarker$selectedText"
            }
            
            // Fallback: use original template replacement for complex cases
            else -> {
                template.replace("text", selectedText)
                    .replace("bold text", selectedText)
                    .replace("italic text", selectedText)
                    .replace("code", selectedText)
                    .replace("strikethrough text", selectedText)
                    .replace("highlighted text", selectedText)
                    .replace("underlined text", selectedText)
                    .replace("link text", selectedText)
                    .replace("alt text", selectedText)
            }
        }
        
        val newText = beforeSelection + wrappedText + afterSelection
        val newCursorPosition = state.selectionStart + wrappedText.length
        
        return TemplateInsertionResult(newText, newCursorPosition)
    }
    
    // Helper function to insert template at cursor position with smart positioning
    private fun insertTemplateAtPosition(
        text: String, 
        cursorPosition: Int, 
        template: String, 
        cursorOffset: Int
    ): TemplateInsertionResult {
        val beforeCursor = text.substring(0, cursorPosition)
        val afterCursor = text.substring(cursorPosition)
        
        val newText = beforeCursor + template + afterCursor
        
        // Smart cursor positioning based on template type
        val newCursorPosition = when {
            cursorOffset > 0 -> cursorPosition + cursorOffset
            
            // Position cursor optimally for different template types
            template.startsWith("**") && template.endsWith("**") -> {
                // Bold: **bold text** -> cursor after "**" for easy typing
                cursorPosition + 2
            }
            template.startsWith("*") && template.endsWith("*") && !template.startsWith("**") -> {
                // Italic: *italic text* -> cursor after "*"
                cursorPosition + 1
            }
            template.startsWith("`") && template.endsWith("`") -> {
                // Code: `code` -> cursor after "`"
                cursorPosition + 1
            }
            template.startsWith("~~") && template.endsWith("~~") -> {
                // Strikethrough: ~~text~~ -> cursor after "~~"
                cursorPosition + 2
            }
            template.contains("[text]") -> {
                // Link: [text](url) -> cursor at "text" position
                cursorPosition + 1
            }
            template.startsWith("#") -> {
                // Headers: # Title Text -> cursor after "# "
                cursorPosition + template.indexOf(' ') + 1
            }
            
            // Default: cursor at end of template
            else -> cursorPosition + template.length
        }
        
        return TemplateInsertionResult(newText, newCursorPosition)
    }
    
    // Data class for template insertion results
    private data class TemplateInsertionResult(
        val text: String,
        val cursorPosition: Int
    )
    
    // Get current content as TextFieldValue for UI components
    fun getContentAsTextFieldValue(): TextFieldValue {
        val currentState = _uiState.value
        return TextFieldValue(
            text = currentState.content,
            selection = TextRange(currentState.selectionStart, currentState.selectionEnd)
        )
    }
    
    // Get current title as TextFieldValue for UI components
    fun getTitleAsTextFieldValue(): TextFieldValue {
        return TextFieldValue(
            text = _uiState.value.title,
            selection = TextRange(_uiState.value.title.length)
        )
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
        val contentText = state.content
        if (state.title.isBlank() && contentText.isBlank()) {
            return // Don't save empty notes
        }
        val generatedMarkdown = PlainTextToMarkdownConverter.convert(
            plainText = contentText,
            title = state.title
        )
        
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isSaving = true)
                
                if (state.isNewNote) {
                    // Create new note
                    val newNote = Note(
                        title = state.title,
                        content = contentText,
                        markdown = generatedMarkdown
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
                            content = contentText,
                            markdown = generatedMarkdown,
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
    val cursorPosition: Int = 0,
    val selectedText: String = "",
    val selectionStart: Int = 0,
    val selectionEnd: Int = 0,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isNewNote: Boolean = true,
    val errorMessage: String? = null,
    val note: Note? = null
)
