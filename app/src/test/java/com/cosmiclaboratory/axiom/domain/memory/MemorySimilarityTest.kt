package com.cosmiclaboratory.axiom.domain.memory

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The single definition of "these are the same memory", replacing three that
 * disagreed. The disagreement had a visible symptom: a memory could be judged
 * novel on the way in by containment and a duplicate a week later by Jaccard,
 * so it was written and then quietly merged away.
 */
class MemorySimilarityTest {

    private val sister = "Riya is the user's younger sister in Pune"

    @Test
    fun `identical texts score 1`() {
        assertEquals(1.0, MemorySimilarity.jaccard(sister, sister), 0.0001)
        assertEquals(1.0, MemorySimilarity.containment(sister, sister), 0.0001)
    }

    @Test
    fun `rewordings of one fact clear the duplicate threshold`() {
        val score = MemorySimilarity.jaccard(
            sister,
            "The user's younger sister Riya lives in Pune"
        )
        assertTrue("was $score", score > MemorySimilarity.DUPLICATE_THRESHOLD)
    }

    @Test
    fun `distinct facts about the same person stay apart`() {
        val score = MemorySimilarity.jaccard(sister, "Riya got engaged in July")
        assertTrue("was $score", score < MemorySimilarity.DUPLICATE_THRESHOLD)
    }

    @Test
    fun `unrelated facts score near zero`() {
        val score = MemorySimilarity.jaccard(sister, "Wants to switch jobs into game development")
        assertTrue("was $score", score < 0.1)
    }

    @Test
    fun `case and punctuation do not matter`() {
        assertEquals(
            1.0,
            MemorySimilarity.jaccard("Loves late-night walks!", "loves LATE night walks"),
            0.0001
        )
    }

    @Test
    fun `empty and symbol-only strings score zero`() {
        assertEquals(0.0, MemorySimilarity.jaccard("", "anything at all"), 0.0001)
        assertEquals(0.0, MemorySimilarity.jaccard("!!! ---", "anything at all"), 0.0001)
        assertEquals(0.0, MemorySimilarity.containment("", "anything at all"), 0.0001)
    }

    @Test
    fun `devanagari tokenizes`() {
        assertEquals(
            1.0,
            MemorySimilarity.jaccard("बहन पुणे में रहती है", "बहन पुणे में रहती है"),
            0.0001
        )
    }

    /**
     * The reason both measures exist. A short note wholly inside a longer memory
     * is redundant (high containment) without being the same memory (low
     * Jaccard) — and picking the wrong one for the wrong job is what made the
     * three old implementations contradict each other.
     */
    @Test
    fun `containment and jaccard diverge on a length mismatch`() {
        val short = "Riya lives in Pune"
        val long = "Riya is the user's younger sister who lives in Pune and works as a dentist"
        assertTrue(MemorySimilarity.containment(short, long) > 0.9)
        assertTrue(MemorySimilarity.jaccard(short, long) < 0.6)
    }

    @Test
    fun `third-person scaffolding does not make unrelated memories look alike`() {
        // "the user's" appears in most stored memories; without the stoplist it
        // would be shared evidence between every pair of them.
        val a = "The user's manager approved the leave"
        val b = "The user's cat is called Miso"
        assertTrue(MemorySimilarity.jaccard(a, b) < 0.1)
    }
}
