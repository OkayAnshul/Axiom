package com.cosmiclaboratory.axiom.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cosmiclaboratory.axiom.data.database.entity.DailySummaryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DailySummaryDao {
    @Query("SELECT * FROM daily_summaries ORDER BY dateIso DESC")
    fun observeAll(): Flow<List<DailySummaryEntity>>

    @Query("SELECT * FROM daily_summaries WHERE dateIso BETWEEN :startIso AND :endIso ORDER BY dateIso ASC")
    fun observeRange(startIso: String, endIso: String): Flow<List<DailySummaryEntity>>

    @Query("SELECT * FROM daily_summaries WHERE dateIso = :dateIso")
    suspend fun get(dateIso: String): DailySummaryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(summary: DailySummaryEntity)

    @Query("DELETE FROM daily_summaries")
    suspend fun clear()
}
