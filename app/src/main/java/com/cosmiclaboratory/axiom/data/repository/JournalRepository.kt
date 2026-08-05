package com.cosmiclaboratory.axiom.data.repository

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.cosmiclaboratory.axiom.data.database.FtsQuerySanitizer
import com.cosmiclaboratory.axiom.data.database.dao.AIInsightDao
import com.cosmiclaboratory.axiom.data.database.dao.EntryDao
import com.cosmiclaboratory.axiom.data.database.dao.TagDao
import com.cosmiclaboratory.axiom.data.database.entity.AIInsightEntity
import com.cosmiclaboratory.axiom.data.database.entity.EntryTagCrossRef
import com.cosmiclaboratory.axiom.data.database.entity.toDomainModel
import com.cosmiclaboratory.axiom.data.database.entity.toEntity
import com.cosmiclaboratory.axiom.domain.model.AIInsight
import com.cosmiclaboratory.axiom.domain.model.Emotion
import com.cosmiclaboratory.axiom.domain.model.Entry
import com.cosmiclaboratory.axiom.domain.model.EntryKind
import com.cosmiclaboratory.axiom.domain.model.Tag
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The single repository over entries, their tags and their AI insights.
 *
 * Replaces NotesRepository and the old answer-entry half of this class.
 * One corpus means the companion can finally see guided journal answers and the
 * summarizer can finally see free-form notes — both were structurally blind
 * before.
 */
