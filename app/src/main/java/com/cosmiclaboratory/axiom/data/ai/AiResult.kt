package com.cosmiclaboratory.axiom.data.ai

sealed class AiResult<out T> {
    data class Ok<T>(val value: T, val tokensUsed: Int = 0, val modelName: String = "") : AiResult<T>()
    data object NoKey : AiResult<Nothing>()
    data object RateLimited : AiResult<Nothing>()
    data class Network(val cause: Throwable) : AiResult<Nothing>()
    data class Parse(val cause: Throwable) : AiResult<Nothing>()

    /**
     * The provider no longer serves the model we asked for.
     *
     * Its own case because it is the one failure here that retrying cannot fix
     * and the user cannot act on. Folded into [Network] — which is where a 404
     * landed before — it made a shut-down model look like a bad connection:
     * workers retried it forever, and the key the user had just pasted was
     * rolled back and blamed. Both Groq's Llama models and Gemini's 2.0 Flash
     * family were withdrawn while this app was pinned to them, so this is not
     * hypothetical.
     */
    data class Unsupported(val detail: String) : AiResult<Nothing>()
}

/**
 * Whether this failure is actually evidence that the key is bad.
 *
 * The save-then-verify flow in Settings and onboarding rolls the key back when
 * the test fails, so that a credential which cannot work is not left behind to
 * fail quietly later. That is right for a rejected key and wrong for everything
 * else: a tunnel, a rate limit, or — as happened when both vendors withdrew the
 * models this app was pinned to — a 404, would delete a perfectly good key and
 * tell the user it had not worked. Only [NoKey] indicts the key.
 */
val AiResult<*>.indictsKey: Boolean
    get() = this is AiResult.NoKey

data class EntrySummary(
    val summary: String,
    val followUp: String,
    val themes: List<String>,
    val mood: String
)
