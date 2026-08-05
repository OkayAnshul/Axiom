package com.cosmiclaboratory.axiom.ui.screens.talks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cosmiclaboratory.axiom.data.database.entity.CompanionMessageEntity
import com.cosmiclaboratory.axiom.data.repository.CompanionRepository
import com.cosmiclaboratory.axiom.data.repository.JournalRepository
import com.cosmiclaboratory.axiom.data.repository.MemoryRepository
import com.cosmiclaboratory.axiom.domain.model.EntryKind
import com.cosmiclaboratory.axiom.domain.model.MemoryItem
import com.cosmiclaboratory.axiom.domain.model.MemorySource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/** One day of talking, and what came out of it. */
data class TalkDay(
    val date: LocalDate,
    val messages: List<CompanionMessageEntity>,
    val opening: String,
    val memoriesFormed: Int,
    val loopsOpened: Int,
    val entryWritten: Boolean
) {
    val messageCount: Int get() = messages.size
}

data class TalksUiState(
    val days: List<TalkDay> = emptyList(),
    val rollingSummary: String = "",
    val loaded: Boolean = false,
    /** Turns a memory demonstrably came from. See [landmarksIn]. */
    val landmarkMessageIds: Set<Long> = emptySet()
)

/**
 * "Our talks" — the conversation half of the story, which had no surface at all.
 *
 * The thread was one endless scroll with no way to jump to a day, and the
 * rolling summary was written on every digest and rendered nowhere. Sessions are
 * grouped by calendar day rather than by silence gap: the digest uses a 3-hour
 * gap internally, but "the talk we had on Tuesday" is how people actually look
 * for a conversation.
 */
@HiltViewModel
class TalksViewModel @Inject constructor(
    private val companionRepo: CompanionRepository,
    private val memories: MemoryRepository,
    private val entries: JournalRepository
) : ViewModel() {

    private val _state = MutableStateFlow(TalksUiState())
    val state: StateFlow<TalksUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                companionRepo.observeThread(THREAD_ID),
                memories.observeAll(),
                entries.observeCompleted()
            ) { messages, allMemories, allEntries ->
                val conversationDates = allEntries
                    .filter { it.kind == EntryKind.CONVERSATION }
                    .map { it.createdAt.toLocalDate() }
                    .toSet()
                val fromTalks = allMemories.filter { it.source == MemorySource.CONVERSATION }
                buildDays(messages, fromTalks, conversationDates) to
                    landmarksIn(messages, fromTalks)
            }.collect { (days, landmarks) ->
                _state.update {
                    it.copy(days = days, landmarkMessageIds = landmarks, loaded = true)
                }
            }
        }
        viewModelScope.launch {
            val summary = runCatching { companionRepo.threadState(THREAD_ID).rollingSummary }
                .getOrDefault("")
            _state.update { it.copy(rollingSummary = summary) }
        }
    }

    fun saveSummary(text: String) {
        _state.update { it.copy(rollingSummary = text) }
        viewModelScope.launch {
            runCatching { companionRepo.updateRollingSummary(THREAD_ID, text) }
        }
    }

    /**
     * The grouping rules are pure and hoisted here so they can be tested
     * directly — the same shape
     * [com.cosmiclaboratory.axiom.data.companion.LocalConversationDigester]
     * uses for its heuristics.
     */
    companion object {
        const val THREAD_ID = "companion"

        /**
         * Below this, a memory is short enough to appear inside an unrelated
         * turn by coincidence, and a coincidence rendered as a landmark is a
         * lie about what the companion noticed.
         */
        const val MIN_LANDMARK_CHARS = 12

        /**
         * Attribution here is honest about its limits. A memory records the message
         * it was extracted *after*, not the sentence that caused it, so counts are
         * assigned to the day that produced them — which is accurate — rather than
         * pinned to a specific turn, which would be a guess dressed as a fact.
         */
        fun buildDays(
            messages: List<CompanionMessageEntity>,
            fromConversation: List<MemoryItem>,
            conversationEntryDates: Set<LocalDate>
        ): List<TalkDay> {
            if (messages.isEmpty()) return emptyList()
            val messageDay = messages.associate { it.id to it.createdAt.toLocalDate() }

            return messages
                .groupBy { it.createdAt.toLocalDate() }
                .toSortedMap(reverseOrder())
                .map { (date, dayMessages) ->
                    val dayMemories = fromConversation.filter { memory ->
                        memory.sourceId?.let { messageDay[it] == date } == true
                    }
                    TalkDay(
                        date = date,
                        messages = dayMessages,
                        opening = dayMessages
                            .firstOrNull { it.role == CompanionMessageEntity.Role.USER.name }
                            ?.content
                            ?: dayMessages.firstOrNull()?.content.orEmpty(),
                        memoriesFormed = dayMemories.count { it.dueAt == null },
                        loopsOpened = dayMemories.count { it.dueAt != null },
                        entryWritten = date in conversationEntryDates
                    )
                }
        }

        /**
         * A turn is marked only when a memory's text is *in* it.
         *
         * The obvious implementation — trust `sourceId` — would mark the wrong turn
         * most of the time. The AI digest stores one id for the whole batch it
         * processed, so every memory from a session points at that session's last
         * message; rendering that as "something stayed with me here" would put a
         * confident mark on an arbitrary turn. Containment is checked against the
         * data instead of asserted from a pointer, so verbatim memories (the local
         * digester stores commitment sentences word for word) light up and
         * paraphrased ones correctly stay unmarked. Nothing is guessed.
         */
        fun landmarksIn(
            messages: List<CompanionMessageEntity>,
            talkMemories: List<MemoryItem>
        ): Set<Long> {
            val quotable = talkMemories
                .map { it.text.trim() }
                .filter { it.length >= MIN_LANDMARK_CHARS }
            if (quotable.isEmpty()) return emptySet()

            return messages.asSequence()
                .filter { it.role == CompanionMessageEntity.Role.USER.name }
                .filter { message -> quotable.any { message.content.contains(it, ignoreCase = true) } }
                .map { it.id }
                .toSet()
        }
    }
}
