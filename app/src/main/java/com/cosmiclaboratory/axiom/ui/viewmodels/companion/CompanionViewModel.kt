package com.cosmiclaboratory.axiom.ui.viewmodels.companion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cosmiclaboratory.axiom.data.ai.AiResult
import com.cosmiclaboratory.axiom.data.companion.CompanionService
import com.cosmiclaboratory.axiom.data.preferences.UserPreferences
import com.cosmiclaboratory.axiom.data.repository.CompanionRepository
import com.cosmiclaboratory.axiom.domain.model.Entry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CompanionMessageUi(
    val id: Long,
    val role: String,
    val content: String,
    val createdAtIso: String,
    val citedEntryIds: List<Long>
)

data class CompanionUiState(
    val messages: List<CompanionMessageUi> = emptyList(),
    val sending: Boolean = false,
    val error: String? = null,
    val keyConnected: Boolean = false,
    val recentlyCitedEntries: List<Entry> = emptyList()
)

@HiltViewModel
class CompanionViewModel @Inject constructor(
    private val service: CompanionService,
    private val repo: CompanionRepository,
    prefs: UserPreferences
) : ViewModel() {

    private val threadId = "default"

    private val _localState = MutableStateFlow(CompanionUiState())
    val keyConnected: StateFlow<Boolean> = prefs.observeGroqKeyPresent().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = false
    )

    val state: StateFlow<CompanionUiState> = _localState.asStateFlow()

    init {
        viewModelScope.launch {
            repo.observeThread(threadId).collect { rows ->
                _localState.value = _localState.value.copy(
                    messages = rows.map { row ->
                        CompanionMessageUi(
                            id = row.id,
                            role = row.role,
                            content = row.content,
                            createdAtIso = row.createdAt.toString(),
                            citedEntryIds = row.citedEntryIdsCsv
                                .split(',')
                                .mapNotNull { it.trim().toLongOrNull() }
                        )
                    }
                )
            }
        }
    }

    fun ask(question: String) {
        if (question.isBlank() || _localState.value.sending) return
        _localState.value = _localState.value.copy(sending = true, error = null)
        viewModelScope.launch {
            when (val res = service.ask(threadId, question.trim())) {
                is AiResult.Ok -> {
                    _localState.value = _localState.value.copy(
                        sending = false,
                        recentlyCitedEntries = res.value.citedEntries
                    )
                }
                AiResult.NoKey -> _localState.value = _localState.value.copy(
                    sending = false,
                    error = "Connect your Groq key to ask the companion."
                )
                AiResult.RateLimited -> _localState.value = _localState.value.copy(
                    sending = false,
                    error = "Groq is rate-limiting; try again in a minute."
                )
                is AiResult.Network -> _localState.value = _localState.value.copy(
                    sending = false,
                    error = "Network: ${res.cause.message ?: "couldn't reach Groq"}"
                )
                is AiResult.Parse -> _localState.value = _localState.value.copy(
                    sending = false,
                    error = "Couldn't parse the response."
                )
            }
        }
    }

    fun clearError() {
        _localState.value = _localState.value.copy(error = null)
    }

    fun clearThread() {
        viewModelScope.launch {
            repo.deleteThread(threadId)
            _localState.value = CompanionUiState()
        }
    }
}
