package com.cosmiclaboratory.axiom.data.database.seed

import android.content.Context
import com.cosmiclaboratory.axiom.data.database.dao.PersonaDao
import com.cosmiclaboratory.axiom.data.database.dao.PromptPackDao
import com.cosmiclaboratory.axiom.data.database.dao.QuestionDao
import com.cosmiclaboratory.axiom.data.database.dao.UserProfileDao
import com.cosmiclaboratory.axiom.data.database.entity.PersonaSettingsEntity
import com.cosmiclaboratory.axiom.data.database.entity.PromptPackEntity
import com.cosmiclaboratory.axiom.data.database.entity.QuestionEntity
import com.cosmiclaboratory.axiom.data.database.entity.UserProfileEntity
import com.cosmiclaboratory.axiom.domain.model.PersonaKey
import com.cosmiclaboratory.axiom.domain.model.QuestionSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseSeeder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val personaDao: PersonaDao,
    private val questionDao: QuestionDao,
    private val promptPackDao: PromptPackDao,
    private val userProfileDao: UserProfileDao
) {
    suspend fun seedIfNeeded() {
        seedPersonas()
        seedUserProfile()
        seedCuratedPrompts()
    }

    private suspend fun seedPersonas() {
        if (personaDao.count() > 0) return
        personaDao.insertAll(DEFAULT_PERSONAS)
    }

    private suspend fun seedUserProfile() {
        if (userProfileDao.get() != null) return
        userProfileDao.upsert(
            UserProfileEntity(
                id = 0,
                displayName = "",
                createdAt = LocalDateTime.now(),
                onboardingComplete = false,
                activePersonaKey = PersonaKey.CALM.storageValue
            )
        )
    }

    private suspend fun seedCuratedPrompts() {
        if (questionDao.countBySource(QuestionSource.CURATED.name) > 0) return
        val asset = loadCuratedPromptsAsset()
        val packIdsByTheme = ensurePacksFor(asset.prompts.map { it.theme }.distinct())
        val now = LocalDateTime.now()
        val rows = asset.prompts.map { dto ->
            QuestionEntity(
                packId = packIdsByTheme[dto.theme],
                theme = dto.theme,
                text = dto.text,
                source = QuestionSource.CURATED.name,
                createdAt = now
            )
        }
        questionDao.insertAll(rows)
    }

    private suspend fun ensurePacksFor(themes: List<String>): Map<String, Long> {
        val result = mutableMapOf<String, Long>()
        for (theme in themes) {
            val id = promptPackDao.insert(
                PromptPackEntity(name = theme.replaceFirstChar { it.titlecase() }, theme = theme)
            )
            if (id != -1L) result[theme] = id
        }
        return result
    }

    private fun loadCuratedPromptsAsset(): CuratedPromptsAsset {
        val json = context.assets.open(ASSET_PATH).bufferedReader().use { it.readText() }
        return Json { ignoreUnknownKeys = true }.decodeFromString(CuratedPromptsAsset.serializer(), json)
    }

    private companion object {
        const val ASSET_PATH = "curated_prompts.json"

        val DEFAULT_PERSONAS = listOf(
            PersonaSettingsEntity(
                key = PersonaKey.CALM.storageValue,
                displayName = "Calm Companion",
                systemPromptFragment = "You are a calm, gentle companion. Use soft, brief sentences."
            ),
            PersonaSettingsEntity(
                key = PersonaKey.ANALYTICAL.storageValue,
                displayName = "Analytical Coach",
                systemPromptFragment = "You are an analytical coach. Be precise and structured. No fluff."
            ),
            PersonaSettingsEntity(
                key = PersonaKey.DEEP.storageValue,
                displayName = "Philosophical Guide",
                systemPromptFragment = "You are a thoughtful, philosophical guide. Ask one probing question."
            ),
            PersonaSettingsEntity(
                key = PersonaKey.EMOTIONAL.storageValue,
                displayName = "Empathetic Listener",
                systemPromptFragment = "You are an empathetic listener. Validate feelings before insight."
            ),
            PersonaSettingsEntity(
                key = PersonaKey.ENERGETIC.storageValue,
                displayName = "Energetic Motivator",
                systemPromptFragment = "You are an upbeat motivator. Be punchy and action-oriented."
            ),
            PersonaSettingsEntity(
                key = PersonaKey.SARCASTIC.storageValue,
                displayName = "Witty Friend",
                systemPromptFragment = "You are a witty, lightly sarcastic friend. Stay kind."
            )
        )
    }
}
