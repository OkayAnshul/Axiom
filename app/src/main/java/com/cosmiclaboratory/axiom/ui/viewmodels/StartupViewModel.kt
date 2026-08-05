package com.cosmiclaboratory.axiom.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cosmiclaboratory.axiom.data.preferences.UserPreferences
import com.cosmiclaboratory.axiom.ui.theme.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Everything MainActivity needs before it can draw a correct first frame.
 *
 * [StartupState.isReady] drives the system splash's keep-on-screen condition, so the
 * splash lasts exactly as long as the preference read takes instead of the hardcoded
 * 3.5 seconds the old hand-rolled splash destination burned on every launch.
 */
@HiltViewModel
class StartupViewModel @Inject constructor(
    private val prefs: UserPreferences
) : ViewModel() {

    data class StartupState(
        val isReady: Boolean = false,
        val themeMode: ThemeMode = ThemeMode.SYSTEM,
        /** Whether the palette drifts with the hour. See `AxiomLight`. */
        val adaptiveLight: Boolean = true,
        val onboardingComplete: Boolean = false
    )

    private val _state = MutableStateFlow(StartupState())
    val state: StateFlow<StartupState> = _state.asStateFlow()

    init {
        // Resolve once so the first frame is already correct, then keep following both
        // appearance preferences so a change in settings applies without an app restart.
        viewModelScope.launch {
            _state.value = StartupState(
                isReady = true,
                themeMode = ThemeMode.fromStorage(prefs.themeOverride.first()),
                adaptiveLight = prefs.adaptiveLight.first(),
                onboardingComplete = prefs.onboardingComplete.first()
            )
            combine(prefs.themeOverride, prefs.adaptiveLight) { override, adaptive ->
                ThemeMode.fromStorage(override) to adaptive
            }.collect { (mode, adaptive) ->
                _state.value = _state.value.copy(themeMode = mode, adaptiveLight = adaptive)
            }
        }
    }
}
