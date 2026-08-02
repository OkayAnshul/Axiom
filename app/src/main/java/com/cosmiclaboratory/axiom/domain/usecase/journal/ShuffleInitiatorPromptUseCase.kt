package com.cosmiclaboratory.axiom.domain.usecase.journal

import com.cosmiclaboratory.axiom.data.preferences.UserPreferences
import com.cosmiclaboratory.axiom.data.repository.QuestionRepository
import com.cosmiclaboratory.axiom.data.work.JournalWorkScheduler
import com.cosmiclaboratory.axiom.domain.model.Question
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class ShuffleInitiatorPromptUseCase @Inject constructor(
    private val questionRepo: QuestionRepository,
    private val prefs: UserPreferences,
    private val scheduler: JournalWorkScheduler
) {
    suspend operator fun invoke(): Question? {
        val persona = prefs.activePersonaKey.first()
        val next = questionRepo.nextQuestion(persona)
        if (questionRepo.unconsumedInitiatorCount(persona) == 0) {
            scheduler.requestImmediateInitiatorBatch()
        }
        return next
    }
}
