package com.cosmiclaboratory.axiom.data.database

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the upgrade path. The failure this catches is the easy one to commit:
 * bumping @Database(version = …) for a new column and forgetting the migration,
 * which throws at open time on every device that already had data.
 */
class AxiomMigrationsTest {

    @Test
    fun `migrations cover every version from the baseline to the current schema`() {
        assertTrue(
            "AxiomDatabase is at version ${AxiomDatabase.LATEST_VERSION} but " +
                "AxiomMigrations.ALL does not reach it — add the missing Migration",
            AxiomMigrations.coversChainTo(AxiomDatabase.LATEST_VERSION)
        )
    }

    @Test
    fun `a version beyond the declared chain is reported as uncovered`() {
        assertFalse(AxiomMigrations.coversChainTo(AxiomDatabase.LATEST_VERSION + 1))
    }

    @Test
    fun `the baseline itself needs no migration`() {
        assertTrue(AxiomMigrations.coversChainTo(AxiomMigrations.BASELINE_VERSION))
    }

    @Test
    fun `every migration starts at or above the baseline and moves forward`() {
        AxiomMigrations.ALL.forEach { migration ->
            assertTrue(
                "migration ${migration.startVersion}->${migration.endVersion} starts below the baseline",
                migration.startVersion >= AxiomMigrations.BASELINE_VERSION
            )
            assertTrue(
                "migration ${migration.startVersion}->${migration.endVersion} does not move forward",
                migration.endVersion > migration.startVersion
            )
        }
    }

    @Test
    fun `no two migrations start from the same version`() {
        val starts = AxiomMigrations.ALL.map { it.startVersion }
        assertEquals(starts.size, starts.distinct().size)
    }
}
