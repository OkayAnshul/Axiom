package com.cosmiclaboratory.axiom.data.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cosmiclaboratory.axiom.data.preferences.UserPreferences
import com.cosmiclaboratory.axiom.data.repository.JournalRepository
import com.cosmiclaboratory.axiom.domain.ml.MoodClassifier
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Reads the mood of a finished entry using a model trained on this device,
 * from this user's own past entries, with no key and no network.
 *
 * Runs only when there is no API key. With one, [SummarizeEntryWorker] infers a
 * named feeling from a far better model and this would only get in its way —
 * whichever writes first wins, since both refuse to overwrite a mood the user
 * chose. Without one, this is the difference between the Patterns tab having
 * data and being empty forever.
 */
@HiltWorker
class LocalMoodWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val prefs: UserPreferences,
    private val journalRepo: JournalRepository
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val entryId = inputData.getLong(KEY_ENTRY_ID, -1L)
        if (entryId <= 0L) return Result.failure()

        // The cloud path is strictly better when it is available.
        if (prefs.groqApiKey() != null) return Result.success()

        val entry = journalRepo.getById(entryId) ?: return Result.success()
        if (entry.mood != null) return Result.success()
        val text = entry.content.ifBlank { entry.markdown }
        if (text.isBlank()) return Result.success()

        // This entry cannot contaminate its own training set: the query only
        // returns entries with a user-chosen mood, and we just established
        // this one has no mood at all.
        val model = MoodClassifier.train(journalRepo.moodTrainingSamples())
            ?: return Result.success()
        val prediction = MoodClassifier.predict(model, text) ?: return Result.success()

        journalRepo.setPredictedMood(entryId, prediction.mood)
        return Result.success()
    }

    companion object {
        const val KEY_ENTRY_ID = "entry_id"
    }
}
