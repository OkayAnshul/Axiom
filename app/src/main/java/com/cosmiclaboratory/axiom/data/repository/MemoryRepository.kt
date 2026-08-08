package com.cosmiclaboratory.axiom.data.repository

import com.cosmiclaboratory.axiom.data.database.FtsQuerySanitizer
import com.cosmiclaboratory.axiom.data.database.dao.MemoryItemDao
import com.cosmiclaboratory.axiom.data.database.entity.MemoryItemEntity
import com.cosmiclaboratory.axiom.data.database.entity.toDomainModel
import com.cosmiclaboratory.axiom.domain.model.MemoryItem
import com.cosmiclaboratory.axiom.domain.model.MemoryKind
import com.cosmiclaboratory.axiom.domain.memory.MemoryConsolidator
import com.cosmiclaboratory.axiom.domain.model.MemorySource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton
import com.cosmiclaboratory.axiom.domain.memory.MemoryDecay
import com.cosmiclaboratory.axiom.domain.memory.MemoryHygiene

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
    private val dao: MemoryItemDao,
    private val semanticIndex: MemorySemanticIndexProvider
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

    /** Protects a memory from being reworded by extraction. */
    suspend fun markUserEdited(id: Long) {
        val existing = dao.getById(id) ?: return
        dao.update(existing.copy(userEdited = true))
    }

    /** Every memory, for periodic consolidation. */
    suspend fun all(): List<MemoryItem> = dao.getAll().map { it.toDomainModel() }

    /**
     * Applies a consolidation plan: survivors are written back with their
     * combined history, then absorbed and pruned rows are removed. Survivors
     * are saved BEFORE anything is deleted, so an interruption leaves
     * duplicates rather than a hole.
     */
    suspend fun applyConsolidation(plan: MemoryConsolidator.Plan): Int {
        var removed = 0
        plan.merges.forEach { merge ->
            restore(merge.survivor)
            merge.absorbedIds.forEach { id ->
                dao.deleteById(id)
                removed++
            }
        }
        plan.pruneIds.forEach { id ->
            dao.deleteById(id)
            removed++
        }
        return removed
    }

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

    /** Every open loop, upcoming ones included, for "Things I'll ask you about". */
    fun observeOpenLoops(): Flow<List<MemoryItem>> =
        dao.observeOpenLoops().map { rows -> rows.map { it.toDomainModel() } }

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
    suspend fun topForPrompt(
        now: LocalDateTime = LocalDateTime.now(),
        /**
         * What is being talked about right now. When given, memories that
         * actually bear on it are preferred over merely heavy ones.
         *
         * Without this the same twenty memories were sent on every single turn:
         * talk about work all evening and the budget still went on your sister,
         * because selection looked only at decayed weight. Weight says what
         * matters *in general*; this says what matters *now*.
         */
        topic: String = ""
    ): Map<MemoryKind, List<MemoryItem>> {
        val topicTerms = FtsQuerySanitizer.retrievalTerms(topic, maxTerms = 12).toSet()
        val candidates = dao.getAll()
            .map { it.toDomainModel() }
            // Applied on the way out, not just on the way in. Rows written
            // before the theme filters existed were still being sent — a live
            // prompt contained "Keeps coming back to message" alongside real
            // facts about the user. This is the only route memories take into a
            // prompt, so filtering here closes it for good.
            .filterNot { MemoryHygiene.isDegenerate(it.text) }

        // Term overlap only fires on shared words. The semantic index catches
        // the same subject said differently — "burnt out at the office" reaching
        // "finds work stressful" — so its scores are folded in as a bonus rather
        // than replacing the overlap term, which stays the more trustworthy
        // signal when the user names a thing outright.
        val semantic: Map<Long, Double> = if (topic.isBlank()) {
            emptyMap()
        } else {
            runCatching {
                semanticIndex.indexFor(candidates)
                    .search(topic, limit = SEMANTIC_CANDIDATES)
                    .associate { it.id to it.score }
            }.getOrDefault(emptyMap())
        }

        val ranked = candidates
            .sortedByDescending { item -> promptScore(item, topicTerms, now, semantic[item.id]) }
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

    /**
     * Blends "how much this matters generally" with "how much it bears on this
     * conversation".
     *
     * PREFERENCE is deliberately exempt from the topic term: a stated preference
     * ("keep replies short", "don't give advice") is a standing rule, not
     * context, and must not be crowded out just because it shares no words with
     * tonight's subject.
     */
    private fun promptScore(
        item: MemoryItem,
        topicTerms: Set<String>,
        now: LocalDateTime,
        /** Cosine score from the semantic index, when it found this one. */
        semanticScore: Double? = null
    ): Double {
        val weight = item.effectiveWeight(now)
        if (topicTerms.isEmpty() || item.kind == MemoryKind.PREFERENCE) return weight
        val itemTerms = FtsQuerySanitizer.retrievalTerms(item.text, maxTerms = 16).toSet()
        if (itemTerms.isEmpty()) {
            return weight * WEIGHT_SHARE + SEMANTIC_SHARE * (semanticScore ?: 0.0)
        }
        // Normalised by the SMALLER of the two term sets, not by the topic's.
        //
        // Dividing by the topic length meant relevance shrank as the user wrote
        // more: a twelve-word message could score a perfectly matching four-word
        // memory at 0.33, so weight dominated exactly when there was most to
        // match on. A symmetric measure asks "how much of the shorter thing is
        // in the longer one", which is the question actually being asked of a
        // one-line memory against a paragraph.
        val shared = itemTerms.count { it in topicTerms }.toDouble()
        val overlap = shared / minOf(itemTerms.size, topicTerms.size)
        return RELEVANCE_SHARE * overlap +
            WEIGHT_SHARE * weight +
            SEMANTIC_SHARE * (semanticScore ?: 0.0)
    }

    companion object {
        /** How much of a memory's prompt score comes from bearing on the topic. */
        private const val RELEVANCE_SHARE = 0.6
        private const val WEIGHT_SHARE = 0.4

        /**
         * Additive, and smaller than either — a bonus, not a third opinion.
         *
         * Cosine scores here run roughly 0.1–0.5, so this can move a semantically
         * related memory up past a heavier unrelated one without ever letting a
         * loose association outrank a memory whose words the user actually just
         * said.
         */
        private const val SEMANTIC_SHARE = 0.35

        /** Enough to cover the per-kind caps several times over. */
        private const val SEMANTIC_CANDIDATES = 40

        const val TOTAL_CAP = 20
        val KIND_CAPS: Map<MemoryKind, Int> = mapOf(
            MemoryKind.PERSON to 5,
            MemoryKind.GOAL to 4,
            MemoryKind.THEME to 4,
            MemoryKind.FACT to 4,
            MemoryKind.EVENT to 2,
            // Raised in Phase 12: preferences no longer compete with facts for
            // prompt space — they render in the style block, where they are the
            // point rather than a footnote.
            MemoryKind.PREFERENCE to 4
        )

        /**
         * The curve itself lives in [MemoryDecay], shared with consolidation —
         * the two used to be separate copies of the same exponential, so a
         * change to one could have pruned memories the prompt was still using.
         */
        fun MemoryItem.effectiveWeight(now: LocalDateTime): Double =
            MemoryDecay.effectiveWeight(this, now)
    }
}
