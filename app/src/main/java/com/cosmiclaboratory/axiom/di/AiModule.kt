package com.cosmiclaboratory.axiom.di

import com.cosmiclaboratory.axiom.data.ai.AiProvider
import com.cosmiclaboratory.axiom.data.ai.GroqAiProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
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
        install(Logging) {
            level = LogLevel.NONE
        }
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class AiBindingsModule {

    @Binds
    @Singleton
    abstract fun bindAiProvider(impl: GroqAiProvider): AiProvider
}
