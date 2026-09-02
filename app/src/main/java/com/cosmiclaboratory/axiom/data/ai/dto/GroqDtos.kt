package com.cosmiclaboratory.axiom.data.ai.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Float = 0.7f,
    @SerialName("max_tokens") val maxTokens: Int = 400,
    @SerialName("response_format") val responseFormat: ResponseFormat? = ResponseFormat(),
    val stream: Boolean = false,
    /**
     * Only meaningful on streams: asks for a final chunk carrying token counts.
     * Without it the most expensive call in the app — the companion reply —
     * reports zero tokens and there is no way to know what anything costs.
     */
    @SerialName("stream_options") val streamOptions: StreamOptions? = null,
    /**
     * Only meaningful on reasoning models, which both current Groq models are.
     * Null for anything that is not one — the field is rejected rather than
     * ignored by models that do not reason.
     */
    @SerialName("reasoning_effort") val reasoningEffort: String? = null
)

@Serializable
internal data class StreamOptions(
    @SerialName("include_usage") val includeUsage: Boolean = true
)

@Serializable
internal data class ChatMessage(
    val role: String,
    val content: String
)

@Serializable
internal data class ResponseFormat(
    val type: String = "json_object"
)

@Serializable
internal data class ChatCompletionResponse(
    val id: String? = null,
    val model: String? = null,
    val choices: List<Choice> = emptyList(),
    val usage: Usage? = null
)

@Serializable
internal data class Choice(
    val index: Int = 0,
    val message: ChatMessage,
    @SerialName("finish_reason") val finishReason: String? = null
)

@Serializable
internal data class Usage(
    @SerialName("prompt_tokens") val promptTokens: Int = 0,
    @SerialName("completion_tokens") val completionTokens: Int = 0,
    @SerialName("total_tokens") val totalTokens: Int = 0
)

@Serializable
internal data class InitiatorPromptsPayload(
    val prompts: List<String> = emptyList()
)

@Serializable
internal data class EntrySummaryPayload(
    val summary: String = "",
    @SerialName("follow_up") val followUp: String = "",
    val themes: List<String> = emptyList(),
    val mood: String = ""
)

/**
 * The single response that replaced three calls. `memory` is deliberately a
 * raw JSON element rather than a typed shape: [com.cosmiclaboratory.axiom.data.companion.MemoryExtractor]
 * already owns parsing and validating that structure, and duplicating it here
 * would give two places to disagree about what a memory looks like.
 */
@Serializable
internal data class SessionDigestPayload(
    val entry: String = "",
    val mood: String = "",
    val summary: String = "",
    val memory: kotlinx.serialization.json.JsonElement? = null
)

/** Entry insight, now carrying the title so one call does the whole job. */
@Serializable
internal data class EntryInsightPayload(
    val summary: String = "",
    @SerialName("follow_up") val followUp: String = "",
    val themes: List<String> = emptyList(),
    val mood: String = "",
    val title: String = "",
    val memory: kotlinx.serialization.json.JsonElement? = null
)

@Serializable
internal data class WhisperTranscriptionResponse(
    val text: String = "",
    val language: String? = null,
    val duration: Double? = null
)

@Serializable
internal data class ChatCompletionChunk(
    val model: String? = null,
    val choices: List<ChunkChoice> = emptyList(),
    /** Present only on the final chunk, and only when `stream_options` asked. */
    val usage: Usage? = null
)

@Serializable
internal data class ChunkChoice(
    val delta: ChunkDelta = ChunkDelta(),
    @SerialName("finish_reason") val finishReason: String? = null
)

@Serializable
internal data class ChunkDelta(
    val content: String? = null
)
