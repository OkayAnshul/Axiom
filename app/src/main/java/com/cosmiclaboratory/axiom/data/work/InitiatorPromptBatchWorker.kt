package com.cosmiclaboratory.axiom.data.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cosmiclaboratory.axiom.data.ai.AiProvider
import com.cosmiclaboratory.axiom.data.ai.AiResult
import com.cosmiclaboratory.axiom.data.preferences.UserPreferences
import com.cosmiclaboratory.axiom.data.repository.JournalRepository
import com.cosmiclaboratory.axiom.data.repository.MemoryRepository
import com.cosmiclaboratory.axiom.data.repository.PersonaRepository
import com.cosmiclaboratory.axiom.data.repository.QuestionRepository
import com.cosmiclaboratory.axiom.domain.model.MemoryKind
import com.cosmiclaboratory.axiom.domain.model.humanizedMemory
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class InitiatorPromptBatchWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val aiProvider: AiProvider,
    private val prefs: UserPreferences,
    private val personaRepo: PersonaRepository,
    private val journalRepo: JournalRepository,
    private val questionRepo: QuestionRepository,
    private val memoryRepo: MemoryRepository
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val personaKey = prefs.activePersonaKey.first()
        val persona = personaRepo.getByKey(personaKey) ?: return Result.success()
        val recentSummaries = journalRepo.recentSummariesForContext(limit = 3)

        // Preferences are instructions about how to speak, not material to ask
        // about — the same split the companion's own prompt makes.
        val memoryLines = runCatching {
            memoryRepo.topForPrompt()
                .filterKeys { it != MemoryKind.PREFERENCE }
                .values
                .flatten()
                .map { it.text.humanizedMemory() }
        }.getOrDefault(emptyList())

        return when (
            val result = aiProvider.generateInitiatorPrompts(persona, recentSummaries, memoryLines)
        ) {
            is AiResult.Ok -> {
                questionRepo.saveInitiatorBatch(personaKey, result.value)
                questionRepo.pruneOldInitiators(personaKey)
                prefs.setLastPromptBatchAt(System.currentTimeMillis())
                Result.success()
            }
            AiResult.NoKey -> Result.success()
            AiResult.RateLimited -> Result.retry()
            is AiResult.Network -> Result.retry()
            is AiResult.Parse -> Result.success()
        }
    }
}
