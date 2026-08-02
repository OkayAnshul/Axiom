package com.cosmiclaboratory.axiom.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.cosmiclaboratory.axiom.domain.model.AiPrompt
import com.cosmiclaboratory.axiom.domain.model.PersonaKey
import java.time.LocalDateTime

@Entity(
    tableName = "ai_prompt_cache",
    indices = [
        Index(value = ["personaKey"]),
        Index(value = ["consumed"]),
        Index(value = ["batchId"])
    ]
)
data class AiPromptCacheEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val batchId: String,
    val text: String,
    val personaKey: String,
    val generatedAt: LocalDateTime,
    val consumed: Boolean = false
)

fun AiPromptCacheEntity.toDomainModel(): AiPrompt = AiPrompt(
    id = id,
    batchId = batchId,
    text = text,
    personaKey = PersonaKey.fromStorage(personaKey),
    generatedAt = generatedAt,
    consumed = consumed
)
