package com.cosmiclaboratory.axiom.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.cosmiclaboratory.axiom.data.database.converter.DateTimeConverter
import com.cosmiclaboratory.axiom.data.database.dao.AIInsightDao
import com.cosmiclaboratory.axiom.data.database.dao.AiPromptCacheDao
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
 * Version 8 drops `attachments`, which was never read or written. It is the
 * first version reached by a real migration rather than by wiping the device:
 * see [AxiomMigrations], where version 7 is the baseline from which user data
 * is preserved.
 *
 * Version 7 added entries.emotion — the named feeling inferred from the writing,
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
 * exportSchema stays OFF, and the earlier note here blamed the wrong thing. It
 * is not a Room version problem: Room 2.8.4 exports a schema happily on a clean
 * tree, then dies the moment a previous schema exists and has to be READ back
 * for comparison — AbstractMethodError on FieldBundle${'$'}${'$'}serializer. Room's
 * schema bundles are compiled against kotlinx-serialization 1.8+, whose
 * GeneratedSerializer no longer declares typeParametersSerializers(), while the
 * serialization compiler plugin bundled with Kotlin 2.0.21 still expects it.
 * Bumping the serialization runtime does not help; the mismatch is in the
 * plugin. The fix is Kotlin 2.1+, which also moves the Compose compiler, so it
 * is its own piece of work.
 *
 * Hand-written migrations do not need exported schemas — see [AxiomMigrations],
 * which is what actually keeps user data safe across an upgrade.
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
        CompanionMessageEntity::class,
        CompanionThreadStateEntity::class
    ],
    version = 8,
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
    abstract fun companionDao(): CompanionDao
    abstract fun companionThreadStateDao(): CompanionThreadStateDao

    companion object {
        const val DATABASE_NAME = "axiom_database"

        /** Keep in step with the @Database version; migration tests assert against it. */
        const val LATEST_VERSION = 8
    }
}
