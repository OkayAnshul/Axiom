package com.cosmiclaboratory.axiom.ui.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cosmiclaboratory.axiom.data.repository.JournalRepository
import com.cosmiclaboratory.axiom.domain.model.AIInsight
import com.cosmiclaboratory.axiom.domain.model.AnswerEntry
import com.cosmiclaboratory.axiom.ui.navigation.AxiomScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InsightsUiState(
    val entry: AnswerEntry? = null,
    val insight: AIInsight? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class InsightsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val journalRepo: JournalRepository
) : ViewModel() {

    private val entryId: Long = savedStateHandle[AxiomScreen.Insights.ENTRY_ID_ARG] ?: -1L

    private val _state = MutableStateFlow(InsightsUiState())
    val state: StateFlow<InsightsUiState> = _state.asStateFlow()

    init { observe() }

    private fun observe() {
        viewModelScope.launch {
            val entry = journalRepo.getById(entryId)
            _state.update { it.copy(entry = entry, isLoading = false) }
        }
        viewModelScope.launch {
            journalRepo.observeInsight(entryId).collect { insight ->
                _state.update { it.copy(insight = insight) }
            }
        }
    }
}
