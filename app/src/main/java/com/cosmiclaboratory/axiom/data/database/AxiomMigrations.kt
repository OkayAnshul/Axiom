package com.cosmiclaboratory.axiom.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Real migrations, from version 7 onward.
 *
 * Version 7 is the baseline: every version below it belongs to development
 * builds that never shipped, so those are still handled destructively (see
 * [BASELINE_VERSION] and the builder in DatabaseModule). From 7 up, a schema
 * change must come with a migration here, because from here on losing the
 * table means losing somebody's journal.
 *
 * The rules, learned the usual way:
 *  - Never edit a migration that has shipped. Add the next one instead.
 *  - Every migration needs a schema JSON on both sides; that is what
 *    room.schemaLocation exists for.
 *  - Prefer additive changes. A dropped column cannot be un-dropped by a user
 *    who downgrades.
 *
 * There is deliberately no MigrationTestHelper test. `room-testing` drags
 * `room-migration` onto the androidTest compile classpath, and its
 * @Serializable schema-bundle classes were built against kotlinx-serialization
 * 1.8+, whose GeneratedSerializer dropped typeParametersSerializers(). The
 * serialization compiler plugin in Kotlin 2.0.21 still expects that method, so
 * androidTest fails to compile with AbstractMethodError on
 * FieldBundle${'$'}${'$'}serializer — the same clash that used to block exportSchema,
 * surfacing somewhere new. Bumping the serialization runtime does not help;
 * the mismatch is in the compiler plugin, so the fix is Kotlin 2.1+, which
 * also moves the Compose compiler and is its own piece of work.
 *
 * Until then [coversChainTo] guards the chain in unit tests, and 7→8 was
 * verified by hand on an API 36 emulator: twelve entries and three memories
 * survived the upgrade, and `attachments` was gone.
 */
object AxiomMigrations {

    /** Below this, wipe; at or above it, migrate. */
    const val BASELINE_VERSION = 7

    /**
     * Drops `attachments`, which was created in the v1 schema and never read or
     * written by anything in thirteen phases of work. Dropping it is safe
     * precisely because nothing ever put a row in it; if attachments come back
     * for voice notes later, they arrive as a new table with a new migration.
     */
    val MIGRATION_7_8 = object : Migration(7, 8) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("DROP TABLE IF EXISTS `attachments`")
        }
    }

    /**
     * Clears `sourceId` on memories the local conversation digester wrote.
     *
     * It stored the journal *entry* id there while marking the row
     * `source = CONVERSATION`, so the pointer resolved against the wrong table —
     * entry 5 and message 5 are unrelated rows that happen to share a number.
     * The correct id was never recorded and cannot be recovered, so the repair
     * is to admit that: a null sourceId reads as "we don't know which turn",
     * which is true, where the old value silently asserted a specific wrong one.
     *
     * Nothing else depends on it — `MemorySource` still says these came from a
     * conversation, and the "why do you remember this" copy degrades to the
     * source alone.
     */
    val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "UPDATE `memory_items` SET `sourceId` = NULL " +
                    "WHERE `sourceType` = 'CONVERSATION' AND `sourceId` IS NOT NULL"
            )
        }
    }

    /** Every migration, in order. Passed wholesale to the database builder. */
    val ALL: Array<Migration> = arrayOf(MIGRATION_7_8, MIGRATION_8_9)

    /**
     * True when [ALL] forms an unbroken chain from [BASELINE_VERSION] to
     * [latestVersion]. A gap means some upgrade path throws at open time on a
     * real device, which is the one failure this whole phase exists to prevent.
     */
    fun coversChainTo(latestVersion: Int): Boolean {
        var reached = BASELINE_VERSION
        while (reached < latestVersion) {
            val next = ALL.firstOrNull { it.startVersion == reached } ?: return false
            reached = next.endVersion
        }
        return reached == latestVersion
    }
}

