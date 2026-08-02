package com.cosmiclaboratory.axiom.data.ai

import com.cosmiclaboratory.axiom.data.ai.dto.ChatCompletionRequest
import com.cosmiclaboratory.axiom.data.ai.dto.ChatCompletionResponse
import com.cosmiclaboratory.axiom.data.ai.dto.ChatMessage
import com.cosmiclaboratory.axiom.data.ai.dto.EntrySummaryPayload
import com.cosmiclaboratory.axiom.data.ai.dto.InitiatorPromptsPayload
import com.cosmiclaboratory.axiom.data.ai.dto.ResponseFormat
import com.cosmiclaboratory.axiom.data.ai.dto.WhisperTranscriptionResponse
import com.cosmiclaboratory.axiom.data.preferences.UserPreferences
import com.cosmiclaboratory.axiom.domain.model.Persona
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GroqAiProvider @Inject constructor(
    private val client: HttpClient,
    private val prefs: UserPreferences,
    private val builder: PersonaPromptBuilder,
    private val json: Json
) : AiProvider {

    override suspend fun generateInitiatorPrompts(
        persona: Persona,
        recentSummaries: List<String>,
        count: Int
    ): AiResult<List<String>> {
        val key = prefs.groqApiKey() ?: return AiResult.NoKey
        val userPrompt = PromptTemplates.initiatorPrompts(recentSummaries, count)
        return jsonChat(key, persona, userPrompt) { content ->
            json.decodeFromString(InitiatorPromptsPayload.serializer(), content)
                .prompts.filter { it.isNotBlank() }
        }
    }

    override suspend fun summarizeEntry(
        plainText: String,
        persona: Persona
    ): AiResult<EntrySummary> {
        val key = prefs.groqApiKey() ?: return AiResult.NoKey
        val userPrompt = PromptTemplates.summarizeEntry(plainText)
        return jsonChat(key, persona, userPrompt) { content ->
            val p = json.decodeFromString(EntrySummaryPayload.serializer(), content)
            EntrySummary(p.summary, p.followUp, p.themes, p.mood)
        }
    }

    override suspend fun testConnection(): AiResult<Unit> {
        val key = prefs.groqApiKey() ?: return AiResult.NoKey
        val request = ChatCompletionRequest(
            model = TEXT_MODEL,
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
        maxTokens: Int
    ): AiResult<String> {
        val key = prefs.groqApiKey() ?: return AiResult.NoKey
        val payload = buildList {
            add(ChatMessage(role = "system", content = systemPrompt))
            messages.forEach { (role, content) -> add(ChatMessage(role = role, content = content)) }
        }
        val request = ChatCompletionRequest(
            model = TEXT_MODEL,
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
            AiResult.Ok(content, response.usage?.totalTokens ?: 0, response.model ?: TEXT_MODEL)
        }.getOrElse { mapError(it) }
    }

    override suspend fun transcribeAudio(audioFile: File, language: String?): AiResult<String> {
        val key = prefs.groqApiKey() ?: return AiResult.NoKey
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
                            append("model", WHISPER_MODEL)
                            append("response_format", "verbose_json")
                            language?.let { append("language", it) }
                            append("temperature", "0")
                        }
                    )
                )
            }.body()
            AiResult.Ok(response.text.trim(), 0, WHISPER_MODEL)
        }.getOrElse { mapError(it) }
    }

    private suspend fun <T> jsonChat(
        apiKey: String,
        persona: Persona,
        userPrompt: String,
        parse: (String) -> T
    ): AiResult<T> {
        val request = ChatCompletionRequest(
            model = TEXT_MODEL,
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
                AiResult.Ok(parse(content), response.usage?.totalTokens ?: 0, response.model ?: TEXT_MODEL)
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
        const val TEXT_MODEL = "llama-3.1-8b-instant"
        const val WHISPER_MODEL = "whisper-large-v3-turbo"
    }
}
