package com.cosmiclaboratory.axiom.data.repository

import com.cosmiclaboratory.axiom.data.database.dao.CompanionDao
import com.cosmiclaboratory.axiom.data.database.dao.CompanionThreadStateDao
import com.cosmiclaboratory.axiom.data.database.entity.CompanionMessageEntity
import com.cosmiclaboratory.axiom.data.database.entity.CompanionThreadStateEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CompanionRepository @Inject constructor(
    private val dao: CompanionDao,
    private val threadStateDao: CompanionThreadStateDao
) {
    fun observeThread(threadId: String): Flow<List<CompanionMessageEntity>> = dao.observeThread(threadId)

    /** The unparked part of the thread — what the conversation screen shows. */
    fun observeThreadAfter(threadId: String, afterId: Long): Flow<List<CompanionMessageEntity>> =
        dao.observeThreadAfter(threadId, afterId)

    suspend fun recentMessages(threadId: String, limit: Int): List<CompanionMessageEntity> =
        dao.recent(threadId, limit)

    suspend fun messagesAfter(threadId: String, afterId: Long): List<CompanionMessageEntity> =
        dao.after(threadId, afterId)

    suspend fun latestMessage(threadId: String): CompanionMessageEntity? = dao.latest(threadId)

    suspend fun countUpTo(threadId: String, upToId: Long): Int = dao.countUpTo(threadId, upToId)

    suspend fun appendUser(threadId: String, content: String): Long = dao.insert(
        CompanionMessageEntity(
            threadId = threadId,
            role = CompanionMessageEntity.Role.USER.name,
            content = content,
            createdAt = LocalDateTime.now(),
            source = CompanionMessageEntity.Source.USER.name
        )
    )

    suspend fun appendAssistant(
        threadId: String,
        content: String,
        citedEntryIds: List<Long>
    ): Long = dao.insert(
        CompanionMessageEntity(
            threadId = threadId,
            role = CompanionMessageEntity.Role.ASSISTANT.name,
            content = content,
            createdAt = LocalDateTime.now(),
            citedEntryIdsCsv = citedEntryIds.joinToString(","),
            source = CompanionMessageEntity.Source.MODEL.name
        )
    )

    /**
     * Assistant-styled message composed on-device (daily opener) — free, offline-safe.
     *
     * [questionId] is carried when the opener came from the curated bank, so the
     * entry the user eventually writes can point back at the question. Without
     * it the "answered" join never matches and the same prompts recycle forever.
     */
    suspend fun appendLocal(
        threadId: String,
        content: String,
        questionId: Long? = null
    ): Long = dao.insert(
        CompanionMessageEntity(
            threadId = threadId,
            role = CompanionMessageEntity.Role.ASSISTANT.name,
            content = content,
            createdAt = LocalDateTime.now(),
            source = CompanionMessageEntity.Source.LOCAL.name,
            questionId = questionId
        )
    )

    /**
     * Rewrites an on-device opener in place — used when the reader swipes to a
     * different prompt. Swiping picks which question is being asked today; it
     * does not add a second one, so the daily guard keeps holding.
     */
    suspend fun updateLocal(messageId: Long, content: String, questionId: Long?) {
        dao.updateLocalContent(messageId, content, questionId)
    }

    suspend fun threadState(threadId: String): CompanionThreadStateEntity =
        threadStateDao.get(threadId) ?: CompanionThreadStateEntity(
            threadId = threadId,
            updatedAt = LocalDateTime.now()
        )

    /**
     * Lets the user correct what the companion thinks your conversations have
     * been about. The summary is a description of them, so they get the same
     * rights over it as over a memory — `MemoryRepository.edit` is the
     * precedent. Blank clears it rather than storing whitespace.
     */
    suspend fun updateRollingSummary(threadId: String, summary: String) {
        val state = threadState(threadId)
        saveThreadState(state.copy(rollingSummary = summary.trim()))
    }

    suspend fun saveThreadState(state: CompanionThreadStateEntity) =
        threadStateDao.upsert(state.copy(updatedAt = LocalDateTime.now()))

    /**
     * Clears the messages and keeps the memory of them.
     *
     * This is the distinction [deleteThread] does not make, and it matters far
     * more now that clearing is routine rather than rare: the rolling summary is
     * the compressed history of *every* conversation there has ever been, so
     * dropping the thread-state row to clear today's messages threw away the
     * companion's entire sense of continuity. Done nightly, as the new
     * start-fresh behaviour does, that is permanent amnesia.
     *
     * The watermarks reset because they are ids into a table that no longer has
     * those rows; the summary survives because it is not about them.
     */
    suspend fun clearMessages(threadId: String) {
        val state = threadState(threadId)
        dao.deleteThread(threadId)
        saveThreadState(
            state.copy(
                summarizedUpToMessageId = 0,
                digestedUpToMessageId = 0,
                parkedUpToMessageId = 0
            )
        )
    }

    /**
     * Hides everything up to [upToMessageId] until the day turns.
     *
     * Not a delete: [clearMessages] is that. The messages stay readable the
     * moment someone asks for them back, which is what makes starting blank a
     * gesture rather than a loss.
     */
    suspend fun park(threadId: String, upToMessageId: Long) {
        val state = threadState(threadId)
        saveThreadState(state.copy(parkedUpToMessageId = upToMessageId))
    }

    /** Winds the park watermark back, restoring the conversation. */
    suspend fun unpark(threadId: String) {
        val state = threadState(threadId)
        saveThreadState(state.copy(parkedUpToMessageId = 0))
    }

    /**
     * Forgets the thread completely — messages, watermarks and rolling summary.
     * Reserved for an explicit "start over", never for a routine clear.
     */
    suspend fun deleteThread(threadId: String) {
        dao.deleteThread(threadId)
        threadStateDao.delete(threadId)
    }
}
