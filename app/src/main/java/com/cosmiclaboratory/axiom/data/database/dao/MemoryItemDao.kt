package com.cosmiclaboratory.axiom.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.cosmiclaboratory.axiom.data.database.entity.MemoryItemEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface MemoryItemDao {

    @Query("SELECT * FROM memory_items ORDER BY weight DESC, lastSeenAt DESC")
    suspend fun getAll(): List<MemoryItemEntity>

    @Query("SELECT * FROM memory_items ORDER BY kind ASC, weight DESC, lastSeenAt DESC")
    fun observeAll(): Flow<List<MemoryItemEntity>>

    @Query("SELECT * FROM memory_items WHERE id = :id")
    suspend fun getById(id: Long): MemoryItemEntity?

    @Query("SELECT * FROM memory_items WHERE kind = :kind ORDER BY weight DESC LIMIT :limit")
    suspend fun topByKind(kind: String, limit: Int): List<MemoryItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: MemoryItemEntity): Long

    @Update
    suspend fun update(item: MemoryItemEntity)

    @Query("DELETE FROM memory_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    /** One-statement reinforcement: bump weight (capped at 1), count the sighting, refresh recency. */
    @Query(
        "UPDATE memory_items SET weight = MIN(1.0, weight + :bump), timesSeen = timesSeen + 1, lastSeenAt = :now WHERE id = :id"
    )
    suspend fun reinforce(id: Long, now: LocalDateTime, bump: Float = 0.15f)

    /** Open loops whose moment has passed — the companion owes the user a "how did it go?". */
    @Query("SELECT * FROM memory_items WHERE dueAt IS NOT NULL AND dueAt <= :now ORDER BY dueAt ASC LIMIT :limit")
    suspend fun dueOpenLoops(now: LocalDateTime, limit: Int): List<MemoryItemEntity>

    /** Asked about; never nag twice. The memory itself survives, only the loop closes. */
    @Query("UPDATE memory_items SET dueAt = NULL WHERE id = :id")
    suspend fun closeLoop(id: Long)
}
