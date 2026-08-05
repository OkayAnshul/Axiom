package com.cosmiclaboratory.axiom.data.repository

import com.cosmiclaboratory.axiom.domain.model.Entry
import com.cosmiclaboratory.axiom.domain.search.SemanticIndex
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps one built [SemanticIndex] in memory and rebuilds it only when the
 * journal has actually changed.
 *
 * Building is linear in total words — trivial for a personal journal, but not
 * something to redo on every keystroke of a search box or every companion
 * message. Staleness is judged by entry count plus the newest updatedAt, which
 * catches additions, edits and deletions between them without hashing the
 * corpus.
 */
@Singleton
class SemanticIndexProvider @Inject constructor(
    private val entries: JournalRepository
) {
    private data class Fingerprint(val count: Int, val newest: LocalDateTime?)

    private val mutex = Mutex()
    private var index: SemanticIndex? = null
    private var fingerprint: Fingerprint? = null

    suspend fun current(): SemanticIndex =
        indexFor(runCatching { entries.allForIndexing() }.getOrDefault(emptyList()))

    /**
     * Builds or reuses the index for a corpus the caller has already loaded.
     *
     * Taking the corpus as a parameter is the point: every search needs both the
     * index and the entries, and [current] loading them separately meant the
     * whole journal was read from disk twice per companion turn.
     */
    private suspend fun indexFor(corpus: List<Entry>): SemanticIndex = mutex.withLock {
        val now = Fingerprint(corpus.size, corpus.maxOfOrNull { it.updatedAt })

        index?.takeIf { fingerprint == now }?.let { return@withLock it }

        SemanticIndex.build(
            corpus.map { entry ->
                // Title carries real signal in a journal and is often the only
                // place a topic is named outright.
                entry.id to listOfNotNull(
                    entry.title.takeIf { it.isNotBlank() },
                    entry.content.ifBlank { entry.markdown }
                ).joinToString(" ")
            }
        ).also {
            index = it
            fingerprint = now
        }
    }

    /**
     * Entries semantically closest to a query, most similar first.
     *
     * Kept for callers that only want entries. Note this loads the corpus a
     * second time to resolve ids; [searchScored] exists because the companion
     * needs the scores anyway and doing both in one pass halves the disk reads
     * on the hot path.
     */
    suspend fun search(query: String, limit: Int = 8): List<Entry> =
        searchScored(query, limit).map { it.first }

    /**
     * Semantically closest entries paired with their cosine score.
     *
     * The score used to be computed and thrown away at this boundary, which is
     * why fusion downstream was impossible and the semantic index was reduced
     * to "whatever keyword search didn't already fill".
     */
    suspend fun searchScored(query: String, limit: Int = 8): List<Pair<Entry, Double>> {
        val corpus = runCatching { entries.allForIndexing() }.getOrDefault(emptyList())
        if (corpus.isEmpty()) return emptyList()
        val hits = indexFor(corpus).search(query, limit)
        if (hits.isEmpty()) return emptyList()
        val byId = corpus.associateBy { it.id }
        return hits.mapNotNull { hit -> byId[hit.id]?.let { it to hit.score } }
    }

    /** Entries most like a given one — powers "more like this". */
    suspend fun similarTo(entryId: Long, limit: Int = 3): List<Entry> {
        val corpus = runCatching { entries.allForIndexing() }.getOrDefault(emptyList())
        if (corpus.isEmpty()) return emptyList()
        val byId = corpus.associateBy { it.id }
        return indexFor(corpus).similarTo(entryId, limit).mapNotNull { byId[it.id] }
    }
}
