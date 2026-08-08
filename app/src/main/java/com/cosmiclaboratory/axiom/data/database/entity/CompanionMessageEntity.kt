package com.cosmiclaboratory.axiom.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(
    tableName = "companion_messages",
    indices = [Index(value = ["threadId"]), Index(value = ["createdAt"])]
)
data class CompanionMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val threadId: String,
    val role: String,
    val content: String,
    val createdAt: LocalDateTime,
    val citedEntryIdsCsv: String = "",
    /**
     * How the message came to be: USER typed it, MODEL generated it, LOCAL is
     * an assistant-styled message the app composed on-device (daily opener).
     * LOCAL messages join the history window — the companion should remember
     * having greeted — but cost nothing and work offline.
     */
    val source: String = Source.MODEL.name,
    /**
     * The curated question this message is asking, when it is a daily opener.
     *
     * Questions track "already answered" by joining `entries.questionId` rather
     * than carrying a boolean, so the id has to survive from the prompt bank all
     * the way to the saved entry. Persisting it here is what lets it survive
     * process death: the opener is a row that outlives the ViewModel that wrote
     * it, and "Write about this" can be tapped a day later.
     */
    val questionId: Long? = null
) {
    enum class Role { USER, ASSISTANT, SYSTEM }
    enum class Source { USER, MODEL, LOCAL }
}
