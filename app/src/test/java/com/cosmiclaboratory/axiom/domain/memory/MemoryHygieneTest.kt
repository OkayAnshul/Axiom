package com.cosmiclaboratory.axiom.domain.memory

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The rules that keep "Keeps coming back to message" out of a prompt.
 *
 * The bias here is deliberate: a real theme wrongly dropped is a small loss, but
 * a filler word presented to the model as something a person keeps returning to
 * is a false claim about them, made in the same breath as true ones.
 */
class MemoryHygieneTest {

    @Test
    fun `filler-word themes are degenerate`() {
        assertTrue(MemoryHygiene.isDegenerate("Keeps coming back to message"))
        assertTrue(MemoryHygiene.isDegenerate("Keeps coming back to something"))
    }

    @Test
    fun `themes shorter than the floor are degenerate`() {
        // The two that actually reached a live prompt.
        assertTrue(MemoryHygiene.isDegenerate("Keeps coming back to long"))
        assertTrue(MemoryHygiene.isDegenerate("Keeps coming back to sad"))
    }

    @Test
    fun `a real theme survives`() {
        assertFalse(MemoryHygiene.isDegenerate("Keeps coming back to the move"))
        assertFalse(MemoryHygiene.isDegenerate("Keeps coming back to money"))
        assertFalse(MemoryHygiene.isDegenerate("Keeps meaning to start running again but has not begun."))
    }

    @Test
    fun `trailing punctuation does not rescue a filler word`() {
        assertTrue(MemoryHygiene.isDegenerate("Keeps coming back to message."))
    }

    @Test
    fun `casing does not rescue a filler word`() {
        assertTrue(MemoryHygiene.isDegenerate("Keeps coming back to Message"))
    }

    @Test
    fun `facts and events are never judged`() {
        // Short because life is short. Judging these would delete real memories
        // to fix a cosmetic problem with one phrasing.
        assertFalse(MemoryHygiene.isDegenerate("They cooked dinner."))
        assertFalse(MemoryHygiene.isDegenerate("has a scan on Tuesday"))
        assertFalse(MemoryHygiene.isDegenerate("Riya is the user's younger sister."))
    }

    @Test
    fun `a transcript stored as a memory is degenerate`() {
        // Exactly what was found in a live prompt: joined turns, truncated at
        // the write-time cap, presented to the model as a recent event.
        val wall = "Testing the send button " + "Message number 1 to make the thread long ".repeat(5)
        assertTrue(MemoryHygiene.isDegenerate(wall))
    }

    @Test
    fun `a long but genuine memory still survives`() {
        val real = "Riya is the user's younger sister, lives in Pune, and they speak most weekends."
        assertFalse(real.length.toString(), MemoryHygiene.isDegenerate(real))
    }
}
