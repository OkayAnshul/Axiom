package com.cosmiclaboratory.axiom.data.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cosmiclaboratory.axiom.data.preferences.UserPreferences
import com.cosmiclaboratory.axiom.data.repository.JournalRepository
import com.cosmiclaboratory.axiom.data.repository.MemoryRepository
import com.cosmiclaboratory.axiom.domain.ml.MoodClassifier
import com.cosmiclaboratory.axiom.domain.model.MemoryKind
import com.cosmiclaboratory.axiom.domain.model.MemorySource
import com.cosmiclaboratory.axiom.domain.nlp.CommitmentDetector
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate

/**
 * Everything the app can work out about a finished entry using only this
 * device: how the day felt, and what the user has coming up.
 *
 * Runs only when no vendor has a key. With one, [SummarizeEntryWorker] does
 * both jobs better — a real model reads mood as a named feeling and spots
 * commitments phrased in ways no regex will ever catch. Without one, this is
 * the difference between a companion that notices things and a text editor:
 * the Patterns tab gets data, and the companion can still ask how Tuesday went.
 */
@HiltWorker
class LocalInsightWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val prefs: UserPreferences,
    private val journalRepo: JournalRepository,
    private val memoryRepo: MemoryRepository
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val entryId = inputData.getLong(KEY_ENTRY_ID, -1L)
        if (entryId <= 0L) return Result.failure()

        // The cloud path is strictly better when it is available. Asked across
        // every vendor, not just Groq: someone on a Gemini key was running both
        // paths, so regex commitments landed beside the model's better ones and
        // the on-device classifier raced it for the entry's mood.
        if (prefs.hasAnyApiKey()) return Result.success()

        val entry = journalRepo.getById(entryId) ?: return Result.success()
        val text = entry.content.ifBlank { entry.markdown }
        if (text.isBlank()) return Result.success()

        readMood(entryId, entry.mood, text)
        recordCommitments(entryId, text)
        return Result.success()
    }

    private suspend fun readMood(entryId: Long, existingMood: Int?, text: String) {
        if (existingMood != null) return
        // This entry cannot contaminate its own training set: the query returns
        // only entries with a user-chosen mood, and this one has none.
        val model = MoodClassifier.train(journalRepo.moodTrainingSamples()) ?: return
        val prediction = MoodClassifier.predict(model, text) ?: return
        journalRepo.setPredictedMood(entryId, prediction.mood)
    }

    /**
     * Each commitment becomes an EVENT memory due the day after it happens, so
     * the existing open-loop machinery asks about it and then closes it. The
     * sentence is stored verbatim because it is what gets quoted back, and the
     * user's own words are the only phrasing guaranteed to make sense to them.
     */
    private suspend fun recordCommitments(entryId: Long, text: String) {
        val commitments = runCatching { CommitmentDetector.detect(text, LocalDate.now()) }
            .getOrDefault(emptyList())
            .take(MAX_COMMITMENTS_PER_ENTRY)
        if (commitments.isEmpty()) return

        val existing = runCatching { memoryRepo.snapshotForExtraction() }
            .getOrDefault(emptyList())
            .map { it.text.lowercase() }
            .toSet()

        commitments.forEach { commitment ->
            if (commitment.sentence.lowercase() in existing) return@forEach
            runCatching {
                memoryRepo.insert(
                    kind = MemoryKind.EVENT,
                    text = commitment.sentence,
                    weight = COMMITMENT_WEIGHT,
                    source = MemorySource.ENTRY,
                    sourceId = entryId,
                    dueAt = commitment.followUpOn.atTime(MORNING_HOUR, 0)
                )
            }
        }
    }

    companion object {
        const val KEY_ENTRY_ID = "entry_id"

        /** More than a couple per entry and it stops being memory and starts being a calendar. */
        const val MAX_COMMITMENTS_PER_ENTRY = 2

        /** Middling weight: real enough to surface, not strong enough to crowd out what they told us. */
        const val COMMITMENT_WEIGHT = 0.5f

        private const val MORNING_HOUR = 9
    }
}
