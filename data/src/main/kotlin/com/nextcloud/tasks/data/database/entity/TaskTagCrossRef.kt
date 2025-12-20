package com.nextcloud.tasks.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "task_tag_cross_ref",
    indices = [
        Index(value = ["task_id"]),
        Index(value = ["tag_id"]),
    ],
    primaryKeys = ["task_id", "tag_id"],
)
data class TaskTagCrossRef(
    @ColumnInfo(name = "task_id")
    val taskId: String,
    @ColumnInfo(name = "tag_id")
    val tagId: String,
)
