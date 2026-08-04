package com.cosmiclaboratory.axiom.data.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.cosmiclaboratory.axiom.data.repository.MemoryRepository
import com.cosmiclaboratory.axiom.domain.memory.MemoryConsolidator
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Tidies long-term memory once a week.
 *
 * Entirely on-device and key-free: it is set arithmetic over text the app
 * already holds, so it runs for everyone rather than only for users who
 * connected a model. Weekly rather than after every extraction because
 * duplicates are a slow accumulation, and rewriting memory rows in the middle
 * of a conversation is a good way to make the companion contradict itself
 * mid-sentence.
 */
@HiltWorker
class MemoryConsolidationWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val memories: MemoryRepository
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val all = runCatching { memories.all() }.getOrElse { return Result.success() }
        if (all.size < MIN_MEMORIES) return Result.success()

        val plan = MemoryConsolidator.plan(all)
        if (plan.isEmpty) return Result.success()

        runCatching { memories.applyConsolidation(plan) }
        return Result.success()
    }

    companion object {
        const val UNIQUE_NAME = "memory-consolidation"

        /** Below a couple of dozen there is nothing to tidy and everything to lose. */
        const val MIN_MEMORIES = 20
    }
}
