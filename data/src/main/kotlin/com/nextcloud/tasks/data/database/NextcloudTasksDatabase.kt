package com.nextcloud.tasks.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.nextcloud.tasks.data.database.converter.InstantTypeConverter
import com.nextcloud.tasks.data.database.dao.PendingOperationsDao
import com.nextcloud.tasks.data.database.dao.TagsDao
import com.nextcloud.tasks.data.database.dao.TaskListsDao
import com.nextcloud.tasks.data.database.dao.TasksDao
import com.nextcloud.tasks.data.database.entity.PendingOperationEntity
import com.nextcloud.tasks.data.database.entity.TagEntity
import com.nextcloud.tasks.data.database.entity.TaskEntity
import com.nextcloud.tasks.data.database.entity.TaskListEntity
import com.nextcloud.tasks.data.database.entity.TaskTagCrossRef

@Database(
    entities = [
        TaskEntity::class,
        TaskListEntity::class,
        TagEntity::class,
        TaskTagCrossRef::class,
        PendingOperationEntity::class,
    ],
    version = 6,
    exportSchema = true,
)
@TypeConverters(InstantTypeConverter::class)
abstract class NextcloudTasksDatabase : RoomDatabase() {
    abstract fun tasksDao(): TasksDao

    abstract fun taskListsDao(): TaskListsDao

    abstract fun tagsDao(): TagsDao

    abstract fun pendingOperationsDao(): PendingOperationsDao
}
