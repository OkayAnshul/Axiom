package com.cosmiclaboratory.axiom.domain.model

enum class PersonaKey(val storageValue: String) {
    CALM("calm"),
    ANALYTICAL("analytical"),
    DEEP("deep"),
    EMOTIONAL("emotional"),
    ENERGETIC("energetic"),
    SARCASTIC("sarcastic");

    companion object {
        fun fromStorage(value: String?): PersonaKey =
            entries.firstOrNull { it.storageValue == value } ?: CALM
    }
}

data class Persona(
    val id: Long,
    val key: PersonaKey,
    val displayName: String,
    val systemPromptFragment: String
)
