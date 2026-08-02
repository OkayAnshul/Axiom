package com.cosmiclaboratory.axiom.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.cosmiclaboratory.axiom.domain.model.MemoryItem
import com.cosmiclaboratory.axiom.domain.model.MemoryKind
import java.time.LocalDateTime

@Entity(tableName = "memory_items")
data class MemoryItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val kind: String,
    val text: String,
    val weight: Float,
    val lastSeenAt: LocalDateTime
)

fun MemoryItemEntity.toDomainModel(): MemoryItem = MemoryItem(
    id = id,
    kind = runCatching { MemoryKind.valueOf(kind) }.getOrDefault(MemoryKind.THEME),
    text = text,
    weight = weight,
    lastSeenAt = lastSeenAt
)
