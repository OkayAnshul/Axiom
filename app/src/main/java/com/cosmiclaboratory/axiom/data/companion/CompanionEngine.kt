package com.cosmiclaboratory.axiom.data.companion

import com.cosmiclaboratory.axiom.data.ai.AiProvider
import com.cosmiclaboratory.axiom.data.ai.AiResult
import com.cosmiclaboratory.axiom.data.ai.ChatStreamEvent
import com.cosmiclaboratory.axiom.data.database.entity.CompanionMessageEntity
import com.cosmiclaboratory.axiom.data.preferences.UserPreferences
import com.cosmiclaboratory.axiom.data.repository.CompanionRepository
import com.cosmiclaboratory.axiom.data.repository.JournalRepository
import com.cosmiclaboratory.axiom.data.repository.MemoryRepository
import com.cosmiclaboratory.axiom.data.repository.PersonaRepository
import com.cosmiclaboratory.axiom.data.repository.SemanticIndexProvider
import com.cosmiclaboratory.axiom.data.work.JournalWorkScheduler
import com.cosmiclaboratory.axiom.domain.model.Entry
import com.cosmiclaboratory.axiom.domain.model.MemoryKind
import com.cosmiclaboratory.axiom.domain.patterns.PatternFinder
import com.cosmiclaboratory.axiom.domain.streak.StreakCalculator
import com.cosmiclaboratory.axiom.domain.style.StyleProfiler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton
import com.cosmiclaboratory.axiom.domain.text.TextTokens

/** Events the UI receives for one companion reply. Terminal: [Done] or [Failed]. */
sealed interface CompanionReplyEvent {
    data class Delta(val text: String) : CompanionReplyEvent
    data class Done(val fullText: String, val citedEntries: List<Entry>) : CompanionReplyEvent
    data class Failed(val error: AiResult<Nothing>, val partialText: String) : CompanionReplyEvent
}

/**
 * The companion's conversation core. Replaces the old CompanionService, which
 * sent only the latest user message — the model literally could not remember
 * the previous exchange. Every turn now carries: rolling summary of the far
 * past, long-term memories, journal context, and the recent verbatim window as
 * proper role messages.
 *
 * Why not on-device embeddings for retrieval: a local sentence-transformer
 * would weigh ~30 MB, cold-start slow, and bring sketchy multilingual support
 * for Hindi/Hinglish. FTS4 already exists, is free, and (for a journal-sized
 * corpus) precise enough — the LLM does the synthesis. Semantic retrieval is
 * the Phase 13 upgrade.
 */
