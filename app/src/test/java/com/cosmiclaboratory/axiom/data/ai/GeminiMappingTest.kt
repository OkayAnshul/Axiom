package com.cosmiclaboratory.axiom.data.ai

import com.cosmiclaboratory.axiom.data.ai.dto.GeminiResponse
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Gemini's wire format differs from the OpenAI-compatible one in ways that are
 * easy to get subtly wrong: the assistant role is "model", and a reply arrives
 * split across parts that have to be rejoined.
 */
class GeminiMappingTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Test
    fun `assistant maps to model and everything else to user`() {
        assertEquals("model", geminiRole("assistant"))
        assertEquals("model", geminiRole("model"))
        assertEquals("user", geminiRole("user"))
        assertEquals("user", geminiRole("system"))
        assertEquals("model", geminiRole("ASSISTANT"))
    }

    @Test
    fun `a reply split across parts is rejoined in order`() {
        val body = """
            {"candidates":[{"content":{"role":"model","parts":[
              {"text":"That sounds "},{"text":"like a long day."}]}}],
             "usageMetadata":{"totalTokenCount":42}}
        """.trimIndent()
        val response = json.decodeFromString(GeminiResponse.serializer(), body)
        assertEquals("That sounds like a long day.", response.text())
        assertEquals(42, response.usageMetadata?.totalTokenCount)
    }

    @Test
    fun `an empty candidate list yields empty text rather than throwing`() {
        val response = json.decodeFromString(GeminiResponse.serializer(), """{"candidates":[]}""")
        assertEquals("", response.text())
    }

    @Test
    fun `unknown fields do not break parsing`() {
        val body = """
            {"candidates":[{"content":{"parts":[{"text":"ok"}]},"safetyRatings":[{"x":1}]},
             {"content":{"parts":[{"text":"second"}]}}],"somethingNew":true}
        """.trimIndent()
        assertEquals("ok", json.decodeFromString(GeminiResponse.serializer(), body).text())
    }

    // ---- streaming ----------------------------------------------------------

    @Test
    fun `a streamed data line yields its text`() {
        val line = """data: {"candidates":[{"content":{"parts":[{"text":"Hello"}]}}]}"""
        assertEquals("Hello", parseGeminiSseLine(line, json))
    }

    @Test
    fun `non-data lines, blanks and sentinels are ignored`() {
        assertNull(parseGeminiSseLine("", json))
        assertNull(parseGeminiSseLine("data:", json))
        assertNull(parseGeminiSseLine("data: [DONE]", json))
        assertNull(parseGeminiSseLine(": keep-alive", json))
        assertNull(parseGeminiSseLine("event: message", json))
    }

    @Test
    fun `malformed json is ignored rather than throwing`() {
        assertNull(parseGeminiSseLine("data: {not json", json))
    }

    @Test
    fun `a chunk with no text produces nothing`() {
        assertNull(parseGeminiSseLine("""data: {"candidates":[{"content":{"parts":[]}}]}""", json))
    }

    @Test
    fun `devanagari survives the wire`() {
        val line = """data: {"candidates":[{"content":{"parts":[{"text":"नमस्ते, कैसे हो?"}]}}]}"""
        assertEquals("नमस्ते, कैसे हो?", parseGeminiSseLine(line, json))
    }

    @Test
    fun `both vendors advertise a key page and a hint`() {
        com.cosmiclaboratory.axiom.domain.model.AiVendor.entries.forEach { vendor ->
            assertTrue(vendor.keyUrl.startsWith("https://"))
            assertTrue(vendor.keyHint.isNotBlank())
            assertTrue(vendor.summary.isNotBlank())
        }
    }
}
