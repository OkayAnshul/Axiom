package com.cosmiclaboratory.axiom.ui.screens.reader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.cosmiclaboratory.axiom.data.repository.JournalRepository
import com.cosmiclaboratory.axiom.domain.model.AIInsight
import com.cosmiclaboratory.axiom.domain.model.Entry
import com.cosmiclaboratory.axiom.domain.model.TaskItem
import com.cosmiclaboratory.axiom.domain.model.TaskParser
import com.cosmiclaboratory.axiom.ui.navigation.Reader
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReaderUiState(
    val isLoading: Boolean = true,
    val entry: Entry? = null,
    val insight: AIInsight? = null
)

@HiltViewModel
class ReaderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val entries: JournalRepository
) : ViewModel() {

    private val entryId: Long = savedStateHandle.toRoute<Reader>().entryId

    private val _state = MutableStateFlow(ReaderUiState())
    val state: StateFlow<ReaderUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            entries.observeById(entryId).collect { entry ->
                _state.update { it.copy(entry = entry, isLoading = false) }
            }
        }
        viewModelScope.launch {
            entries.observeInsight(entryId).collect { insight ->
                _state.update { it.copy(insight = insight) }
            }
        }
    }

    fun toggleFavorite() {
        val entry = _state.value.entry ?: return
        viewModelScope.launch { entries.setFavorite(entry.id, !entry.isFavorite) }
    }

    /**
     * Checkbox toggles rewrite the markdown in place via the existing
     * [TaskParser], so a task ticked in the reader persists as real text rather
     * than as separate state that could drift from the entry.
     */
    fun toggleTask(task: TaskItem) {
        val entry = _state.value.entry ?: return
        viewModelScope.launch {
            val source = entry.markdown.ifBlank { entry.content }
            // Re-resolve against live text: the item handed up by the renderer
            // carries no reliable line index.
            val match = TaskParser.extractTasks(source)
                .firstOrNull { it.text == task.text && it.isCompleted == task.isCompleted }
                ?: return@launch
            val updated = TaskParser.toggleTaskCompletion(source, match)
            entries.upsert(entry.copy(markdown = updated, content = updated))
        }
    }
}
