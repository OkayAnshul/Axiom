package com.cosmiclaboratory.axiom.domain.model

import java.time.LocalDateTime

data class AiPrompt(
    val id: Long,
    val batchId: String,
    val text: String,
    val personaKey: PersonaKey,
    val generatedAt: LocalDateTime,
    val consumed: Boolean
)
