package com.cosmiclaboratory.axiom.ui.screens.composer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.cosmiclaboratory.axiom.data.database.dao.QuestionDao
import com.cosmiclaboratory.axiom.data.repository.JournalRepository
import com.cosmiclaboratory.axiom.data.work.JournalWorkScheduler
import com.cosmiclaboratory.axiom.domain.model.Entry
import com.cosmiclaboratory.axiom.domain.model.EntryKind
import com.cosmiclaboratory.axiom.domain.model.Tag
import com.cosmiclaboratory.axiom.ui.design.components.SaveState
import com.cosmiclaboratory.axiom.ui.navigation.Composer
import com.cosmiclaboratory.axiom.utils.PlainTextToMarkdownConverter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.LocalTime
import javax.inject.Inject

enum class ComposerMode { Write, Preview, Focus }

data class ComposerUiState(
    val isLoading: Boolean = true,
    val title: String = "",
    val body: String = "",
    val mode: ComposerMode = ComposerMode.Write,
    val saveState: SaveState = SaveState.Idle,
    val entryId: Long? = null,
    val promptText: String? = null,
    val questionId: Long? = null,
    val mood: Int? = null,
    val tags: List<Tag> = emptyList(),
    val openVoiceOnStart: Boolean = false
) {
    val wordCount: Int get() = body.split(Regex("\\s+")).count { it.isNotBlank() }
    val charCount: Int get() = body.length
    val isBlank: Boolean get() = title.isBlank() && body.isBlank()
}

/**
 * One composer for every kind of writing — free-form, guided prompt, and voice.
 * Replaces the NoteDetailViewModel / AnswerCaptureViewModel split, which
 * duplicated autosave, duration tracking and markdown conversion.
 */
