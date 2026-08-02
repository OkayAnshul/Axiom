package com.cosmiclaboratory.axiom.data.repository

import com.cosmiclaboratory.axiom.data.database.dao.CompanionDao
import com.cosmiclaboratory.axiom.data.database.entity.CompanionMessageEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CompanionRepository @Inject constructor(
    private val dao: CompanionDao
) {
    fun observeThread(threadId: String): Flow<List<CompanionMessageEntity>> = dao.observeThread(threadId)

    suspend fun appendUser(threadId: String, content: String): Long = dao.insert(
        CompanionMessageEntity(
            threadId = threadId,
            role = CompanionMessageEntity.Role.USER.name,
            content = content,
            createdAt = LocalDateTime.now()
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
            citedEntryIdsCsv = citedEntryIds.joinToString(",")
        )
    )

    suspend fun deleteThread(threadId: String) = dao.deleteThread(threadId)
}
