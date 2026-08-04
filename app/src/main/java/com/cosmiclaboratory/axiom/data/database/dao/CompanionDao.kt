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

    @Query("SELECT DISTINCT threadId FROM companion_messages ORDER BY createdAt DESC")
    suspend fun listThreadIds(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: CompanionMessageEntity): Long

    @Query("DELETE FROM companion_messages WHERE threadId = :threadId")
    suspend fun deleteThread(threadId: String)

    @Query("DELETE FROM companion_messages")
    suspend fun clear()
}
