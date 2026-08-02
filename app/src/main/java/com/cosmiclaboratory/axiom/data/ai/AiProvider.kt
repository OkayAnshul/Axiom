package com.cosmiclaboratory.axiom.data.ai

import com.cosmiclaboratory.axiom.domain.model.Persona
import java.io.File

interface AiProvider {
    suspend fun generateInitiatorPrompts(
        persona: Persona,
        recentSummaries: List<String>,
        count: Int = 5
    ): AiResult<List<String>>

    suspend fun summarizeEntry(
        plainText: String,
        persona: Persona
    ): AiResult<EntrySummary>

    /** Cheap ping to verify the API key works. Sends a 1-token completion. */
    suspend fun testConnection(): AiResult<Unit>

    /**
     * Free-form chat — used by the Companion tab. Caller provides the full message
     * stack (system + history + user turn). Response is plain text, not JSON.
     */
    suspend fun chat(
        systemPrompt: String,
        messages: List<Pair<String, String>>,
        maxTokens: Int = 800
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
