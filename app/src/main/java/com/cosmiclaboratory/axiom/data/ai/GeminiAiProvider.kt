package com.cosmiclaboratory.axiom.data.ai

import com.cosmiclaboratory.axiom.data.ai.dto.EntrySummaryPayload
import com.cosmiclaboratory.axiom.data.ai.dto.GeminiContent
import com.cosmiclaboratory.axiom.data.ai.dto.GeminiGenerationConfig
import com.cosmiclaboratory.axiom.data.ai.dto.GeminiPart
import com.cosmiclaboratory.axiom.data.ai.dto.GeminiRequest
import com.cosmiclaboratory.axiom.data.ai.dto.GeminiResponse
import com.cosmiclaboratory.axiom.data.ai.dto.InitiatorPromptsPayload
import com.cosmiclaboratory.axiom.data.security.SecureKeyStore
import com.cosmiclaboratory.axiom.domain.model.AiVendor
import com.cosmiclaboratory.axiom.domain.model.Persona
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.retry
import io.ktor.client.plugins.timeout
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Model routing for Gemini, mirroring the split used for Groq.
 *
 * These were `gemini-2.0-flash` and `gemini-2.0-flash-lite` until Google shut
 * the whole 2.0 Flash family down on 1 June 2026. Same failure as the Groq
 * side, arriving two months earlier and even more quietly, because Gemini
 * reports a dead model the same way it reports everything else — a 400.
 */
object GeminiModels {
    const val CHAT = "gemini-3.7-flash"
    const val BACKGROUND = "gemini-3.5-flash-lite"

    /**
     * Where the quality tier drops to when it is unavailable.
     *
     * The same string as [BACKGROUND] today, but named separately because the
     * two are chosen for different reasons: one is "cheap enough to run on
     * every entry", this one is "the best thing still answering". Retuning
     * either must not silently move the other.
     */
    const val CHAT_FALLBACK = BACKGROUND
}

/**
 * Gemini via Google AI Studio's free tier.
 *
 * Offered alongside Groq for two reasons: it removes a single-vendor
 * dependency for a feature the app's whole character rests on, and it is
 * markedly better at Hindi and Hinglish — which for this user base is not a
 * nice-to-have.
 *
 * It cannot transcribe audio here. Gemini does accept audio, but as
 * base64-inlined bytes with its own size limits rather than a multipart upload,
 * and voice notes are the one place where Whisper is both simpler and better.
 * [RoutingAiProvider] keeps transcription on Groq for that reason.
 */
