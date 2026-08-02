package com.cosmiclaboratory.axiom.domain.usecase.journal

import com.cosmiclaboratory.axiom.data.repository.JournalRepository
import com.cosmiclaboratory.axiom.data.work.JournalWorkScheduler
import javax.inject.Inject

class CompleteAnswerUseCase @Inject constructor(
    private val journalRepo: JournalRepository,
    private val scheduler: JournalWorkScheduler
) {
    suspend operator fun invoke(answerEntryId: Long) {
        journalRepo.markComplete(answerEntryId)
        scheduler.enqueueSummarize(answerEntryId)
    }
}
