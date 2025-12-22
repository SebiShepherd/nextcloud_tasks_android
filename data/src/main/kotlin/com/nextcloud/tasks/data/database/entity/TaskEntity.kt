package com.nextcloud.tasks.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "list_id")
    val listId: String,
    val title: String,
    val description: String?,
    val completed: Boolean,
    val due: Instant?,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant,
    val priority: Int?,
    val status: String?,
    @ColumnInfo(name = "completed_at")
    val completedAt: Instant?,
    val uid: String?,
    val etag: String?,
    val href: String?,
    @ColumnInfo(name = "parent_uid")
    val parentUid: String?,
)
