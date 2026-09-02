package com.cosmiclaboratory.axiom.data.companion

import com.cosmiclaboratory.axiom.data.ai.AiProvider
import com.cosmiclaboratory.axiom.data.ai.AiResult
import com.cosmiclaboratory.axiom.data.ai.AiTasks
import com.cosmiclaboratory.axiom.data.ai.PromptTemplates
import com.cosmiclaboratory.axiom.data.ai.dto.SessionDigestPayload
import com.cosmiclaboratory.axiom.data.database.entity.CompanionMessageEntity
import com.cosmiclaboratory.axiom.data.repository.CompanionRepository
import com.cosmiclaboratory.axiom.data.repository.JournalRepository
import com.cosmiclaboratory.axiom.data.repository.MemoryRepository
import com.cosmiclaboratory.axiom.domain.model.EmotionMapper
import com.cosmiclaboratory.axiom.domain.model.Entry
import com.cosmiclaboratory.axiom.domain.model.EntryKind
import com.cosmiclaboratory.axiom.domain.model.MemorySource
import kotlinx.serialization.json.Json
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Turns a stretch of conversation into a journal entry, memories, and a rolling
 * summary. Extracted from ConversationDigestWorker so it has two callers.
 *
 * The second caller is the reason this exists. Clearing a conversation used to
 * delete the messages outright, and because the worker only runs ~3h after the
 * user goes quiet, everything said that day had been read by nothing. A whole
 * conversation could be started, finished and erased without leaving a single
 * row behind — verified on a device, where a sixteen-turn conversation left
 * zero entries and zero memories.
 *
 * Enqueuing the worker before deleting would not have fixed it: WorkManager is
 * free to defer, so the delete would race the digest and lose intermittently,
 * which is worse than losing every time. Clearing calls [digest] and waits.
 *
 * Watermark-based rather than time-based, so a user with no key accumulates a
 * backlog that gets digested properly the day they connect one.
 */
