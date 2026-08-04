package com.cosmiclaboratory.axiom.domain.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SemanticIndexTest {

    /** A small journal with two clear worlds: work strain, and family warmth. */
    private val corpus = listOf(
        1L to "Exhausted after the sprint review, work drained everything out of me",
        2L to "Another draining day at work, the deadline is crushing and I am exhausted",
        3L to "Wiped out by work again, the sprint never ends and sleep is not helping",
        4L to "Long call with Riya, my sister always makes the evening lighter",
        5L to "Riya visited and we cooked, family evenings like this are the best",
        6L to "Sister came over, we cooked and laughed, a genuinely lovely evening",
        7L to "Read a book about volcanoes and geology on the balcony"
    )

    private val index = SemanticIndex.build(corpus)

    @Test
    fun `it finds the right cluster for a query`() {
        val hits = index.search("work is exhausting me").map { it.id }
        assertTrue("expected work entries, got $hits", hits.take(3).all { it in setOf(1L, 2L, 3L) })
    }

    @Test
    fun `it separates unrelated clusters`() {
        val family = index.search("evening with my sister").map { it.id }
        assertTrue("expected family entries, got $family", family.take(2).all { it in setOf(4L, 5L, 6L) })
        assertFalse("work entries should not surface", family.take(2).any { it in setOf(1L, 2L, 3L) })
    }

    @Test
    fun `a partial overlap still surfaces the whole cluster`() {
        // "wiped out" (entry 3) shares only "work" with this query, yet ranks
        // alongside the entries that literally say "exhausted" and "draining".
        val hits = index.search("work is wearing me down").map { it.id }
        assertTrue("expected all three work entries, got $hits", hits.containsAll(listOf(1L, 2L, 3L)))
    }

    @Test
    fun `query expansion pulls in terms that keep company`() {
        // "exhausted" stems to "exhaust"; expansion adds what it co-occurs with.
        val expanded = index.expandQuery("exhausted")
        assertTrue("expected the stem itself, got $expanded", expanded.contains("exhaust"))
        assertTrue("expected learned company, got $expanded", expanded.size > 1)
        assertTrue(
            "expansion should come from the work cluster, got $expanded",
            expanded.any { it in setOf("drain", "work", "sprint", "deadline", "wip") }
        )
    }

    @Test
    fun `stemming folds tense and plural onto one term`() {
        assertEquals("drain", SemanticIndex.stem("drained"))
        assertEquals("drain", SemanticIndex.stem("draining"))
        assertEquals("sprint", SemanticIndex.stem("sprint"))
        // Too short to strip safely, and left alone.
        assertEquals("bed", SemanticIndex.stem("bed"))
        // Non-ASCII scripts are untouched.
        assertEquals("परेशान", SemanticIndex.stem("परेशान"))
    }

    @Test
    fun `expansion never outranks a literal match`() {
        // Entry 7 is the only one about volcanoes; it must win its own query.
        assertEquals(7L, index.search("volcanoes geology").first().id)
    }

    @Test
    fun `unrelated queries return nothing rather than the least-bad guess`() {
        assertTrue(index.search("quantum chromodynamics lattice").isEmpty())
    }

    @Test
    fun `an empty or stopword-only query returns nothing`() {
        assertTrue(index.search("").isEmpty())
        assertTrue(index.search("the and was for").isEmpty())
    }

    @Test
    fun `similar entries can be found from an entry itself`() {
        val similar = index.similarTo(4L).map { it.id }
        assertTrue("expected the other family entries, got $similar", similar.any { it in setOf(5L, 6L) })
        assertFalse("should not return itself", similar.contains(4L))
    }

    @Test
    fun `scores are cosine values in range and ordered`() {
        val hits = index.search("work exhausted sprint")
        assertTrue(hits.isNotEmpty())
        assertTrue(hits.all { it.score in 0.0..1.0001 })
        assertTrue(hits.zipWithNext().all { (a, b) -> a.score >= b.score })
    }

    @Test
    fun `a long entry does not beat a short one on length alone`() {
        val padded = listOf(
            1L to "cat",
            2L to "cat " + (1..300).joinToString(" ") { "unrelated$it" }
        )
        val idx = SemanticIndex.build(padded)
        assertEquals(1L, idx.search("cat", expand = false).first().id)
    }

    @Test
    fun `an empty corpus is safe`() {
        val empty = SemanticIndex.build(emptyList())
        assertEquals(0, empty.documentCount)
        assertTrue(empty.search("anything").isEmpty())
        assertTrue(empty.similarTo(1L).isEmpty())
    }

    @Test
    fun `blank documents are skipped without breaking the index`() {
        val idx = SemanticIndex.build(listOf(1L to "   ", 2L to "real content about running"))
        assertEquals(1, idx.documentCount)
        assertEquals(2L, idx.search("running").first().id)
    }

    @Test
    fun `devanagari and hinglish are indexed like any other vocabulary`() {
        val idx = SemanticIndex.build(
            listOf(
                1L to "आज बहुत थका हुआ था और परेशान भी",
                2L to "आज बहुत खुश और शांत महसूस हुआ",
                3L to "meeting bahut lambi thi aur main thoda tired tha"
            )
        )
        assertEquals(1L, idx.search("थका परेशान", expand = false).first().id)
        assertEquals(3L, idx.search("meeting lambi", expand = false).first().id)
    }
}
