package com.cosmiclaboratory.axiom.data.database.dao

import androidx.room.*
import com.cosmiclaboratory.axiom.data.database.entity.EntryEntity
import com.cosmiclaboratory.axiom.data.database.entity.EntryTagCrossRef
import com.cosmiclaboratory.axiom.data.database.entity.EntryWithTags
import com.cosmiclaboratory.axiom.data.database.entity.TagEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

/**
 * The single entry DAO. Absorbs everything AnswerEntryDao used to do.
 *
 * Anything reaching an FTS `MATCH` here must already have been through
 * [com.cosmiclaboratory.axiom.data.database.FtsQuerySanitizer] — raw user text
 * throws SQLiteException on ordinary punctuation.
 */
@Dao
interface EntryDao {

    // ---- reads -------------------------------------------------------------

    /**
     * Entries with their tags in ONE query. The previous shape resolved tags with
     * a suspend call per entry inside the Flow's map — N+1 round trips on every
     * emission.
     */
    @Transaction
    @Query("SELECT * FROM entries WHERE isArchived = 0 ORDER BY updatedAt DESC")
    fun observeAllWithTags(): Flow<List<EntryWithTags>>

    /** Every entry, archived included — a backup that silently drops rows is not a backup. */
    @Query("SELECT * FROM entries ORDER BY createdAt ASC")
    suspend fun allForBackup(): List<EntryEntity>

    @Query("SELECT * FROM entry_tag_cross_ref")
    suspend fun allTagCrossRefs(): List<EntryTagCrossRef>

    @Transaction
    @Query("SELECT * FROM entries WHERE isArchived = 0 AND isComplete = 1 ORDER BY createdAt DESC")
    fun observeCompletedWithTags(): Flow<List<EntryWithTags>>

    @Transaction
    @Query("SELECT * FROM entries WHERE isFavorite = 1 AND isArchived = 0 AND isComplete = 1 ORDER BY updatedAt DESC")
    fun observeFavoritesWithTags(): Flow<List<EntryWithTags>>

    @Transaction
    @Query("SELECT * FROM entries WHERE isArchived = 1 ORDER BY updatedAt DESC")
    fun observeArchivedWithTags(): Flow<List<EntryWithTags>>

    @Transaction
    /*
     * `isComplete = 1` is not optional here, and its absence was a real bug: the
     * "All" timeline filters drafts out (they are surfaced as "finish what you
     * started" instead), so a draft that stayed visible under Written or Voice
     * looked like an entry the All tab had lost. Every timeline filter must
     * agree on what counts as an entry.
     */
    @Query("SELECT * FROM entries WHERE kind = :kind AND isArchived = 0 AND isComplete = 1 ORDER BY updatedAt DESC")
    fun observeByKindWithTags(kind: String): Flow<List<EntryWithTags>>

    @Transaction
    @Query(
        """
        SELECT entries.* FROM entries
        INNER JOIN entry_tag_cross_ref ON entries.id = entry_tag_cross_ref.entryId
        WHERE entry_tag_cross_ref.tagId = :tagId
        ORDER BY entries.updatedAt DESC
        """
    )
    fun observeByTagWithTags(tagId: Long): Flow<List<EntryWithTags>>

    @Transaction
    @Query("SELECT * FROM entries WHERE id = :entryId LIMIT 1")
    suspend fun getWithTags(entryId: Long): EntryWithTags?

    @Transaction
    @Query("SELECT * FROM entries WHERE id = :entryId LIMIT 1")
    fun observeWithTags(entryId: Long): Flow<EntryWithTags?>

    @Query("SELECT * FROM entries WHERE id = :entryId LIMIT 1")
    suspend fun getById(entryId: Long): EntryEntity?

    /** Drafts — started and never finished. Feeds Today's "continue writing". */
    @Query(
        """
        SELECT * FROM entries
        WHERE isComplete = 0 AND isArchived = 0 AND (content != '' OR title != '')
        ORDER BY updatedAt DESC LIMIT :limit
        """
    )
    suspend fun drafts(limit: Int): List<EntryEntity>

    @Query("SELECT * FROM entries WHERE createdAt >= :start AND createdAt < :end AND isArchived = 0 ORDER BY createdAt DESC")
    suspend fun forDay(start: LocalDateTime, end: LocalDateTime): List<EntryEntity>

    @Query("SELECT DISTINCT date(createdAt) FROM entries WHERE isArchived = 0 ORDER BY createdAt DESC")
    suspend fun distinctEntryDates(): List<String>

    /** Finished entries only — a half-typed draft is not yet a moment kept. */
    @Query("SELECT COUNT(*) FROM entries WHERE isArchived = 0 AND isComplete = 1")
    suspend fun count(): Int

    // ---- search ------------------------------------------------------------