@Singleton
class ConversationDigester @Inject constructor(
    private val ai: AiProvider,
    private val companionRepo: CompanionRepository,
    private val journalRepo: JournalRepository,
    private val extractor: MemoryExtractor,
    private val localDigester: LocalConversationDigester,
    private val memories: MemoryRepository,
    private val json: Json
) {

    /**
     * [Retry] is the only outcome a caller must respect: the conversation was
     * NOT written and deleting it would lose it. [NothingToDo] means there was
     * nothing worth keeping — fewer than [MIN_USER_TURNS] turns.
     */
    enum class Outcome { Done, NothingToDo, Retry }

    suspend fun digest(threadId: String = THREAD_ID_DEFAULT): Outcome {
        val state = companionRepo.threadState(threadId)
        val pending = companionRepo.messagesAfter(threadId, state.digestedUpToMessageId)

        val userTurns = pending.count { it.role == CompanionMessageEntity.Role.USER.name }
        if (userTurns < MIN_USER_TURNS) return Outcome.NothingToDo

        val transcript = pending.joinToString("\n") { message ->
            val speaker = if (message.role == CompanionMessageEntity.Role.USER.name) "User" else "Companion"
            "$speaker: ${message.content}"
        }

        /*
         * One call, on the strong model, for what used to be three on the weak
         * one: the entry, the memory deltas and the rolling summary. The old
         * fan-out sent this same transcript three times and let the three
         * results describe the conversation differently.
         */
        val snapshot = runCatching { memories.snapshotForExtraction(MEMORY_SNAPSHOT) }
            .getOrDefault(emptyList())
        val digest = ai.completeJson(
            systemPrompt = PromptTemplates.SESSION_SYSTEM,
            userPrompt = PromptTemplates.sessionDigest(
                transcript = transcript,
                existingSummary = state.rollingSummary,
                existingItems = snapshot.map { Triple(it.id, it.kind.name, it.text) }
            ),
            maxTokens = SESSION_MAX_TOKENS,
            model = AiTasks.QUALITY
        )
        when (digest) {
            is AiResult.Ok -> Unit
            AiResult.NoKey -> {
                /*
                 * No key: fall back to the on-device digest rather than doing
                 * nothing. This used to return silently, which is why talking
                 * taught the companion nothing while writing an entry did.
                 *
                 * `digestedUpToMessageId` advances so entries never duplicate,
                 * but `summarizedUpToMessageId` deliberately does NOT — when a
                 * key is connected later, the AI still gets to run memory
                 * extraction and the rolling summary over this stretch.
                 */
                return localFallback(state.digestedUpToMessageId, pending, threadId)
            }
            AiResult.RateLimited -> return Outcome.Retry
            is AiResult.Network -> return Outcome.Retry
            is AiResult.Unsupported -> {
                // Retrying a withdrawn model never succeeds, and the watermark
                // would never advance — the conversation would be re-sent on
                // every pass until the app is updated. Treat it like no key.
                return localFallback(state.digestedUpToMessageId, pending, threadId)
            }
            is AiResult.Parse -> {
                // A malformed reply used to lose the entry silently and forever,
                // because the watermark still advanced. Fall back to the local
                // digest so the conversation is at least kept.
                return localFallback(state.digestedUpToMessageId, pending, threadId)
            }
        }

        val payload = runCatching {
            json.decodeFromString(SessionDigestPayload.serializer(), digest.value)
        }.getOrNull() ?: return localFallback(state.digestedUpToMessageId, pending, threadId)

        if (payload.entry.isNotBlank()) {
            saveDigestEntry(payload.entry, payload.mood, pending)
        }
        runCatching {
            extractor.applyRawBlock(payload.memory, MemorySource.CONVERSATION, pending.last().id)
        }

        companionRepo.saveThreadState(
            companionRepo.threadState(threadId).copy(
                rollingSummary = payload.summary.ifBlank { state.rollingSummary },
                summarizedUpToMessageId = pending.last().id,
                digestedUpToMessageId = pending.last().id
            )
        )
        return Outcome.Done
    }

    /**
     * Re-reads thread state before saving because the local digest writes an
     * entry, and anything that ran in between must not be clobbered by a stale
     * copy taken before the network call.
     */
    private suspend fun localFallback(
        digestedUpTo: Long,
        pending: List<CompanionMessageEntity>,
        threadId: String
    ): Outcome {
        localDigester.digest(pending)
        val fresh = companionRepo.threadState(threadId)
        companionRepo.saveThreadState(
            fresh.copy(digestedUpToMessageId = maxOf(fresh.digestedUpToMessageId, pending.last().id))
        )
        return Outcome.Done
    }

    private suspend fun saveDigestEntry(
        raw: String,
        moodFromPayload: String,
        session: List<CompanionMessageEntity>
    ) {
        // The mood arrives as its own JSON field now. The trailing "MOOD: word"
        // line is still parsed as a fallback because older models sometimes put
        // it there out of habit.
        val (body, moodLine) = splitMoodLine(raw)
        val moodWord = moodFromPayload.ifBlank { moodLine.orEmpty() }.lowercase().ifBlank { null }
        if (body.isBlank()) return
        val sessionDate = session.last().createdAt.toLocalDate()
        val emotion = EmotionMapper.fromWord(moodWord)
        val mood = emotion?.valence

        val existing = journalRepo.forDay(sessionDate)
            .firstOrNull { it.kind == EntryKind.CONVERSATION }
        if (existing != null) {
            val content = existing.content + "\n\n" + body
            journalRepo.upsert(
                existing.copy(
                    content = content,
                    markdown = content,
                    wordCount = content.split(WHITESPACE).size,
                    charCount = content.length,
                    // Respect a user-chosen mood; only fill inferred mood into a blank.
                    mood = existing.mood ?: mood,
                    emotion = existing.emotion ?: emotion
                )
            )
        } else {
            val title = "From a conversation — " + sessionDate.format(TITLE_DATE)
            journalRepo.upsert(
                Entry(
                    title = title,
                    content = body,
                    markdown = body,
                    kind = EntryKind.CONVERSATION,
                    isComplete = true,
                    // Dated to the conversation, not to whenever the worker got
                    // round to it — a talk from last night belongs to last night.
                    createdAt = session.last().createdAt,
                    mood = mood,
                    // null moodCapturedAt marks the mood as inferred, not chosen.
                    moodCapturedAt = null,
                    emotion = emotion,
                    wordCount = body.split(WHITESPACE).size,
                    charCount = body.length
                )
            )
        }
    }

    private fun splitMoodLine(raw: String): Pair<String, String?> {
        val lines = raw.trim().lines()
        val moodLine = lines.lastOrNull()?.trim()
        return if (moodLine != null && moodLine.startsWith("MOOD:", ignoreCase = true)) {
            lines.dropLast(1).joinToString("\n").trim() to moodLine.substringAfter(':').trim().lowercase()
        } else {
            raw.trim() to null
        }
    }

    companion object {
        const val THREAD_ID_DEFAULT = "companion"

        /** Below this there is no session worth writing up — a greeting is not a talk. */
        const val MIN_USER_TURNS = 2

        /** Room for entry + summary + memory deltas in one response. */
        const val SESSION_MAX_TOKENS = 900

        /** Trimmed from 40: the Jaccard guard and weekly consolidator carry the rest. */
        const val MEMORY_SNAPSHOT = 24
        private val WHITESPACE = Regex("\\s+")
        private val TITLE_DATE = DateTimeFormatter.ofPattern("d MMMM", Locale.ENGLISH)
    }
}
