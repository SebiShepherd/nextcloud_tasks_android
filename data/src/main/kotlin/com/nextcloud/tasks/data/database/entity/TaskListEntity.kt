package com.nextcloud.tasks.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "task_lists")
data class TaskListEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    val name: String,
    val color: String?,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant,
    val etag: String?,
    val href: String?,
    val order: Int?,
)
