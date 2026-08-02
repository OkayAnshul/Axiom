package com.cosmiclaboratory.axiom.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cosmiclaboratory.axiom.data.database.entity.QuestionEntity

@Dao
interface QuestionDao {

    @Query("SELECT COUNT(*) FROM questions WHERE source = :source")
    suspend fun countBySource(source: String): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(questions: List<QuestionEntity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(question: QuestionEntity): Long

    @Query("SELECT * FROM questions WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): QuestionEntity?

    /**
     * Pending follow-up: AI_FOLLOWUP question whose id has no answer entry yet.
     * Highest priority in the question engine.
     */
    @Query("""
        SELECT q.* FROM questions q
        LEFT JOIN answer_entries a ON a.questionId = q.id
        WHERE q.source = 'AI_FOLLOWUP' AND a.id IS NULL
        ORDER BY q.createdAt DESC LIMIT 1
    """)
    suspend fun pendingFollowUp(): QuestionEntity?

    /**
     * Curated questions matching a theme that have not been answered in the last [sinceEpochSec] seconds.
     */
    @Query("""
        SELECT q.* FROM questions q
        LEFT JOIN answer_entries a ON a.questionId = q.id
        WHERE q.source = 'CURATED'
          AND (q.theme = :theme OR :theme IS NULL)
          AND (a.id IS NULL OR a.createdAt < :cutoffIso)
        ORDER BY RANDOM() LIMIT 1
    """)
    suspend fun randomCuratedNotRecent(theme: String?, cutoffIso: String): QuestionEntity?

    @Query("""
        SELECT q.* FROM questions q
        LEFT JOIN answer_entries a ON a.questionId = q.id
        WHERE q.source = 'CURATED' AND a.id IS NULL
        ORDER BY RANDOM() LIMIT 1
    """)
    suspend fun randomCuratedUnanswered(): QuestionEntity?
}