@Singleton
class JournalRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val entryDao: EntryDao,
    private val insightDao: AIInsightDao,
    private val tagDao: TagDao
) {

    /**
     * Home-screen widgets read this repository directly, but nothing ever told
     * them the data changed — they only refreshed on their 30-minute tick, so a
     * just-written entry was invisible for up to half an hour. Every mutation
     * now nudges them.
     */
    private suspend fun refreshWidgets() {
        try {
            com.cosmiclaboratory.axiom.widget.NotesListWidget().updateAll(context)
            com.cosmiclaboratory.axiom.widget.QuickNoteWidget().updateAll(context)
        } catch (e: Exception) {
            // A widget that isn't placed, or a launcher that rejects the update,
            // must never fail the save that triggered it.
        }
    }

    // ---- observing ---------------------------------------------------------

    fun observeAll(): Flow<List<Entry>> =
        entryDao.observeAllWithTags().map { rows -> rows.map { it.toDomainModel() } }

    fun observeCompleted(): Flow<List<Entry>> =
        entryDao.observeCompletedWithTags().map { rows -> rows.map { it.toDomainModel() } }

    fun observeFavorites(): Flow<List<Entry>> =
        entryDao.observeFavoritesWithTags().map { rows -> rows.map { it.toDomainModel() } }

    fun observeArchived(): Flow<List<Entry>> =
        entryDao.observeArchivedWithTags().map { rows -> rows.map { it.toDomainModel() } }

    fun observeByKind(kind: EntryKind): Flow<List<Entry>> =
        entryDao.observeByKindWithTags(kind.name).map { rows -> rows.map { it.toDomainModel() } }

    fun observeByTag(tagId: Long): Flow<List<Entry>> =
        entryDao.observeByTagWithTags(tagId).map { rows -> rows.map { it.toDomainModel() } }

    fun observeById(id: Long): Flow<Entry?> =
        entryDao.observeWithTags(id).map { it?.toDomainModel() }

    suspend fun getById(id: Long): Entry? = entryDao.getWithTags(id)?.toDomainModel()

    suspend fun drafts(limit: Int = 5): List<Entry> =
        entryDao.drafts(limit).map { it.toDomainModel() }

    suspend fun forDay(date: LocalDate): List<Entry> =
        entryDao.forDay(date.atStartOfDay(), date.plusDays(1).atStartOfDay())
            .map { it.toDomainModel() }

    suspend fun entryDates(): List<LocalDate> =
        entryDao.distinctEntryDates().mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }

    suspend fun count(): Int = entryDao.count()

    // ---- writing -----------------------------------------------------------

    suspend fun upsert(entry: Entry): Long {
        val now = LocalDateTime.now()
        val row = entry.copy(
            createdAt = if (entry.id == 0L) now else entry.createdAt,
            updatedAt = now
        ).toEntity()
        val id = entryDao.insert(row)
        val entryId = if (entry.id == 0L) id else entry.id
        syncTags(entryId, entry.tags)
        refreshWidgets()
        return entryId
    }

    suspend fun delete(entry: Entry) {
        entryDao.delete(entry.toEntity())
        refreshWidgets()
    }

    suspend fun deleteById(id: Long) {
        entryDao.deleteById(id)
        refreshWidgets()
    }

    /**
     * One-time rescue for entries stranded as drafts by the old back-exits-as-draft
     * behaviour. Safe to run on every launch: it only touches drafts that have not
     * been edited for [idleMinutes], so an in-progress entry is never swept up.
     */
    suspend fun completeAbandonedDrafts(idleMinutes: Long = 60): Int {
        val rescued = entryDao.completeAbandonedDrafts(
            LocalDateTime.now().minusMinutes(idleMinutes)
        )
        if (rescued > 0) refreshWidgets()
        return rescued
    }

    /**
     * Fills in a title the user never wrote. Reads the row first and checks
     * again, so a title typed while the background job was in flight wins —
     * their words are never overwritten by a guess.
     */
    suspend fun setTitleIfBlank(id: Long, title: String) {
        if (title.isBlank()) return
        val existing = entryDao.getWithTags(id)?.toDomainModel() ?: return
        if (existing.title.isNotBlank()) return
        entryDao.update(existing.copy(title = title, updatedAt = LocalDateTime.now()).toEntity())
        refreshWidgets()
    }

    /**
     * Promotes extracted themes to real tags.
     *
     * Themes were being asked for, stored as a CSV, rendered as chips and then
     * going nowhere — not searchable, not filterable, invisible to the Patterns
     * tab. As tags they become all three for free. Matched case-insensitively so
     * "Work" and "work" never become two tags.
     */
    suspend fun attachThemeTags(entryId: Long, themes: List<String>) {
        val clean = themes.map { it.trim().lowercase() }
            .filter { it.length in MIN_TAG_LENGTH..MAX_TAG_LENGTH }
            .distinct()
        if (clean.isEmpty()) return
        val entry = entryDao.getWithTags(entryId)?.toDomainModel() ?: return
        val held = entry.tags.map { it.name.lowercase() }.toSet()
        val existingByName = tagDao.getAllTags().first()
            .map { it.toDomainModel() }
            .associateBy { it.name.lowercase() }

        val toAttach = clean.filter { it !in held }.map { name ->
            existingByName[name] ?: Tag(id = tagDao.insertTag(Tag(name = name).toEntity()), name = name)
        }
        if (toAttach.isEmpty()) return
        syncTags(entryId, entry.tags + toAttach)
    }

    suspend fun markComplete(id: Long) {
        entryDao.markComplete(id, LocalDateTime.now())
        refreshWidgets()
    }

    suspend fun setMood(id: Long, mood: Int?) =
        entryDao.setMood(id, mood, LocalDateTime.now())

    /** Records an inferred feeling; a mood the user chose always wins. */
    suspend fun setInferredMood(id: Long, emotion: Emotion) {
        entryDao.setInferredMood(id, emotion.valence, emotion.name)
    }

    /** Records the on-device classifier's number, leaving `emotion` unset. */
    suspend fun setPredictedMood(id: Long, mood: Int) {
        entryDao.setPredictedMood(id, mood)
    }

    /** (text, mood) pairs the user labelled themselves, for on-device training. */
    suspend fun moodTrainingSamples(): List<Pair<String, Int>> =
        entryDao.userLabelledForTraining().mapNotNull { row ->
            val mood = row.mood ?: return@mapNotNull null
            row.content.ifBlank { row.markdown }.takeIf { it.isNotBlank() }?.let { it to mood }
        }

    suspend fun setFavorite(id: Long, favorite: Boolean) =
        entryDao.setFavorite(id, favorite, LocalDateTime.now())

    suspend fun setArchived(id: Long, archived: Boolean) =
        entryDao.setArchived(id, archived, LocalDateTime.now())

    /**
     * Replaces an entry's tags wholesale. The previous implementation only
     * inserted cross-refs and never deleted them, so a removed tag stayed
     * attached forever.
     */
    private suspend fun syncTags(entryId: Long, tags: List<Tag>) {
        entryDao.deleteTagCrossRefsFor(entryId)
        tags.forEach { tag ->
            val tagId = if (tag.id == 0L) tagDao.insertTag(tag.toEntity()) else tag.id
            entryDao.insertTagCrossRef(EntryTagCrossRef(entryId = entryId, tagId = tagId))
        }
    }

    // ---- search ------------------------------------------------------------

    /** Free-text search. Input is sanitized here; callers pass raw user text. */
    suspend fun search(rawQuery: String): List<Entry> {
        val q = FtsQuerySanitizer.forSearch(rawQuery)
        if (q.isBlank()) return emptyList()
        return entryDao.search(q).map { it.toDomainModel() }
    }

    /** Retrieval for the AI companion — stopwords dropped, no prefix globbing. */
    suspend fun searchForRetrieval(rawQuestion: String): List<Entry> {
        val q = FtsQuerySanitizer.forRetrieval(rawQuestion)
        if (q.isBlank()) return emptyList()
        return entryDao.search(q).map { it.toDomainModel() }
    }

    /** Every non-archived entry, for building the on-device semantic index. */
    suspend fun allForIndexing(): List<Entry> =
        entryDao.allForBackup().filterNot { it.isArchived }.map { it.toDomainModel() }

    // ---- tags --------------------------------------------------------------

    fun observeAllTags(): Flow<List<Tag>> =
        tagDao.getAllTags().map { rows -> rows.map { it.toDomainModel() } }

    suspend fun insertTag(tag: Tag): Long = tagDao.insertTag(tag.toEntity())

    suspend fun deleteTag(tag: Tag) = tagDao.deleteTag(tag.toEntity())

    // ---- AI insights -------------------------------------------------------

    fun observeInsight(entryId: Long): Flow<AIInsight?> =
        insightDao.observeByEntryId(entryId).map { it?.toDomainModel() }

    suspend fun getInsight(entryId: Long): AIInsight? =
        insightDao.getByEntryId(entryId)?.toDomainModel()

    suspend fun saveInsight(insight: AIInsightEntity): Long = insightDao.insert(insight)

    /** Summaries of recently-summarized entries, used as context for prompt generation. */
    suspend fun recentSummariesForContext(limit: Int = 3): List<String> =
        entryDao.recentSummarized(limit).mapNotNull { insightDao.getByEntryId(it.id)?.summary }

    private companion object {
        /** Two-letter "tags" are noise; anything long is a sentence, not a tag. */
        const val MIN_TAG_LENGTH = 3
        const val MAX_TAG_LENGTH = 24
    }

}
