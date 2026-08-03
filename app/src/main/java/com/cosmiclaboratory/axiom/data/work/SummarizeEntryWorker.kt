package com.cosmiclaboratory.axiom.data.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cosmiclaboratory.axiom.data.ai.AiProvider
import com.cosmiclaboratory.axiom.data.ai.AiResult
import com.cosmiclaboratory.axiom.data.companion.MemoryExtractor
import com.cosmiclaboratory.axiom.data.database.entity.AIInsightEntity
import com.cosmiclaboratory.axiom.data.preferences.UserPreferences
import com.cosmiclaboratory.axiom.data.repository.JournalRepository
import com.cosmiclaboratory.axiom.data.repository.PersonaRepository
import com.cosmiclaboratory.axiom.data.repository.QuestionRepository
import com.cosmiclaboratory.axiom.domain.model.EmotionMapper
import com.cosmiclaboratory.axiom.domain.model.EntryKind
import com.cosmiclaboratory.axiom.domain.model.MemorySource
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
    private val questionRepo: QuestionRepository,
    private val extractor: MemoryExtractor
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
                // The mood word was being asked for, stored, and then ignored —
                // never displayed, never scaled, never written back. It now
                // becomes the entry's feeling unless the user chose one.
                EmotionMapper.fromWord(v.mood)?.let { emotion ->
                    journalRepo.setInferredMood(entryId, emotion)
                }
                // Best-effort memory extraction. CONVERSATION digests are skipped —
                // their raw transcript was already extracted by the digest worker,
                // and extracting the digest again would double-count everything.
                if (entry.kind != EntryKind.CONVERSATION) {
                    runCatching { extractor.extract(plain, MemorySource.ENTRY, entryId) }
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
