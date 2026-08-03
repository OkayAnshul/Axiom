package com.cosmiclaboratory.axiom.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.cosmiclaboratory.axiom.domain.model.MemoryItem
import com.cosmiclaboratory.axiom.domain.model.MemoryKind
import com.cosmiclaboratory.axiom.domain.model.MemorySource
import java.time.LocalDateTime

@Entity(tableName = "memory_items")
data class MemoryItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val kind: String,
    val text: String,
    /** Raw weight 0..1. Effective weight decays with time since lastSeenAt — computed at read time, never stored. */
    val weight: Float,
    val timesSeen: Int = 1,
    val createdAt: LocalDateTime,
    val lastSeenAt: LocalDateTime,
    /** [MemorySource] name: CONVERSATION or ENTRY. */
    val sourceType: String,
    /** entryId when sourceType is ENTRY; companion message id when CONVERSATION. */
    val sourceId: Long? = null,
    /** User-edited memories are never auto-revised by extraction, only reinforced. */
    val userEdited: Boolean = false,
    /**
     * An open loop: something the companion should circle back to after this
     * moment ("interview on Tuesday" → ask on Wednesday). Null means nothing to
     * follow up. Cleared once asked, so the companion never nags twice.
     */
    val dueAt: LocalDateTime? = null
)

fun MemoryItemEntity.toDomainModel(): MemoryItem = MemoryItem(
    id = id,
    kind = runCatching { MemoryKind.valueOf(kind) }.getOrDefault(MemoryKind.THEME),
    text = text,
    weight = weight,
    timesSeen = timesSeen,
    createdAt = createdAt,
    lastSeenAt = lastSeenAt,
    source = runCatching { MemorySource.valueOf(sourceType) }.getOrDefault(MemorySource.CONVERSATION),
    sourceId = sourceId,
    userEdited = userEdited,
    dueAt = dueAt
)
