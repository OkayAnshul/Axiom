package com.cosmiclaboratory.axiom.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.cosmiclaboratory.axiom.data.database.converter.DateTimeConverter
import com.cosmiclaboratory.axiom.data.database.dao.AIInsightDao
import com.cosmiclaboratory.axiom.data.database.dao.AiPromptCacheDao
import com.cosmiclaboratory.axiom.data.database.dao.AttachmentDao
import com.cosmiclaboratory.axiom.data.database.dao.CompanionDao
import com.cosmiclaboratory.axiom.data.database.dao.CompanionThreadStateDao
import com.cosmiclaboratory.axiom.data.database.dao.EntryDao
import com.cosmiclaboratory.axiom.data.database.dao.MemoryItemDao
import com.cosmiclaboratory.axiom.data.database.dao.PersonaDao
import com.cosmiclaboratory.axiom.data.database.dao.PromptPackDao
import com.cosmiclaboratory.axiom.data.database.dao.QuestionDao
import com.cosmiclaboratory.axiom.data.database.dao.TagDao
import com.cosmiclaboratory.axiom.data.database.dao.UserProfileDao
import com.cosmiclaboratory.axiom.data.database.entity.*

/**
 * Version 7 adds entries.emotion — the named feeling inferred from the writing,
 * carrying what the 1..5 mood number cannot.
 *
 * Version 6 added memory_items.dueAt — open loops the companion should circle
 * back on ("how did the interview go?").
 *
 * Version 5 was the companion pivot: memory_items grew provenance columns
 * (timesSeen, createdAt, sourceType, sourceId, userEdited), companion_messages
 * gains a `source` column, companion_thread_state is new, and the never-populated
 * daily_summaries table is gone.
 *
 * Version 4 collapsed `answer_entries` into `entries`; the answer-entry tables and
 * their FTS index are gone.
 *
 * exportSchema is OFF and fallbackToDestructiveMigration is still in place because
 * the app is pre-production. BOTH must change before the first Play release, or an
 * update wipes user journals.
 *
 * Enabling exportSchema currently fails: Room 2.8.2's schema exporter carries
 * serializers compiled against an older kotlinx-serialization than the one on the
 * KSP classpath, so KSP dies with AbstractMethodError on FieldBundle${'$'}${'$'}serializer.
 * Forcing the version on the ksp configuration does not help — room-compiler
 * resolves its own copy inside the KSP worker. Needs a Room bump to fix, which is
 * a pre-release task, not a blocker now.
 */
@Database(
    entities = [
        EntryEntity::class,
        EntryFts::class,
        EntryTagCrossRef::class,
        TagEntity::class,
        UserProfileEntity::class,
        PersonaSettingsEntity::class,
        PromptPackEntity::class,
        QuestionEntity::class,
        AIInsightEntity::class,
        MemoryItemEntity::class,
        AiPromptCacheEntity::class,
        AttachmentEntity::class,
        CompanionMessageEntity::class,
        CompanionThreadStateEntity::class
    ],
    version = 7,
    exportSchema = false
)
@TypeConverters(DateTimeConverter::class)
abstract class AxiomDatabase : RoomDatabase() {

    abstract fun entryDao(): EntryDao
    abstract fun tagDao(): TagDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun personaDao(): PersonaDao
    abstract fun promptPackDao(): PromptPackDao
    abstract fun questionDao(): QuestionDao
    abstract fun aiInsightDao(): AIInsightDao
    abstract fun memoryItemDao(): MemoryItemDao
    abstract fun aiPromptCacheDao(): AiPromptCacheDao
    abstract fun attachmentDao(): AttachmentDao
    abstract fun companionDao(): CompanionDao
    abstract fun companionThreadStateDao(): CompanionThreadStateDao

    companion object {
        const val DATABASE_NAME = "axiom_database"
    }
}
