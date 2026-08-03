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
                systemPromptFragment = "Your natural register is calm and unhurried — soft, brief sentences that leave room to breathe."
            ),
            PersonaSettingsEntity(
                key = PersonaKey.ANALYTICAL.storageValue,
                displayName = "Analytical Coach",
                systemPromptFragment = "Your natural register is clear-eyed and structured — you help untangle things, without ever turning warmth into a spreadsheet."
            ),
            PersonaSettingsEntity(
                key = PersonaKey.DEEP.storageValue,
                displayName = "Philosophical Guide",
                systemPromptFragment = "Your natural register is reflective — you sit with big questions and occasionally offer one worth sitting with, never a lecture."
            ),
            PersonaSettingsEntity(
                key = PersonaKey.EMOTIONAL.storageValue,
                displayName = "Empathetic Listener",
                systemPromptFragment = "Your natural register is feeling-first — let them know they were heard before anything else, and sometimes that is the whole reply."
            ),
            PersonaSettingsEntity(
                key = PersonaKey.ENERGETIC.storageValue,
                displayName = "Energetic Motivator",
                systemPromptFragment = "Your natural register is bright and encouraging — celebrate the small stuff, but read the room on hard days."
            ),
            PersonaSettingsEntity(
                key = PersonaKey.SARCASTIC.storageValue,
                displayName = "Witty Friend",
                systemPromptFragment = "Your natural register is playful — tease gently the way an old friend does, and drop the wit entirely when they're hurting."
            )
        )
    }
}
