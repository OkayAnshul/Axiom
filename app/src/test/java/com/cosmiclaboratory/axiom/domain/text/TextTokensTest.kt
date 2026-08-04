package com.cosmiclaboratory.axiom.domain.text

import com.cosmiclaboratory.axiom.data.database.FtsQuerySanitizer
import com.cosmiclaboratory.axiom.domain.ml.MoodClassifier
import com.cosmiclaboratory.axiom.domain.style.StyleProfiler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Every one of these pins the same bug, which shipped in seven places at once:
 * Unicode calls Devanagari vowel signs Marks, not Letters, so a tokenizer built
 * on `\p{L}` splits थका into थक + ा and परेशान into पर + श + न. A minimum-length
 * filter then discards the fragments, and Hindi text becomes invisible to
 * search, memory, patterns and the classifier — silently, with no error
 * anywhere.
 */
class TextTokensTest {

    @Test
    fun `devanagari words survive tokenization whole`() {
        val tokens = TextTokens.words("आज बहुत थका और परेशान महसूस हुआ")
        assertTrue("थका was split apart: $tokens", tokens.contains("थका"))
        assertTrue("परेशान was split apart: $tokens", tokens.contains("परेशान"))
        assertTrue(tokens.contains("महसूस"))
    }

    @Test
    fun `a hindi word is one token, not three fragments`() {
        assertEquals(listOf("परेशान"), TextTokens.words("परेशान"))
    }

    @Test
    fun `english is unaffected`() {
        assertEquals(
            listOf("exhausted", "after", "another", "brutal", "day"),
            TextTokens.words("Exhausted after another brutal day!")
        )
    }

    @Test
    fun `hinglish keeps both scripts intact`() {
        val tokens = TextTokens.words("Meeting ठीक रही but I was बहुत tired")
        assertTrue(tokens.contains("ठीक"))
        assertTrue(tokens.contains("बहुत"))
        assertTrue(tokens.contains("meeting"))
        assertTrue(tokens.contains("tired"))
    }

    @Test
    fun `punctuation and emoji separate words without eating them`() {
        val tokens = TextTokens.words("work — 'rough', ok? 🙂 fine")
        assertTrue(tokens.contains("work"))
        assertTrue(tokens.contains("rough"))
        assertTrue(tokens.contains("fine"))
    }

    @Test
    fun `minimum length filters short tokens only after splitting correctly`() {
        // "थका" is exactly three characters once its vowel sign is kept.
        assertTrue(TextTokens.words("थका", minLength = 3).contains("थका"))
    }

    // ---- the call sites that had the bug ------------------------------------

    @Test
    fun `fts sanitizer keeps hindi searchable`() {
        val query = FtsQuerySanitizer.forSearch("परेशान")
        assertTrue("Hindi query was mangled to '$query'", query.contains("परेशान"))
    }

    @Test
    fun `fts retrieval keeps hindi terms`() {
        val query = FtsQuerySanitizer.forRetrieval("आज बहुत परेशान था")
        assertTrue("Hindi retrieval was mangled to '$query'", query.contains("परेशान"))
    }

    @Test
    fun `the classifier can learn from hindi`() {
        val samples = (1..7).map { "आज बहुत थका हुआ और परेशान महसूस हुआ" to 2 } +
            (1..7).map { "आज बहुत खुश और शांत महसूस हुआ" to 4 }
        val model = MoodClassifier.train(samples)
        requireNotNull(model)
        assertEquals(2, MoodClassifier.predict(model, "बहुत थका और परेशान")?.mood)
    }

    @Test
    fun `style profiling still reads devanagari as hindi`() {
        assertEquals(
            LanguageMixHindi,
            StyleProfiler.languageMixFor(listOf("आज का दिन बहुत मुश्किल था।")).name
        )
    }

    private val LanguageMixHindi = "HINDI"
}
