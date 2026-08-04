package com.cosmiclaboratory.axiom.data.ai

import com.cosmiclaboratory.axiom.domain.model.Persona
import kotlinx.coroutines.flow.Flow
import java.io.File

/**
 * Model routing: the companion conversation runs on the strongest model Groq's
 * free tier offers; everything that runs in the background (summaries, memory
 * extraction, digests) uses the cheap fast model — those jobs are structured
 * JSON tasks where the small model is adequate and rate-limit pools per model
 * are separate.
 */
object GroqModels {
    const val CHAT = "llama-3.3-70b-versatile"
    const val BACKGROUND = "llama-3.1-8b-instant"
    const val WHISPER = "whisper-large-v3-turbo"
}

/**
 * Events emitted by [AiProvider.chatStream]. The flow never throws: it always
 * terminates with exactly one [Done] or [Failed]. [Failed] carries whatever
 * partial text streamed before the failure so the caller can keep it — losing
 * a half-written reply is worse than showing one.
 */
sealed interface ChatStreamEvent {
    data class Delta(val text: String) : ChatStreamEvent
    data class Done(val fullText: String, val tokensUsed: Int, val modelName: String) : ChatStreamEvent
    data class Failed(val error: AiResult<Nothing>, val partialText: String) : ChatStreamEvent
}

interface AiProvider {
    /**
     * The daily batch of conversation openers. [memoryLines] are plain sentences
     * about the user — kept as strings so this layer stays ignorant of how
     * memory is stored.
     */
    suspend fun generateInitiatorPrompts(
        persona: Persona,
        recentSummaries: List<String>,
        memoryLines: List<String> = emptyList(),
        count: Int = 5
    ): AiResult<List<String>>

    suspend fun summarizeEntry(
        plainText: String,
        persona: Persona
    ): AiResult<EntrySummary>

    /** Cheap ping to verify the API key works. Sends a 1-token completion. */
    suspend fun testConnection(): AiResult<Unit>

    /**
     * Free-form chat — one-shot, full response. Caller provides the full message
     * stack (system + history + user turn). Response is plain text, not JSON.
     */
    suspend fun chat(
        systemPrompt: String,
        messages: List<Pair<String, String>>,
        maxTokens: Int = 800,
        model: String = GroqModels.CHAT
    ): AiResult<String>

    /**
     * Streaming chat for the companion conversation. Same contract as [chat]
     * but deltas arrive as they are generated. See [ChatStreamEvent] for the
     * termination guarantees.
     */
    fun chatStream(
        systemPrompt: String,
        messages: List<Pair<String, String>>,
        maxTokens: Int = 500,
        model: String = GroqModels.CHAT
    ): Flow<ChatStreamEvent>

    /**
     * One-shot completion in Groq's JSON mode — the model is constrained to
     * emit a single JSON object. Used by background jobs (memory extraction);
     * the caller owns parsing so this stays schema-agnostic.
     */
    suspend fun completeJson(
        systemPrompt: String,
        userPrompt: String,
        maxTokens: Int = 600,
        model: String = GroqModels.BACKGROUND
    ): AiResult<String>

    /**
     * Whisper-large-v3-turbo transcription. [language] is a BCP-47 hint or null
     * for auto-detect (best for Hinglish code-mix).
     */
    suspend fun transcribeAudio(
        audioFile: File,
        language: String? = null
    ): AiResult<String>
}
