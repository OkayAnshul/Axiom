package com.cosmiclaboratory.axiom.data.ai

import com.cosmiclaboratory.axiom.data.ai.dto.ChatCompletionRequest
import com.cosmiclaboratory.axiom.data.ai.dto.ChatCompletionResponse
import com.cosmiclaboratory.axiom.data.ai.dto.ChatMessage
import com.cosmiclaboratory.axiom.data.ai.dto.EntrySummaryPayload
import com.cosmiclaboratory.axiom.data.ai.dto.InitiatorPromptsPayload
import com.cosmiclaboratory.axiom.data.ai.dto.ResponseFormat
import com.cosmiclaboratory.axiom.data.ai.dto.WhisperTranscriptionResponse
import com.cosmiclaboratory.axiom.data.security.SecureKeyStore
import com.cosmiclaboratory.axiom.domain.model.AiVendor
import com.cosmiclaboratory.axiom.domain.model.Persona
import com.cosmiclaboratory.axiom.data.ai.dto.ChatCompletionChunk
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.timeout
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
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

@Singleton
class GroqAiProvider @Inject constructor(
    private val client: HttpClient,
    private val keys: SecureKeyStore,
    private val builder: PersonaPromptBuilder,
    private val json: Json
) : AiProvider {

    override suspend fun generateInitiatorPrompts(
        persona: Persona,
        recentSummaries: List<String>,
        memoryLines: List<String>,
        count: Int
    ): AiResult<List<String>> {
        val key = keys.apiKey(AiVendor.GROQ) ?: return AiResult.NoKey
        val userPrompt = PromptTemplates.initiatorPrompts(recentSummaries, memoryLines, count)
        return jsonChat(key, persona, userPrompt) { content ->
            json.decodeFromString(InitiatorPromptsPayload.serializer(), content)
                .prompts.filter { it.isNotBlank() }
        }
    }

    override suspend fun summarizeEntry(
        plainText: String,
        persona: Persona
    ): AiResult<EntrySummary> {
        val key = keys.apiKey(AiVendor.GROQ) ?: return AiResult.NoKey
        val userPrompt = PromptTemplates.summarizeEntry(plainText)
        return jsonChat(key, persona, userPrompt) { content ->
            val p = json.decodeFromString(EntrySummaryPayload.serializer(), content)
            EntrySummary(p.summary, p.followUp, p.themes, p.mood)
        }
    }

    override suspend fun testConnection(): AiResult<Unit> {
        val key = keys.apiKey(AiVendor.GROQ) ?: return AiResult.NoKey
        val request = ChatCompletionRequest(
            model = GroqModels.BACKGROUND,
            messages = listOf(ChatMessage(role = "user", content = "ping")),
            maxTokens = 1,
            responseFormat = null
        )
        return runCatching {
            val response: ChatCompletionResponse = client.post(CHAT_ENDPOINT) {
                contentType(ContentType.Application.Json)
                headers { append(HttpHeaders.Authorization, "Bearer $key") }
                setBody(request)
            }.body()
            if (response.choices.isNotEmpty()) AiResult.Ok(Unit) else AiResult.Parse(IllegalStateException("Empty response"))
        }.getOrElse { e ->
            mapError(e)
        }
    }

    override suspend fun chat(
        systemPrompt: String,
        messages: List<Pair<String, String>>,
        maxTokens: Int,
        model: String
    ): AiResult<String> {
        val key = keys.apiKey(AiVendor.GROQ) ?: return AiResult.NoKey
        val payload = buildList {
            add(ChatMessage(role = "system", content = systemPrompt))
            messages.forEach { (role, content) -> add(ChatMessage(role = role, content = content)) }
        }
        val request = ChatCompletionRequest(
            model = model,
            messages = payload,
            maxTokens = maxTokens,
            responseFormat = null
        )
        return runCatching {
            val response: ChatCompletionResponse = client.post(CHAT_ENDPOINT) {
                contentType(ContentType.Application.Json)
                headers { append(HttpHeaders.Authorization, "Bearer $key") }
                setBody(request)
            }.body()
            val content = response.choices.firstOrNull()?.message?.content?.trim()
                ?: return AiResult.Parse(IllegalStateException("Empty Groq response"))
            AiResult.Ok(content, response.usage?.totalTokens ?: 0, response.model ?: model)
        }.getOrElse { mapError(it) }
    }

    override fun chatStream(
        systemPrompt: String,
        messages: List<Pair<String, String>>,
        maxTokens: Int,
        model: String
    ): Flow<ChatStreamEvent> = flow {
        val key = keys.apiKey(AiVendor.GROQ)
        if (key == null) {
            emit(ChatStreamEvent.Failed(AiResult.NoKey, ""))
            return@flow
        }
        val payload = buildList {
            add(ChatMessage(role = "system", content = systemPrompt))
            messages.forEach { (role, content) -> add(ChatMessage(role = role, content = content)) }
        }
        val request = ChatCompletionRequest(
            model = model,
            messages = payload,
            maxTokens = maxTokens,
            responseFormat = null,
            stream = true
        )
        val accumulated = StringBuilder()
        var modelName = model
        try {
            client.preparePost(CHAT_ENDPOINT) {
                contentType(ContentType.Application.Json)
                headers { append(HttpHeaders.Authorization, "Bearer $key") }
                setBody(request)
                // The client-wide 30s budget is sized for one-shot calls; a
                // stream stays open for its whole generation.
                timeout { requestTimeoutMillis = STREAM_TIMEOUT_MS }
            }.execute { response ->
                val channel = response.bodyAsChannel()
                while (true) {
                    val line = channel.readUTF8Line() ?: break
                    when (val parsed = parseGroqSseLine(line, json)) {
                        is SseLine.Delta -> {
                            parsed.model?.let { modelName = it }
                            if (parsed.text.isNotEmpty()) {
                                accumulated.append(parsed.text)
                                emit(ChatStreamEvent.Delta(parsed.text))
                            }
                        }
                        SseLine.Done -> return@execute
                        null -> Unit
                    }
                }
            }
            emit(ChatStreamEvent.Done(accumulated.toString(), 0, modelName))
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
    ): AiResult<String> {
        val key = keys.apiKey(AiVendor.GROQ) ?: return AiResult.NoKey
        val request = ChatCompletionRequest(
            model = model,
            messages = listOf(
                ChatMessage(role = "system", content = systemPrompt),
                ChatMessage(role = "user", content = userPrompt)
            ),
            maxTokens = maxTokens,
            responseFormat = ResponseFormat()
        )
        return runCatching {
            val response: ChatCompletionResponse = client.post(CHAT_ENDPOINT) {
                contentType(ContentType.Application.Json)
                headers { append(HttpHeaders.Authorization, "Bearer $key") }
                setBody(request)
            }.body()
            val content = response.choices.firstOrNull()?.message?.content?.trim()
                ?: return AiResult.Parse(IllegalStateException("Empty Groq response"))
            AiResult.Ok(content, response.usage?.totalTokens ?: 0, response.model ?: model)
        }.getOrElse { mapError(it) }
    }

    override suspend fun transcribeAudio(audioFile: File, language: String?): AiResult<String> {
        val key = keys.apiKey(AiVendor.GROQ) ?: return AiResult.NoKey
        if (!audioFile.exists() || audioFile.length() == 0L) {
            return AiResult.Parse(IllegalArgumentException("Empty audio file"))
        }
        return runCatching {
            val response: WhisperTranscriptionResponse = client.post(TRANSCRIPTION_ENDPOINT) {
                headers { append(HttpHeaders.Authorization, "Bearer $key") }
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append(
                                key = "file",
                                value = audioFile.readBytes(),
                                headers = Headers.build {
                                    append(HttpHeaders.ContentType, "audio/m4a")
                                    append(HttpHeaders.ContentDisposition, "filename=\"${audioFile.name}\"")
                                }
                            )
                            append("model", GroqModels.WHISPER)
                            append("response_format", "verbose_json")
                            language?.let { append("language", it) }
                            append("temperature", "0")
                        }
                    )
                )
            }.body()
            AiResult.Ok(response.text.trim(), 0, GroqModels.WHISPER)
        }.getOrElse { mapError(it) }
    }

    private suspend fun <T> jsonChat(
        apiKey: String,
        persona: Persona,
        userPrompt: String,
        parse: (String) -> T
    ): AiResult<T> {
        val request = ChatCompletionRequest(
            model = GroqModels.BACKGROUND,
            messages = builder.buildMessages(persona, userPrompt),
            responseFormat = ResponseFormat()
        )
        return runCatching {
            val response: ChatCompletionResponse = client.post(CHAT_ENDPOINT) {
                contentType(ContentType.Application.Json)
                headers { append(HttpHeaders.Authorization, "Bearer $apiKey") }
                setBody(request)
            }.body()
            val content = response.choices.firstOrNull()?.message?.content?.trim()
                ?: return AiResult.Parse(IllegalStateException("Empty Groq response"))
            try {
                AiResult.Ok(parse(content), response.usage?.totalTokens ?: 0, response.model ?: GroqModels.BACKGROUND)
            } catch (e: Throwable) {
                AiResult.Parse(e)
            }
        }.getOrElse { mapError(it) }
    }

    private fun mapError(e: Throwable): AiResult<Nothing> = when (e) {
        is ResponseException -> when (e.response.status) {
            HttpStatusCode.TooManyRequests -> AiResult.RateLimited
            HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden -> AiResult.NoKey
            else -> AiResult.Network(e)
        }
        else -> AiResult.Network(e)
    }

    private companion object {
        const val CHAT_ENDPOINT = "https://api.groq.com/openai/v1/chat/completions"
        const val TRANSCRIPTION_ENDPOINT = "https://api.groq.com/openai/v1/audio/transcriptions"
        const val STREAM_TIMEOUT_MS = 120_000L
    }
}

/** One parsed server-sent-event line from a Groq streaming response. */
internal sealed interface SseLine {
    data class Delta(val text: String, val model: String?) : SseLine
    data object Done : SseLine
}

/**
 * Parses a single line of a Groq SSE stream. Pure so it can be unit-tested
 * without Ktor. Returns null for anything that is not a data line or that
 * carries no delta (keep-alive blanks, role-only first chunk, finish chunk).
 */
internal fun parseGroqSseLine(line: String, json: Json): SseLine? {
    val trimmed = line.trim()
    if (!trimmed.startsWith("data:")) return null
    val payload = trimmed.removePrefix("data:").trim()
    if (payload.isEmpty()) return null
    if (payload == "[DONE]") return SseLine.Done
    val chunk = runCatching { json.decodeFromString(ChatCompletionChunk.serializer(), payload) }
        .getOrNull() ?: return null
    val content = chunk.choices.firstOrNull()?.delta?.content ?: return null
    return SseLine.Delta(content, chunk.model)
}
