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

    suspend fun recentMessages(threadId: String, limit: Int): List<CompanionMessageEntity> =
        dao.recent(threadId, limit)

    suspend fun messagesAfter(threadId: String, afterId: Long): List<CompanionMessageEntity> =
        dao.after(threadId, afterId)

    suspend fun latestMessage(threadId: String): CompanionMessageEntity? = dao.latest(threadId)

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

    /** Assistant-styled message composed on-device (daily opener) — free, offline-safe. */
    suspend fun appendLocal(threadId: String, content: String): Long = dao.insert(
        CompanionMessageEntity(
            threadId = threadId,
            role = CompanionMessageEntity.Role.ASSISTANT.name,
            content = content,
            createdAt = LocalDateTime.now(),
            source = CompanionMessageEntity.Source.LOCAL.name
        )
    )

    suspend fun threadState(threadId: String): CompanionThreadStateEntity =
        threadStateDao.get(threadId) ?: CompanionThreadStateEntity(
            threadId = threadId,
            updatedAt = LocalDateTime.now()
        )

    suspend fun saveThreadState(state: CompanionThreadStateEntity) =
        threadStateDao.upsert(state.copy(updatedAt = LocalDateTime.now()))

    suspend fun deleteThread(threadId: String) {
        dao.deleteThread(threadId)
        threadStateDao.delete(threadId)
    }
}
