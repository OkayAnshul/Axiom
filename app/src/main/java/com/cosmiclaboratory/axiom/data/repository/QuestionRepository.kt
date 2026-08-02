package com.cosmiclaboratory.axiom.data.repository

import com.cosmiclaboratory.axiom.data.database.dao.AiPromptCacheDao
import com.cosmiclaboratory.axiom.data.database.dao.QuestionDao
import com.cosmiclaboratory.axiom.data.database.entity.AiPromptCacheEntity
import com.cosmiclaboratory.axiom.data.database.entity.QuestionEntity
import com.cosmiclaboratory.axiom.data.database.entity.toDomainModel
import com.cosmiclaboratory.axiom.domain.model.PersonaKey
import com.cosmiclaboratory.axiom.domain.model.Question
import com.cosmiclaboratory.axiom.domain.model.QuestionSource
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuestionRepository @Inject constructor(
    private val questionDao: QuestionDao,
    private val cacheDao: AiPromptCacheDao
) {
    /**
     * Picks the next question per the priority order:
     * 1. Pending AI follow-up (from a recent insight, not yet answered).
     * 2. AI initiator (oldest unconsumed cached prompt for this persona) — promoted to a QuestionEntity on use.
     * 3. Curated, theme-relevant if a theme is provided, not answered in the last 14 days.
     * 4. Curated, any theme, never answered.
     */
    suspend fun nextQuestion(persona: PersonaKey, lastTheme: String? = null): Question? {
        questionDao.pendingFollowUp()?.let { return it.toDomainModel() }

        cacheDao.nextUnconsumed(persona.storageValue)?.let { cached ->
            cacheDao.markConsumed(cached.id)
            val now = LocalDateTime.now()
            val id = questionDao.insert(
                QuestionEntity(
                    text = cached.text,
                    source = QuestionSource.AI_INITIATOR.name,
                    createdAt = now
                )
            )
            return questionDao.getById(id)?.toDomainModel()
        }

        val cutoffIso = LocalDateTime.now().minusDays(14)
            .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        questionDao.randomCuratedNotRecent(lastTheme, cutoffIso)
            ?.let { return it.toDomainModel() }

        questionDao.randomCuratedUnanswered()?.let { return it.toDomainModel() }
        return null
    }

    suspend fun unconsumedInitiatorCount(persona: PersonaKey): Int =
        cacheDao.unconsumedCount(persona.storageValue)

    suspend fun saveInitiatorBatch(persona: PersonaKey, prompts: List<String>): Int {
        if (prompts.isEmpty()) return 0
        val now = LocalDateTime.now()
        val batchId = "batch_${System.currentTimeMillis()}"
        val rows = prompts.map { text ->
            AiPromptCacheEntity(
                batchId = batchId,
                text = text.trim(),
                personaKey = persona.storageValue,
                generatedAt = now,
                consumed = false
            )
        }
        cacheDao.insertAll(rows)
        return rows.size
    }

    suspend fun pruneOldInitiators(persona: PersonaKey, daysOld: Long = 7) {
        val cutoffIso = LocalDateTime.now().minusDays(daysOld)
            .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        cacheDao.pruneOlderThan(persona.storageValue, cutoffIso)
    }

    suspend fun saveFollowUp(parentAnswerId: Long, text: String): Long {
        val now = LocalDateTime.now()
        return questionDao.insert(
            QuestionEntity(
                text = text,
                source = QuestionSource.AI_FOLLOWUP.name,
                parentAnswerId = parentAnswerId,
                createdAt = now
            )
        )
    }
}
