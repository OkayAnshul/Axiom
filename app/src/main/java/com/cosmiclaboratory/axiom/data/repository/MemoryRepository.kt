package com.cosmiclaboratory.axiom.data.repository

import com.cosmiclaboratory.axiom.data.database.dao.MemoryItemDao
import com.cosmiclaboratory.axiom.data.database.entity.MemoryItemEntity
import com.cosmiclaboratory.axiom.data.database.entity.toDomainModel
import com.cosmiclaboratory.axiom.domain.model.MemoryItem
import com.cosmiclaboratory.axiom.domain.model.MemoryKind
import com.cosmiclaboratory.axiom.domain.model.MemorySource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Duration
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.exp

/**
 * The companion's long-term memory. Decay is lazy: nothing rewrites stored
 * weights on a schedule — instead the effective weight fades exponentially with
 * time since the memory was last seen (half-life ~62 days). A memory the user
 * keeps bringing up stays vivid; one never mentioned again quietly recedes,
 * exactly like human recall. Deleting and editing are user rights, not admin
 * features: the "What I remember" screen calls straight into [edit]/[delete].
 */
@Singleton
class MemoryRepository @Inject constructor(
    private val dao: MemoryItemDao
) {

    fun observeAll(): Flow<List<MemoryItem>> =
        dao.observeAll().map { rows -> rows.map { it.toDomainModel() } }

    suspend fun edit(id: Long, newText: String) {
        val existing = dao.getById(id) ?: return
        dao.update(existing.copy(text = newText.trim(), userEdited = true))
    }

    suspend fun delete(id: Long) = dao.deleteById(id)

    /**
     * Extraction-driven rewording. User-edited memories are sacred — the model
     * may reinforce them but never rewrite them.
     */
    suspend fun revise(id: Long, newText: String, now: LocalDateTime = LocalDateTime.now()) {
        val existing = dao.getById(id) ?: return
        if (existing.userEdited) {
            dao.reinforce(id, now)
        } else {
            dao.update(existing.copy(text = newText.trim(), lastSeenAt = now))
        }
    }

    suspend fun exists(id: Long): Boolean = dao.getById(id) != null

    /** Puts back a just-deleted memory exactly as it was — powers undo. */
    suspend fun restore(item: MemoryItem) {
        dao.upsert(
            MemoryItemEntity(
                id = item.id,
                kind = item.kind.name,
                text = item.text,
                weight = item.weight,
                timesSeen = item.timesSeen,
                createdAt = item.createdAt,
                lastSeenAt = item.lastSeenAt,
                sourceType = item.source.name,
                sourceId = item.sourceId,
                userEdited = item.userEdited,
                dueAt = item.dueAt
            )
        )
    }

    suspend fun reinforce(id: Long, now: LocalDateTime = LocalDateTime.now(), bump: Float = 0.15f) =
        dao.reinforce(id, now, bump)

    suspend fun insert(
        kind: MemoryKind,
        text: String,
        weight: Float,
        source: MemorySource,
        sourceId: Long?,
        dueAt: LocalDateTime? = null,
        now: LocalDateTime = LocalDateTime.now()
    ): Long = dao.upsert(
        MemoryItemEntity(
            kind = kind.name,
            text = text.trim(),
            weight = weight.coerceIn(0f, 1f),
            createdAt = now,
            lastSeenAt = now,
            sourceType = source.name,
            sourceId = sourceId,
            dueAt = dueAt
        )
    )

    /** Open loops the companion owes a follow-up on, oldest first. */
    suspend fun dueOpenLoops(now: LocalDateTime = LocalDateTime.now(), limit: Int = 3): List<MemoryItem> =
        dao.dueOpenLoops(now, limit).map { it.toDomainModel() }

    suspend fun closeLoop(id: Long) = dao.closeLoop(id)

    /** Same-kind snapshot used by extraction for dedup context. */
    suspend fun snapshotForExtraction(limit: Int = 40): List<MemoryItem> {
        val now = LocalDateTime.now()
        return dao.getAll()
            .map { it.toDomainModel() }
            .sortedByDescending { it.effectiveWeight(now) }
            .take(limit)
    }

    /**
     * The memory block injected into the companion's system prompt: up to
     * [TOTAL_CAP] items by effective weight, capped per kind so one chatty
     * category can't crowd out the others. Ordered map, stable kind order.
     */
    suspend fun topForPrompt(now: LocalDateTime = LocalDateTime.now()): Map<MemoryKind, List<MemoryItem>> {
        val ranked = dao.getAll()
            .map { it.toDomainModel() }
            .sortedByDescending { it.effectiveWeight(now) }
        val result = linkedMapOf<MemoryKind, MutableList<MemoryItem>>()
        var total = 0
        for (item in ranked) {
            if (total >= TOTAL_CAP) break
            val cap = KIND_CAPS[item.kind] ?: 0
            val bucket = result.getOrPut(item.kind) { mutableListOf() }
            if (bucket.size < cap) {
                bucket.add(item)
                total++
            }
        }
        return result.filterValues { it.isNotEmpty() }
    }

    companion object {
        const val TOTAL_CAP = 20
        val KIND_CAPS: Map<MemoryKind, Int> = mapOf(
            MemoryKind.PERSON to 5,
            MemoryKind.GOAL to 4,
            MemoryKind.THEME to 4,
            MemoryKind.FACT to 4,
            MemoryKind.EVENT to 2,
            MemoryKind.PREFERENCE to 1
        )

        private const val DECAY_DAYS = 90.0

        fun MemoryItem.effectiveWeight(now: LocalDateTime): Double {
            val days = Duration.between(lastSeenAt, now).toHours() / 24.0
            return weight * exp(-days.coerceAtLeast(0.0) / DECAY_DAYS)
        }
    }
}
