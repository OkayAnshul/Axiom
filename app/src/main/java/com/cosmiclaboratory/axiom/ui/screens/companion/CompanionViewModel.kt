package com.cosmiclaboratory.axiom.ui.screens.companion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cosmiclaboratory.axiom.data.companion.CompanionEngine
import com.cosmiclaboratory.axiom.data.companion.CompanionReplyEvent
import com.cosmiclaboratory.axiom.data.database.entity.CompanionMessageEntity
import com.cosmiclaboratory.axiom.data.preferences.UserPreferences
import com.cosmiclaboratory.axiom.data.repository.CompanionRepository
import com.cosmiclaboratory.axiom.data.repository.JournalRepository
import com.cosmiclaboratory.axiom.data.repository.MemoryRepository
import com.cosmiclaboratory.axiom.data.repository.QuestionRepository
import com.cosmiclaboratory.axiom.data.voice.CompanionSpeaker
import com.cosmiclaboratory.axiom.data.voice.MultilingualVoiceManager
import com.cosmiclaboratory.axiom.data.voice.extractSpeakableSentences
import com.cosmiclaboratory.axiom.domain.model.Emotion
import com.cosmiclaboratory.axiom.domain.model.Entry
import com.cosmiclaboratory.axiom.domain.model.EntryKind
import com.cosmiclaboratory.axiom.domain.model.VoiceLanguage
import com.cosmiclaboratory.axiom.domain.model.humanizedMemory
import com.cosmiclaboratory.axiom.domain.streak.StreakCalculator
import com.cosmiclaboratory.axiom.utils.VoiceRecognitionResult
import com.cosmiclaboratory.axiom.ui.design.AxiomError
import com.cosmiclaboratory.axiom.ui.design.toAxiomError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject

data class CompanionUiMessage(
    val id: Long,
    val isUser: Boolean,
    val text: String,
    val createdAt: LocalDateTime,
    val citedEntryIds: List<Long> = emptyList()
)

data class CompanionUiState(
    val messages: List<CompanionUiMessage> = emptyList(),
    val draft: String = "",
    val sending: Boolean = false,
    /** Partial companion reply while it streams; null when idle. Persisted on completion. */
    val streamingText: String? = null,
    val error: AxiomError? = null,
    val keyConnected: Boolean = false,
    /** Entries cited by the most recent answer, rendered as tappable chips. */
    val citedEntries: List<Entry> = emptyList(),
    // ---- ritual header (the Today remnant: one thin row, not a dashboard) ----
    val streak: StreakCalculator.Result = StreakCalculator.Result(0, 0, List(7) { false }),
    val todayMood: Int? = null,
    /** Named feeling read from today's writing, when the user hasn't chosen one. */
    val todayEmotion: Emotion? = null,
    /** True when [todayMood] came from inference — the chip then reads as correctable. */
    val todayMoodInferred: Boolean = false,
    /** Most recent unfinished entry, surfaced as a "continue writing" chip. */
    val writingDraft: Entry? = null,
    /** The last user message that failed to send — powers "Save to your journal". */
    val lastFailedText: String? = null,
    // ---- voice --------------------------------------------------------------
    val listening: Boolean = false,
    /** Whisper upload in flight (Hinglish mode only). */
    val transcribing: Boolean = false,
    val speaking: Boolean = false,
    /** On-device TTS engine initialized and usable; hides speak affordances when false. */
    val ttsAvailable: Boolean = false,
    val autoSpeak: Boolean = false,
    val handsFree: Boolean = false,
    val voiceLanguage: VoiceLanguage = VoiceLanguage.ENGLISH_IN,
    /** Transient STT failure, surfaced as a snackbar. */
    val voiceError: String? = null
)

/**
 * The home surface. The conversation IS the app now: the greeting, the daily
 * prompt, mood and streak all live here, folded into companion behavior
 * instead of dashboard cards. Everything except the model reply works with no
 * API key — the screen must never dead-end.
 */
