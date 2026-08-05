package com.cosmiclaboratory.axiom.domain.search

import com.cosmiclaboratory.axiom.domain.model.Entry

/**
 * Chooses which journal entries the companion gets to see.
 *
 * This is the highest-leverage code in the app for whether the companion feels
 * like it knows you, and all three of its jobs were previously done wrong:
 *
 * 1. **Keyword hits were ordered by `updatedAt DESC`** — so the "most relevant"
 *    entries were really just the most recently *edited* ones that happened to
 *    contain any query word.
 * 2. **The semantic index's cosine scores were computed and discarded**, and its
 *    results were simply appended after the keyword list. Since the caller took
 *    the first four, a keyword search returning four rows meant the semantic
 *    index contributed nothing at all.
 * 3. **The query was the last message only** — so "did that go okay?" reduced to
 *    almost no content terms and retrieved nothing, exactly when context from
 *    the previous turn would have made it obvious.
 *
 * Everything here is pure so the ranking can be tested without a database.
 */
object Retriever {

    /**
     * Ranks FTS candidates by how much of the query they actually contain.
     *
     * FTS4 can tell us a row matched but not how well, so overlap is counted
     * here: how many distinct query terms appear, normalised by query length,
     * with a small bonus for terms in the title (in a journal the title is
     * often the only place a topic is named outright). Recency breaks ties
     * only — it is a tiebreak, not a ranking.
     */
    fun rankByOverlap(candidates: List<Entry>, terms: List<String>): List<Entry> {
        if (terms.isEmpty()) return candidates
        return candidates
            .map { entry -> entry to overlapScore(entry, terms) }
            .filter { (_, score) -> score > 0.0 }
            .sortedWith(
                compareByDescending<Pair<Entry, Double>> { it.second }
                    .thenByDescending { it.first.createdAt }
            )
            .map { it.first }
    }

    private fun overlapScore(entry: Entry, terms: List<String>): Double {
        val haystack = (entry.content.ifBlank { entry.markdown }).lowercase()
        val title = entry.title.lowercase()
        var hits = 0.0
        terms.forEach { term ->
            val inTitle = title.contains(term)
            val inBody = haystack.contains(term)
            if (inTitle) hits += TITLE_WEIGHT else if (inBody) hits += 1.0
        }
        return hits / terms.size
    }

    /**
     * Reciprocal-rank fusion across several ranked lists.
     *
     * `score(d) = Σ 1 / (k + rank(d))`, the standard formulation. It needs no
     * calibration between the two retrievers — which matters here because an
     * FTS overlap ratio and a TF-IDF cosine are not on comparable scales and
     * any attempt to weight them directly would be a fudge factor. An entry
     * placed well by *either* retriever surfaces; one placed well by both wins.
     */
    fun fuse(rankings: List<List<Long>>, limit: Int, k: Int = RRF_K): List<Long> {
        if (rankings.isEmpty()) return emptyList()
        val scores = mutableMapOf<Long, Double>()
        val firstSeen = mutableMapOf<Long, Int>()
        rankings.forEach { ranking ->
            ranking.forEachIndexed { index, id ->
                scores[id] = (scores[id] ?: 0.0) + 1.0 / (k + index + 1)
                firstSeen.putIfAbsent(id, index)
            }
        }
        return scores.entries
            .sortedWith(
                compareByDescending<Map.Entry<Long, Double>> { it.value }
                    // Stable and deterministic when scores tie.
                    .thenBy { firstSeen[it.key] ?: Int.MAX_VALUE }
                    .thenBy { it.key }
            )
            .take(limit)
            .map { it.key }
    }

    /**
     * Builds the text to retrieve on from the conversation, not just the last
     * thing said.
     *
     * Follow-ups are the normal shape of talking — "did that go okay?", "and
     * then?", "yeah exactly" — and they carry almost no content words of their
     * own. Their subject lives in the turn before. Older turns are included but
     * the newest text leads, so a genuinely new topic still dominates.
     */
    fun queryFrom(
        latest: String,
        previousUserTurns: List<String> = emptyList(),
        extraContext: List<String> = emptyList()
    ): String = buildString {
        append(latest.trim())
        previousUserTurns.asReversed().take(CONTEXT_TURNS).forEach { turn ->
            val t = turn.trim()
            if (t.isNotEmpty()) {
                append(' ')
                append(t.take(CONTEXT_TURN_CAP))
            }
        }
        extraContext.forEach { extra ->
            val e = extra.trim()
            if (e.isNotEmpty()) {
                append(' ')
                append(e.take(CONTEXT_TURN_CAP))
            }
        }
    }.trim()

    /** A title hit says more than a body hit; worth a little more than one term. */
    private const val TITLE_WEIGHT = 1.5

    /** Standard RRF constant; damps the influence of any single list's top slot. */
    const val RRF_K = 60

    /** How many previous user turns fold into the retrieval query. */
    const val CONTEXT_TURNS = 2

    private const val CONTEXT_TURN_CAP = 300
}
