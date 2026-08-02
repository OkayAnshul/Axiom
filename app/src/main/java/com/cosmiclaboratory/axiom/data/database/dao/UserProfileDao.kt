package com.cosmiclaboratory.axiom.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.cosmiclaboratory.axiom.data.database.entity.UserProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {

    @Query("SELECT * FROM user_profile WHERE id = 0")
    fun observe(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile WHERE id = 0")
    suspend fun get(): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: UserProfileEntity)

    @Update
    suspend fun update(profile: UserProfileEntity)

    @Query("UPDATE user_profile SET activePersonaKey = :key WHERE id = 0")
    suspend fun setActivePersonaKey(key: String)

    @Query("UPDATE user_profile SET onboardingComplete = :complete WHERE id = 0")
    suspend fun setOnboardingComplete(complete: Boolean)
}
