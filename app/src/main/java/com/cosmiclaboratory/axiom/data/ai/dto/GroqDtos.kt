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
    val stream: Boolean = false
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

@Serializable
internal data class WhisperTranscriptionResponse(
    val text: String = "",
    val language: String? = null,
    val duration: Double? = null
)

@Serializable
internal data class ChatCompletionChunk(
    val model: String? = null,
    val choices: List<ChunkChoice> = emptyList()
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