    @Transaction
    @Query(
        """
        SELECT entries.* FROM entries
        JOIN entry_fts ON entries.id = entry_fts.docid
        WHERE entry_fts MATCH :query AND entries.isArchived = 0
        ORDER BY entries.updatedAt DESC
        """
    )
    suspend fun search(query: String): List<EntryWithTags>

    /** Completed entries that already carry an AI insight — context for prompt generation. */
    @Query(
        """
        SELECT e.* FROM entries e
        JOIN ai_insights i ON i.entryId = e.id
        WHERE e.isComplete = 1
        ORDER BY e.createdAt DESC LIMIT :limit
        """
    )
    suspend fun recentSummarized(limit: Int): List<EntryEntity>

    // ---- writes ------------------------------------------------------------

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: EntryEntity): Long

    @Update
    suspend fun update(entry: EntryEntity)

    @Delete
    suspend fun delete(entry: EntryEntity)

    @Query("DELETE FROM entries WHERE id = :entryId")
    suspend fun deleteById(entryId: Long)

    @Query("UPDATE entries SET isComplete = 1, updatedAt = :now WHERE id = :entryId")
    suspend fun markComplete(entryId: Long, now: LocalDateTime)

    /**
     * Rescues writing stranded as a draft.
     *
     * Backing out of the composer used to leave a draft, and drafts are kept out
     * of the timeline — so anything written before that changed is now invisible
     * in every filter. Nobody should have to learn that their own words are
     * hidden behind a state they never chose.
     *
     * The [cutoff] protects the entry currently being typed: autosave writes a
     * draft row from the first keystroke, and completing that mid-sentence would
     * be its own small betrayal. `updatedAt` is left alone so the rescue does not
     * reorder someone's timeline.
     */
    @Query(
        "UPDATE entries SET isComplete = 1 WHERE isComplete = 0 " +
            "AND (trim(title) != '' OR trim(content) != '') AND updatedAt < :cutoff"
    )
    suspend fun completeAbandonedDrafts(cutoff: LocalDateTime): Int

    @Query("UPDATE entries SET mood = :mood, moodCapturedAt = :capturedAt, updatedAt = :capturedAt WHERE id = :entryId")
    suspend fun setMood(entryId: Long, mood: Int?, capturedAt: LocalDateTime)

    /**
     * Records an inferred feeling, but only where the user has not chosen one.
     * The `mood IS NULL` guard is in the statement rather than in a read-then-
     * write so a mood tapped while the summarizer was in flight can never be
     * silently overwritten by the model's guess. moodCapturedAt stays null,
     * which is how the rest of the app tells inferred from chosen.
     */
    @Query("UPDATE entries SET mood = :mood, emotion = :emotion WHERE id = :entryId AND mood IS NULL")
    suspend fun setInferredMood(entryId: Long, mood: Int, emotion: String): Int

    /**
     * The on-device classifier's output. It predicts a number on the 1..5 scale
     * and has no opinion about which named feeling produced it, so [emotion]
     * stays untouched rather than being filled with a guess.
     */
    @Query("UPDATE entries SET mood = :mood WHERE id = :entryId AND mood IS NULL")
    suspend fun setPredictedMood(entryId: Long, mood: Int): Int

    /** Entries whose mood the user chose — the only trustworthy training labels. */
    @Query(
        "SELECT * FROM entries WHERE mood IS NOT NULL AND moodCapturedAt IS NOT NULL " +
            "AND isComplete = 1 AND content != '' ORDER BY createdAt DESC LIMIT :limit"
    )
    suspend fun userLabelledForTraining(limit: Int = 500): List<EntryEntity>

    @Query("UPDATE entries SET isFavorite = :favorite, updatedAt = :now WHERE id = :entryId")
    suspend fun setFavorite(entryId: Long, favorite: Boolean, now: LocalDateTime)

    @Query("UPDATE entries SET isArchived = :archived, updatedAt = :now WHERE id = :entryId")
    suspend fun setArchived(entryId: Long, archived: Boolean, now: LocalDateTime)

    // ---- tags --------------------------------------------------------------

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTagCrossRef(crossRef: EntryTagCrossRef)

    /**
     * Required for tag edits to be lossless. The old updateNote only ever
     * inserted cross-refs, so removing a tag from an entry was impossible.
     */
    @Query("DELETE FROM entry_tag_cross_ref WHERE entryId = :entryId")
    suspend fun deleteTagCrossRefsFor(entryId: Long)

    @Query(
        """
        SELECT tags.* FROM tags
        INNER JOIN entry_tag_cross_ref ON tags.id = entry_tag_cross_ref.tagId
        WHERE entry_tag_cross_ref.entryId = :entryId
        """
    )
    suspend fun tagsFor(entryId: Long): List<TagEntity>
}
