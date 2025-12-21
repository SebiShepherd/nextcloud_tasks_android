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

    val all: Array<Migration> = arrayOf(MIGRATION_1_2)
}
