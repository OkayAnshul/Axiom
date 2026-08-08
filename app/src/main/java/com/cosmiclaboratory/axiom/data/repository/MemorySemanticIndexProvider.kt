package com.cosmiclaboratory.axiom.data.repository

import com.cosmiclaboratory.axiom.domain.model.MemoryItem
import com.cosmiclaboratory.axiom.domain.search.SemanticIndex
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A TF-IDF index over what the companion remembers.
 *
 * The app already had one of these — for journal entries — and memory retrieval
 * was left on raw term overlap, which is exact-match only. Talk about being
 * "burnt out at the office" and a memory reading "finds work stressful" scored
 * zero, because the two share no word. That is the single biggest reason a
 * remembered thing failed to come up when it was relevant.
 *
 * Built from the same [SemanticIndex] as entries, so memories get its query
 * expansion and stemming for free, and its rejection of a bundled encoder
 * (30-100 MB of APK, and weak on exactly the Hindi and Hinglish this app cannot
 * afford to be weak on) applies here for the same reasons.
 *
 * Rebuild is keyed on a cheap fingerprint rather than a timestamp: memories are
 * reinforced constantly (every mention bumps `lastSeenAt`) without their *text*
 * changing, and rebuilding the index on a reinforcement would mean rebuilding it
 * on nearly every turn for no gain.
 */
@Singleton
class MemorySemanticIndexProvider @Inject constructor() {

    /** Enough to notice a memory being added, removed, edited or merged. */
    private data class Fingerprint(val count: Int, val textHash: Int)

    private val mutex = Mutex()
    private var cached: SemanticIndex? = null
    private var fingerprint: Fingerprint? = null

    suspend fun indexFor(memories: List<MemoryItem>): SemanticIndex {
        val next = fingerprintOf(memories)
        mutex.withLock {
            val existing = cached
            if (existing != null && fingerprint == next) return existing
            val built = SemanticIndex.build(memories.map { it.id to it.text })
            cached = built
            fingerprint = next
            return built
        }
    }

    private fun fingerprintOf(memories: List<MemoryItem>): Fingerprint =
        Fingerprint(
            count = memories.size,
            // Order-independent, so the caller's sort cannot invalidate the
            // cache — topForPrompt re-sorts on every single turn.
            textHash = memories.fold(0) { acc, item -> acc xor (31 * item.id.hashCode() + item.text.hashCode()) }
        )
}
