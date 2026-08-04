package com.cosmiclaboratory.axiom.domain.search

import com.cosmiclaboratory.axiom.domain.text.TextTokens
import kotlin.math.ln
import kotlin.math.sqrt

/**
 * Meaning-based retrieval over the user's own journal, computed on this device
 * with no model file, no network and no API key.
 *
 * FTS4 answers "which entries contain these words". That fails the question
 * people actually ask a journal — "when did I feel like this before?" — because
 * the words they use today are rarely the words they used then. This scores
 * whole-document similarity in a vector space instead, so an entry about being
 * "wiped out after the sprint" can surface for a query about feeling
 * "exhausted at work" even with no word in common.
 *
 * Why TF-IDF rather than sentence embeddings: a bundled encoder costs 30-100 MB
 * of APK, cold-starts slowly, and its multilingual coverage of Hindi and
 * Hinglish is exactly where this app cannot afford to be weak. This learns the
 * vocabulary of the person using it, so code-mixed writing and private
 * shorthand work by construction rather than by hoping the pretraining
 * included them. The honest trade: it cannot know two words are related unless
 * the user's own writing puts them in similar company — which is what
 * [expandQuery] recovers.
 */
class SemanticIndex private constructor(
    private val idf: Map<String, Double>,
    private val vectors: Map<Long, Map<String, Double>>,
    /** term -> terms that keep company with it, strongest first. */
    private val neighbours: Map<String, List<String>>,
    val documentCount: Int
) {

    data class Scored(val id: Long, val score: Double)

    /**
     * Ranks documents by cosine similarity. Returns only results above
     * [MIN_SCORE]: a weak match is worse than no match, because it costs the
     * companion prompt space and puts an irrelevant memory in front of the user.
     */
    fun search(query: String, limit: Int = 8, expand: Boolean = true): List<Scored> {
        val typed = tokenise(query)
        if (typed.isEmpty()) return emptyList()

        // Words the user actually typed carry full term-frequency weight;
        // anything the index added on their behalf comes in damped, so an
        // expansion can nudge ranking but never outrank a real match.
        val weights = mutableMapOf<String, Double>()
        typed.groupingBy { it }.eachCount().forEach { (term, count) ->
            weights[term] = 1 + ln(count.toDouble())
        }
        if (expand) {
            expansionsFor(typed).forEach { term ->
                if (term !in weights) weights[term] = EXPANSION_WEIGHT
            }
        }

        val queryVector = normalise(
            weights.mapNotNull { (term, weight) ->
                idf[term]?.let { term to weight * it }
            }.toMap()
        )
        if (queryVector.isEmpty()) return emptyList()

        return vectors.mapNotNull { (id, docVector) ->
            val score = cosine(queryVector, docVector)
            if (score >= MIN_SCORE) Scored(id, score) else null
        }.sortedByDescending { it.score }.take(limit)
    }

    /** Documents most similar to one already in the index — "more like this". */
    fun similarTo(id: Long, limit: Int = 5): List<Scored> {
        val source = vectors[id] ?: return emptyList()
        return vectors.mapNotNull { (other, vector) ->
            if (other == id) return@mapNotNull null
            val score = cosine(source, vector)
            if (score >= MIN_SCORE) Scored(other, score) else null
        }.sortedByDescending { it.score }.take(limit)
    }

    /**
     * Widens a query with terms that share the user's own contexts. This is
     * where the "semantic" part comes from without an embedding model: if their
     * entries about being drained also tend to say "exhausted" and "sleep",
     * those terms come along, weighted below the words actually typed so an
     * expansion can never outrank a real match.
     */
    internal fun expansionsFor(typed: List<String>): List<String> =
        typed.flatMap { term -> neighbours[term].orEmpty().take(EXPANSION_PER_TERM) }
            .distinct()
            .filterNot { it in typed }
            .take(MAX_EXPANSION)

    /** Visible for tests: what a query would be widened to. */
    internal fun expandQuery(query: String): List<String> {
        val typed = tokenise(query)
        return typed + expansionsFor(typed)
    }

    private fun tokenise(text: String): List<String> = tokensOf(text)

    companion object {
        /** Below this, a "match" is coincidence. */
        const val MIN_SCORE = 0.08
        const val MIN_TOKEN_LENGTH = 3
        const val EXPANSION_PER_TERM = 2
        const val MAX_EXPANSION = 6

        /** How much an inferred term counts next to one the user typed. */
        const val EXPANSION_WEIGHT = 0.4

        /** A term must appear in at least this many documents to have learned company. */
        private const val MIN_DOC_FREQ_FOR_NEIGHBOURS = 2
        private const val MAX_NEIGHBOURS_KEPT = 4

        /**
         * Deliberately tiny and English-only. A large stopword list would strip
         * the Hindi and Hinglish function words that carry real signal in a
         * code-mixed journal, and idf already suppresses anything ubiquitous.
         */
        private val STOPWORDS = setOf(
            "the", "and", "was", "for", "that", "this", "with", "have", "had",
            "but", "not", "you", "are", "its", "from", "they", "were", "been"
        )

        /** Longest first, so "ing" wins over "g"-adjacent shorter matches. */
        private val SUFFIXES = listOf("ingly", "edly", "ing", "ed", "ly", "es", "s")

        /**
         * Crude suffix stripping so "drained" and "draining" are one term.
         * Without it, tense and plural split a small personal vocabulary into
         * near-duplicates and the index learns nothing about either.
         *
         * It is applied identically to documents and queries, so it does not
         * need to be linguistically correct — only consistent. "happiness"
         * becoming "happines" costs nothing as long as it happens on both
         * sides. Non-ASCII words are left untouched: these are English
         * suffixes, and Devanagari inflection does not work this way.
         */
        internal fun stem(word: String): String {
            if (!word.all { it in 'a'..'z' }) return word
            SUFFIXES.forEach { suffix ->
                if (word.length > suffix.length + 2 && word.endsWith(suffix)) {
                    return word.dropLast(suffix.length)
                }
            }
            return word
        }

        private fun tokensOf(text: String): List<String> =
            TextTokens.words(text, MIN_TOKEN_LENGTH)
                .filterNot { it in STOPWORDS }
                .map(::stem)

        fun build(documents: List<Pair<Long, String>>): SemanticIndex {
            val tokenised = documents.map { (id, text) -> id to tokensOf(text) }
                .filter { it.second.isNotEmpty() }

            if (tokenised.isEmpty()) {
                return SemanticIndex(emptyMap(), emptyMap(), emptyMap(), 0)
            }

            val docCount = tokenised.size
            val docFreq = mutableMapOf<String, Int>()
            tokenised.forEach { (_, tokens) ->
                tokens.distinct().forEach { term -> docFreq[term] = (docFreq[term] ?: 0) + 1 }
            }

            // Smoothed idf: a term in every document scores ~0 rather than
            // negative, which would actively push matching documents down.
            val idf = docFreq.mapValues { (_, freq) ->
                ln((docCount + 1.0) / (freq + 1.0)) + 1.0
            }

            val vectors = tokenised.associate { (id, tokens) ->
                val counts = tokens.groupingBy { it }.eachCount()
                id to normalise(
                    counts.mapValues { (term, count) ->
                        (1 + ln(count.toDouble())) * (idf[term] ?: 0.0)
                    }
                )
            }

            return SemanticIndex(idf, vectors, buildNeighbours(tokenised, docFreq), docCount)
        }

        /**
         * Distributional similarity, cheaply: two terms are related when they
         * keep appearing in the same entries. Restricted to terms seen in more
         * than one document, because a word used once has no company to learn
         * from and would only add noise.
         */
        private fun buildNeighbours(
            tokenised: List<Pair<Long, List<String>>>,
            docFreq: Map<String, Int>
        ): Map<String, List<String>> {
            val eligible = docFreq.filterValues { it >= MIN_DOC_FREQ_FOR_NEIGHBOURS }.keys
            if (eligible.size < 2) return emptyMap()

            val cooccurrence = mutableMapOf<String, MutableMap<String, Int>>()
            tokenised.forEach { (_, tokens) ->
                val present = tokens.distinct().filter { it in eligible }
                present.forEach { a ->
                    val row = cooccurrence.getOrPut(a) { mutableMapOf() }
                    present.forEach { b -> if (a != b) row[b] = (row[b] ?: 0) + 1 }
                }
            }

            return cooccurrence.mapValues { (term, row) ->
                val termFreq = docFreq[term] ?: 1
                row.entries
                    // Normalised by both frequencies so a common word is not
                    // everybody's neighbour just for being common.
                    .sortedByDescending { (other, together) ->
                        together / sqrt(termFreq.toDouble() * (docFreq[other] ?: 1))
                    }
                    .take(MAX_NEIGHBOURS_KEPT)
                    .map { it.key }
            }
        }

        private fun normalise(vector: Map<String, Double>): Map<String, Double> {
            val magnitude = sqrt(vector.values.sumOf { it * it })
            if (magnitude == 0.0) return emptyMap()
            return vector.mapValues { (_, value) -> value / magnitude }
        }

        /** Both vectors are unit length, so the dot product IS the cosine. */
        private fun cosine(a: Map<String, Double>, b: Map<String, Double>): Double {
            val (small, large) = if (a.size <= b.size) a to b else b to a
            return small.entries.sumOf { (term, weight) -> weight * (large[term] ?: 0.0) }
        }
    }
}
