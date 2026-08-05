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
 * Which model does which job.
 *
 * The strong model used to have exactly one call site — the live conversation —
 * while every task where quality is durable and visible (the journal entry
 * written in the user's voice, what gets remembered about them for months) ran
 * on the cheap one. That is backwards: a mediocre chat reply scrolls away, a
 * mediocre memory persists.
 *
 * The consolidation that pays for it: merging the digest's three calls into one
 * and the entry's two into one means the same text is sent once instead of
 * three times, so the stronger model costs roughly what the old fan-out did.
 */
object AiTasks {
    /** Journal entry in the user's voice, memory extraction, weekly recap. */
    const val QUALITY = GroqModels.CHAT

    /** Openers, proactive one-liners, query rewriting, connection tests. */
    const val CHEAP = GroqModels.BACKGROUND
}

object Temperatures {
    /** JSON and extraction: obey the schema, don't embellish. */
    const val STRUCTURED = 0.2f

    /** Anything the user reads as the companion's own voice. */
    const val CONVERSATIONAL = 0.7f
}

/**
 * Events emitted by [AiProvider.chatStream]. The flow never throws: it always
 * terminates with exactly one [Done] or [Failed]. [Failed] carries whatever
 * partial text streamed before the failure so the caller can keep it — losing
 * a half-written reply is worse than showing one.
 */
sealed interface ChatStreamEvent {
    data class Delta(val text: String) : ChatStreamEvent

    /**
     * [truncated] is true when the model stopped because it hit the token cap
     * rather than because it had finished. Providers report this as
     * `finish_reason: "length"` (Groq) or `MAX_TOKENS` (Gemini); it was parsed
     * and discarded before, so a reply cut off mid-sentence was stored and shown
     * as though the companion had simply said something odd.
     */
    data class Done(
        val fullText: String,
        val tokensUsed: Int,
        val modelName: String,
        val truncated: Boolean = false
    ) : ChatStreamEvent

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
        maxTokens: Int = 800,
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
        model: String = GroqModels.BACKGROUND,
        /**
         * Structured extraction is not a creative task. Everything used to run
         * at 0.7 — the same setting as open conversation — which is why JSON
         * replies were occasionally reworded into invalid shapes.
         */
        temperature: Float = Temperatures.STRUCTURED
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
