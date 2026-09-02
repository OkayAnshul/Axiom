package com.cosmiclaboratory.axiom.data.ai

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GroqSseParserTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private fun parse(line: String) = parseGroqSseLine(line, json)

    @Test
    fun `delta line yields its content`() {
        val line = """data: {"id":"x","model":"openai/gpt-oss-120b","choices":[{"delta":{"content":"Hello"},"finish_reason":null}]}"""
        val result = parse(line) as SseLine.Delta
        assertEquals("Hello", result.text)
        assertEquals("openai/gpt-oss-120b", result.model)
    }

    @Test
    fun `done sentinel is recognized`() {
        assertEquals(SseLine.Done, parse("data: [DONE]"))
    }

    @Test
    fun `done sentinel survives missing space after colon`() {
        assertEquals(SseLine.Done, parse("data:[DONE]"))
    }

    @Test
    fun `blank and keep-alive lines are ignored`() {
        assertNull(parse(""))
        assertNull(parse("   "))
        assertNull(parse("data:"))
        assertNull(parse(": keep-alive comment"))
    }

    @Test
    fun `non-data lines are ignored`() {
        assertNull(parse("event: message"))
        assertNull(parse("retry: 3000"))
    }

    @Test
    fun `role-only first chunk without content is ignored`() {
        val line = """data: {"choices":[{"delta":{"role":"assistant"},"finish_reason":null}]}"""
        assertNull(parse(line))
    }

    @Test
    fun `finish chunk carries its reason instead of being dropped`() {
        val line = """data: {"choices":[{"delta":{},"finish_reason":"stop"}]}"""
        val result = parse(line) as SseLine.Meta
        assertEquals("stop", result.finishReason)
    }

    /**
     * The reason this parser changed: a reply cut off at max_tokens used to look
     * exactly like one that finished, so it was stored and shown as complete.
     */
    @Test
    fun `truncation is distinguishable from a normal finish`() {
        val truncated = """data: {"choices":[{"delta":{},"finish_reason":"length"}]}"""
        assertEquals("length", (parse(truncated) as SseLine.Meta).finishReason)
    }

    @Test
    fun `usage chunk yields token count`() {
        val line = """data: {"choices":[],"usage":{"total_tokens":1234}}"""
        val result = parse(line) as SseLine.Meta
        assertEquals(1234, result.totalTokens)
    }

    @Test
    fun `role-only chunk still yields nothing at all`() {
        // No content, no finish reason, no usage — genuinely nothing to report.
        val line = """data: {"choices":[{"delta":{"role":"assistant"},"finish_reason":null}]}"""
        assertNull(parse(line))
    }

    @Test
    fun `malformed json is ignored rather than throwing`() {
        assertNull(parse("data: {not json at all"))
        assertNull(parse("data: 42"))
    }

    @Test
    fun `empty content delta is preserved as empty text`() {
        val line = """data: {"choices":[{"delta":{"content":""},"finish_reason":null}]}"""
        val result = parse(line) as SseLine.Delta
        assertEquals("", result.text)
    }

    @Test
    fun `unicode content survives`() {
        val line = """data: {"choices":[{"delta":{"content":"नमस्ते — ठीक हो?"},"finish_reason":null}]}"""
        val result = parse(line) as SseLine.Delta
        assertEquals("नमस्ते — ठीक हो?", result.text)
    }
}
