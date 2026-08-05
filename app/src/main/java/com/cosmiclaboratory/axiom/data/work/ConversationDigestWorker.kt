package com.cosmiclaboratory.axiom.data.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cosmiclaboratory.axiom.data.ai.AiProvider
import com.cosmiclaboratory.axiom.data.ai.AiResult
import com.cosmiclaboratory.axiom.data.ai.AiTasks
import com.cosmiclaboratory.axiom.data.ai.PromptTemplates
import com.cosmiclaboratory.axiom.data.ai.dto.SessionDigestPayload
import com.cosmiclaboratory.axiom.data.repository.MemoryRepository
import kotlinx.serialization.json.Json
import com.cosmiclaboratory.axiom.data.companion.LocalConversationDigester
import com.cosmiclaboratory.axiom.data.companion.MemoryExtractor
import com.cosmiclaboratory.axiom.data.database.entity.CompanionMessageEntity
import com.cosmiclaboratory.axiom.data.repository.CompanionRepository
import com.cosmiclaboratory.axiom.data.repository.JournalRepository
import com.cosmiclaboratory.axiom.domain.model.EmotionMapper
import com.cosmiclaboratory.axiom.domain.model.Entry
import com.cosmiclaboratory.axiom.domain.model.EntryKind
import com.cosmiclaboratory.axiom.domain.model.MemorySource
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Fires once per conversation session — scheduled with a rolling delay that
 * resets on every message, so it runs ~3h after the user goes quiet. Turns the
 * session into a first-person journal entry (journaling that emerged from
 * talking, the core promise), extracts long-term memories from the raw
 * transcript, and folds the session into the rolling thread summary.
 *
 * Digesting is deliberately watermark-based rather than time-based: if the
 * user has no API key the watermark stays put, and connecting a key later
 * digests everything missed.
 */
@HiltWorker
class ConversationDigestWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val ai: AiProvider,
    private val companionRepo: CompanionRepository,
    private val journalRepo: JournalRepository,
    private val extractor: MemoryExtractor,
    private val localDigester: LocalConversationDigester,
    private val memories: MemoryRepository,
    private val json: Json
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val threadId = inputData.getString(KEY_THREAD_ID) ?: THREAD_ID_DEFAULT
        val state = companionRepo.threadState(threadId)
        val pending = companionRepo.messagesAfter(threadId, state.digestedUpToMessageId)

        val userTurns = pending.count { it.role == CompanionMessageEntity.Role.USER.name }
        if (userTurns < MIN_USER_TURNS) return Result.success()

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
                localDigester.digest(pending)
                companionRepo.saveThreadState(
                    state.copy(digestedUpToMessageId = pending.last().id)
                )
                return Result.success()
            }
            AiResult.RateLimited -> return Result.retry()
            is AiResult.Network -> return Result.retry()
            is AiResult.Parse -> {
                // A malformed reply used to lose the entry silently and forever,
                // because the watermark still advanced. Fall back to the local
                // digest so the conversation is at least kept.
                localDigester.digest(pending)
                companionRepo.saveThreadState(
                    state.copy(digestedUpToMessageId = pending.last().id)
                )
                return Result.success()
            }
        }

        val payload = runCatching {
            json.decodeFromString(SessionDigestPayload.serializer(), digest.value)
        }.getOrNull() ?: run {
            localDigester.digest(pending)
            companionRepo.saveThreadState(state.copy(digestedUpToMessageId = pending.last().id))
            return Result.success()
        }

        if (payload.entry.isNotBlank()) {
            saveDigestEntry(payload.entry, payload.mood, pending)
        }
        runCatching {
            extractor.applyRawBlock(payload.memory, MemorySource.CONVERSATION, pending.last().id)
        }

        companionRepo.saveThreadState(
            state.copy(
                rollingSummary = payload.summary.ifBlank { state.rollingSummary },
                summarizedUpToMessageId = pending.last().id,
                digestedUpToMessageId = pending.last().id
            )
        )
        return Result.success()
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
        const val KEY_THREAD_ID = "thread_id"
        const val THREAD_ID_DEFAULT = "companion"
        const val MIN_USER_TURNS = 2

        /** Room for entry + summary + memory deltas in one response. */
        const val SESSION_MAX_TOKENS = 900

        /** Trimmed from 40: the Jaccard guard and weekly consolidator carry the rest. */
        const val MEMORY_SNAPSHOT = 24
        private val WHITESPACE = Regex("\\s+")
        private val TITLE_DATE = DateTimeFormatter.ofPattern("d MMMM", Locale.ENGLISH)
    }
}
