package com.cosmiclaboratory.axiom.ui.viewmodels.companion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cosmiclaboratory.axiom.data.ai.AiProvider
import com.cosmiclaboratory.axiom.data.ai.AiResult
import com.cosmiclaboratory.axiom.data.preferences.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class TestConnectionState {
    data object Idle : TestConnectionState()
    data object Testing : TestConnectionState()
    data object Success : TestConnectionState()
    data class Failed(val reason: String) : TestConnectionState()
}

@HiltViewModel
class ConnectAiViewModel @Inject constructor(
    private val prefs: UserPreferences,
    private val ai: AiProvider
) : ViewModel() {

    private val _testState = MutableStateFlow<TestConnectionState>(TestConnectionState.Idle)
    val testState: StateFlow<TestConnectionState> = _testState.asStateFlow()

    fun testKey(candidate: String) {
        if (candidate.isBlank()) return
        _testState.value = TestConnectionState.Testing
        viewModelScope.launch {
            // Provisionally store, run ping, restore previous if it fails so a bad
            // paste doesn't replace a working key.
            val previous = prefs.groqApiKey()
            prefs.setGroqApiKey(candidate)
            val result = ai.testConnection()
            _testState.value = when (result) {
                is AiResult.Ok -> TestConnectionState.Success
                AiResult.NoKey -> TestConnectionState.Failed("Key was rejected by Groq")
                AiResult.RateLimited -> TestConnectionState.Failed("Rate-limited; key looks fine, try again in a minute")
                is AiResult.Network -> {
                    if (previous != null) prefs.setGroqApiKey(previous) else prefs.setGroqApiKey(null)
                    TestConnectionState.Failed("Couldn't reach Groq: ${result.cause.message ?: "network error"}")
                }
                is AiResult.Parse -> TestConnectionState.Failed("Unexpected response from Groq")
            }
            // If the test was a network failure we already restored above; for other
            // failures (NoKey, RateLimited) leave the candidate so user can retry.
            if (_testState.value is TestConnectionState.Failed && previous != null && result is AiResult.NoKey) {
                prefs.setGroqApiKey(previous)
            }
        }
    }

    fun saveKey(candidate: String, onSaved: () -> Unit) {
        if (candidate.isBlank()) return
        viewModelScope.launch {
            prefs.setGroqApiKey(candidate)
            onSaved()
        }
    }

    fun clearKey(onCleared: () -> Unit) {
        viewModelScope.launch {
            prefs.setGroqApiKey(null)
            _testState.value = TestConnectionState.Idle
            onCleared()
        }
    }
}
