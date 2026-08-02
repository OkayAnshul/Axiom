package com.cosmiclaboratory.axiom.domain.model

import java.time.LocalDateTime

data class UserProfile(
    val id: Long = 0,
    val displayName: String,
    val createdAt: LocalDateTime,
    val onboardingComplete: Boolean,
    val activePersonaKey: PersonaKey
)
