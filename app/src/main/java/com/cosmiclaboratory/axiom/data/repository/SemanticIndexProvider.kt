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

    suspend fun current(): SemanticIndex = mutex.withLock {
        val corpus = runCatching { entries.allForIndexing() }.getOrDefault(emptyList())
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

    /** Entries semantically closest to a query, most similar first. */
    suspend fun search(query: String, limit: Int = 8): List<Entry> {
        val hits = current().search(query, limit)
        if (hits.isEmpty()) return emptyList()
        val byId = runCatching { entries.allForIndexing() }.getOrDefault(emptyList()).associateBy { it.id }
        return hits.mapNotNull { byId[it.id] }
    }
}
