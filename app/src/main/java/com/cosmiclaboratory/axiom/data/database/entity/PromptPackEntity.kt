package com.cosmiclaboratory.axiom.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.cosmiclaboratory.axiom.domain.model.PromptPack

@Entity(tableName = "prompt_packs")
data class PromptPackEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val theme: String,
    val isBuiltIn: Boolean = true
)

fun PromptPackEntity.toDomainModel(): PromptPack = PromptPack(
    id = id,
    name = name,
    theme = theme,
    isBuiltIn = isBuiltIn
)
