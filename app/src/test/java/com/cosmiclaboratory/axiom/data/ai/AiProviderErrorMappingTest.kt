package com.cosmiclaboratory.axiom.data.ai

import com.cosmiclaboratory.axiom.data.security.SecureKeyStore
import com.cosmiclaboratory.axiom.domain.model.AiVendor
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito

/**
 * What each provider makes of an HTTP status.
 *
 * This is the layer that failed. When Groq and Google withdrew the models this
 * app was pinned to, both answered every request with a status the providers
 * misread — Groq's 404 became "network error", so background workers retried a
 * request that could never succeed and Settings deleted the user's valid key and
 * blamed it. The mapping is pure policy over a status code, the cheapest thing
 * in the codebase to test, and it had no test at all.
 *
 * `ktor-client-mock` was already a declared dependency and had never been used.
 *
 * The client here deliberately omits `HttpRequestRetry`, which production
 * installs: nothing below depends on retry behaviour, and its exponential
 * backoff would add tens of seconds to the 429 cases. `expectSuccess = true` is
 * kept, because that is what turns a non-2xx into the exception the mapping
 * reads.
 */
class AiProviderErrorMappingTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    private fun engineReturning(
        status: HttpStatusCode,
        body: String = """{"error":{"message":"x"}}"""
    ): MockEngine = MockEngine { respond(body, status, jsonHeaders) }

    private fun engine(
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData
    ): MockEngine = MockEngine(handler)

    private fun clientOn(engine: MockEngine) = HttpClient(engine) {
        expectSuccess = true
        install(ContentNegotiation) { json(json) }
    }

    /** Every URL the engine was asked for, in order. */
    private val MockEngine.urls: List<String>
        get() = requestHistory.map { it.url.toString() }

    /** A key store that always has a key, so nothing short-circuits to NoKey. */
    private fun keyStore(): SecureKeyStore {
        val mock = Mockito.mock(SecureKeyStore::class.java)
        runBlocking {
            Mockito.`when`(mock.apiKey(AiVendor.GROQ)).thenReturn("gsk_test")
            Mockito.`when`(mock.apiKey(AiVendor.GEMINI)).thenReturn("AIza_test")
        }
        return mock
    }

    private fun groq(engine: MockEngine) =
        GroqAiProvider(clientOn(engine), keyStore(), PersonaPromptBuilder(), json)

    private fun gemini(engine: MockEngine) = GeminiAiProvider(clientOn(engine), keyStore(), json)

    // ---- Groq -----------------------------------------------------------

    /**
     * The regression. Groq answers a withdrawn model with 404
     * `model_decommissioned`; it must not look like a connection problem.
     */
    @Test
    fun `groq 404 is a withdrawn model, not a network failure`() = runBlocking {
        val body = """{"error":{"message":"The model `x` has been decommissioned",""" +
            """"type":"invalid_request_error","code":"model_decommissioned"}}"""
        val result = groq(engineReturning(HttpStatusCode.NotFound, body)).testConnection()

        assertTrue("404 must map to Unsupported, got $result", result is AiResult.Unsupported)
    }

    @Test
    fun `groq 401 blames the key`() = runBlocking {
        val result = groq(engineReturning(HttpStatusCode.Unauthorized)).testConnection()
        assertEquals(AiResult.NoKey, result)
    }

    @Test
    fun `groq 429 is a rate limit`() = runBlocking {
        val result = groq(engineReturning(HttpStatusCode.TooManyRequests)).testConnection()
        assertEquals(AiResult.RateLimited, result)
    }

    @Test
    fun `groq 500 stays a network failure`() = runBlocking {
        val result = groq(engineReturning(HttpStatusCode.InternalServerError)).testConnection()
        assertTrue("5xx is genuinely retryable, got $result", result is AiResult.Network)
    }

    // ---- Gemini ---------------------------------------------------------

    /**
     * Gemini reports a rejected key as 400, not 401 — the reason 400 cannot be
     * mapped to NoKey wholesale. The body is what separates the two.
     */
    @Test
    fun `gemini 400 with API_KEY_INVALID blames the key`() = runBlocking {
        val body = """{"error":{"code":400,"message":"API key not valid. Please pass a valid API key.",""" +
            """"status":"INVALID_ARGUMENT","details":[{"reason":"API_KEY_INVALID"}]}}"""
        val result = gemini(engineReturning(HttpStatusCode.BadRequest, body)).testConnection()

        assertEquals(AiResult.NoKey, result)
    }

    /**
     * The other half of that split, and the one that matters most: a malformed
     * or oversized request must NOT be read as a bad key, because Settings
     * deletes the key it is told is bad.
     */
    @Test
    fun `gemini 400 for anything else does not blame the key`() = runBlocking {
        val body = """{"error":{"code":400,"message":"Request contains an invalid argument.",""" +
            """"status":"INVALID_ARGUMENT"}}"""
        val result = gemini(engineReturning(HttpStatusCode.BadRequest, body)).testConnection()

        assertTrue("must not be NoKey, got $result", result is AiResult.Parse)
    }

    // ---- Gemini quality-tier fallback -----------------------------------

    private val geminiOkBody =
        """{"candidates":[{"content":{"role":"model","parts":[{"text":"hello"}]},""" +
            """"finishReason":"STOP"}],"usageMetadata":{"totalTokenCount":7}}"""

    /**
     * `gemini-3.7-flash` may not be on the free tier this app is built around,
     * and that cannot be checked from here. A 404 on the quality tier has to
     * degrade to something that answers rather than failing the feature.
     */
    @Test
    fun `gemini falls back to the lighter model when the quality tier is gone`() = runBlocking {
        val engine = engine { request ->
            if (request.url.toString().contains(GeminiModels.CHAT)) {
                respond("""{"error":{"code":404,"message":"not found"}}""", HttpStatusCode.NotFound, jsonHeaders)
            } else {
                respond(geminiOkBody, HttpStatusCode.OK, jsonHeaders)
            }
        }

        val result = gemini(engine).chat("sys", listOf("user" to "hi"), 100, GroqModels.CHAT)

        assertTrue("should have recovered, got $result", result is AiResult.Ok)
        assertEquals(GeminiModels.CHAT_FALLBACK, (result as AiResult.Ok).modelName)
        assertEquals("one failed attempt, then one that worked", 2, engine.urls.size)
    }

    /**
     * And it should only pay for that discovery once. A provider that re-probes
     * a withdrawn model on every request wastes a call per message, forever.
     */
    @Test
    fun `the withdrawn quality tier is remembered for the rest of the process`() = runBlocking {
        val engine = engine { request ->
            if (request.url.toString().contains(GeminiModels.CHAT)) {
                respond("""{"error":{"code":404,"message":"not found"}}""", HttpStatusCode.NotFound, jsonHeaders)
            } else {
                respond(geminiOkBody, HttpStatusCode.OK, jsonHeaders)
            }
        }
        val provider = gemini(engine)

        provider.chat("sys", listOf("user" to "one"), 100, GroqModels.CHAT)
        val afterFirst = engine.urls.size
        provider.chat("sys", listOf("user" to "two"), 100, GroqModels.CHAT)

        val secondCall = engine.urls.drop(afterFirst)
        assertEquals("second call must not re-probe the dead model", 1, secondCall.size)
        assertTrue(secondCall.single().contains(GeminiModels.CHAT_FALLBACK))
    }

    /** A bad key is not fixed by a different model — falling back would waste a call. */
    @Test
    fun `gemini does not fall back when the key is the problem`() = runBlocking {
        val body = """{"error":{"code":400,"message":"API key not valid","details":[{"reason":"API_KEY_INVALID"}]}}"""
        val engine = engineReturning(HttpStatusCode.BadRequest, body)

        val result = gemini(engine).chat("sys", listOf("user" to "hi"), 100, GroqModels.CHAT)

        assertEquals(AiResult.NoKey, result)
        assertEquals("must not retry on another model", 1, engine.urls.size)
    }
}
