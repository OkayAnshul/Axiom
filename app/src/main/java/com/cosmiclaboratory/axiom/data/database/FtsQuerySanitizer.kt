package com.cosmiclaboratory.axiom.data.database

/**
 * Turns arbitrary user text into something SQLite FTS4 `MATCH` will accept.
 *
 * This is not cosmetic. FTS4 treats `"`, `*`, `(`, `)`, `-`, `^`, `:` and the
 * bare words AND/OR/NOT as query syntax, so passing raw input straight to MATCH
 * throws `SQLiteException: malformed MATCH expression` on an apostrophe — i.e.
 * searching for `what's up?` used to fail. Two divergent private copies of this
 * logic existed (one in the journal repository, one in the companion service);
 * this is the single version, and the only supported route to MATCH.
 *
 * Strategy: strip everything that isn't a letter, digit or whitespace, drop
 * 1-character noise, then OR the terms together with a trailing prefix glob so
 * partial words still match while typing.
 */
object FtsQuerySanitizer {

    /** FTS4 keywords must never reach MATCH unquoted. */
    private val RESERVED = setOf("and", "or", "not", "near")

    private val STOPWORDS = setOf(
        "the", "and", "for", "with", "that", "this", "have", "had", "has",
        "are", "was", "were", "been", "but", "did", "doing", "does", "you",
        "your", "yours", "what", "when", "where", "how", "why", "who",
        "about", "from", "into", "than", "then", "there", "these", "those",
        "ive", "we", "they", "them", "their", "its", "our", "will", "would"
    )

    /**
     * For a user-typed search box: prefix-globbed OR, so "jour" matches "journal".
     * Returns "" when nothing usable survives — callers must treat that as
     * "no results" rather than passing it on.
     */
    fun forSearch(raw: String, maxTerms: Int = 12): String =
        tokenize(raw, dropStopwords = false)
            .take(maxTerms)
            .joinToString(" OR ") { "$it*" }

    /**
     * For retrieval behind a natural-language question (the AI companion), where
     * stopwords are pure noise and would match nearly every entry.
     */
    fun forRetrieval(raw: String, maxTerms: Int = 8): String =
        tokenize(raw, dropStopwords = true)
            .take(maxTerms)
            .joinToString(" OR ")

    private fun tokenize(raw: String, dropStopwords: Boolean): List<String> =
        raw.lowercase()
            // Keep letters/digits from ANY script — \p{L} covers Devanagari, so
            // Hindi queries survive to reach the unicode61-tokenized index.
            .replace(Regex("[^\\p{L}\\p{N}\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { token ->
                val minLength = if (dropStopwords) 3 else 2
                token.length >= minLength &&
                    token !in RESERVED &&
                    (!dropStopwords || token !in STOPWORDS)
            }
            .distinct()
}
