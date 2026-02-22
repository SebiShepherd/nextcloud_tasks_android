package com.nextcloud.tasks.data.database.migrations

import kotlin.test.Test
import kotlin.test.assertEquals

class DatabaseMigrationsTest {
    @Test
    fun `all migrations contains all five migrations in order`() {
        val all = DatabaseMigrations.all

        assertEquals(5, all.size)
        assertEquals(1, all[0].startVersion)
        assertEquals(2, all[0].endVersion)
        assertEquals(2, all[1].startVersion)
        assertEquals(3, all[1].endVersion)
        assertEquals(3, all[2].startVersion)
        assertEquals(4, all[2].endVersion)
        assertEquals(4, all[3].startVersion)
        assertEquals(5, all[3].endVersion)
        assertEquals(5, all[4].startVersion)
        assertEquals(6, all[4].endVersion)
    }

    @Test
    fun `MIGRATION_1_2 has correct version range`() {
        assertEquals(1, DatabaseMigrations.MIGRATION_1_2.startVersion)
        assertEquals(2, DatabaseMigrations.MIGRATION_1_2.endVersion)
    }

    @Test
    fun `MIGRATION_2_3 has correct version range`() {
        assertEquals(2, DatabaseMigrations.MIGRATION_2_3.startVersion)
        assertEquals(3, DatabaseMigrations.MIGRATION_2_3.endVersion)
    }

    @Test
    fun `MIGRATION_3_4 has correct version range`() {
        assertEquals(3, DatabaseMigrations.MIGRATION_3_4.startVersion)
        assertEquals(4, DatabaseMigrations.MIGRATION_3_4.endVersion)
    }

    @Test
    fun `MIGRATION_4_5 has correct version range`() {
        assertEquals(4, DatabaseMigrations.MIGRATION_4_5.startVersion)
        assertEquals(5, DatabaseMigrations.MIGRATION_4_5.endVersion)
    }

    @Test
    fun `MIGRATION_5_6 has correct version range`() {
        assertEquals(5, DatabaseMigrations.MIGRATION_5_6.startVersion)
        assertEquals(6, DatabaseMigrations.MIGRATION_5_6.endVersion)
    }

    @Test
    fun `migrations are contiguous`() {
        val all = DatabaseMigrations.all
        for (i in 0 until all.size - 1) {
            assertEquals(
                all[i].endVersion,
                all[i + 1].startVersion,
                "Migration ${all[i].startVersion}->${all[i].endVersion} " +
                    "should connect to ${all[i + 1].startVersion}->${all[i + 1].endVersion}",
            )
        }
    }
}
