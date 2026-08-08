package com.cosmiclaboratory.axiom.domain.memory

import com.cosmiclaboratory.axiom.domain.model.MemoryItem
import java.time.Duration
import java.time.LocalDateTime
import kotlin.math.exp

/**
 * How a memory fades.
 *
 * Decay is lazy: nothing rewrites stored weights on a schedule. A memory's
 * effective weight is computed from its raw weight and how long since it was
 * last seen, so a memory the user keeps bringing up stays vivid and one never
 * mentioned again quietly recedes — which is what recall actually does.
 *
 * This existed twice, in `MemoryRepository` and `MemoryConsolidator`, as two
 * copies of `exp(-days / 90.0)` kept in step by a comment saying they should
 * be. They were the two halves of one decision — what the model sees, and what
 * gets pruned — and a change to one that missed the other would have pruned
 * memories the prompt was still using, or the reverse.
 */
object MemoryDecay {

    /**
     * Time constant, in days. The half-life is `90 * ln 2`, about 62 days: long
     * enough that a real fact survives a quiet month, short enough that a
     * one-off from last spring stops competing with this week.
     */
    const val DECAY_DAYS = 90.0

    fun effectiveWeight(rawWeight: Float, ageDays: Double): Double =
        rawWeight * exp(-ageDays.coerceAtLeast(0.0) / DECAY_DAYS)

    fun effectiveWeight(item: MemoryItem, now: LocalDateTime): Double =
        effectiveWeight(item.weight, ageInDays(item.lastSeenAt, now))

    /**
     * Fractional days, from hours. Whole days would make everything seen today
     * identical in weight to everything seen this morning, which matters during
     * the one conversation where several memories are reinforced at once.
     */
    fun ageInDays(lastSeenAt: LocalDateTime, now: LocalDateTime): Double =
        Duration.between(lastSeenAt, now).toHours() / 24.0
}
