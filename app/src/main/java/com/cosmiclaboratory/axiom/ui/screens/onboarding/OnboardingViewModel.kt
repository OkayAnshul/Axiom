package com.cosmiclaboratory.axiom.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cosmiclaboratory.axiom.data.preferences.UserPreferences
import com.cosmiclaboratory.axiom.data.repository.JournalRepository
import com.cosmiclaboratory.axiom.data.repository.PersonaRepository
import com.cosmiclaboratory.axiom.data.repository.UserProfileRepository
import com.cosmiclaboratory.axiom.domain.model.Entry
import com.cosmiclaboratory.axiom.domain.model.EntryKind
import com.cosmiclaboratory.axiom.domain.model.Persona
import com.cosmiclaboratory.axiom.domain.model.PersonaKey
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingUiState(
    val displayName: String = "",
    val persona: PersonaKey = PersonaKey.CALM,
    val personas: List<Persona> = emptyList(),
    val nudgeEnabled: Boolean = false,
    val firstEntry: String = ""
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val prefs: UserPreferences,
    private val profile: UserProfileRepository,
    private val personas: PersonaRepository,
    private val entries: JournalRepository
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            personas.observeAll().collect { list -> _state.update { it.copy(personas = list) } }
        }
    }

    fun setDisplayName(value: String) = _state.update { it.copy(displayName = value) }
    fun setPersona(key: PersonaKey) = _state.update { it.copy(persona = key) }
    fun setNudge(enabled: Boolean) = _state.update { it.copy(nudgeEnabled = enabled) }
    fun setFirstEntry(value: String) = _state.update { it.copy(firstEntry = value) }

    /**
     * Commits everything and, if the user wrote something, saves it as a real
     * entry so they land on Today with a streak of 1 rather than an empty app.
     *
     * completeOnboarding() had zero callers before this — the whole write path
     * existed but was never reachable.
     */
    fun finish(onDone: () -> Unit) {
        val s = _state.value
        viewModelScope.launch {
            profile.completeOnboarding(s.displayName.trim(), s.persona)
            prefs.setDailyNudgeEnabled(s.nudgeEnabled)
            prefs.setOnboardingComplete(true)

            if (s.firstEntry.isNotBlank()) {
                entries.upsert(
                    Entry(
                        content = s.firstEntry.trim(),
                        markdown = s.firstEntry.trim(),
                        kind = EntryKind.FREE_FORM,
                        isComplete = true
                    )
                )
            }
            onDone()
        }
    }
}
