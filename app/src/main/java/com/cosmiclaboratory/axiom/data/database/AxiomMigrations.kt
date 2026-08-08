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

    /**
     * Deletes theme memories whose subject is a filler word.
     *
     * The digester's frequency counter decided that someone "keeps coming back
     * to message" and "keeps coming back to long". A length floor and a stop
     * list were added later, but only at write time, so the rows already stored
     * kept going out in every prompt — the model was told these were recurring
     * themes in a person's life, alongside real facts about them.
     *
     * The word list below is **deliberately a frozen copy** of
     * [com.cosmiclaboratory.axiom.domain.memory.MemoryHygiene.NON_THEMES] rather
     * than a reference to it. A migration has to produce the same result on
     * every device forever; one that read a live constant would quietly change
     * what a 9→10 upgrade does the day someone edits that list. The duplication
     * is the point, not an oversight.
     *
     * Two shapes of junk, in opposite directions: a theme too thin to mean
     * anything, and any row long enough to be a transcript rather than a
     * memory. Short facts and events are left alone — "They cooked dinner." is
     * short because life is short, and widening that would delete real memories
     * to fix a cosmetic problem.
     */
    val MIGRATION_9_10 = object : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
            val nonThemes = listOf(
                "thing", "things", "really", "actually", "maybe", "think", "thought",
                "feel", "feeling", "going", "still", "quite", "pretty", "little",
                "message", "number", "today", "yesterday", "tomorrow", "again",
                "something", "anything", "nothing", "everything", "someone", "people"
            )
            val prefix = "Keeps coming back to "
            // Subject shorter than the five-character floor.
            db.execSQL(
                "DELETE FROM `memory_items` WHERE `kind` = 'THEME' " +
                    "AND `text` LIKE '$prefix%' " +
                    "AND length(trim(rtrim(substr(`text`, ${prefix.length + 1}), '.'))) < 5"
            )
            // A memory that is a wall of text was never a memory. The digester
            // used to join every turn before hunting for commitments, storing a
            // whole conversation as one "sentence"; a truncated version of that
            // was still being sent. 160 sits between the longest real memory
            // seen (~55 chars) and the 200-char write cap.
            db.execSQL("DELETE FROM `memory_items` WHERE length(trim(`text`)) > 160")
            nonThemes.forEach { word ->
                db.execSQL(
                    "DELETE FROM `memory_items` WHERE `kind` = 'THEME' " +
                        "AND lower(trim(rtrim(substr(`text`, ${prefix.length + 1}), '.'))) = '$word'"
                )
            }
        }
    }

    /**
     * Adds `companion_messages.questionId`, so a daily opener remembers which
     * curated question it is asking.
     *
     * Questions have never carried an "answered" flag — [dao.QuestionDao] decides
     * it by joining `entries.questionId`, which is self-healing: delete the entry
     * and the question returns to rotation with no cleanup code. The catch is
     * that the join only works if the id actually reaches the entry, and it never
     * did. The opener persisted the question's *text* and threw the id away, so
     * `entries.questionId` was always null, nothing ever matched, and the fifty
     * curated prompts recycled indefinitely.
     *
     * Nullable and additive: existing openers keep asking their question, they
     * simply cannot be credited retroactively — the text is all that was kept, and
     * guessing which question it came from by string-matching would be a fine way
     * to mark the wrong one answered.
     */
    val MIGRATION_10_11 = object : Migration(10, 11) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `companion_messages` ADD COLUMN `questionId` INTEGER")
        }
    }

    /**
     * Two additions that happen to land together.
     *
     * **`companion_thread_state.parkedUpToMessageId`** — the conversation now
     * starts blank on every fresh launch, with what you said earlier still there
     * behind a "pick up where we left off" chip until its window closes. That
     * needs to distinguish "hidden" from "gone", and this table already tracks
     * exactly this kind of watermark for the summarizer and the digester, so
     * parking is a third one rather than a new mechanism.
     *
     * One column, not two: the retention window is measured from the parked
     * conversation's last message, which `companion_messages.createdAt` already
     * records, so there is nothing to store about when parking happened.
     *
     * **`memory_items` indices** — the table had none at all. Every single
     * companion turn ran `SELECT *` across it and sorted in memory, which is
     * fine at a few hundred rows and is on the critical path of a reply at a few
     * thousand. `lastSeenAt` backs decay ordering, `kind` backs the per-kind
     * caps, and `dueAt` backs the open-loop lookup that runs on every check-in.
     * Indices are pure addition: no row changes, and a downgrade just ignores
     * them.
     */
    val MIGRATION_11_12 = object : Migration(11, 12) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE `companion_thread_state` " +
                    "ADD COLUMN `parkedUpToMessageId` INTEGER NOT NULL DEFAULT 0"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_memory_items_lastSeenAt` " +
                    "ON `memory_items` (`lastSeenAt`)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_memory_items_kind` " +
                    "ON `memory_items` (`kind`)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_memory_items_dueAt` " +
                    "ON `memory_items` (`dueAt`)"
            )
        }
    }

    /** Every migration, in order. Passed wholesale to the database builder. */
    val ALL: Array<Migration> =
        arrayOf(MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12)

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

