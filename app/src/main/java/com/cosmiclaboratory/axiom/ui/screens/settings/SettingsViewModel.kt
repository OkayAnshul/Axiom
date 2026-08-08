package com.cosmiclaboratory.axiom.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cosmiclaboratory.axiom.data.ai.AiProvider
import com.cosmiclaboratory.axiom.data.ai.AiResult
import com.cosmiclaboratory.axiom.data.preferences.UserPreferences
import com.cosmiclaboratory.axiom.data.repository.JournalRepository
import com.cosmiclaboratory.axiom.data.work.JournalWorkScheduler
import com.cosmiclaboratory.axiom.domain.model.AiVendor
import com.cosmiclaboratory.axiom.domain.model.ConversationRetention
import com.cosmiclaboratory.axiom.domain.notification.CheckInTimes
import com.cosmiclaboratory.axiom.domain.voice.VoicePreset
import com.cosmiclaboratory.axiom.domain.voice.VoiceProfile
import com.cosmiclaboratory.axiom.domain.model.VoiceLanguage
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
import com.cosmiclaboratory.axiom.domain.model.CompanionIdentity

/** Result of a live key check. Every AiResult branch has a visible outcome. */
sealed interface KeyTestState {
    data object Idle : KeyTestState
    data object Testing : KeyTestState
    data object Success : KeyTestState
    data class Failed(val error: AxiomError) : KeyTestState
}

