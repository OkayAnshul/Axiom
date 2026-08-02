package com.cosmiclaboratory.axiom.ui.screens.ask

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cosmiclaboratory.axiom.data.ai.AiResult
import com.cosmiclaboratory.axiom.data.companion.CompanionService
import com.cosmiclaboratory.axiom.data.preferences.UserPreferences
import com.cosmiclaboratory.axiom.data.repository.CompanionRepository
import com.cosmiclaboratory.axiom.data.repository.PersonaRepository
import com.cosmiclaboratory.axiom.domain.model.Entry
import com.cosmiclaboratory.axiom.domain.model.Persona
import com.cosmiclaboratory.axiom.domain.model.PersonaKey
import com.cosmiclaboratory.axiom.ui.design.AxiomError
import com.cosmiclaboratory.axiom.ui.design.toAxiomError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AskMessage(
    val id: Long,
    val isUser: Boolean,
    val text: String,
    val citedEntryIds: List<Long> = emptyList()
)

data class AskUiState(
    val messages: List<AskMessage> = emptyList(),
    val draft: String = "",
    val sending: Boolean = false,
    val error: AxiomError? = null,
    val keyConnected: Boolean = false,
    val personas: List<Persona> = emptyList(),
    val activePersona: PersonaKey = PersonaKey.CALM,
    /** Entries cited by the most recent answer, rendered as tappable chips. */
    val citedEntries: List<Entry> = emptyList()
)

@HiltViewModel
class AskViewModel @Inject constructor(
    private val companion: CompanionService,
    private val companionRepo: CompanionRepository,
    private val personas: PersonaRepository,
    private val prefs: UserPreferences
) : ViewModel() {

    private val _state = MutableStateFlow(AskUiState())
    val state: StateFlow<AskUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            companionRepo.observeThread(THREAD_ID).collect { rows ->
                _state.update { s ->
                    s.copy(
                        messages = rows.map { row ->
                            AskMessage(
                                id = row.id,
                                isUser = row.role == "user",
                                text = row.content,
                                citedEntryIds = row.citedEntryIdsCsv
                                    .split(",")
                                    .mapNotNull { it.trim().toLongOrNull() }
                            )
                        }
                    )
                }
            }
        }
        viewModelScope.launch {
            prefs.observeGroqKeyPresent().collect { present ->
                _state.update { it.copy(keyConnected = present) }
            }
        }
        viewModelScope.launch {
            personas.observeAll().collect { list ->
                _state.update { it.copy(personas = list) }
            }
        }
        viewModelScope.launch {
            _state.update { it.copy(activePersona = prefs.activePersonaKey.first()) }
        }
    }

    fun setDraft(value: String) = _state.update { it.copy(draft = value) }

    fun setPersona(key: PersonaKey) {
        viewModelScope.launch {
            prefs.setActivePersona(key)
            _state.update { it.copy(activePersona = key) }
        }
    }

    fun send() {
        val question = _state.value.draft.trim()
        if (question.isBlank() || _state.value.sending) return

        _state.update { it.copy(draft = "", sending = true, error = null) }
        viewModelScope.launch {
            when (val result = companion.ask(THREAD_ID, question)) {
                is AiResult.Ok -> _state.update {
                    it.copy(sending = false, citedEntries = result.value.citedEntries)
                }
                // Every non-Ok branch flows through the single exhaustive mapper,
                // so no AiResult case can reach the UI without a designed surface.
                else -> _state.update {
                    it.copy(sending = false, error = result.toAxiomError())
                }
            }
        }
    }

    /** Retries the last question the user asked, not the last message shown. */
    fun retry() {
        val lastUser = _state.value.messages.lastOrNull { it.isUser }?.text ?: return
        _state.update { it.copy(draft = lastUser, error = null) }
        send()
    }

    fun dismissError() = _state.update { it.copy(error = null) }

    fun clearThread() {
        viewModelScope.launch {
            companionRepo.deleteThread(THREAD_ID)
            _state.update { it.copy(citedEntries = emptyList(), error = null) }
        }
    }

    private companion object {
        const val THREAD_ID = "default"
    }
}
