package com.cosmiclaboratory.axiom.di

import com.cosmiclaboratory.axiom.data.ai.AiProvider
import com.cosmiclaboratory.axiom.data.ai.RoutingAiProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import javax.inject.Singleton
import com.cosmiclaboratory.axiom.BuildConfig
import io.ktor.http.HttpHeaders
import android.util.Log
import io.ktor.client.plugins.logging.Logger

@Module
@InstallIn(SingletonComponent::class)
object AiNetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    @Provides
    @Singleton
    fun provideHttpClient(json: Json): HttpClient = HttpClient(Android) {
        expectSuccess = true
        install(ContentNegotiation) {
            json(json)
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 10_000
        }
        /*
         * Both providers return 429 under free-tier limits and both send
         * Retry-After, which nothing read. Background jobs used to bounce all
         * the way out to WorkManager's backoff for a wait the server had
         * already told us the length of.
         *
         * Streaming chat opts out per-request: a half-delivered reply must not
         * be silently restarted underneath the user.
         */
        install(HttpRequestRetry) {
            retryOnServerErrors(maxRetries = 2)
            retryIf { _, response -> response.status == HttpStatusCode.TooManyRequests }
            exponentialDelay(base = 2.0, maxDelayMs = 20_000)
            modifyRequest { request ->
                request.headers.append("x-axiom-retry", retryCount.toString())
            }
        }
        /*
         * Debug builds log the full request body; release builds log nothing.
         *
         * The body IS the journal — memories, excerpts, the conversation so far.
         * On a release build that would put a user's private writing into
         * logcat, where any app holding READ_LOGS could take it, so the release
         * path stays NONE and is not configurable from the UI.
         *
         * On debug it is the only way to see what the model was actually told.
         * Without it, every claim about what the companion remembers is inferred
         * from its replies — which is guessing at a black box.
         *
         * The Authorization header is redacted in both. A key in a log survives
         * long after the log stops being interesting, and gets pasted into bug
         * reports by people who never thought to look.
         */
        install(Logging) {
            level = if (BuildConfig.DEBUG) LogLevel.BODY else LogLevel.NONE
            sanitizeHeader { header -> header == HttpHeaders.Authorization }
            /*
             * An explicit logger, because Ktor's default one writes nothing here.
             * Logger.DEFAULT resolves through SLF4J, which Android has no binding
             * for, so BODY logging appears to work and silently discards every
             * line.
             *
             * Chunked because logcat drops a message at roughly 4 KB and a loaded
             * system prompt is longer than that — the truncated part would be the
             * memories and excerpts, which are the whole reason for looking.
             */
            logger = object : Logger {
                override fun log(message: String) {
                    message.chunked(LOG_CHUNK).forEach { Log.d(HTTP_TAG, it) }
                }
            }
        }
    }
}

/** Debug-only HTTP logging. See the Logging block in [AiNetworkModule]. */
private const val HTTP_TAG = "AxiomHttp"
private const val LOG_CHUNK = 3_000

@Module
@InstallIn(SingletonComponent::class)
abstract class AiBindingsModule {

    /**
     * Everything asks for AiProvider and gets the router, which forwards to
     * whichever vendor the user chose. Nothing else in the app knows there is
     * more than one.
     */
    @Binds
    @Singleton
    abstract fun bindAiProvider(impl: RoutingAiProvider): AiProvider
}
