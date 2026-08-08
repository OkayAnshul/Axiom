package com.cosmiclaboratory.axiom.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cosmiclaboratory.axiom.data.ai.AiProvider
import com.cosmiclaboratory.axiom.data.ai.AiResult
import com.cosmiclaboratory.axiom.data.preferences.UserPreferences
import com.cosmiclaboratory.axiom.data.repository.JournalRepository
import com.cosmiclaboratory.axiom.data.repository.UserProfileRepository
import com.cosmiclaboratory.axiom.data.work.JournalWorkScheduler
import com.cosmiclaboratory.axiom.domain.model.AiVendor
import com.cosmiclaboratory.axiom.domain.model.Entry
import com.cosmiclaboratory.axiom.domain.model.EntryKind
import com.cosmiclaboratory.axiom.domain.notification.CheckInTimes
import com.cosmiclaboratory.axiom.domain.voice.VoicePreset
import com.cosmiclaboratory.axiom.ui.design.AxiomError
import com.cosmiclaboratory.axiom.ui.design.toAxiomError
import com.cosmiclaboratory.axiom.ui.screens.settings.KeyTestState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingUiState(
    val displayName: String = "",
    /**
     * The same [VoicePreset] settings edits, not the old [PersonaKey].
     *
     * Onboarding used to ask for a persona under the heading "Pick a companion
     * voice" while settings asked for a voice under "How I talk" — two disjoint
     * six-option lists for one question. Worse, only the settings one reached
     * the model: CompanionEngine read the persona into a local and never used
     * it, so the very first choice anyone made in this app did nothing.
     */
    val voice: VoicePreset = VoicePreset.Warm,
    val nudgeEnabled: Boolean = false,
    val checkInMinuteOfDay: Int = CheckInTimes.DEFAULT_MINUTE_OF_DAY,
    val firstEntry: String = "",
    // ---- the optional key, offered on the last page --------------------------
    val aiVendor: AiVendor = AiVendor.GROQ,
    val keyDraft: String = "",
    val keyMasked: Boolean = true,
    val keyTest: KeyTestState = KeyTestState.Idle
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val prefs: UserPreferences,
    private val profile: UserProfileRepository,
    private val entries: JournalRepository,
    private val ai: AiProvider,
    private val scheduler: JournalWorkScheduler
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    fun setDisplayName(value: String) = _state.update { it.copy(displayName = value) }
    fun setVoice(preset: VoicePreset) = _state.update { it.copy(voice = preset) }
    fun setNudge(enabled: Boolean) = _state.update { it.copy(nudgeEnabled = enabled) }
    fun setCheckInMinuteOfDay(minute: Int) = _state.update { it.copy(checkInMinuteOfDay = minute) }
    fun setFirstEntry(value: String) = _state.update { it.copy(firstEntry = value) }

    fun setAiVendor(vendor: AiVendor) =
        _state.update { it.copy(aiVendor = vendor, keyTest = KeyTestState.Idle) }

    fun setKeyDraft(value: String) =
        _state.update { it.copy(keyDraft = value, keyTest = KeyTestState.Idle) }

    fun toggleKeyMask() = _state.update { it.copy(keyMasked = !it.keyMasked) }

    /**
     * Same save-then-verify shape as Settings, and for the same reason:
     * [AiProvider] reads the key from storage, so testing before saving would
     * mean duplicating the transport. A failed key is rolled back rather than
     * left behind to fail quietly later — doubly important here, where the
     * person has no idea yet what "connected" is supposed to feel like.
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
                    // There is nothing to backfill during onboarding, but the
                    // opener batch matters: connect a key now and tomorrow's
                    // greeting is already written for you.
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

    /** The row written by [commitFirstEntry], so it is never written twice. */
    private var firstEntryId: Long? = null

    /**
     * Saves the first entry as soon as the user moves past writing it, rather
     * than at the end of the flow.
     *
     * The key page that follows opens with "everything you just wrote is already
     * saved", which is the sentence that earns the right to ask for anything at
     * all — the app proving it works before requesting a favour. It had better
     * be true at the moment it is read. Idempotent, because both the Continue
     * button and Skip can reach it.
     */
    fun commitFirstEntry() {
        val text = _state.value.firstEntry.trim()
        if (text.isEmpty()) return
        viewModelScope.launch {
            firstEntryId = entries.upsert(
                Entry(
                    id = firstEntryId ?: 0L,
                    content = text,
                    markdown = text,
                    kind = EntryKind.FREE_FORM,
                    isComplete = true
                )
            )
        }
    }

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
            val name = s.displayName.trim()
            // The persona is derived, not chosen: it survives only as the
            // partition key for the generated-opener cache. See
            // VoicePreset.toPersonaKey.
            profile.completeOnboarding(name, s.voice.toPersonaKey())
            prefs.applyVoicePreset(s.voice)
            // The Room copy is not the one anyone reads. Every consumer of the
            // name — the greeting, CompanionEngine, ProactiveMessenger, Settings,
            // BackupManager — reads the DataStore key, so writing only the profile
            // row silently discarded a name the user had just typed, and greetings
            // stayed anonymous until it was entered again in Settings.
            if (name.isNotEmpty()) prefs.setDisplayName(name)
            prefs.setDailyNudgeEnabled(s.nudgeEnabled)
            if (s.nudgeEnabled) prefs.setDailyNudgeMinuteOfDay(s.checkInMinuteOfDay)
            // Someone who just declined a daily nudge has not agreed to a
            // weekly one. The recap preference defaults on, which meant "No
            // thanks" still produced a Sunday notification nobody asked for.
            if (!s.nudgeEnabled) prefs.setWeeklyRecapEnabled(false)
            prefs.setOnboardingComplete(true)

            val text = s.firstEntry.trim()
            if (text.isNotEmpty()) {
                // Reuses the id when commitFirstEntry already ran, so skipping
                // ahead from the writing page cannot produce two copies.
                firstEntryId = entries.upsert(
                    Entry(
                        id = firstEntryId ?: 0L,
                        content = text,
                        markdown = text,
                        kind = EntryKind.FREE_FORM,
                        isComplete = true
                    )
                )
            }
            onDone()
        }
    }
}
