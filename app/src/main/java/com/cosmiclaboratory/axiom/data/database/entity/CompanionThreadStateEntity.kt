package com.cosmiclaboratory.axiom.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

/**
 * Per-thread companion bookkeeping. The rolling summary compresses everything
 * older than the verbatim history window, so the model always sees "summary of
 * the far past + recent turns" instead of an amnesiac single message. The two
 * watermarks track which messages the summarizer and the journal-digest worker
 * have already consumed.
 */
@Entity(tableName = "companion_thread_state")
data class CompanionThreadStateEntity(
    @PrimaryKey val threadId: String,
    val rollingSummary: String = "",
    val summarizedUpToMessageId: Long = 0,
    val digestedUpToMessageId: Long = 0,
    val updatedAt: LocalDateTime
)
