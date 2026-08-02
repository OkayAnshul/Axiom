package com.cosmiclaboratory.axiom.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.cosmiclaboratory.axiom.domain.model.Persona
import com.cosmiclaboratory.axiom.domain.model.PersonaKey

@Entity(
    tableName = "persona_settings",
    indices = [Index(value = ["key"], unique = true)]
)
data class PersonaSettingsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val key: String,
    val displayName: String,
    val systemPromptFragment: String
)

fun PersonaSettingsEntity.toDomainModel(): Persona = Persona(
    id = id,
    key = PersonaKey.fromStorage(key),
    displayName = displayName,
    systemPromptFragment = systemPromptFragment
)