@HiltViewModel
class CompanionViewModel @Inject constructor(
    private val engine: CompanionEngine,
    private val companionRepo: CompanionRepository,
    private val entries: JournalRepository,
    private val questions: QuestionRepository,
    private val memories: MemoryRepository,
    private val prefs: UserPreferences,
    private val voice: MultilingualVoiceManager,
    private val speaker: CompanionSpeaker
) : ViewModel() {

    private val _state = MutableStateFlow(CompanionUiState())
    val state: StateFlow<CompanionUiState> = _state.asStateFlow()

    /** Draft text as it stood when the mic opened — partials append to this base. */
    private var draftBeforeVoice: String = ""

    /** Streamed text not yet flushed to the speaker as a complete sentence. */
    private var speechTail: String = ""

    init {
        viewModelScope.launch {
            maybePostDailyOpener()
            companionRepo.observeThread(THREAD_ID).collect { rows ->
                _state.update { s ->
                    s.copy(
                        messages = rows.map { row ->
                            CompanionUiMessage(
                                id = row.id,
                                isUser = row.role == CompanionMessageEntity.Role.USER.name,
                                text = row.content,
                                createdAt = row.createdAt,
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
            // Streak, mood and the draft chip all derive from the corpus, so they
            // recompute whenever it changes — not only on first load.
            entries.observeAll().collect { refreshRitual() }
        }
        // ---- voice wiring ----------------------------------------------------
        viewModelScope.launch {
            prefs.preferredVoiceLanguage.collect { raw ->
                val language = VoiceLanguage.fromStorage(raw)
                _state.update { it.copy(voiceLanguage = language) }
                speaker.warmUp(language)
                speaker.setLanguage(language)
            }
        }
        viewModelScope.launch {
            prefs.autoSpeakEnabled.collect { on -> _state.update { it.copy(autoSpeak = on) } }
        }
        viewModelScope.launch {
            speaker.available.collect { ok -> _state.update { it.copy(ttsAvailable = ok) } }
        }
        viewModelScope.launch {
            speaker.speaking.collect { speaking ->
                _state.update { it.copy(speaking = speaking) }
                // Hands-free loop: the companion finished talking → listen again.
                val s = _state.value
                if (!speaking && s.handsFree && !s.sending && !s.listening && !s.transcribing) {
                    startListening()
                }
            }
        }
        viewModelScope.launch {
            voice.results().collect { result -> onVoiceResult(result) }
        }
    }

    private fun onVoiceResult(result: VoiceRecognitionResult) {
        when (result) {
            VoiceRecognitionResult.ReadyForSpeech,
            VoiceRecognitionResult.SpeechStarted ->
                _state.update { it.copy(listening = true, voiceError = null) }

            VoiceRecognitionResult.SpeechEnded -> _state.update {
                it.copy(listening = false, transcribing = !it.voiceLanguage.onDevice)
            }

            is VoiceRecognitionResult.PartialResult -> _state.update {
                it.copy(draft = joinTranscript(draftBeforeVoice, result.text))
            }

            is VoiceRecognitionResult.Success -> {
                _state.update {
                    it.copy(
                        draft = joinTranscript(draftBeforeVoice, result.text),
                        listening = false,
                        transcribing = false
                    )
                }
                // Hands-free: heard → send, no tap needed.
                if (_state.value.handsFree && _state.value.draft.isNotBlank()) send()
            }

            is VoiceRecognitionResult.Error -> _state.update {
                it.copy(listening = false, transcribing = false, voiceError = result.message)
            }

            else -> Unit // VolumeChanged — the pulse animation is enough feedback
        }
    }

    /**
     * Touch barge-in: the mic button always silences the companion first. TTS
     * and the recognizer must never run at once — the recognizer would happily
     * transcribe the companion's own voice back at it.
     */
    fun toggleListening() {
        val s = _state.value
        when {
            s.listening -> voice.stop() // whisper path uploads on stop
            else -> {
                speaker.stop()
                startListening()
            }
        }
    }

    private fun startListening() {
        val s = _state.value
        if (s.listening || s.transcribing) return
        speaker.stop()
        draftBeforeVoice = s.draft
        voice.start(s.voiceLanguage)
    }

    fun setHandsFree(enabled: Boolean) {
        _state.update { it.copy(handsFree = enabled) }
        if (enabled) {
            startListening()
        } else {
            voice.cancel()
            speaker.stop()
            _state.update { it.copy(listening = false, transcribing = false) }
        }
    }

    fun setAutoSpeak(enabled: Boolean) {
        viewModelScope.launch { prefs.setAutoSpeakEnabled(enabled) }
        if (!enabled) speaker.stop()
    }

    /** Per-message play button: read one past reply aloud. */
    fun speakMessage(text: String) {
        speaker.stop()
        val (sentences, tail) = extractSpeakableSentences(text)
        sentences.forEach { speaker.speakSentence(it) }
        if (tail.isNotBlank()) speaker.speakSentence(tail)
    }

    fun dismissVoiceError() = _state.update { it.copy(voiceError = null) }

    private fun joinTranscript(base: String, transcript: String): String =
        if (base.isBlank()) transcript else (base.trimEnd() + " " + transcript)

    /**
     * First open of a day: the companion speaks first, like a friend saying hi.
     * Built locally — no LLM, no key, no latency, works offline — and persisted
     * as a normal assistant message with source LOCAL so it joins the history
     * window (the companion should remember having greeted).
     *
     * An open loop always wins over a generic prompt: being asked "how did the
     * interview go?" is the whole difference between a companion and a form.
     */
    private suspend fun maybePostDailyOpener() {
        val latest = companionRepo.latestMessage(THREAD_ID)
        if (latest != null && latest.createdAt.toLocalDate() == LocalDate.now()) return
        val name = prefs.displayName.first()
        val greeting = greetingFor(LocalTime.now(), name)

        val loop = runCatching { memories.dueOpenLoops(limit = 1) }.getOrDefault(emptyList()).firstOrNull()
        if (loop != null) {
            companionRepo.appendLocal(
                THREAD_ID,
                "$greeting. Earlier you mentioned: ${loop.text.humanizedMemory()} How did that go?"
            )
            // Asked once. The memory survives; only the follow-up closes.
            runCatching { memories.closeLoop(loop.id) }
            return
        }

        val prompt = runCatching { questions.nextQuestion(prefs.activePersonaKey.first()) }
            .getOrNull()?.text ?: FALLBACK_PROMPTS.random()
        companionRepo.appendLocal(THREAD_ID, "$greeting. $prompt")
    }

    private suspend fun refreshRitual() {
        val today = LocalDate.now()
        val todaysEntries = runCatching { entries.forDay(today) }.getOrDefault(emptyList())
        // A mood the user chose outranks one that was read from their writing.
        val chosen = todaysEntries.firstOrNull { e -> e.mood != null && e.moodCapturedAt != null }
        val inferred = todaysEntries.firstOrNull { e -> e.mood != null && e.moodCapturedAt == null }
        val source = chosen ?: inferred
        _state.update {
            it.copy(
                streak = StreakCalculator.compute(entries.entryDates()),
                todayMood = source?.mood,
                todayEmotion = inferred?.emotion.takeIf { chosen == null },
                todayMoodInferred = chosen == null && inferred != null,
                writingDraft = entries.drafts(1).firstOrNull()
            )
        }
    }

    /**
     * Records a mood for today with a single tap.
     *
     * If nothing has been written today there is no row to attach it to, so we
     * create a content-less entry that exists purely to carry the mood. That is
     * deliberate: most days a user will not write, and without this the pattern
     * engine would only ever see labels from days that were already good enough
     * to write about — a badly biased sample.
     */
    fun recordMood(mood: Int) {
        viewModelScope.launch {
            val today = LocalDate.now()
            val existing = entries.forDay(today).firstOrNull()
            if (existing != null) {
                entries.setMood(existing.id, mood)
            } else {
                val now = LocalDateTime.now()
                entries.upsert(
                    Entry(
                        content = "",
                        kind = EntryKind.FREE_FORM,
                        createdAt = now,
                        updatedAt = now,
                        isComplete = true,
                        mood = mood,
                        moodCapturedAt = now
                    )
                )
            }
            refreshRitual()
        }
    }

    fun setDraft(value: String) = _state.update { it.copy(draft = value) }

    fun send() {
        val text = _state.value.draft.trim()
        if (text.isBlank() || _state.value.sending) return
        _state.update { it.copy(draft = "", sending = true, error = null, lastFailedText = null) }
        sendInternal(text, persistUserTurn = true)
    }

    /**
     * Retries the last question the user asked. The failed attempt already
     * persisted the user turn, so the engine must not append it again — the
     * history would otherwise show a duplicated bubble.
     */
    fun retry() {
        val lastUser = _state.value.messages.lastOrNull { it.isUser }?.text ?: return
        if (_state.value.sending) return
        _state.update { it.copy(sending = true, error = null, lastFailedText = null) }
        sendInternal(lastUser, persistUserTurn = false)
    }

    private fun sendInternal(text: String, persistUserTurn: Boolean) {
        speechTail = ""
        viewModelScope.launch {
            engine.send(THREAD_ID, text, persistUserTurn).collect { event ->
                when (event) {
                    is CompanionReplyEvent.Delta -> {
                        _state.update {
                            it.copy(streamingText = it.streamingText.orEmpty() + event.text)
                        }
                        // Speak sentence-by-sentence as the reply streams: first
                        // audio lands ~one sentence after the first token.
                        if (shouldSpeakReplies()) {
                            val (sentences, tail) = extractSpeakableSentences(speechTail + event.text)
                            sentences.forEach { speaker.speakSentence(it) }
                            speechTail = tail
                        } else {
                            speechTail = ""
                        }
                    }
                    is CompanionReplyEvent.Done -> {
                        if (shouldSpeakReplies() && speechTail.isNotBlank()) {
                            speaker.speakSentence(speechTail)
                        }
                        speechTail = ""
                        _state.update {
                            // The Room flow emission replaces the streamed text
                            // with the persisted message, so clearing is seamless.
                            it.copy(sending = false, streamingText = null, citedEntries = event.citedEntries)
                        }
                        // Hands-free with a silent reply: reopen the mic ourselves,
                        // since no speaking=false transition will do it for us.
                        val s = _state.value
                        if (s.handsFree && !s.speaking && !s.listening) startListening()
                    }
                    is CompanionReplyEvent.Failed -> {
                        speechTail = ""
                        _state.update {
                            it.copy(
                                sending = false,
                                streamingText = null,
                                error = event.error.toAxiomError(),
                                // What they typed is not lost: offer to journal it.
                                lastFailedText = text,
                                // A failing send would loop the mic forever.
                                handsFree = false
                            )
                        }
                    }
                }
            }
        }
    }

    private fun shouldSpeakReplies(): Boolean =
        _state.value.let { (it.autoSpeak || it.handsFree) && it.ttsAvailable }

    fun dismissError() = _state.update { it.copy(error = null, lastFailedText = null) }

    fun clearThread() {
        viewModelScope.launch {
            companionRepo.deleteThread(THREAD_ID)
            _state.update { it.copy(citedEntries = emptyList(), error = null) }
            maybePostDailyOpener()
        }
    }

    override fun onCleared() {
        voice.cancel()
        speaker.stop()
    }

    private fun greetingFor(time: LocalTime, name: String): String {
        val period = when (time.hour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..20 -> "Good evening"
            else -> "Hello"
        }
        return if (name.isBlank()) period else "$period, $name"
    }

    private companion object {
        /**
         * One continuous thread: a friendship is one relationship, not a stack
         * of daily documents. Sessions are delimited by silence gaps in the
         * digest worker, never by switching threads.
         */
        const val THREAD_ID = "companion"

        val FALLBACK_PROMPTS = listOf(
            "What surprised you this week?",
            "What's a small win from today that's worth remembering?",
            "What are you avoiding, and what's the smallest first step?",
            "Who or what gave you energy today?",
            "What's a pattern you've noticed in yourself lately?"
        )
    }
}
