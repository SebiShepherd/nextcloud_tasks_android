package com.nextcloud.tasks.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.nextcloud.tasks.data.local.dao.TaskDao
import com.nextcloud.tasks.data.local.dao.TaskListDao
import com.nextcloud.tasks.data.local.entity.TagEntity
import com.nextcloud.tasks.data.local.entity.TaskEntity
import com.nextcloud.tasks.data.local.entity.TaskListEntity
import com.nextcloud.tasks.data.local.entity.TaskTagCrossRef

@Database(
    entities = [
        TaskEntity::class,
        TaskListEntity::class,
        TagEntity::class,
        TaskTagCrossRef::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class NextcloudTasksDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun taskListDao(): TaskListDao
}
