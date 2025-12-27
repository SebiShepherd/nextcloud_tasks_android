package com.nextcloud.tasks.data.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {
    val MIGRATION_1_2 =
        object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add new columns to tasks table for CalDAV support
                db.execSQL("ALTER TABLE tasks ADD COLUMN priority INTEGER")
                db.execSQL("ALTER TABLE tasks ADD COLUMN status TEXT")
                db.execSQL("ALTER TABLE tasks ADD COLUMN completed_at INTEGER")
                db.execSQL("ALTER TABLE tasks ADD COLUMN uid TEXT")
                db.execSQL("ALTER TABLE tasks ADD COLUMN etag TEXT")
                db.execSQL("ALTER TABLE tasks ADD COLUMN href TEXT")

                // Add new columns to task_lists table for CalDAV support
                db.execSQL("ALTER TABLE task_lists ADD COLUMN etag TEXT")
                db.execSQL("ALTER TABLE task_lists ADD COLUMN href TEXT")
                db.execSQL("ALTER TABLE task_lists ADD COLUMN `order` INTEGER")
            }
        }

    val MIGRATION_2_3 =
        object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add parent_uid column for sub-task support
                db.execSQL("ALTER TABLE tasks ADD COLUMN parent_uid TEXT")
            }
        }

    val MIGRATION_3_4 =
        object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add account_id to tasks and task_lists for multi-account support
                // This is a destructive migration - all existing data will be cleared
                // as we cannot retroactively assign accounts to existing data

                // Drop and recreate tasks table
                db.execSQL("DROP TABLE IF EXISTS tasks")
                db.execSQL(
                    """
                    CREATE TABLE tasks (
                        id TEXT PRIMARY KEY NOT NULL,
                        account_id TEXT NOT NULL,
                        list_id TEXT NOT NULL,
                        title TEXT NOT NULL,
                        description TEXT,
                        completed INTEGER NOT NULL,
                        due INTEGER,
                        updated_at INTEGER NOT NULL,
                        priority INTEGER,
                        status TEXT,
                        completed_at INTEGER,
                        uid TEXT,
                        etag TEXT,
                        href TEXT,
                        parent_uid TEXT
                    )
                    """.trimIndent(),
                )

                // Drop and recreate task_lists table
                db.execSQL("DROP TABLE IF EXISTS task_lists")
                db.execSQL(
                    """
                    CREATE TABLE task_lists (
                        id TEXT PRIMARY KEY NOT NULL,
                        account_id TEXT NOT NULL,
                        name TEXT NOT NULL,
                        color TEXT,
                        updated_at INTEGER NOT NULL,
                        etag TEXT,
                        href TEXT,
                        `order` INTEGER
                    )
                    """.trimIndent(),
                )

                // Clear task_tag_cross_ref since tasks were deleted
                db.execSQL("DELETE FROM task_tag_cross_ref")
            }
        }

    val all: Array<Migration> = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
}