@Singleton
class GeminiAiProvider @Inject constructor(
    private val client: HttpClient,
    private val keys: SecureKeyStore,
    private val json: Json
) : AiProvider {

    override suspend fun generateInitiatorPrompts(
        persona: Persona,
        recentSummaries: List<String>,
        memoryLines: List<String>,
        count: Int
    ): AiResult<List<String>> = jsonCall(
        systemPrompt = persona.systemPromptFragment,
        userPrompt = PromptTemplates.initiatorPrompts(recentSummaries, memoryLines, count)
    ) { content ->
        json.decodeFromString(InitiatorPromptsPayload.serializer(), content)
            .prompts.filter { it.isNotBlank() }
    }

    override suspend fun summarizeEntry(
        plainText: String,
        persona: Persona
    ): AiResult<EntrySummary> = jsonCall(
        systemPrompt = persona.systemPromptFragment,
        userPrompt = PromptTemplates.summarizeEntry(plainText)
    ) { content ->
        val p = json.decodeFromString(EntrySummaryPayload.serializer(), content)
        EntrySummary(p.summary, p.followUp, p.themes, p.mood)
    }

    override suspend fun testConnection(): AiResult<Unit> {
        val key = keys.apiKey(AiVendor.GEMINI) ?: return AiResult.NoKey
        val request = GeminiRequest(
            contents = listOf(GeminiContent("user", listOf(GeminiPart("ping")))),
            generationConfig = GeminiGenerationConfig(maxOutputTokens = 1)
        )
        return runCatching {
            val response: GeminiResponse = client.post(endpoint(GeminiModels.BACKGROUND, "generateContent", key)) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
            if (response.candidates.isNotEmpty() || response.text().isNotEmpty()) {
                AiResult.Ok(Unit)
            } else {
                AiResult.Parse(IllegalStateException("Empty Gemini response"))
            }
        }.getOrElse { mapError(it) }
    }

    override suspend fun chat(
        systemPrompt: String,
        messages: List<Pair<String, String>>,
        maxTokens: Int,
        model: String
    ): AiResult<String> {
        val key = keys.apiKey(AiVendor.GEMINI) ?: return AiResult.NoKey
        return withChatFallback(model) { resolved ->
            chatOnce(key, systemPrompt, messages, maxTokens, resolved)
        }
    }

    private suspend fun chatOnce(
        key: String,
        systemPrompt: String,
        messages: List<Pair<String, String>>,
        maxTokens: Int,
        model: String
    ): AiResult<String> {
        val request = GeminiRequest(
            contents = messages.map { (role, content) ->
                GeminiContent(geminiRole(role), listOf(GeminiPart(content)))
            },
            systemInstruction = systemPrompt.takeIf { it.isNotBlank() }
                ?.let { GeminiContent(parts = listOf(GeminiPart(it))) },
            generationConfig = GeminiGenerationConfig(maxOutputTokens = maxTokens)
        )
        return runCatching {
            val response: GeminiResponse = client.post(endpoint(model, "generateContent", key)) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
            val text = response.text().trim()
            if (text.isEmpty()) {
                AiResult.Parse(IllegalStateException("Empty Gemini response"))
            } else {
                AiResult.Ok(text, response.usageMetadata?.totalTokenCount ?: 0, model)
            }
        }.getOrElse { mapError(it) }
    }

    override fun chatStream(
        systemPrompt: String,
        messages: List<Pair<String, String>>,
        maxTokens: Int,
        model: String
    ): Flow<ChatStreamEvent> = flow {
        val key = keys.apiKey(AiVendor.GEMINI)
        if (key == null) {
            emit(ChatStreamEvent.Failed(AiResult.NoKey, ""))
            return@flow
        }
        val first = resolveChatModel(model)
        var terminal = streamOnce(key, systemPrompt, messages, maxTokens, first)
        /*
         * Falling back is only safe while the reader has seen nothing. A failure
         * that arrives after the first delta means someone is already watching
         * the reply, and starting a second generation underneath them is exactly
         * the hazard `noRetry()` above exists to prevent — so a partially
         * delivered stream keeps its failure and its text.
         */
        if (first != GeminiModels.CHAT_FALLBACK && terminal.isRecoverableBeforeFirstDelta()) {
            if ((terminal as ChatStreamEvent.Failed).error is AiResult.Unsupported) {
                chatTierWithdrawn = true
            }
            terminal = streamOnce(key, systemPrompt, messages, maxTokens, GeminiModels.CHAT_FALLBACK)
        }
        emit(terminal)
    }.flowOn(Dispatchers.IO)

    /**
     * Runs one streaming attempt, emitting deltas as they arrive and *returning*
     * the terminal event rather than emitting it.
     *
     * Returning it is what makes the fallback above possible: the caller has to
     * see how the attempt ended before deciding whether a second one is allowed,
     * and [ChatStreamEvent] promises exactly one Done or Failed per flow.
     */
    private suspend fun FlowCollector<ChatStreamEvent>.streamOnce(
        key: String,
        systemPrompt: String,
        messages: List<Pair<String, String>>,
        maxTokens: Int,
        model: String
    ): ChatStreamEvent {
        val request = GeminiRequest(
            contents = messages.map { (role, content) ->
                GeminiContent(geminiRole(role), listOf(GeminiPart(content)))
            },
            systemInstruction = systemPrompt.takeIf { it.isNotBlank() }
                ?.let { GeminiContent(parts = listOf(GeminiPart(it))) },
            generationConfig = GeminiGenerationConfig(maxOutputTokens = maxTokens)
        )
        val accumulated = StringBuilder()
        var finishReason: String? = null
        var totalTokens = 0
        return try {
            client.preparePost(endpoint(model, "streamGenerateContent", key, sse = true)) {
                contentType(ContentType.Application.Json)
                setBody(request)
                timeout { requestTimeoutMillis = STREAM_TIMEOUT_MS }
                // See the note in GroqAiProvider.chatStream: a stream must
                // never be restarted underneath a reader.
                retry { noRetry() }
            }.execute { response ->
                val channel = response.bodyAsChannel()
                while (true) {
                    val line = channel.readUTF8Line() ?: break
                    val chunk = decodeGeminiSseChunk(line, json) ?: continue
                    chunk.candidates.firstOrNull()?.finishReason?.let { finishReason = it }
                    chunk.usageMetadata?.totalTokenCount?.let { if (it > 0) totalTokens = it }
                    val delta = chunk.text()
                    if (delta.isNotEmpty()) {
                        accumulated.append(delta)
                        emit(ChatStreamEvent.Delta(delta))
                    }
                }
            }
            ChatStreamEvent.Done(
                fullText = accumulated.toString(),
                tokensUsed = totalTokens,
                modelName = model,
                truncated = finishReason == FINISH_MAX_TOKENS
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            ChatStreamEvent.Failed(mapError(e), accumulated.toString())
        }
    }

    override suspend fun completeJson(
        systemPrompt: String,
        userPrompt: String,
        maxTokens: Int,
        model: String,
        temperature: Float
    ): AiResult<String> = jsonCall(systemPrompt, userPrompt, maxTokens) { it }

    /**
     * Not supported. Gemini takes audio as inlined base64 with its own limits,
     * where Groq's Whisper endpoint is a plain multipart upload that already
     * handles Hinglish better than anything else available free.
     */
    override suspend fun transcribeAudio(audioFile: File, language: String?): AiResult<String> =
        AiResult.NoKey

    private suspend fun <T> jsonCall(
        systemPrompt: String,
        userPrompt: String,
        maxTokens: Int = 600,
        parse: (String) -> T
    ): AiResult<T> {
        val key = keys.apiKey(AiVendor.GEMINI) ?: return AiResult.NoKey
        val request = GeminiRequest(
            contents = listOf(GeminiContent("user", listOf(GeminiPart(userPrompt)))),
            systemInstruction = systemPrompt.takeIf { it.isNotBlank() }
                ?.let { GeminiContent(parts = listOf(GeminiPart(it))) },
            generationConfig = GeminiGenerationConfig(
                maxOutputTokens = maxTokens,
                responseMimeType = "application/json"
            )
        )
        return runCatching {
            val response: GeminiResponse = client.post(
                endpoint(GeminiModels.BACKGROUND, "generateContent", key)
            ) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
            val content = response.text().trim()
            if (content.isEmpty()) return AiResult.Parse(IllegalStateException("Empty Gemini response"))
            try {
                AiResult.Ok(parse(content), response.usageMetadata?.totalTokenCount ?: 0, GeminiModels.BACKGROUND)
            } catch (e: Throwable) {
                AiResult.Parse(e)
            }
        }.getOrElse { mapError(it) }
    }

    /** The caller speaks in Groq model names; translate to the nearest Gemini tier. */
    private fun mapModel(model: String): String =
        if (model == GroqModels.BACKGROUND) GeminiModels.BACKGROUND else GeminiModels.CHAT

    /**
     * Set once the quality model has answered 404 in this process.
     *
     * Google retires model IDs on a schedule a shipped build cannot see, and
     * this app's whole premise is a free tier whose exact contents are behind a
     * login. So the first request to a withdrawn model discovers it, and every
     * later one skips straight to what still answers — one wasted call per
     * process rather than one per request.
     *
     * Deliberately not persisted. A model coming back, or the user moving to a
     * paid tier, should cost a restart, not a reinstall.
     */
    @Volatile
    private var chatTierWithdrawn = false

    private fun resolveChatModel(requested: String): String {
        val tier = mapModel(requested)
        return if (tier == GeminiModels.CHAT && chatTierWithdrawn) GeminiModels.CHAT_FALLBACK else tier
    }

    /**
     * Runs [call] on the resolved tier, dropping to [GeminiModels.CHAT_FALLBACK]
     * once if the quality model will not serve this request.
     *
     * The two recoverable failures are treated differently on purpose.
     * [AiResult.Unsupported] is permanent — the model is gone — so it is
     * remembered. [AiResult.RateLimited] is a busy minute; falling back for this
     * one call is worth it, but remembering it would quietly demote a user to
     * the weaker model for the rest of the session. Everything else is either
     * about the key or about the request, and a different model does not fix it.
     */
    private suspend fun <T> withChatFallback(
        requested: String,
        call: suspend (String) -> AiResult<T>
    ): AiResult<T> {
        val model = resolveChatModel(requested)
        val first = call(model)
        if (model == GeminiModels.CHAT_FALLBACK) return first
        return when (first) {
            is AiResult.Unsupported -> {
                chatTierWithdrawn = true
                call(GeminiModels.CHAT_FALLBACK)
            }
            // Keep the original 429 if the fallback fails too, so the UI still
            // shows a rate limit rather than whatever the second attempt hit.
            AiResult.RateLimited -> call(GeminiModels.CHAT_FALLBACK).takeIf { it is AiResult.Ok } ?: first
            else -> first
        }
    }

    /**
     * Whether a finished stream may be retried on another model: it failed for a
     * reason a different model could fix, and the reader has seen nothing yet.
     */
    private fun ChatStreamEvent.isRecoverableBeforeFirstDelta(): Boolean {
        if (this !is ChatStreamEvent.Failed || partialText.isNotEmpty()) return false
        return error is AiResult.Unsupported || error is AiResult.RateLimited
    }

    private fun endpoint(model: String, method: String, key: String, sse: Boolean = false): String {
        val query = if (sse) "?alt=sse&key=$key" else "?key=$key"
        return "$BASE_URL/$model:$method$query"
    }

    /**
     * 400 is deliberately NOT mapped to [AiResult.NoKey].
     *
     * Gemini returns 400 for a malformed request, an oversized prompt, or an
     * unsupported parameter — none of which mean the key is bad. Because
     * `SettingsViewModel.saveAndTestKey` rolls the key back on any non-Ok
     * result, mapping 400 here meant one bad request silently *deleted a
     * perfectly valid key* and told the user it didn't work. It is a request
     * problem, so it maps to Parse.
     */
    private fun mapError(e: Throwable): AiResult<Nothing> = when (e) {
        is ResponseException -> when (e.response.status) {
            HttpStatusCode.TooManyRequests -> AiResult.RateLimited
            HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden -> AiResult.NoKey
            /* A model Google has retired. Permanent, and not the key's fault. */
            HttpStatusCode.NotFound -> AiResult.Unsupported(e.message ?: "Model unavailable")
            HttpStatusCode.BadRequest -> classifyBadRequest(e)
            else -> AiResult.Network(e)
        }
        else -> AiResult.Network(e)
    }

    /**
     * Gemini says "bad key" and "bad request" with the same status code.
     *
     * Unlike every other provider, an invalid API key here is a 400, not a 401
     * — the reason for the note above about mapping 400 to [AiResult.NoKey]
     * wholesale. But the body distinguishes the two perfectly well: a rejected
     * key carries `API_KEY_INVALID`. Matching on that is narrow enough to be
     * safe, and anything unrecognised still falls through to [AiResult.Parse],
     * so a body Google reworded costs a wrong error message rather than a
     * deleted credential.
     */
    private fun classifyBadRequest(e: ResponseException): AiResult<Nothing> =
        if (e.message?.contains(API_KEY_INVALID, ignoreCase = true) == true) {
            AiResult.NoKey
        } else {
            AiResult.Parse(e)
        }

    private companion object {
        const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
        const val STREAM_TIMEOUT_MS = 120_000L
        /** Gemini's equivalent of OpenAI's `finish_reason: "length"`. */
        const val FINISH_MAX_TOKENS = "MAX_TOKENS"
        /** `error.details[].reason` on a rejected key. See [classifyBadRequest]. */
        const val API_KEY_INVALID = "API_KEY_INVALID"
    }
}

/** Gemini calls the assistant "model"; everything else is a user turn. */
internal fun geminiRole(role: String): String =
    if (role.equals("assistant", ignoreCase = true) || role.equals("model", ignoreCase = true)) {
        "model"
    } else {
        "user"
    }

/**
 * Parses one SSE line from Gemini's streamGenerateContent. Pure, so it can be
 * tested without Ktor. Returns null for anything that is not a data line or
 * that carries no text.
 */
/**
 * Decodes one SSE line into the whole chunk, so a caller can read the text, the
 * finish reason and the usage from a single parse rather than three.
 */
internal fun decodeGeminiSseChunk(line: String, json: Json): GeminiResponse? {
    val trimmed = line.trim()
    if (!trimmed.startsWith("data:")) return null
    val payload = trimmed.removePrefix("data:").trim()
    if (payload.isEmpty() || payload == "[DONE]") return null
    return runCatching { json.decodeFromString(GeminiResponse.serializer(), payload) }.getOrNull()
}

/** Just the text of an SSE line, or null when it carries none. */
internal fun parseGeminiSseLine(line: String, json: Json): String? =
    decodeGeminiSseChunk(line, json)?.text()?.takeIf { it.isNotEmpty() }
