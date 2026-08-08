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
        LEFT JOIN entries a ON a.questionId = q.id AND a.isComplete = 1
        WHERE q.source = 'AI_FOLLOWUP' AND a.id IS NULL
        ORDER BY q.createdAt DESC LIMIT 1
    """)
    suspend fun pendingFollowUp(): QuestionEntity?

    /**
     * Curated questions matching a theme that have not been answered in the last [sinceEpochSec] seconds.
     */
    @Query("""
        SELECT q.* FROM questions q
        LEFT JOIN entries a ON a.questionId = q.id AND a.isComplete = 1
        WHERE q.source = 'CURATED'
          AND (q.theme = :theme OR :theme IS NULL)
          AND (a.id IS NULL OR a.createdAt < :cutoffIso)
        ORDER BY RANDOM() LIMIT 1
    """)
    suspend fun randomCuratedNotRecent(theme: String?, cutoffIso: String): QuestionEntity?

    @Query("""
        SELECT q.* FROM questions q
        LEFT JOIN entries a ON a.questionId = q.id AND a.isComplete = 1
        WHERE q.source = 'CURATED' AND a.id IS NULL
        ORDER BY RANDOM() LIMIT 1
    """)
    suspend fun randomCuratedUnanswered(): QuestionEntity?

    /**
     * A handful of other curated questions to offer alongside today's, so the
     * prompt can be swiped rather than obeyed.
     *
     * Never-answered questions come first, then ones answered longer ago than
     * [cutoffIso]; within each group the order is random, so the alternatives
     * are not the same five every morning.
     *
     * GROUP BY is load-bearing here, unlike in the LIMIT 1 queries above: a
     * question answered three times joins to three entry rows, which would
     * otherwise return the same prompt three times in one batch.
     */
    @Query("""
        SELECT q.* FROM questions q
        LEFT JOIN entries a ON a.questionId = q.id AND a.isComplete = 1
        WHERE q.source = 'CURATED' AND q.id NOT IN (:excludeIds)
        GROUP BY q.id
        HAVING MAX(a.createdAt) IS NULL OR MAX(a.createdAt) < :cutoffIso
        ORDER BY (MAX(a.createdAt) IS NOT NULL), RANDOM()
        LIMIT :limit
    """)
    suspend fun curatedAlternatives(
        limit: Int,
        excludeIds: List<Long>,
        cutoffIso: String
    ): List<QuestionEntity>
}
