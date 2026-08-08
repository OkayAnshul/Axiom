package com.cosmiclaboratory.axiom.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

/**
 * Per-thread companion bookkeeping. The rolling summary compresses everything
 * older than the verbatim history window, so the model always sees "summary of
 * the far past + recent turns" instead of an amnesiac single message. The
 * watermarks track which messages each consumer has already dealt with.
 */
@Entity(tableName = "companion_thread_state")
data class CompanionThreadStateEntity(
    @PrimaryKey val threadId: String,
    val rollingSummary: String = "",
    val summarizedUpToMessageId: Long = 0,
    val digestedUpToMessageId: Long = 0,
    /**
     * Everything at or below this id is out of sight but not gone.
     *
     * Opening the app is meant to feel like arriving, not like walking back into
     * a room mid-sentence, so a fresh launch parks the conversation and shows a
     * blank screen. Parking is deliberately not deletion: for as long as the
     * retention window allows, a "pick up where we left off" chip winds this
     * back to zero and the messages return exactly as they were.
     *
     * There is deliberately no companion `parkedAt` timestamp. How long a parked
     * conversation stays resumable is measured from its last message, not from
     * when it was set down — otherwise opening the app re-parks with a fresh
     * clock and a conversation glanced at daily never expires. See
     * [com.cosmiclaboratory.axiom.domain.model.ConversationRetention].
     */
    val parkedUpToMessageId: Long = 0,
    val updatedAt: LocalDateTime
)
