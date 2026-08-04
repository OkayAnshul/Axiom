package com.cosmiclaboratory.axiom.domain.memory

import com.cosmiclaboratory.axiom.domain.model.MemoryItem
import com.cosmiclaboratory.axiom.domain.text.TextTokens
import java.time.Duration
import java.time.LocalDateTime

/**
 * Keeps long-term memory from silting up.
 *
 * Extraction runs after every conversation and every entry, and its dedup guard
 * only compares against the strongest forty memories. Over months that leaks:
 * the same fact arrives phrased three ways, and the "What I remember" screen —
 * whose entire job is being legible — turns into a list nobody reads.
 *
 * Two operations, deliberately unequal in aggression:
 *
 *  - MERGE near-duplicates. Safe, and strictly an improvement: the survivor
 *    inherits the combined sighting count, so merging makes a memory stronger
 *    rather than losing evidence.
 *  - PRUNE almost nothing. Only memories that were seen once, never reinforced,
 *    never touched by the user, and have sat untouched for half a year. Deleting
 *    someone's memories on their behalf is the one thing this file must be
 *    timid about.
 */
object MemoryConsolidator {

    /** Same-kind memories above this token overlap are the same memory. */
    const val MERGE_THRESHOLD = 0.55

    /** Never reinforced, never edited, and this old, before pruning is considered. */
    const val PRUNE_AFTER_DAYS = 180L

    /** Effective weight below which a memory has effectively left the model's view. */
    const val PRUNE_WEIGHT_BELOW = 0.05

    data class Merge(
        /** The memory that stays, already updated. */
        val survivor: MemoryItem,
        /** Ids to delete once the survivor is saved. */
        val absorbedIds: List<Long>
    )

    data class Plan(val merges: List<Merge>, val pruneIds: List<Long>) {
        val isEmpty: Boolean get() = merges.isEmpty() && pruneIds.isEmpty()
    }

    fun plan(memories: List<MemoryItem>, now: LocalDateTime = LocalDateTime.now()): Plan {
        val merges = planMerges(memories)
        val absorbed = merges.flatMap { it.absorbedIds }.toSet()
        val survivors = merges.associateBy { it.survivor.id }

        val pruneIds = memories
            .filterNot { it.id in absorbed || it.id in survivors }
            .filter { shouldPrune(it, now) }
            .map { it.id }

        return Plan(merges, pruneIds)
    }

    /**
     * Groups same-kind memories by token overlap. The survivor is chosen so
     * the user's own words always win: anything they edited beats anything
     * inferred, then the most-reinforced, then the oldest — because the
     * original phrasing is usually the one they would recognise.
     */
    internal fun planMerges(memories: List<MemoryItem>): List<Merge> {
        val remaining = memories.toMutableList()
        val merges = mutableListOf<Merge>()

        while (remaining.isNotEmpty()) {
            val seed = remaining.removeAt(0)
            val seedTokens = tokens(seed.text)
            if (seedTokens.isEmpty()) continue

            val duplicates = remaining.filter { candidate ->
                candidate.kind == seed.kind && overlap(seedTokens, tokens(candidate.text)) >= MERGE_THRESHOLD
            }
            if (duplicates.isEmpty()) continue
            remaining.removeAll(duplicates)

            val group = listOf(seed) + duplicates
            val keeper = group.sortedWith(
                compareByDescending<MemoryItem> { it.userEdited }
                    .thenByDescending { it.timesSeen }
                    .thenBy { it.createdAt }
            ).first()

            merges += Merge(
                survivor = keeper.copy(
                    // Evidence adds up: three sightings phrased three ways are
                    // still three sightings of the same thing.
                    timesSeen = group.sumOf { it.timesSeen },
                    weight = group.maxOf { it.weight },
                    createdAt = group.minOf { it.createdAt },
                    lastSeenAt = group.maxOf { it.lastSeenAt },
                    // An open loop anywhere in the group survives the merge.
                    dueAt = group.mapNotNull { it.dueAt }.minOrNull()
                ),
                absorbedIds = group.filter { it.id != keeper.id }.map { it.id }
            )
        }
        return merges
    }

    private fun shouldPrune(memory: MemoryItem, now: LocalDateTime): Boolean {
        if (memory.userEdited) return false
        if (memory.timesSeen > 1) return false
        if (memory.dueAt != null) return false
        val ageDays = Duration.between(memory.lastSeenAt, now).toDays()
        if (ageDays < PRUNE_AFTER_DAYS) return false
        return effectiveWeight(memory, ageDays) < PRUNE_WEIGHT_BELOW
    }

    /** Same decay curve the prompt selection uses, so the two agree on "faded". */
    private fun effectiveWeight(memory: MemoryItem, ageDays: Long): Double =
        memory.weight * Math.exp(-ageDays.coerceAtLeast(0L) / 90.0)

    /**
     * Content words only. Two things dilute overlap without carrying meaning:
     * ordinary function words, and the scaffolding the extraction prompt
     * produces — memories are written in the third person, so "the user's"
     * appears in half of them and would otherwise make unrelated memories look
     * alike while making a first-person edit of the same fact look different.
     */
    private fun tokens(text: String): Set<String> =
        TextTokens.words(text, minLength = 3)
            .filterNot { it in NON_CONTENT }
            .toSet()

    private val NON_CONTENT = setOf(
        "the", "and", "but", "for", "with", "that", "this", "they", "she", "her",
        "his", "him", "its", "was", "were", "are", "has", "had", "have",
        "user", "users", "their", "them", "who", "which", "about"
    )

    private fun overlap(a: Set<String>, b: Set<String>): Double {
        if (a.isEmpty() || b.isEmpty()) return 0.0
        return a.intersect(b).size.toDouble() / a.union(b).size
    }
}
