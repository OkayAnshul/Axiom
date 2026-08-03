package com.cosmiclaboratory.axiom.ui.screens.memory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cosmiclaboratory.axiom.data.repository.MemoryRepository
import com.cosmiclaboratory.axiom.domain.model.MemoryItem
import com.cosmiclaboratory.axiom.domain.model.MemoryKind
import com.cosmiclaboratory.axiom.domain.model.MemorySource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MemoryUiState(
    val groups: Map<MemoryKind, List<MemoryItem>> = emptyMap(),
    val loaded: Boolean = false,
    /** The memory currently open in the edit sheet, if any. */
    val editing: MemoryItem? = null,
    /** True while the "tell it how to talk to you" sheet is open. */
    val addingPreference: Boolean = false
)

@HiltViewModel
class MemoryViewModel @Inject constructor(
    private val memories: MemoryRepository
) : ViewModel() {

    private val _state = MutableStateFlow(MemoryUiState())
    val state: StateFlow<MemoryUiState> = _state.asStateFlow()

    /** Kept so the undo snackbar can restore the exact row, id and history included. */
    private var lastDeleted: MemoryItem? = null

    init {
        viewModelScope.launch {
            memories.observeAll().collect { items ->
                _state.update { s ->
                    s.copy(
                        groups = items.groupBy { it.kind }
                            .toSortedMap(compareBy { KIND_ORDER.indexOf(it) }),
                        loaded = true
                    )
                }
            }
        }
    }

    fun startEditing(item: MemoryItem) = _state.update { it.copy(editing = item) }

    fun cancelEditing() = _state.update { it.copy(editing = null) }

    fun saveEdit(newText: String) {
        val item = _state.value.editing ?: return
        _state.update { it.copy(editing = null) }
        if (newText.isBlank() || newText.trim() == item.text) return
        viewModelScope.launch { memories.edit(item.id, newText) }
    }

    fun delete(item: MemoryItem) {
        lastDeleted = item
        viewModelScope.launch { memories.delete(item.id) }
    }

    fun startAddingPreference() = _state.update { it.copy(addingPreference = true) }

    fun cancelAddingPreference() = _state.update { it.copy(addingPreference = false) }

    /**
     * A preference the user typed themselves. Saved at full weight and marked
     * user-edited, so extraction may reinforce it but can never reword it —
     * being told how to talk to someone should not be overridden by a guess.
     */
    fun addPreference(text: String) {
        _state.update { it.copy(addingPreference = false) }
        val clean = text.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            val id = memories.insert(
                kind = MemoryKind.PREFERENCE,
                text = clean,
                weight = 1f,
                source = MemorySource.MANUAL,
                sourceId = null
            )
            memories.markUserEdited(id)
        }
    }

    fun undoDelete() {
        val item = lastDeleted ?: return
        lastDeleted = null
        viewModelScope.launch { memories.restore(item) }
    }

    companion object {
        val KIND_ORDER = listOf(
            MemoryKind.PERSON,
            MemoryKind.GOAL,
            MemoryKind.THEME,
            MemoryKind.FACT,
            MemoryKind.EVENT,
            MemoryKind.PREFERENCE
        )
    }
}
