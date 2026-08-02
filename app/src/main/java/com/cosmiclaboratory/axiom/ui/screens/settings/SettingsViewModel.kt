package com.cosmiclaboratory.axiom.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cosmiclaboratory.axiom.data.ai.AiProvider
import com.cosmiclaboratory.axiom.data.ai.AiResult
import com.cosmiclaboratory.axiom.data.preferences.UserPreferences
import com.cosmiclaboratory.axiom.data.repository.JournalRepository
import com.cosmiclaboratory.axiom.data.repository.PersonaRepository
import com.cosmiclaboratory.axiom.domain.model.Persona
import com.cosmiclaboratory.axiom.domain.model.PersonaKey
import com.cosmiclaboratory.axiom.ui.design.AxiomError
import com.cosmiclaboratory.axiom.ui.design.toAxiomError
import com.cosmiclaboratory.axiom.ui.theme.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Result of a live key check. Every AiResult branch has a visible outcome. */
sealed interface KeyTestState {
    data object Idle : KeyTestState
    data object Testing : KeyTestState
    data object Success : KeyTestState
    data class Failed(val error: AxiomError) : KeyTestState
}

data class SettingsUiState(
    val displayName: String = "",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val keyConnected: Boolean = false,
    val keyDraft: String = "",
    val keyMasked: Boolean = true,
    val keyTest: KeyTestState = KeyTestState.Idle,
    val personas: List<Persona> = emptyList(),
    val activePersona: PersonaKey = PersonaKey.CALM,
    val dailyNudgeEnabled: Boolean = false,
    val entryCount: Int = 0
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: UserPreferences,
    private val personas: PersonaRepository,
    private val entries: JournalRepository,
    private val ai: AiProvider
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    displayName = prefs.displayName.first(),
                    themeMode = ThemeMode.fromStorage(prefs.themeOverride.first()),
                    activePersona = prefs.activePersonaKey.first(),
                    dailyNudgeEnabled = prefs.dailyNudgeEnabled.first(),
                    entryCount = entries.count()
                )
            }
        }
        viewModelScope.launch {
            prefs.observeGroqKeyPresent().collect { present ->
                _state.update { it.copy(keyConnected = present) }
            }
        }
        viewModelScope.launch {
            personas.observeAll().collect { list -> _state.update { it.copy(personas = list) } }
        }
    }

    fun setDisplayName(value: String) {
        _state.update { it.copy(displayName = value) }
        viewModelScope.launch { prefs.setDisplayName(value) }
    }

    fun setThemeMode(mode: ThemeMode) {
        _state.update { it.copy(themeMode = mode) }
        // Applies live — StartupViewModel follows themeOverride, so no restart.
        viewModelScope.launch { prefs.setThemeOverride(mode.storageValue) }
    }

    fun setPersona(key: PersonaKey) {
        _state.update { it.copy(activePersona = key) }
        viewModelScope.launch { prefs.setActivePersona(key) }
    }

    fun setDailyNudge(enabled: Boolean) {
        _state.update { it.copy(dailyNudgeEnabled = enabled) }
        viewModelScope.launch { prefs.setDailyNudgeEnabled(enabled) }
    }

    fun setKeyDraft(value: String) =
        _state.update { it.copy(keyDraft = value, keyTest = KeyTestState.Idle) }

    fun toggleKeyMask() = _state.update { it.copy(keyMasked = !it.keyMasked) }

    /**
     * Saves the key, then verifies it. Saving first is deliberate: [AiProvider]
     * reads from storage, so a "test before save" flow would have to duplicate
     * the transport. On failure the key is rolled back rather than left behind
     * to fail silently later.
     */
    fun saveAndTestKey() {
        val candidate = _state.value.keyDraft.trim()
        if (candidate.isBlank()) return
        _state.update { it.copy(keyTest = KeyTestState.Testing) }
        viewModelScope.launch {
            val previous = prefs.groqApiKey()
            prefs.setGroqApiKey(candidate)
            when (val result = ai.testConnection()) {
                is AiResult.Ok -> _state.update {
                    it.copy(keyTest = KeyTestState.Success, keyDraft = "")
                }
                else -> {
                    prefs.setGroqApiKey(previous)
                    _state.update {
                        it.copy(
                            keyTest = KeyTestState.Failed(
                                result.toAxiomError() ?: AxiomError.Unknown("Unknown failure")
                            )
                        )
                    }
                }
            }
        }
    }

    fun removeKey() {
        viewModelScope.launch {
            prefs.setGroqApiKey(null)
            _state.update { it.copy(keyTest = KeyTestState.Idle, keyDraft = "") }
        }
    }
}