data class SettingsUiState(
    val displayName: String = "",
    /** What the user calls the companion; defaults to the app's own name. */
    val companionName: String = CompanionIdentity.DEFAULT_NAME,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    /** Whether the palette drifts with the hour. See `AxiomLight`. */
    val adaptiveLight: Boolean = true,
    /** How the companion talks. Replaces the old persona choice. */
    val voice: VoiceProfile = VoiceProfile(),
    /** Any vendor has a key — "is AI switched on at all". */
    val keyConnected: Boolean = false,
    val aiVendor: AiVendor = AiVendor.GROQ,
    /** The currently selected vendor specifically has a key. */
    val vendorKeyConnected: Boolean = false,
    val keyDraft: String = "",
    val keyMasked: Boolean = true,
    val keyTest: KeyTestState = KeyTestState.Idle,
    val dailyNudgeEnabled: Boolean = false,
    val weeklyRecapEnabled: Boolean = true,
    /** Earliest moment the companion may check in, as minutes past midnight. */
    val checkInMinuteOfDay: Int = CheckInTimes.DEFAULT_MINUTE_OF_DAY,
    /** How long a parked conversation stays resumable. */
    val conversationRetention: ConversationRetention = ConversationRetention.SAME_DAY,
    val entryCount: Int = 0,
    val voiceLanguage: VoiceLanguage = VoiceLanguage.ENGLISH_IN,
    val whisperFallbackEnabled: Boolean = true,
    val autoSpeakEnabled: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefs: UserPreferences,
    private val entries: JournalRepository,
    private val ai: AiProvider,
    private val scheduler: JournalWorkScheduler
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    displayName = prefs.displayName.first(),
                    companionName = prefs.companionName.first(),
                    themeMode = ThemeMode.fromStorage(prefs.themeOverride.first()),
                    adaptiveLight = prefs.adaptiveLight.first(),
                    voice = prefs.voice.first(),
                    dailyNudgeEnabled = prefs.dailyNudgeEnabled.first(),
                    weeklyRecapEnabled = prefs.weeklyRecapEnabled.first(),
                    checkInMinuteOfDay = prefs.dailyNudgeMinuteOfDay.first(),
                    conversationRetention = prefs.conversationRetention.first(),
                    entryCount = entries.count(),
                    aiVendor = prefs.aiVendor.first(),
                    vendorKeyConnected = prefs.apiKey(prefs.aiVendor.first()) != null,
                    voiceLanguage = VoiceLanguage.fromStorage(prefs.preferredVoiceLanguage.first()),
                    whisperFallbackEnabled = prefs.whisperFallbackEnabled.first(),
                    autoSpeakEnabled = prefs.autoSpeakEnabled.first()
                )
            }
        }
        viewModelScope.launch {
            prefs.observeGroqKeyPresent().collect { present ->
                _state.update { it.copy(keyConnected = present) }
            }
        }
        viewModelScope.launch {
            AiVendor.entries.forEach { vendor ->
                launch {
                    prefs.observeKeyPresent(vendor).collect { present ->
                        _state.update { s ->
                            if (s.aiVendor == vendor) s.copy(vendorKeyConnected = present) else s
                        }
                    }
                }
            }
        }
    }

    fun setDisplayName(value: String) {
        _state.update { it.copy(displayName = value) }
        viewModelScope.launch { prefs.setDisplayName(value) }
    }

    /** Applies live: StartupViewModel follows companionName, so nothing restarts. */
    fun setCompanionName(value: String) {
        val resolved = CompanionIdentity.resolve(value)
        _state.update { it.copy(companionName = resolved) }
        viewModelScope.launch { prefs.setCompanionName(value) }
    }

    fun setThemeMode(mode: ThemeMode) {
        _state.update { it.copy(themeMode = mode) }
        // Applies live — StartupViewModel follows themeOverride, so no restart.
        viewModelScope.launch { prefs.setThemeOverride(mode.storageValue) }
    }

    fun setAdaptiveLight(enabled: Boolean) {
        _state.update { it.copy(adaptiveLight = enabled) }
        // Applies live — StartupViewModel follows this preference too.
        viewModelScope.launch { prefs.setAdaptiveLight(enabled) }
    }

    fun applyVoicePreset(preset: VoicePreset) {
        _state.update { it.copy(voice = preset.profile().copy(preset = preset)) }
        viewModelScope.launch {
            prefs.applyVoicePreset(preset)
            // The persona is derived from the voice now, and it partitions the
            // generated-opener cache — so changing voice has to move the
            // partition with it, or the new voice keeps reading openers written
            // for the old one. Onboarding does the same on the way in.
            prefs.setActivePersona(preset.toPersonaKey())
        }
    }

    fun setVoice(profile: VoiceProfile) {
        _state.update { it.copy(voice = profile) }
        viewModelScope.launch { prefs.setVoice(profile) }
    }

    fun setDailyNudge(enabled: Boolean) {
        _state.update { it.copy(dailyNudgeEnabled = enabled) }
        viewModelScope.launch {
            prefs.setDailyNudgeEnabled(enabled)
            syncProactiveSchedule()
        }
    }

    fun setWeeklyRecap(enabled: Boolean) {
        _state.update { it.copy(weeklyRecapEnabled = enabled) }
        viewModelScope.launch {
            prefs.setWeeklyRecapEnabled(enabled)
            syncProactiveSchedule()
        }
    }

    fun setCheckInMinuteOfDay(minuteOfDay: Int) {
        _state.update { it.copy(checkInMinuteOfDay = minuteOfDay) }
        viewModelScope.launch { prefs.setDailyNudgeMinuteOfDay(minuteOfDay) }
    }

    fun setConversationRetention(value: ConversationRetention) {
        _state.update { it.copy(conversationRetention = value) }
        viewModelScope.launch { prefs.setConversationRetention(value) }
    }

    /** The hourly tick exists only while something proactive is switched on. */
    private suspend fun syncProactiveSchedule() {
        val wanted = prefs.dailyNudgeEnabled.first() || prefs.weeklyRecapEnabled.first()
        if (wanted) scheduler.scheduleProactiveCheckIns() else scheduler.cancelProactiveCheckIns()
    }

    fun setVoiceLanguage(language: VoiceLanguage) {
        _state.update { it.copy(voiceLanguage = language) }
        viewModelScope.launch { prefs.setPreferredVoiceLanguage(language.name) }
    }

    fun setAutoSpeak(enabled: Boolean) {
        _state.update { it.copy(autoSpeakEnabled = enabled) }
        viewModelScope.launch { prefs.setAutoSpeakEnabled(enabled) }
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
        val vendor = _state.value.aiVendor
        _state.update { it.copy(keyTest = KeyTestState.Testing) }
        viewModelScope.launch {
            val previous = prefs.apiKey(vendor)
            prefs.setApiKey(vendor, candidate)
            when (val result = ai.testConnection()) {
                is AiResult.Ok -> {
                    _state.update { it.copy(keyTest = KeyTestState.Success, keyDraft = "") }
                    // Everything written before this moment has no summary, no
                    // memories and no themes. Catch it up, paced.
                    runCatching { scheduler.enqueueBackfill() }
                    // And get personalized openers today rather than whenever
                    // the daily periodic worker next happens to fire. This call
                    // existed and had never had a caller.
                    runCatching { scheduler.requestImmediateInitiatorBatch() }
                }
                else -> {
                    prefs.setApiKey(vendor, previous)
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
        val vendor = _state.value.aiVendor
        viewModelScope.launch {
            prefs.setApiKey(vendor, null)
            _state.update { it.copy(keyTest = KeyTestState.Idle, keyDraft = "") }
        }
    }

    /**
     * Switching vendor never discards the other key — someone trying Gemini for
     * a week should be able to go back to Groq without fetching a new one.
     */
    fun setAiVendor(vendor: AiVendor) {
        _state.update { it.copy(aiVendor = vendor, keyTest = KeyTestState.Idle, keyDraft = "") }
        viewModelScope.launch {
            prefs.setAiVendor(vendor)
            _state.update { it.copy(vendorKeyConnected = prefs.apiKey(vendor) != null) }
        }
    }
}
