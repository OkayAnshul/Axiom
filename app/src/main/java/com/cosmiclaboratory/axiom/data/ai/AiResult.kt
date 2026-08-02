package com.cosmiclaboratory.axiom.data.ai

sealed class AiResult<out T> {
    data class Ok<T>(val value: T, val tokensUsed: Int = 0, val modelName: String = "") : AiResult<T>()
    data object NoKey : AiResult<Nothing>()
    data object RateLimited : AiResult<Nothing>()
    data class Network(val cause: Throwable) : AiResult<Nothing>()
    data class Parse(val cause: Throwable) : AiResult<Nothing>()
}

data class EntrySummary(
    val summary: String,
    val followUp: String,
    val themes: List<String>,
    val mood: String
)
