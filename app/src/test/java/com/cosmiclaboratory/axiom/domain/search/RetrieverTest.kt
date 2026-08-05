package com.cosmiclaboratory.axiom.domain.search

import com.cosmiclaboratory.axiom.domain.model.Entry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class RetrieverTest {

    private fun entry(
        id: Long,
        content: String,
        title: String = "",
        daysAgo: Long = 0
    ) = Entry(
        id = id,
        title = title,
        content = content,
        createdAt = LocalDateTime.now().minusDays(daysAgo),
        updatedAt = LocalDateTime.now().minusDays(daysAgo)
    )

    // ---- ranking -----------------------------------------------------------

    @Test
    fun `ranks by how much of the query an entry contains, not by recency`() {
        val candidates = listOf(
            // Newest, but matches one term of three — this is what the old
            // `ORDER BY updatedAt DESC` would have put first.
            entry(1, "Quiet day, went for a walk and felt calm", daysAgo = 0),
            entry(2, "The interview with Riya went well and I felt calm", daysAgo = 30)
        )
        val ranked = Retriever.rankByOverlap(candidates, listOf("interview", "riya", "calm"))
        assertEquals(listOf(2L, 1L), ranked.map { it.id })
    }

    @Test
    fun `a title hit outranks a body hit`() {
        val candidates = listOf(
            entry(1, "mentioned the interview once in passing here"),
            entry(2, "unrelated body text", title = "Interview day")
        )
        val ranked = Retriever.rankByOverlap(candidates, listOf("interview"))
        assertEquals(2L, ranked.first().id)
    }

    @Test
    fun `entries matching nothing are dropped`() {
        val candidates = listOf(entry(1, "completely unrelated"))
        assertTrue(Retriever.rankByOverlap(candidates, listOf("interview")).isEmpty())
    }

    @Test
    fun `recency only breaks ties`() {
        val candidates = listOf(
            entry(1, "the interview", daysAgo = 10),
            entry(2, "the interview", daysAgo = 1)
        )
        val ranked = Retriever.rankByOverlap(candidates, listOf("interview"))
        assertEquals(listOf(2L, 1L), ranked.map { it.id })
    }

    // ---- fusion ------------------------------------------------------------

    @Test
    fun `an entry ranked well by both retrievers beats one ranked well by either`() {
        val keyword = listOf(1L, 2L, 3L)
        val semantic = listOf(3L, 4L, 5L)
        // 3 is 3rd and 1st; 1 is 1st and absent.
        assertEquals(3L, Retriever.fuse(listOf(keyword, semantic), limit = 5).first())
    }

    /**
     * The bug this replaces: results were concatenated and truncated, so when
     * keyword search filled every slot the semantic index contributed nothing.
     */
    @Test
    fun `semantic hits survive a full keyword list`() {
        val keyword = listOf(1L, 2L, 3L, 4L)
        val semantic = listOf(9L, 8L, 7L, 6L)
        val fused = Retriever.fuse(listOf(keyword, semantic), limit = 4)
        assertTrue("semantic hit missing from $fused", fused.contains(9L))
        assertTrue("keyword hit missing from $fused", fused.contains(1L))
    }

    @Test
    fun `fusion is deterministic and respects the limit`() {
        val a = listOf(1L, 2L, 3L)
        val b = listOf(4L, 5L, 6L)
        val once = Retriever.fuse(listOf(a, b), limit = 3)
        assertEquals(3, once.size)
        assertEquals(once, Retriever.fuse(listOf(a, b), limit = 3))
    }

    @Test
    fun `fusion tolerates empty inputs`() {
        assertTrue(Retriever.fuse(emptyList(), limit = 4).isEmpty())
        assertTrue(Retriever.fuse(listOf(emptyList(), emptyList()), limit = 4).isEmpty())
        assertEquals(listOf(1L), Retriever.fuse(listOf(listOf(1L), emptyList()), limit = 4))
    }

    // ---- query building ----------------------------------------------------

    /**
     * The whole reason follow-ups used to retrieve nothing: "did that go okay?"
     * has no content words of its own, and its subject is in the turn before.
     */
    @Test
    fun `a bare follow-up inherits its subject from earlier turns`() {
        val query = Retriever.queryFrom(
            latest = "did that go okay?",
            previousUserTurns = listOf(
                "I have the interview with Riya on Tuesday",
                "feeling nervous about it"
            )
        )
        assertTrue(query, query.contains("interview"))
        assertTrue(query, query.contains("Riya"))
    }

    @Test
    fun `the newest message leads so a new topic still dominates`() {
        val query = Retriever.queryFrom(
            latest = "actually let's talk about the trip to Goa",
            previousUserTurns = listOf("the interview went fine")
        )
        assertTrue(query.startsWith("actually let's talk about the trip to Goa"))
    }

    @Test
    fun `open loops fold into the query as extra context`() {
        val query = Retriever.queryFrom(
            latest = "how are you",
            extraContext = listOf("scan on Tuesday")
        )
        assertTrue(query, query.contains("scan"))
    }
}
