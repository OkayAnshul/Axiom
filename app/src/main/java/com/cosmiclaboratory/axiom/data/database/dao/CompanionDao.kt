package com.cosmiclaboratory.axiom.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cosmiclaboratory.axiom.data.database.entity.CompanionMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CompanionDao {
    @Query("SELECT * FROM companion_messages ORDER BY id ASC")
    suspend fun allForBackup(): List<CompanionMessageEntity>

    @Query("SELECT * FROM companion_messages WHERE threadId = :threadId ORDER BY createdAt ASC")
    fun observeThread(threadId: String): Flow<List<CompanionMessageEntity>>

    /**
     * The live part of the thread: everything past the park watermark.
     *
     * Ordered by createdAt like [observeThread] but filtered by id, because the
     * watermark is an id and the display order is chronological — for a single
     * append-only thread those agree, and the id is what parking can point at.
     */
    @Query(
        "SELECT * FROM companion_messages WHERE threadId = :threadId AND id > :afterId " +
            "ORDER BY createdAt ASC"
    )
    fun observeThreadAfter(threadId: String, afterId: Long): Flow<List<CompanionMessageEntity>>

    /** Newest [limit] messages, returned oldest-first — the verbatim history window. */
    @Query(
        "SELECT * FROM (SELECT * FROM companion_messages WHERE threadId = :threadId ORDER BY id DESC LIMIT :limit) ORDER BY id ASC"
    )
    suspend fun recent(threadId: String, limit: Int): List<CompanionMessageEntity>

    /** Messages after a watermark id, oldest-first — feeds the digest worker and summarizer. */
    @Query("SELECT * FROM companion_messages WHERE threadId = :threadId AND id > :afterId ORDER BY id ASC")
    suspend fun after(threadId: String, afterId: Long): List<CompanionMessageEntity>

    @Query("SELECT * FROM companion_messages WHERE threadId = :threadId ORDER BY id DESC LIMIT 1")
    suspend fun latest(threadId: String): CompanionMessageEntity?

    /** How much is parked, for the "pick up where we left off" offer. */
    @Query("SELECT COUNT(*) FROM companion_messages WHERE threadId = :threadId AND id <= :upToId")
    suspend fun countUpTo(threadId: String, upToId: Long): Int

    @Query("SELECT DISTINCT threadId FROM companion_messages ORDER BY createdAt DESC")
    suspend fun listThreadIds(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: CompanionMessageEntity): Long

    /**
     * Rewrites an on-device opener when the reader swipes to another prompt.
     * Scoped to LOCAL so this can never edit something the user or the model
     * actually said — swiping picks a question, it does not rewrite history.
     */
    @Query(
        "UPDATE companion_messages SET content = :content, questionId = :questionId " +
            "WHERE id = :messageId AND source = 'LOCAL'"
    )
    suspend fun updateLocalContent(messageId: Long, content: String, questionId: Long?)

    @Query("DELETE FROM companion_messages WHERE threadId = :threadId")
    suspend fun deleteThread(threadId: String)

    @Query("DELETE FROM companion_messages")
    suspend fun clear()
}
