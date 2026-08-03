package com.cosmiclaboratory.axiom.data.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cosmiclaboratory.axiom.data.ai.AiProvider
import com.cosmiclaboratory.axiom.data.ai.AiResult
import com.cosmiclaboratory.axiom.data.ai.GroqModels
import com.cosmiclaboratory.axiom.data.ai.PromptTemplates
import com.cosmiclaboratory.axiom.data.companion.MemoryExtractor
import com.cosmiclaboratory.axiom.data.database.entity.CompanionMessageEntity
import com.cosmiclaboratory.axiom.data.repository.CompanionRepository
import com.cosmiclaboratory.axiom.data.repository.JournalRepository
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
    private val extractor: MemoryExtractor
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

        // 1. Digest the session into a journal entry.
        val digest = ai.chat(
            systemPrompt = "You write short journal entries on behalf of a user, in their voice.",
            messages = listOf("user" to PromptTemplates.conversationDigest(transcript)),
            maxTokens = 400,
            model = GroqModels.BACKGROUND
        )
        when (digest) {
            is AiResult.Ok -> saveDigestEntry(digest.value, pending)
            AiResult.NoKey -> return Result.success()
            AiResult.RateLimited -> return Result.retry()
            is AiResult.Network -> return Result.retry()
            is AiResult.Parse -> Unit // digest lost, but memory + summary can still proceed
        }

        // 2. Extract long-term memories from the raw transcript (best effort).
        runCatching { extractor.extract(transcript, MemorySource.CONVERSATION, pending.last().id) }

        // 3. Fold the session into the rolling summary (best effort).
        val summary = ai.chat(
            systemPrompt = "You maintain running summaries.",
            messages = listOf("user" to PromptTemplates.rollingSummary(state.rollingSummary, transcript)),
            maxTokens = 300,
            model = GroqModels.BACKGROUND
        )
        val newSummary = (summary as? AiResult.Ok)?.value?.trim() ?: state.rollingSummary

        companionRepo.saveThreadState(
            state.copy(
                rollingSummary = newSummary,
                summarizedUpToMessageId = pending.last().id,
                digestedUpToMessageId = pending.last().id
            )
        )
        return Result.success()
    }

    private suspend fun saveDigestEntry(raw: String, session: List<CompanionMessageEntity>) {
        val (body, moodWord) = splitMoodLine(raw)
        if (body.isBlank()) return
        val sessionDate = session.last().createdAt.toLocalDate()
        val mood = moodWordToScale(moodWord)

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
                    mood = existing.mood ?: mood
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

    private fun moodWordToScale(word: String?): Int? = when (word) {
        null -> null
        in setOf("awful", "terrible", "devastated", "hopeless", "miserable") -> 1
        in setOf("sad", "low", "anxious", "stressed", "frustrated", "angry", "worried", "tired", "drained", "lonely") -> 2
        in setOf("okay", "neutral", "fine", "mixed", "calm", "steady") -> 3
        in setOf("good", "content", "hopeful", "relieved", "motivated", "grateful", "productive") -> 4
        in setOf("great", "happy", "excited", "joyful", "thrilled", "proud", "energized") -> 5
        else -> null
    }

    companion object {
        const val KEY_THREAD_ID = "thread_id"
        const val THREAD_ID_DEFAULT = "companion"
        const val MIN_USER_TURNS = 2
        private val WHITESPACE = Regex("\\s+")
        private val TITLE_DATE = DateTimeFormatter.ofPattern("d MMMM", Locale.ENGLISH)
    }
}
