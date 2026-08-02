package com.cosmiclaboratory.axiom.domain.usecase.journal

import com.cosmiclaboratory.axiom.data.repository.JournalRepository
import com.cosmiclaboratory.axiom.domain.model.Entry
import com.cosmiclaboratory.axiom.domain.model.EntryKind
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * Saves a guided-prompt answer as a draft [Entry] of kind PROMPTED.
 *
 * `isComplete = false` keeps it out of the timeline and surfaces it as a draft
 * until [CompleteAnswerUseCase] runs.
 */
class SaveAnswerUseCase @Inject constructor(
    private val journalRepo: JournalRepository
) {
    suspend operator fun invoke(
        existingId: Long?,
        questionId: Long?,
        promptSnapshot: String,
        markdown: String,
        plainText: String,
        durationMs: Long
    ): Long {
        val existing = existingId?.let { journalRepo.getById(it) }
        val now = LocalDateTime.now()
        return journalRepo.upsert(
            Entry(
                id = existingId ?: 0L,
                title = existing?.title.orEmpty(),
                content = plainText,
                markdown = markdown,
                kind = EntryKind.PROMPTED,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
                isComplete = false,
                mood = existing?.mood,
                moodCapturedAt = existing?.moodCapturedAt,
                energy = existing?.energy,
                questionId = questionId,
                promptSnapshot = promptSnapshot,
                durationMs = durationMs,
                tags = existing?.tags.orEmpty()
            )
        )
    }
}
