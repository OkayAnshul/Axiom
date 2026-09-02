package com.cosmiclaboratory.axiom.ui.design

import com.cosmiclaboratory.axiom.data.ai.AiResult
import com.cosmiclaboratory.axiom.data.ai.indictsKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * Guards the one place AiResult becomes something the UI can render.
 *
 * The mapping is an exhaustive `when` with no `else`, so a sixth AiResult case
 * breaks the build rather than falling through silently. These tests pin the
 * behaviour that matters: every failure produces a surface, and success does not.
 */
class AxiomErrorMappingTest {

    @Test
    fun `every failure case maps to an error`() {
        val failures = listOf(
            AiResult.NoKey,
            AiResult.RateLimited,
            AiResult.Network(IOException("offline")),
            AiResult.Parse(IllegalStateException("bad json")),
            AiResult.Unsupported("model_decommissioned")
        )
        failures.forEach { result ->
            assertNotNull("${result::class.simpleName} must map to an AxiomError", result.toAxiomError())
        }
    }

    @Test
    fun `success maps to null`() {
        assertNull(AiResult.Ok("hello").toAxiomError())
    }

    @Test
    fun `each failure maps to its own distinct type`() {
        assertEquals(AxiomError.NoAiKey, AiResult.NoKey.toAxiomError())
        assertTrue(AiResult.RateLimited.toAxiomError() is AxiomError.RateLimited)
        assertTrue(AiResult.Network(IOException()).toAxiomError() is AxiomError.Network)
        assertTrue(AiResult.Parse(IllegalStateException()).toAxiomError() is AxiomError.Malformed)
        assertTrue(AiResult.Unsupported("gone").toAxiomError() is AxiomError.ModelUnavailable)
    }

    @Test
    fun `causes survive the mapping so details stay reportable`() {
        val cause = IOException("connection reset")
        val mapped = AiResult.Network(cause).toAxiomError() as AxiomError.Network
        assertEquals(cause, mapped.cause)
    }

    @Test
    fun `retryability matches what the user can actually act on`() {
        // Retrying without a key just fails again — offer "Connect", not "Retry".
        assertTrue(!AxiomError.NoAiKey.isRetryable)
        assertTrue(!AxiomError.Permission("android.permission.RECORD_AUDIO").isRetryable)
        assertTrue(AxiomError.Network().isRetryable)
        assertTrue(AxiomError.RateLimited().isRetryable)
        assertTrue(AxiomError.Storage().isRetryable)
        // A withdrawn model is fixed by shipping a new build, never by tapping
        // again — offering a retry here is just a slower way to fail.
        assertTrue(!AxiomError.ModelUnavailable("decommissioned").isRetryable)
    }

    /**
     * The regression behind "I connected a valid key and the app said it did
     * not work". Save-then-verify rolls the key back when the test fails, so
     * exactly one failure may be allowed to trigger that rollback. When both
     * vendors retired the models this app was pinned to, every other branch was
     * deleting good credentials and blaming them.
     */
    @Test
    fun `only a rejected key counts as evidence against the key`() {
        assertTrue(AiResult.NoKey.indictsKey)

        assertTrue(!AiResult.RateLimited.indictsKey)
        assertTrue(!AiResult.Network(IOException("offline")).indictsKey)
        assertTrue(!AiResult.Parse(IllegalStateException("bad json")).indictsKey)
        assertTrue(!AiResult.Unsupported("model_decommissioned").indictsKey)
        assertTrue(!AiResult.Ok(Unit).indictsKey)
    }

    @Test
    fun `rate limit carries a countdown so retry can be gated`() {
        val e = AiResult.RateLimited.toAxiomError() as AxiomError.RateLimited
        assertTrue("countdown must be positive", e.retryAfterSeconds > 0)
    }
}
