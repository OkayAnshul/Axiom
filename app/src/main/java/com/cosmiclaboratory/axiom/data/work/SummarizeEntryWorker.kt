package com.cosmiclaboratory.axiom.data.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cosmiclaboratory.axiom.data.ai.AiProvider
import com.cosmiclaboratory.axiom.data.ai.AiResult
import com.cosmiclaboratory.axiom.data.ai.AiTasks
import com.cosmiclaboratory.axiom.data.ai.PromptTemplates
import com.cosmiclaboratory.axiom.data.ai.dto.EntryInsightPayload
import com.cosmiclaboratory.axiom.data.companion.MemoryExtractor
import com.cosmiclaboratory.axiom.data.repository.MemoryRepository
import kotlinx.serialization.json.Json
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
    private val extractor: MemoryExtractor,
    private val memories: MemoryRepository,
    private val json: Json
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val entryId = inputData.getLong(KEY_ENTRY_ID, -1L)
        if (entryId <= 0L) return Result.failure()

        val existing = journalRepo.getInsight(entryId)
        if (existing != null) return Result.success()

        val entry = journalRepo.getById(entryId) ?: return Result.failure()
        val plain = entry.content.ifBlank { entry.markdown }
        if (plain.isBlank()) return Result.success()

        /*
         * One call, on the strong model. Summary, follow-up question, themes,
         * mood, title AND memory deltas now come from a single read of the
         * entry — it used to be sent twice, once to summarize and once to
         * extract, over exactly the same text.
         */
        val snapshot = runCatching { memories.snapshotForExtraction(MEMORY_SNAPSHOT) }
            .getOrDefault(emptyList())
        val result = aiProvider.completeJson(
            systemPrompt = PromptTemplates.ENTRY_SYSTEM,
            userPrompt = PromptTemplates.entryInsight(
                plainText = plain,
                hasTitle = entry.title.isNotBlank(),
                existingItems = snapshot.map { Triple(it.id, it.kind.name, it.text) }
            ),
            maxTokens = INSIGHT_MAX_TOKENS,
            model = AiTasks.QUALITY
        )

        return when (result) {
            is AiResult.Ok -> {
                val v = runCatching {
                    json.decodeFromString(EntryInsightPayload.serializer(), result.value)
                }.getOrNull() ?: return Result.success()

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
                // Only ever fills a blank. A title the user wrote is theirs, and
                // a later edit must never be overwritten by a background job.
                if (entry.title.isBlank() && v.title.isNotBlank()) {
                    runCatching { journalRepo.setTitleIfBlank(entryId, v.title.trim().take(MAX_TITLE)) }
                }
                // Themes were extracted, shown as chips, and went nowhere.
                // Promoting them to real tags makes them searchable and feeds
                // the Patterns tab for free.
                runCatching { journalRepo.attachThemeTags(entryId, v.themes.take(MAX_THEME_TAGS)) }
                // CONVERSATION entries are skipped: their transcript was already
                // extracted by the digest worker, and extracting the digest of
                // it again would double-count everything.
                if (entry.kind != EntryKind.CONVERSATION) {
                    runCatching { extractor.applyRawBlock(v.memory, MemorySource.ENTRY, entryId) }
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

        /** Room for summary + follow-up + themes + title + memory deltas. */
        const val INSIGHT_MAX_TOKENS = 700
        const val MEMORY_SNAPSHOT = 24
        const val MAX_TITLE = 60
        const val MAX_THEME_TAGS = 3
    }
}
