package com.cosmiclaboratory.axiom.data.companion

import com.cosmiclaboratory.axiom.data.ai.AiProvider
import com.cosmiclaboratory.axiom.data.ai.AiResult
import com.cosmiclaboratory.axiom.data.ai.AiTasks
import com.cosmiclaboratory.axiom.data.ai.PromptTemplates
import com.cosmiclaboratory.axiom.data.ai.ChatStreamEvent
import com.cosmiclaboratory.axiom.data.database.FtsQuerySanitizer
import com.cosmiclaboratory.axiom.data.database.entity.CompanionMessageEntity
import com.cosmiclaboratory.axiom.data.preferences.UserPreferences
import com.cosmiclaboratory.axiom.data.repository.CompanionRepository
import com.cosmiclaboratory.axiom.data.repository.JournalRepository
import com.cosmiclaboratory.axiom.data.repository.MemoryRepository
import com.cosmiclaboratory.axiom.data.repository.PersonaRepository
import com.cosmiclaboratory.axiom.data.repository.SemanticIndexProvider
import com.cosmiclaboratory.axiom.data.work.JournalWorkScheduler
import com.cosmiclaboratory.axiom.domain.model.Entry
import com.cosmiclaboratory.axiom.domain.safety.CareLevel
import com.cosmiclaboratory.axiom.domain.search.Retriever
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
        persistUserTurn: Boolean = true,
        /** Set when the turn plainly disclosed distress; changes how to answer. */
        care: CareLevel? = null
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

        // Fold anything that has scrolled out of the verbatim window into the
        // running summary BEFORE building the prompt, so this turn already
        // benefits from it.
        runCatching { compactIfNeeded(threadId) }

        val topK = retrieve(threadId, text)

        val systemPrompt = promptBuilder.buildSystemPrompt(buildContext(threadId, topK, care, text))

        val history = companionRepo
            .recentMessages(threadId, CompanionPromptBuilder.MAX_HISTORY_MESSAGES)
            .map { roleFor(it) to it.content }
        val windowed = promptBuilder.windowHistory(history)

        ai.chatStream(systemPrompt, windowed, maxTokens = REPLY_MAX_TOKENS).collect { event ->
            when (event) {
                is ChatStreamEvent.Delta -> emit(CompanionReplyEvent.Delta(event.text))
                is ChatStreamEvent.Done -> {
                    var reply = event.fullText.trim()
                    if (reply.isEmpty()) {
                        emit(
                            CompanionReplyEvent.Failed(
                                AiResult.Parse(IllegalStateException("Empty companion reply")), ""
                            )
                        )
                    } else {
                        // The model stopped because it ran out of room, not
                        // because it had finished. Sending that as-is leaves a
                        // sentence hanging mid-word and reads as the companion
                        // losing its train of thought. Ask once for the rest.
                        if (event.truncated) {
                            val tail = continueReply(systemPrompt, windowed, reply)
                            if (tail.isNotBlank()) {
                                emit(CompanionReplyEvent.Delta(tail))
                                reply = (reply + tail).trim()
                            }
                        }
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

    /**
     * Keeps a long conversation from losing its own beginning.
     *
     * The verbatim window holds the newest [CompanionPromptBuilder.MAX_HISTORY_MESSAGES]
     * turns. Everything older used to simply vanish from the model's view until
     * the digest ran three hours after the user went quiet — so explaining
     * something over twenty turns meant the companion had forgotten the start of
     * it while they were still talking.
     *
     * This folds what has scrolled out into `rollingSummary`, which already
     * means "everything before the window". It advances `summarizedUpToMessageId`
     * only: `digestedUpToMessageId` is untouched, so the digest still receives
     * the whole session and still writes one journal entry from it.
     *
     * Keyless is a no-op — the wider window is the floor, and a local summary of
     * a live conversation would be worse than none.
     */
    private suspend fun compactIfNeeded(threadId: String) {
        val state = companionRepo.threadState(threadId)
        val unsummarized = companionRepo.messagesAfter(threadId, state.summarizedUpToMessageId)
        // Only the part that has already fallen out of the verbatim window.
        val fallingOut = unsummarized.dropLast(CompanionPromptBuilder.MAX_HISTORY_MESSAGES)
        if (fallingOut.size < COMPACT_THRESHOLD) return

        val transcript = fallingOut.joinToString("\n") { message ->
            val speaker = if (message.role == CompanionMessageEntity.Role.USER.name) "User" else "Companion"
            "$speaker: ${message.content}"
        }
        val result = ai.chat(
            systemPrompt = PromptTemplates.COMPACTION_SYSTEM,
            messages = listOf("user" to PromptTemplates.compactConversation(state.rollingSummary, transcript)),
            maxTokens = COMPACTION_MAX_TOKENS,
            model = AiTasks.CHEAP
        )
        val summary = (result as? AiResult.Ok)?.value?.trim().orEmpty()
        if (summary.isBlank()) return

        companionRepo.saveThreadState(
            state.copy(
                rollingSummary = summary,
                summarizedUpToMessageId = fallingOut.last().id
            )
        )
    }

    /**
     * Picks the journal excerpts this turn gets to see.
     *
     * Two retrievers, fused rather than concatenated. Keyword search is precise
     * when the user names a thing; the semantic index catches the same subject
     * in different words. Appending one after the other meant whichever ran
     * first monopolised the four slots — usually keyword, ordered by edit date.
     *
     * The query is built from the conversation, not the last message, because
     * "did that go okay?" has no content terms of its own and used to retrieve
     * nothing at all.
     */
    private suspend fun retrieve(threadId: String, text: String): List<Entry> {
        val recentUserTurns = runCatching {
            companionRepo.recentMessages(threadId, CompanionPromptBuilder.MAX_HISTORY_MESSAGES)
                .filter { it.role == CompanionMessageEntity.Role.USER.name }
                .map { it.content }
        }.getOrDefault(emptyList())

        val dueLoops = runCatching { memories.dueOpenLoops(limit = 1).map { it.text } }
            .getOrDefault(emptyList())

        val query = Retriever.queryFrom(
            latest = text,
            previousUserTurns = recentUserTurns.dropLast(1),
            extraContext = dueLoops
        )
        val terms = FtsQuerySanitizer.retrievalTerms(query)
        if (terms.isEmpty()) return emptyList()

        val keywordRanked = runCatching {
            Retriever.rankByOverlap(entries.searchForRetrieval(query), terms)
        }.getOrDefault(emptyList())

        val semanticRanked = runCatching {
            semanticIndex.searchScored(query, RETRIEVAL_CANDIDATES).map { it.first }
        }.getOrDefault(emptyList())

        val byId = (keywordRanked + semanticRanked).associateBy { it.id }
        return Retriever
            .fuse(
                rankings = listOf(keywordRanked.map { it.id }, semanticRanked.map { it.id }),
                limit = CompanionPromptBuilder.MAX_EXCERPTS
            )
            .mapNotNull { byId[it] }
            .filter { it.content.isNotBlank() || it.markdown.isNotBlank() }
    }

    /**
     * Asks for the rest of a reply that hit the token cap.
     *
     * One attempt only, and best-effort: if it fails the user still has the
     * truncated text, which is better than nothing and better than a spinner.
     * The partial reply goes back as an assistant turn so the model continues
     * its own sentence rather than starting a new thought.
     */
    private suspend fun continueReply(
        systemPrompt: String,
        history: List<Pair<String, String>>,
        partial: String
    ): String {
        val messages = history + listOf(
            "assistant" to partial,
            "user" to CONTINUE_INSTRUCTION
        )
        val result = runCatching {
            ai.chat(systemPrompt, messages, maxTokens = CONTINUATION_MAX_TOKENS)
        }.getOrNull()
        val text = (result as? AiResult.Ok)?.value?.trim().orEmpty()
        if (text.isEmpty()) return ""
        // Join without swallowing a word boundary; the model may or may not
        // begin with its own leading space.
        return if (partial.endsWith(" ") || text.startsWith(" ")) text else " $text"
    }

    private suspend fun buildContext(
        threadId: String,
        topK: List<Entry>,
        care: CareLevel? = null,
        topicText: String = ""
    ): CompanionPromptBuilder.Context {
        val now = LocalDateTime.now()
        val today = LocalDate.now()
        val entryDates = runCatching { entries.entryDates() }.getOrDefault(emptyList())
        val todaysEntries = runCatching { entries.forDay(today) }.getOrDefault(emptyList())
        val chosenToday = todaysEntries.firstOrNull { it.mood != null && it.moodCapturedAt != null }
        val inferredToday = todaysEntries.firstOrNull { it.mood != null && it.moodCapturedAt == null }
        val persona = personaRepo.getByKey(prefs.activePersonaKey.first())
        // The topic is the turn itself, so what the companion recalls bends
        // toward what is actually being discussed.
        val memoryBlock = memories.topForPrompt(now, topic = topicText)

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
            val corpus = entries.observeCompleted().first()
            val known = memoryBlock.values.flatten()
            PatternFinder.find(
                entries = corpus,
                memories = known,
                // Omitted before, so the companion could never say the most
                // human thing the pattern engine knows: that someone's days
                // read brighter when a particular person comes up.
                personMentions = PatternFinder.personMentions(corpus, known),
                today = today
            ).firstOrNull()?.text
        }.getOrNull()
        return CompanionPromptBuilder.Context(
            displayName = prefs.displayName.first(),
            voice = prefs.voice.first(),
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
            care = care,
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
        /**
         * Raised from 500. At 500 the 70B routinely stopped mid-sentence on
         * anything reflective, and nothing detected it. Truncation is now caught
         * (see [continueReply]), but the cheapest fix is to need it less often.
         */
        /** How deep each retriever goes before fusion picks the final four. */
        const val RETRIEVAL_CANDIDATES = 12

        /** Enough fallen-out turns to be worth a call; below this, just let them go. */
        const val COMPACT_THRESHOLD = 6
        const val COMPACTION_MAX_TOKENS = 300

        const val REPLY_MAX_TOKENS = 800

        /** One extra request's worth of room to finish a thought. */
        const val CONTINUATION_MAX_TOKENS = 400

        const val CONTINUE_INSTRUCTION =
            "Continue exactly where you left off, mid-sentence if that is where you stopped. " +
                "Do not repeat anything you have already said and do not start over."
        const val MENTION_BUMP = 0.05f
        const val MIN_TOKEN_LENGTH = 5
    }
}
