package com.cosmiclaboratory.axiom.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cosmiclaboratory.axiom.data.database.entity.PersonaSettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonaDao {

    @Query("SELECT * FROM persona_settings ORDER BY id ASC")
    fun observeAll(): Flow<List<PersonaSettingsEntity>>

    @Query("SELECT * FROM persona_settings ORDER BY id ASC")
    suspend fun getAll(): List<PersonaSettingsEntity>

    @Query("SELECT * FROM persona_settings WHERE key = :key LIMIT 1")
    suspend fun getByKey(key: String): PersonaSettingsEntity?

    @Query("SELECT COUNT(*) FROM persona_settings")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(personas: List<PersonaSettingsEntity>)
}
