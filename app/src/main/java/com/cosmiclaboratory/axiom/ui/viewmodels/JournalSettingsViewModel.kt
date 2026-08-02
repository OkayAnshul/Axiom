package com.cosmiclaboratory.axiom.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cosmiclaboratory.axiom.data.preferences.UserPreferences
import com.cosmiclaboratory.axiom.data.repository.PersonaRepository
import com.cosmiclaboratory.axiom.data.repository.UserProfileRepository
import com.cosmiclaboratory.axiom.domain.model.Persona
import com.cosmiclaboratory.axiom.domain.model.PersonaKey
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class JournalSettingsUiState(
    val activePersona: PersonaKey = PersonaKey.CALM,
    val personas: List<Persona> = emptyList(),
    val groqApiKey: String = "",
    val keyMasked: Boolean = true,
    val weeklyRecapEnabled: Boolean = true
)

@HiltViewModel
class JournalSettingsViewModel @Inject constructor(
    private val prefs: UserPreferences,
    private val personaRepo: PersonaRepository,
    private val userProfileRepo: UserProfileRepository
) : ViewModel() {

    private val _state = MutableStateFlow(JournalSettingsUiState())
    val state: StateFlow<JournalSettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            personaRepo.observeAll().collect { personas ->
                _state.update { it.copy(personas = personas) }
            }
        }
        viewModelScope.launch {
            prefs.activePersonaKey.collect { active ->
                _state.update { it.copy(activePersona = active) }
            }
        }
        viewModelScope.launch {
            // v2: key now lives in encrypted storage. Refresh on init; updates
            // happen through saveApiKey().
            _state.update { it.copy(groqApiKey = prefs.groqApiKey().orEmpty()) }
        }
        viewModelScope.launch {
            prefs.weeklyRecapEnabled.collect { enabled ->
                _state.update { it.copy(weeklyRecapEnabled = enabled) }
            }
        }
    }

    fun setPersona(key: PersonaKey) {
        viewModelScope.launch { userProfileRepo.setPersona(key) }
    }

    fun updateApiKey(value: String) {
        _state.update { it.copy(groqApiKey = value) }
    }

    fun saveApiKey() {
        viewModelScope.launch {
            prefs.setGroqApiKey(_state.value.groqApiKey.trim())
            _state.update { it.copy(groqApiKey = prefs.groqApiKey().orEmpty()) }
        }
    }

    fun toggleMask() = _state.update { it.copy(keyMasked = !it.keyMasked) }

    fun setWeeklyRecap(enabled: Boolean) {
        viewModelScope.launch { prefs.setWeeklyRecapEnabled(enabled) }
    }
}
