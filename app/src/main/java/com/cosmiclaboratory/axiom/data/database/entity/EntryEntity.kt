package com.cosmiclaboratory.axiom.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.cosmiclaboratory.axiom.domain.model.Emotion
import com.cosmiclaboratory.axiom.domain.model.Entry
import com.cosmiclaboratory.axiom.domain.model.EntryKind
import com.cosmiclaboratory.axiom.domain.model.Tag
import java.time.LocalDateTime

/**
 * The single entry table. Absorbs what used to live in `answer_entries`:
 * guided-prompt answers are now just entries with [kind] == PROMPTED, carrying
 * their question in [questionId] / [promptSnapshot].
 *
 * Splitting them cost us a companion that could not see journal answers and a
 * summarizer that could not see notes; one table removes both by construction.
 */
@Entity(
    tableName = "entries",
    indices = [
        Index(value = ["kind"]),
        Index(value = ["createdAt"]),
        Index(value = ["mood"]),
        Index(value = ["questionId"]),
        Index(value = ["isComplete"]),
        Index(value = ["isArchived", "createdAt"])
    ]
)
data class EntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    /** Optional — journal entries are frequently untitled. */
    val title: String = "",
    /** Plain text. Absorbs the old AnswerEntryEntity.plainText. */
    val content: String,
    val markdown: String = "",
    val kind: String = EntryKind.FREE_FORM.name,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val version: Int = 1,
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    /** false = draft. PROMPTED entries start false and flip on Done. */
    val isComplete: Boolean = true,
    /** 1..5. The supervised label for the pattern engine — previously never written. */
    val mood: Int? = null,
    /** null => the mood was inferred rather than chosen by the user. */
    val moodCapturedAt: LocalDateTime? = null,
    /**
     * [com.cosmiclaboratory.axiom.domain.model.Emotion] name, inferred from the
     * writing. Carries the feeling the number cannot: "anxious" and "angry" are
     * both mood 2.
     */
    val emotion: String? = null,
    /** 1..5, optional second axis. */
    val energy: Int? = null,
    val questionId: Long? = null,
    /** Absorbs the old AnswerEntryEntity.questionTextSnapshot. */
    val promptSnapshot: String? = null,
    val durationMs: Long = 0L,
    /** Denormalised so day-feature builds don't re-split every entry's text. */
    val wordCount: Int = 0,
    val charCount: Int = 0,
    val sourceLang: String? = null,
    /** 0 = not embedded. Reserved for the opt-in semantic-search tier. */
    val embeddingVersion: Int = 0
)

fun EntryEntity.toDomainModel(tags: List<Tag> = emptyList()): Entry = Entry(
    id = id,
    title = title,
    content = content,
    markdown = markdown,
    kind = EntryKind.fromStorage(kind),
    createdAt = createdAt,
    updatedAt = updatedAt,
    version = version,
    tags = tags,
    isFavorite = isFavorite,
    isArchived = isArchived,
    isComplete = isComplete,
    mood = mood,
    moodCapturedAt = moodCapturedAt,
    emotion = Emotion.fromStorage(emotion),
    energy = energy,
    questionId = questionId,
    promptSnapshot = promptSnapshot,
    durationMs = durationMs,
    wordCount = wordCount,
    charCount = charCount,
    sourceLang = sourceLang,
    embeddingVersion = embeddingVersion
)

fun Entry.toEntity(): EntryEntity = EntryEntity(
    id = id,
    title = title,
    content = content,
    markdown = markdown,
    kind = kind.name,
    createdAt = createdAt,
    updatedAt = updatedAt,
    version = version,
    isFavorite = isFavorite,
    isArchived = isArchived,
    isComplete = isComplete,
    mood = mood,
    moodCapturedAt = moodCapturedAt,
    emotion = emotion?.name,
    energy = energy,
    questionId = questionId,
    promptSnapshot = promptSnapshot,
    durationMs = durationMs,
    // Recomputed on every write so callers can never let them drift.
    wordCount = content.split(Regex("\\s+")).count { it.isNotBlank() },
    charCount = content.length,
    sourceLang = sourceLang,
    embeddingVersion = embeddingVersion
)
