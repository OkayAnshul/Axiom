package com.cosmiclaboratory.axiom.data.ai.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Gemini's generateContent shape. It differs from the OpenAI-compatible one in
 * three ways that matter: the system prompt is a separate `systemInstruction`
 * rather than a message with role "system", the assistant role is called
 * "model", and content is a list of parts rather than a string.
 */
@Serializable
internal data class GeminiRequest(
    val contents: List<GeminiContent>,
    @SerialName("systemInstruction") val systemInstruction: GeminiContent? = null,
    @SerialName("generationConfig") val generationConfig: GeminiGenerationConfig? = null
)

@Serializable
internal data class GeminiContent(
    val role: String? = null,
    val parts: List<GeminiPart> = emptyList()
)

@Serializable
internal data class GeminiPart(val text: String = "")

@Serializable
internal data class GeminiGenerationConfig(
    val temperature: Float = 0.7f,
    @SerialName("maxOutputTokens") val maxOutputTokens: Int = 500,
    /** "application/json" puts the model into strict JSON mode. */
    @SerialName("responseMimeType") val responseMimeType: String? = null
)

@Serializable
internal data class GeminiResponse(
    val candidates: List<GeminiCandidate> = emptyList(),
    @SerialName("usageMetadata") val usageMetadata: GeminiUsage? = null,
    @SerialName("modelVersion") val modelVersion: String? = null
) {
    /** Gemini splits a reply across parts; callers want one string. */
    fun text(): String = candidates.firstOrNull()
        ?.content?.parts
        ?.joinToString("") { it.text }
        .orEmpty()
}

@Serializable
internal data class GeminiCandidate(
    val content: GeminiContent = GeminiContent(),
    @SerialName("finishReason") val finishReason: String? = null
)

@Serializable
internal data class GeminiUsage(
    @SerialName("totalTokenCount") val totalTokenCount: Int = 0
)
