package com.cosmiclaboratory.axiom.ui.design

import com.cosmiclaboratory.axiom.data.ai.AiResult

/**
 * Every failure the UI can show, as a closed set.
 *
 * The point of this type is [toAxiomError] below: it is an exhaustive `when`
 * over [AiResult] with no `else` branch, so adding a sixth AiResult case becomes
 * a compile error rather than a silently unhandled state. Previously each
 * ViewModel hardcoded its own error strings per branch and it was impossible to
 * tell, by reading, whether all five were covered.
 */
sealed interface AxiomError {
    /** No API key configured. Not an error so much as an unmade choice. */
    data object NoAiKey : AxiomError

    /** Provider throttled us. [retryAfterSeconds] drives a countdown. */
    data class RateLimited(val retryAfterSeconds: Int = 20) : AxiomError

    /** Offline, DNS failure, timeout. Retryable. */
    data class Network(val cause: Throwable? = null) : AxiomError

    /** The response arrived but did not parse. Not the user's fault, not retryable by them. */
    data class Malformed(val cause: Throwable? = null) : AxiomError

    /** Disk/database failure. */
    data class Storage(val cause: Throwable? = null) : AxiomError

    /** A runtime permission was denied. [permission] is the manifest name. */
    data class Permission(val permission: String) : AxiomError

    /**
     * The provider withdrew the model this build asks for. Only an app update
     * fixes it, so it must never be offered as a retry or blamed on the key.
     */
    data class ModelUnavailable(val detail: String) : AxiomError

    /** Anything not otherwise classified; carries a caller-supplied message. */
    data class Unknown(val message: String) : AxiomError
}

/**
 * The single mapping point from the AI layer to the UI's error vocabulary.
 * Returns null for [AiResult.Ok] — success is not an error.
 */
fun AiResult<*>.toAxiomError(): AxiomError? = when (this) {
    is AiResult.Ok -> null
    AiResult.NoKey -> AxiomError.NoAiKey
    AiResult.RateLimited -> AxiomError.RateLimited()
    is AiResult.Network -> AxiomError.Network(cause)
    is AiResult.Parse -> AxiomError.Malformed(cause)
    is AiResult.Unsupported -> AxiomError.ModelUnavailable(detail)
}

/** True when the user can meaningfully retry the same action unchanged. */
val AxiomError.isRetryable: Boolean
    get() = when (this) {
        is AxiomError.Network, is AxiomError.RateLimited, is AxiomError.Storage -> true
        is AxiomError.Malformed -> true
        AxiomError.NoAiKey, is AxiomError.Permission, is AxiomError.Unknown -> false
        is AxiomError.ModelUnavailable -> false
    }
