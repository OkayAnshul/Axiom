package com.cosmiclaboratory.axiom.domain.usecase.journal

import com.cosmiclaboratory.axiom.data.preferences.UserPreferences
import com.cosmiclaboratory.axiom.data.repository.QuestionRepository
import com.cosmiclaboratory.axiom.data.work.JournalWorkScheduler
import com.cosmiclaboratory.axiom.domain.model.Question
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class GetNextQuestionUseCase @Inject constructor(
    private val questionRepo: QuestionRepository,
    private val prefs: UserPreferences,
    private val scheduler: JournalWorkScheduler
) {
    suspend operator fun invoke(lastTheme: String? = null): Question? {
        val persona = prefs.activePersonaKey.first()
        val q = questionRepo.nextQuestion(persona, lastTheme)
        if (questionRepo.unconsumedInitiatorCount(persona) <= REFILL_THRESHOLD) {
            scheduler.requestImmediateInitiatorBatch()
        }
        return q
    }

    private companion object {
        const val REFILL_THRESHOLD = 1
    }
}
