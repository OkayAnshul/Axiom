package com.cosmiclaboratory.axiom.data.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cosmiclaboratory.axiom.data.ai.AiProvider
import com.cosmiclaboratory.axiom.data.ai.AiResult
import com.cosmiclaboratory.axiom.data.database.entity.AIInsightEntity
import com.cosmiclaboratory.axiom.data.preferences.UserPreferences
import com.cosmiclaboratory.axiom.data.repository.JournalRepository
import com.cosmiclaboratory.axiom.data.repository.PersonaRepository
import com.cosmiclaboratory.axiom.data.repository.QuestionRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime

@HiltWorker
class SummarizeEntryWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val aiProvider: AiProvider,
    private val prefs: UserPreferences,
    private val personaRepo: PersonaRepository,
    private val journalRepo: JournalRepository,
    private val questionRepo: QuestionRepository
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val entryId = inputData.getLong(KEY_ENTRY_ID, -1L)
        if (entryId <= 0L) return Result.failure()

        val existing = journalRepo.getInsight(entryId)
        if (existing != null) return Result.success()

        val entry = journalRepo.getById(entryId) ?: return Result.failure()
        val plain = entry.content.ifBlank { entry.markdown }
        if (plain.isBlank()) return Result.success()

        val personaKey = prefs.activePersonaKey.first()
        val persona = personaRepo.getByKey(personaKey) ?: return Result.success()

        return when (val result = aiProvider.summarizeEntry(plain, persona)) {
            is AiResult.Ok -> {
                val v = result.value
                journalRepo.saveInsight(
                    AIInsightEntity(
                        entryId = entryId,
                        summary = v.summary,
                        followUpQuestionText = v.followUp,
                        themesCsv = v.themes.joinToString(","),
                        mood = v.mood,
                        modelName = result.modelName,
                        totalTokens = result.tokensUsed,
                        createdAt = LocalDateTime.now()
                    )
                )
                if (v.followUp.isNotBlank()) {
                    questionRepo.saveFollowUp(entryId, v.followUp)
                }
                Result.success()
            }
            AiResult.NoKey -> Result.success()
            AiResult.RateLimited -> Result.retry()
            is AiResult.Network -> Result.retry()
            is AiResult.Parse -> Result.success()
        }
    }

    companion object {
        const val KEY_ENTRY_ID = "entry_id"
    }
}
