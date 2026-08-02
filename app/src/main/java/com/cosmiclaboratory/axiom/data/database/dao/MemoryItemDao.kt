package com.cosmiclaboratory.axiom.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cosmiclaboratory.axiom.data.database.entity.MemoryItemEntity

@Dao
interface MemoryItemDao {

    @Query("SELECT * FROM memory_items ORDER BY weight DESC, lastSeenAt DESC")
    suspend fun getAll(): List<MemoryItemEntity>

    @Query("SELECT * FROM memory_items WHERE kind = :kind ORDER BY weight DESC LIMIT :limit")
    suspend fun topByKind(kind: String, limit: Int): List<MemoryItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: MemoryItemEntity): Long
}
