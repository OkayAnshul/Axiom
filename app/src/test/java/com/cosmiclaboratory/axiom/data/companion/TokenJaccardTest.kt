package com.cosmiclaboratory.axiom.data.companion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The Jaccard guard converts near-duplicate "new" memories into reinforcements
 * of the existing item. Threshold in production is [MemoryExtractor.DUPLICATE_THRESHOLD].
 */
class TokenJaccardTest {

    @Test
    fun `identical texts score 1`() {
        assertEquals(1.0, tokenJaccard("wants to run a marathon", "wants to run a marathon"), 0.0001)
    }

    @Test
    fun `rewordings of the same fact clear the duplicate threshold`() {
        val similarity = tokenJaccard(
            "Riya is the user's younger sister in Pune",
            "The user's younger sister Riya lives in Pune"
        )
        assertTrue("expected > threshold, was $similarity", similarity > MemoryExtractor.DUPLICATE_THRESHOLD)
    }

    @Test
    fun `unrelated facts stay under the threshold`() {
        val similarity = tokenJaccard(
            "Riya is the user's younger sister in Pune",
            "Wants to switch jobs into game development"
        )
        assertTrue("expected < threshold, was $similarity", similarity < MemoryExtractor.DUPLICATE_THRESHOLD)
    }

    @Test
    fun `related but distinct facts about the same person stay under the threshold`() {
        val similarity = tokenJaccard(
            "Riya is the user's younger sister in Pune",
            "Riya got engaged in July"
        )
        assertTrue("expected < threshold, was $similarity", similarity < MemoryExtractor.DUPLICATE_THRESHOLD)
    }

    @Test
    fun `case and punctuation differences do not matter`() {
        assertEquals(1.0, tokenJaccard("Loves late-night walks!", "loves LATE night walks"), 0.0001)
    }

    @Test
    fun `empty or symbol-only strings score 0`() {
        assertEquals(0.0, tokenJaccard("", "anything at all"), 0.0001)
        assertEquals(0.0, tokenJaccard("!!! ---", "anything at all"), 0.0001)
    }

    @Test
    fun `devanagari text tokenizes`() {
        assertTrue(tokenJaccard("बहन पुणे में रहती है", "बहन पुणे में रहती है") == 1.0)
    }
}