@HiltViewModel
class ComposerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val entries: JournalRepository,
    private val questionDao: QuestionDao,
    private val workScheduler: JournalWorkScheduler
) : ViewModel() {

    private val args: Composer = savedStateHandle.toRoute()

    private val _state = MutableStateFlow(ComposerUiState(openVoiceOnStart = args.voice))
    val state: StateFlow<ComposerUiState> = _state.asStateFlow()

    private var autosaveJob: Job? = null
    private var startedAt = System.currentTimeMillis()
    /** Accrued focus time, paused across idle gaps. Feeds the pattern engine. */
    private var accruedMs = 0L
    private var lastTypedAt = System.currentTimeMillis()

    init {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        val existing = args.entryId?.let { entries.getById(it) }
        val question = args.questionId?.let { questionDao.getById(it) }
        _state.update {
            it.copy(
                isLoading = false,
                entryId = existing?.id,
                title = existing?.title.orEmpty(),
                body = existing?.content ?: args.initialText.orEmpty(),
                mood = existing?.mood,
                tags = existing?.tags.orEmpty(),
                questionId = args.questionId ?: existing?.questionId,
                promptText = question?.text ?: args.promptText ?: existing?.promptSnapshot
            )
        }
        // Text arriving from a share intent is unsaved work — persist immediately
        // rather than waiting for the user to type.
        if (!args.initialText.isNullOrBlank()) scheduleSave(immediate = true)
    }

    fun setTitle(value: String) {
        _state.update { it.copy(title = value, saveState = SaveState.Editing) }
        trackTyping()
        scheduleSave()
    }

    fun setBody(value: String) {
        _state.update { it.copy(body = value, saveState = SaveState.Editing) }
        trackTyping()
        scheduleSave()
    }

    fun setMode(mode: ComposerMode) {
        _state.update { it.copy(mode = mode) }
        // Mode changes are a natural commit point — don't wait out the debounce.
        scheduleSave(immediate = true)
    }

    fun setMood(mood: Int?) {
        _state.update { it.copy(mood = mood) }
        scheduleSave(immediate = true)
    }

    /**
     * The mic has been opened, so the route's request is spent. Held in state
     * rather than read straight from [args] so it survives exactly once: the
     * permission dialog recomposes this screen, and a rotation rebuilds it from
     * the same route, both of which would otherwise reopen the mic.
     */
    fun consumeVoiceAutoStart() {
        if (!_state.value.openVoiceOnStart) return
        _state.update { it.copy(openVoiceOnStart = false) }
    }

    fun appendTranscript(text: String) {
        if (text.isBlank()) return
        _state.update { s ->
            val separator = if (s.body.isBlank() || s.body.endsWith("\n")) "" else "\n\n"
            s.copy(body = s.body + separator + text, saveState = SaveState.Editing)
        }
        scheduleSave(immediate = true)
    }

    /**
     * Duration only accrues while the user is actually engaged. Without the idle
     * gate, leaving the composer open overnight would record eight hours of
     * "writing" and poison the effort signal.
     */
    private fun trackTyping() {
        val now = System.currentTimeMillis()
        if (now - lastTypedAt < IDLE_GAP_MS) accruedMs += now - lastTypedAt
        lastTypedAt = now
    }

    private fun scheduleSave(immediate: Boolean = false) {
        autosaveJob?.cancel()
        autosaveJob = viewModelScope.launch {
            if (!immediate) delay(AUTOSAVE_DEBOUNCE_MS)
            persist()
        }
    }

    private suspend fun persist(complete: Boolean = false) {
        val s = _state.value
        if (s.isBlank && s.entryId == null) return

        _state.update { it.copy(saveState = SaveState.Saving) }
        val result = runCatching {
            val existing = s.entryId?.let { entries.getById(it) }
            val now = LocalDateTime.now()
            val id = entries.upsert(
                Entry(
                    id = s.entryId ?: 0L,
                    title = s.title,
                    content = s.body,
                    markdown = PlainTextToMarkdownConverter.convert(s.body, s.title),
                    kind = when {
                        // A prompt is a prompt whether it came from the curated
                        // bank (questionId) or from the companion's own opener
                        // (promptText). Keying only on questionId filed every
                        // answered opener under "Written".
                        s.questionId != null || s.promptText != null -> EntryKind.PROMPTED
                        existing?.kind == EntryKind.VOICE -> EntryKind.VOICE
                        else -> EntryKind.FREE_FORM
                    },
                    createdAt = existing?.createdAt ?: now,
                    updatedAt = now,
                    // Autosave must never change completeness. The old condition
                    // added `&& s.questionId == null`, which meant every autosave
                    // while editing an already-finished *prompted* entry quietly
                    // un-completed it — dropping it out of the timeline until the
                    // user pressed Done again.
                    isComplete = complete || existing?.isComplete == true,
                    mood = s.mood,
                    moodCapturedAt = if (s.mood != null) (existing?.moodCapturedAt ?: now) else null,
                    questionId = s.questionId,
                    promptSnapshot = s.promptText,
                    durationMs = (existing?.durationMs ?: 0L) + accruedMs,
                    tags = s.tags
                )
            )
            accruedMs = 0L
            id
        }

        result.fold(
            onSuccess = { id ->
                _state.update { it.copy(entryId = id, saveState = SaveState.Saved(LocalTime.now())) }
            },
            onFailure = { e ->
                _state.update { it.copy(saveState = SaveState.Failed(e.message ?: "unknown")) }
            }
        )
    }

    fun retrySave() {
        viewModelScope.launch { persist() }
    }

    /** Done: complete the entry and kick summarisation if a key exists. */
    fun complete(onDone: (Long?) -> Unit) {
        viewModelScope.launch { onDone(finish()) }
    }

    /**
     * Back: keep it, the same as Done.
     *
     * This used to leave a draft, and drafts are excluded from the timeline —
     * so writing something and pressing back made it vanish from your own
     * journal. Nobody expects leaving a page to discard what they wrote; a
     * journal is not a form you have to submit. Both exits now keep the entry,
     * and an entry with no title and no body is deleted rather than left as a
     * zombie row.
     *
     * Drafts still exist for genuinely interrupted sessions — autosave writes
     * one while you type, so a crash mid-sentence is still recoverable through
     * "finish what you started". They are just no longer something a deliberate
     * exit can create.
     */
    fun saveAndExit(onDone: () -> Unit) {
        viewModelScope.launch {
            val s = _state.value
            if (s.isBlank) {
                s.entryId?.let { entries.deleteById(it) }
            } else {
                finish()
            }
            onDone()
        }
    }

    /**
     * The app went to the background with the composer open.
     *
     * Back and Keep both finish an entry, but neither runs when you press home
     * or swipe the app away — the ViewModel is not cleared, so what you wrote
     * would sit as a draft, out of the timeline, until the abandoned-draft
     * rescue caught it an hour later. Leaving your phone is not a decision to
     * discard a thought.
     */
    fun finishIfWritten() {
        if (_state.value.isBlank) return
        viewModelScope.launch { finish() }
    }

    /** Persist, mark complete, and schedule the follow-up work. Returns the id. */
    private suspend fun finish(): Long? {
        persist(complete = true)
        val id = _state.value.entryId
        if (id != null) {
            entries.markComplete(id)
            runCatching { workScheduler.enqueueSummarize(id) }
            // No-ops when a key exists; the summarizer does both jobs better.
            runCatching { workScheduler.enqueueLocalInsight(id) }
        }
        return id
    }

    override fun onCleared() {
        super.onCleared()
        autosaveJob?.cancel()
    }

    private companion object {
        const val AUTOSAVE_DEBOUNCE_MS = 800L
        const val IDLE_GAP_MS = 60_000L
    }
}
