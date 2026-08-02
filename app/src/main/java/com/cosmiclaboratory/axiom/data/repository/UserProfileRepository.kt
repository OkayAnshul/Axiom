package com.cosmiclaboratory.axiom.data.repository

import com.cosmiclaboratory.axiom.data.database.dao.UserProfileDao
import com.cosmiclaboratory.axiom.data.database.entity.UserProfileEntity
import com.cosmiclaboratory.axiom.data.database.entity.toDomainModel
import com.cosmiclaboratory.axiom.data.preferences.UserPreferences
import com.cosmiclaboratory.axiom.domain.model.PersonaKey
import com.cosmiclaboratory.axiom.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserProfileRepository @Inject constructor(
    private val dao: UserProfileDao,
    private val prefs: UserPreferences
) {
    fun observe(): Flow<UserProfile?> = dao.observe().map { it?.toDomainModel() }

    suspend fun get(): UserProfile? = dao.get()?.toDomainModel()

    suspend fun completeOnboarding(displayName: String, persona: PersonaKey) {
        val now = LocalDateTime.now()
        dao.upsert(
            UserProfileEntity(
                id = 0,
                displayName = displayName,
                createdAt = dao.get()?.createdAt ?: now,
                onboardingComplete = true,
                activePersonaKey = persona.storageValue
            )
        )
        prefs.setOnboardingComplete(true)
        prefs.setActivePersona(persona)
    }

    suspend fun setPersona(persona: PersonaKey) {
        dao.setActivePersonaKey(persona.storageValue)
        prefs.setActivePersona(persona)
    }
}
