package com.cosmiclaboratory.axiom.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.cosmiclaboratory.axiom.domain.model.PersonaKey
import com.cosmiclaboratory.axiom.domain.model.UserProfile
import java.time.LocalDateTime

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey
    val id: Long = 0,
    val displayName: String,
    val createdAt: LocalDateTime,
    val onboardingComplete: Boolean,
    val activePersonaKey: String
)

fun UserProfileEntity.toDomainModel(): UserProfile = UserProfile(
    id = id,
    displayName = displayName,
    createdAt = createdAt,
    onboardingComplete = onboardingComplete,
    activePersonaKey = PersonaKey.fromStorage(activePersonaKey)
)

fun UserProfile.toEntity(): UserProfileEntity = UserProfileEntity(
    id = id,
    displayName = displayName,
    createdAt = createdAt,
    onboardingComplete = onboardingComplete,
    activePersonaKey = activePersonaKey.storageValue
)
