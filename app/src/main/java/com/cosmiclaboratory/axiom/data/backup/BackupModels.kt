package com.cosmiclaboratory.axiom.data.backup

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The on-disk backup format.
 *
 * Deliberately its own set of types rather than the Room entities. A backup is
 * a contract with the user's future self: it has to keep working after the
 * database changes shape, and it should stay readable by anything that can open
 * a JSON file, including a person with a text editor and no copy of Axiom.
 * Coupling it to the entities would break that the first time a column moves.
 *
 * Row ids are exported but treated as local-only handles on the way back in —
 * they exist purely so entry/tag/insight relationships survive the trip.
 */
@Serializable
data class BackupFile(
    val format: String = FORMAT,
    val version: Int = VERSION,
    @SerialName("exported_at") val exportedAt: String,
    @SerialName("display_name") val displayName: String = "",
    val entries: List<BackupEntry> = emptyList(),
    val tags: List<BackupTag> = emptyList(),
    @SerialName("entry_tags") val entryTags: List<BackupEntryTag> = emptyList(),
    val insights: List<BackupInsight> = emptyList(),
    val memories: List<BackupMemory> = emptyList(),
    @SerialName("companion_messages") val companionMessages: List<BackupMessage> = emptyList()
) {
    companion object {
        const val FORMAT = "axiom.backup"
        const val VERSION = 1
    }
}

@Serializable
data class BackupEntry(
    val id: Long,
    val title: String = "",
    val content: String,
    val markdown: String = "",
    val kind: String = "FREE_FORM",
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("is_favorite") val isFavorite: Boolean = false,
    @SerialName("is_archived") val isArchived: Boolean = false,
    @SerialName("is_complete") val isComplete: Boolean = true,
    val mood: Int? = null,
    @SerialName("mood_captured_at") val moodCapturedAt: String? = null,
    val emotion: String? = null,
    val energy: Int? = null,
    @SerialName("prompt_snapshot") val promptSnapshot: String? = null,
    @SerialName("duration_ms") val durationMs: Long = 0L,
    @SerialName("source_lang") val sourceLang: String? = null
)

/** [color] is a packed ARGB value, as stored — not a hex string. */
@Serializable
data class BackupTag(
    val id: Long,
    val name: String,
    val color: Long = 0L,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class BackupEntryTag(@SerialName("entry_id") val entryId: Long, @SerialName("tag_id") val tagId: Long)

@Serializable
data class BackupInsight(
    @SerialName("entry_id") val entryId: Long,
    val summary: String = "",
    @SerialName("follow_up") val followUp: String = "",
    val themes: String = "",
    val mood: String = "",
    @SerialName("model_name") val modelName: String = "",
    @SerialName("total_tokens") val totalTokens: Int = 0,
    @SerialName("created_at") val createdAt: String
)

@Serializable
data class BackupMemory(
    val kind: String,
    val text: String,
    val weight: Float = 0.5f,
    @SerialName("times_seen") val timesSeen: Int = 1,
    @SerialName("created_at") val createdAt: String,
    @SerialName("last_seen_at") val lastSeenAt: String,
    @SerialName("source_type") val sourceType: String = "CONVERSATION",
    @SerialName("user_edited") val userEdited: Boolean = false,
    @SerialName("due_at") val dueAt: String? = null
)

@Serializable
data class BackupMessage(
    @SerialName("thread_id") val threadId: String,
    val role: String,
    val content: String,
    @SerialName("created_at") val createdAt: String,
    val source: String = "MODEL"
)

/**
 * The envelope written when the user supplies a passphrase. The parameters are
 * recorded in the file rather than assumed by the reader, so a future change to
 * the iteration count cannot make today's backups unopenable.
 */
@Serializable
data class EncryptedBackup(
    val format: String = FORMAT,
    val version: Int = 1,
    val kdf: String = "PBKDF2WithHmacSHA256",
    val iterations: Int,
    val salt: String,
    val iv: String,
    val ciphertext: String
) {
    companion object {
        const val FORMAT = "axiom.backup.encrypted"
    }
}

/** What a restore actually did, so the user is told rather than reassured. */
data class RestoreSummary(
    val entriesAdded: Int = 0,
    val entriesSkipped: Int = 0,
    val memoriesAdded: Int = 0,
    val messagesAdded: Int = 0,
    val tagsAdded: Int = 0
) {
    val totalAdded: Int get() = entriesAdded + memoriesAdded + messagesAdded + tagsAdded
}
