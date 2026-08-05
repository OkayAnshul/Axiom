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
        install(Logging) {
            level = LogLevel.NONE
        }
    }
}

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
