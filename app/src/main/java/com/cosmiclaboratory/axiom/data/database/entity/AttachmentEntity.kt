package com.cosmiclaboratory.axiom.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(
    tableName = "attachments",
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["entryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["entryId"])]
)
data class AttachmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val entryId: Long,
    val kind: String,
    val uri: String,
    val durationMs: Long? = null,
    val createdAt: LocalDateTime
) {
    enum class Kind { IMAGE, AUDIO }
}
