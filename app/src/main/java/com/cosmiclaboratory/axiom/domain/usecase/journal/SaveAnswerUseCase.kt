package com.cosmiclaboratory.axiom.domain.usecase.journal

import com.cosmiclaboratory.axiom.data.database.entity.AnswerEntryEntity
import com.cosmiclaboratory.axiom.data.repository.JournalRepository
import java.time.LocalDateTime
import javax.inject.Inject

class SaveAnswerUseCase @Inject constructor(
    private val journalRepo: JournalRepository
) {
    suspend operator fun invoke(
        existingId: Long?,
        questionId: Long?,
        questionTextSnapshot: String,
        markdown: String,
        plainText: String,
        durationMs: Long
    ): Long {
        val now = LocalDateTime.now()
        val entry = AnswerEntryEntity(
            id = existingId ?: 0L,
            questionId = questionId,
            questionTextSnapshot = questionTextSnapshot,
            markdown = markdown,
            plainText = plainText,
            durationMs = durationMs,
            isComplete = false,
            createdAt = if (existingId == null) now else (journalRepo.getById(existingId)?.createdAt ?: now),
            updatedAt = now
        )
        return journalRepo.upsertEntity(entry)
    }
}
