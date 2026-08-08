package com.cosmiclaboratory.axiom.domain.memory

import com.cosmiclaboratory.axiom.domain.text.TextTokens

/**
 * Whether two pieces of remembered text are about the same thing.
 *
 * There were three answers to this question in the codebase, and they disagreed:
 * true Jaccard at 0.6 in `MemoryExtractor`, containment at 0.6 in
 * `LocalConversationDigester`, and Jaccard-with-a-stoplist at 0.55 in
 * `MemoryConsolidator`. Because containment and Jaccard diverge sharply when one
 * text is much shorter, a memory could be judged novel on the way in and a
 * duplicate a week later at consolidation — so it was written, then silently
 * merged away, which reads to the user as the app forgetting what they said.
 *
 * One definition, two uses:
 *
 *  - [jaccard] for "are these the same memory", where length matters. A
 *    one-clause fact and a paragraph that contains it are not the same memory.
 *  - [containment] for "have we already got this", where it does not. A short
 *    new observation entirely covered by an existing memory adds nothing.
 */
object MemorySimilarity {

    /** Above this, two same-kind memories are treated as one. */
    const val DUPLICATE_THRESHOLD = 0.6

    /** Consolidation is slightly keener, because it also merges the evidence. */
    const val MERGE_THRESHOLD = 0.55

    /**
     * Content words only. Two things dilute overlap without carrying meaning:
     * ordinary function words, and the scaffolding the extraction prompt
     * produces — memories are written in the third person, so "the user's"
     * appears in half of them and would otherwise make unrelated memories look
     * alike while making a first-person edit of the same fact look different.
     */
    fun tokens(text: String): Set<String> =
        TextTokens.words(text, minLength = MIN_TOKEN_LENGTH)
            .filterNot { it in NON_CONTENT }
            .toSet()

    /** Intersection over union. Symmetric, and punishes a length mismatch. */
    fun jaccard(a: String, b: String): Double = jaccard(tokens(a), tokens(b))

    fun jaccard(a: Set<String>, b: Set<String>): Double {
        if (a.isEmpty() || b.isEmpty()) return 0.0
        return a.intersect(b).size.toDouble() / a.union(b).size
    }

    /**
     * Intersection over the *smaller* set. Asymmetric on purpose: it answers
     * "is one of these wholly inside the other", which is the right question for
     * a short new note against an existing longer memory.
     */
    fun containment(a: String, b: String): Double = containment(tokens(a), tokens(b))

    fun containment(a: Set<String>, b: Set<String>): Double {
        if (a.isEmpty() || b.isEmpty()) return 0.0
        return a.intersect(b).size.toDouble() / minOf(a.size, b.size)
    }

    private const val MIN_TOKEN_LENGTH = 3

    private val NON_CONTENT = setOf(
        "the", "and", "but", "for", "with", "that", "this", "they", "she", "her",
        "his", "him", "its", "was", "were", "are", "has", "had", "have",
        "user", "users", "their", "them", "who", "which", "about"
    )
}