@Singleton
class CompanionEngine @Inject constructor(
    private val ai: AiProvider,
    private val entries: JournalRepository,
    private val personaRepo: PersonaRepository,
    private val companionRepo: CompanionRepository,
    private val memories: MemoryRepository,
    private val semanticIndex: SemanticIndexProvider,
    private val prefs: UserPreferences,
    private val promptBuilder: CompanionPromptBuilder,
    private val scheduler: JournalWorkScheduler
) {

    /**
     * Sends one user turn. The user message is persisted before the model is
     * called; the assistant message is persisted on completion — including the
     * partial text of a failed stream, because losing a half-written reply is
     * worse than keeping it.
     */
    fun send(
        threadId: String,
        userText: String,
        persistUserTurn: Boolean = true
    ): Flow<CompanionReplyEvent> = flow {
        val text = userText.trim()
        if (text.isEmpty()) {
            emit(CompanionReplyEvent.Failed(AiResult.Parse(IllegalArgumentException("Empty message")), ""))
            return@flow
        }
        if (persistUserTurn) {
            companionRepo.appendUser(threadId, text)
            reinforceMentionedMemories(text)
        }
        // Rolling session-end timer: every message pushes the digest out again,
        // so it fires once, after the user has actually gone quiet.
        scheduler.scheduleConversationDigest(threadId)

        // Hybrid retrieval. Keyword hits lead because an exact word match is
        // the strongest signal there is; the semantic index then fills the
        // remaining slots with entries that are about the same thing in
        // different words — which is most of what a journal search needs.
        val keywordHits = runCatching { entries.searchForRetrieval(text) }.getOrDefault(emptyList())
        val semanticHits = runCatching { semanticIndex.search(text, CompanionPromptBuilder.MAX_EXCERPTS) }
            .getOrDefault(emptyList())
        val topK = (keywordHits + semanticHits)
            .distinctBy { it.id }
            .filter { it.content.isNotBlank() || it.markdown.isNotBlank() }
            .take(CompanionPromptBuilder.MAX_EXCERPTS)

        val systemPrompt = promptBuilder.buildSystemPrompt(buildContext(threadId, topK))

        val history = companionRepo
            .recentMessages(threadId, CompanionPromptBuilder.MAX_HISTORY_MESSAGES)
            .map { roleFor(it) to it.content }
        val windowed = promptBuilder.windowHistory(history)

        ai.chatStream(systemPrompt, windowed, maxTokens = REPLY_MAX_TOKENS).collect { event ->
            when (event) {
                is ChatStreamEvent.Delta -> emit(CompanionReplyEvent.Delta(event.text))
                is ChatStreamEvent.Done -> {
                    val reply = event.fullText.trim()
                    if (reply.isEmpty()) {
                        emit(
                            CompanionReplyEvent.Failed(
                                AiResult.Parse(IllegalStateException("Empty companion reply")), ""
                            )
                        )
                    } else {
                        companionRepo.appendAssistant(threadId, reply, topK.map { it.id })
                        emit(CompanionReplyEvent.Done(reply, topK))
                    }
                }
                is ChatStreamEvent.Failed -> {
                    if (event.partialText.isNotBlank()) {
                        companionRepo.appendAssistant(threadId, event.partialText, topK.map { it.id })
                    }
                    emit(CompanionReplyEvent.Failed(event.error, event.partialText))
                }
            }
        }
    }

    private suspend fun buildContext(threadId: String, topK: List<Entry>): CompanionPromptBuilder.Context {
        val now = LocalDateTime.now()
        val today = LocalDate.now()
        val entryDates = runCatching { entries.entryDates() }.getOrDefault(emptyList())
        val todaysEntries = runCatching { entries.forDay(today) }.getOrDefault(emptyList())
        val chosenToday = todaysEntries.firstOrNull { it.mood != null && it.moodCapturedAt != null }
        val inferredToday = todaysEntries.firstOrNull { it.mood != null && it.moodCapturedAt == null }
        val persona = personaRepo.getByKey(prefs.activePersonaKey.first())
        val memoryBlock = memories.topForPrompt(now)

        // How they write is measured from their own turns, not the companion's.
        val userTurns = companionRepo
            .recentMessages(threadId, STYLE_SAMPLE_MESSAGES)
            .filter { it.role == CompanionMessageEntity.Role.USER.name }
            .map { it.content }
        val style = StyleProfiler.profile(
            userMessages = userTurns,
            preferences = memoryBlock[MemoryKind.PREFERENCE].orEmpty(),
            now = now
        )

        // One pattern at most. A companion that opens with three statistics
        // about you is a dashboard wearing a friend's voice.
        val noticed = runCatching {
            PatternFinder.find(
                entries = entries.observeCompleted().first(),
                memories = memoryBlock.values.flatten(),
                today = today
            ).firstOrNull()?.text
        }.getOrNull()
        return CompanionPromptBuilder.Context(
            displayName = prefs.displayName.first(),
            personaFragment = persona?.systemPromptFragment.orEmpty(),
            memories = memoryBlock,
            rollingSummary = companionRepo.threadState(threadId).rollingSummary,
            recentInsights = runCatching { entries.recentSummariesForContext() }.getOrDefault(emptyList()),
            excerpts = topK.map { entry ->
                CompanionPromptBuilder.Excerpt(
                    date = entry.createdAt.toLocalDate(),
                    mood = entry.mood,
                    text = entry.markdown.ifBlank { entry.content }
                )
            },
            now = now,
            moodToday = (chosenToday ?: inferredToday)?.mood,
            streakDays = StreakCalculator.compute(entryDates, today).current,
            // Only flagged as inferred when the user has not chosen a mood, so
            // the companion never hedges about something they told it directly.
            emotionToday = inferredToday?.emotion.takeIf { chosenToday == null },
            noticed = noticed,
            style = style
        )
    }

    /**
     * Cheap recency signal: a memory whose distinctive tokens appear in the
     * user's message was just "seen" again. Small bump — real reinforcement
     * happens in extraction, this only counters decay for things the user
     * keeps talking about.
     */
    private suspend fun reinforceMentionedMemories(userText: String) {
        val userTokens = tokenize(userText)
        if (userTokens.isEmpty()) return
        runCatching {
            memories.snapshotForExtraction().forEach { memory ->
                val mentioned = tokenize(memory.text).any { it in userTokens }
                if (mentioned) memories.reinforce(memory.id, bump = MENTION_BUMP)
            }
        }
    }

    private fun tokenize(text: String): Set<String> =
        text.lowercase()
            .split(TextTokens.SEPARATOR)
            .filter { it.length >= MIN_TOKEN_LENGTH }
            .toSet()

    private fun roleFor(message: CompanionMessageEntity): String =
        if (message.role == CompanionMessageEntity.Role.USER.name) "user" else "assistant"

    private companion object {
        /** Wider than the history window: style should settle over more than one sitting. */
        const val STYLE_SAMPLE_MESSAGES = 40
        const val REPLY_MAX_TOKENS = 500
        const val MENTION_BUMP = 0.05f
        const val MIN_TOKEN_LENGTH = 5
    }
}
