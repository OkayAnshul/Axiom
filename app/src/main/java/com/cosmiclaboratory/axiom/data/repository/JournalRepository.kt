package com.cosmiclaboratory.axiom.data.repository

import com.cosmiclaboratory.axiom.data.database.dao.AIInsightDao
import com.cosmiclaboratory.axiom.data.database.dao.AnswerEntryDao
import com.cosmiclaboratory.axiom.data.database.entity.AIInsightEntity
import com.cosmiclaboratory.axiom.data.database.entity.AnswerEntryEntity
import com.cosmiclaboratory.axiom.data.database.entity.toDomainModel
import com.cosmiclaboratory.axiom.data.database.entity.toEntity
import com.cosmiclaboratory.axiom.domain.model.AIInsight
import com.cosmiclaboratory.axiom.domain.model.AnswerEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JournalRepository @Inject constructor(
    private val answerDao: AnswerEntryDao,
    private val insightDao: AIInsightDao
) {
    fun observeAll(): Flow<List<AnswerEntry>> =
        answerDao.observeAll().map { rows -> rows.map { it.toDomainModel() } }

    fun observeCompleted(): Flow<List<AnswerEntry>> =
        answerDao.observeCompleted().map { rows -> rows.map { it.toDomainModel() } }

    suspend fun getById(id: Long): AnswerEntry? = answerDao.getById(id)?.toDomainModel()

    fun observeById(id: Long): Flow<AnswerEntry?> =
        answerDao.observeById(id).map { it?.toDomainModel() }

    suspend fun upsert(entry: AnswerEntry): Long = answerDao.insert(entry.toEntity())

    suspend fun upsertEntity(entry: AnswerEntryEntity): Long = answerDao.insert(entry)

    suspend fun update(entry: AnswerEntry) = answerDao.update(entry.toEntity())

    suspend fun markComplete(id: Long) {
        answerDao.markComplete(id, LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
    }

    suspend fun search(query: String): List<AnswerEntry> {
        val sanitized = sanitizeFtsQuery(query)
        if (sanitized.isBlank()) return emptyList()
        return answerDao.search(sanitized).map { it.toDomainModel() }
    }

    suspend fun recentSummariesForContext(limit: Int = 3): List<String> {
        val entries = answerDao.recentSummarized(limit)
        return entries.mapNotNull { entry ->
            insightDao.getByAnswerEntryId(entry.id)?.summary
        }
    }

    fun observeInsight(answerEntryId: Long): Flow<AIInsight?> =
        insightDao.observeByAnswerEntryId(answerEntryId).map { it?.toDomainModel() }

    suspend fun getInsight(answerEntryId: Long): AIInsight? =
        insightDao.getByAnswerEntryId(answerEntryId)?.toDomainModel()

    suspend fun saveInsight(insight: AIInsightEntity): Long = insightDao.insert(insight)

    private fun sanitizeFtsQuery(raw: String): String {
        val cleaned = raw.replace(Regex("[^A-Za-z0-9\\s]"), " ").trim()
        if (cleaned.isEmpty()) return ""
        return cleaned.split(Regex("\\s+")).joinToString(" ") { "$it*" }
    }
}
