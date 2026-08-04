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
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** Model routing for Gemini, mirroring the split used for Groq. */
object GeminiModels {
    const val CHAT = "gemini-2.0-flash"
    const val BACKGROUND = "gemini-2.0-flash-lite"
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
        val request = GeminiRequest(
            contents = messages.map { (role, content) ->
                GeminiContent(geminiRole(role), listOf(GeminiPart(content)))
            },
            systemInstruction = systemPrompt.takeIf { it.isNotBlank() }
                ?.let { GeminiContent(parts = listOf(GeminiPart(it))) },
            generationConfig = GeminiGenerationConfig(maxOutputTokens = maxTokens)
        )
        return runCatching {
            val response: GeminiResponse = client.post(endpoint(mapModel(model), "generateContent", key)) {
                contentType(ContentType.Application.Json)
                setBody(request)
            }.body()
            val text = response.text().trim()
            if (text.isEmpty()) {
                AiResult.Parse(IllegalStateException("Empty Gemini response"))
            } else {
                AiResult.Ok(text, response.usageMetadata?.totalTokenCount ?: 0, mapModel(model))
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
        val request = GeminiRequest(
            contents = messages.map { (role, content) ->
                GeminiContent(geminiRole(role), listOf(GeminiPart(content)))
            },
            systemInstruction = systemPrompt.takeIf { it.isNotBlank() }
                ?.let { GeminiContent(parts = listOf(GeminiPart(it))) },
            generationConfig = GeminiGenerationConfig(maxOutputTokens = maxTokens)
        )
        val accumulated = StringBuilder()
        try {
            client.preparePost(endpoint(mapModel(model), "streamGenerateContent", key, sse = true)) {
                contentType(ContentType.Application.Json)
                setBody(request)
                timeout { requestTimeoutMillis = STREAM_TIMEOUT_MS }
            }.execute { response ->
                val channel = response.bodyAsChannel()
                while (true) {
                    val line = channel.readUTF8Line() ?: break
                    val delta = parseGeminiSseLine(line, json) ?: continue
                    if (delta.isNotEmpty()) {
                        accumulated.append(delta)
                        emit(ChatStreamEvent.Delta(delta))
                    }
                }
            }
            emit(ChatStreamEvent.Done(accumulated.toString(), 0, mapModel(model)))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            emit(ChatStreamEvent.Failed(mapError(e), accumulated.toString()))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun completeJson(
        systemPrompt: String,
        userPrompt: String,
        maxTokens: Int,
        model: String
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

    private fun endpoint(model: String, method: String, key: String, sse: Boolean = false): String {
        val query = if (sse) "?alt=sse&key=$key" else "?key=$key"
        return "$BASE_URL/$model:$method$query"
    }

    private fun mapError(e: Throwable): AiResult<Nothing> = when (e) {
        is ResponseException -> when (e.response.status) {
            HttpStatusCode.TooManyRequests -> AiResult.RateLimited
            HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden, HttpStatusCode.BadRequest ->
                AiResult.NoKey
            else -> AiResult.Network(e)
        }
        else -> AiResult.Network(e)
    }

    private companion object {
        const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
        const val STREAM_TIMEOUT_MS = 120_000L
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
internal fun parseGeminiSseLine(line: String, json: Json): String? {
    val trimmed = line.trim()
    if (!trimmed.startsWith("data:")) return null
    val payload = trimmed.removePrefix("data:").trim()
    if (payload.isEmpty() || payload == "[DONE]") return null
    val chunk = runCatching { json.decodeFromString(GeminiResponse.serializer(), payload) }
        .getOrNull() ?: return null
    return chunk.text().takeIf { it.isNotEmpty() }
}
