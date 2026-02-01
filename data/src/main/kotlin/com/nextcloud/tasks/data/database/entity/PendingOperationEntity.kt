package com.nextcloud.tasks.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * Represents a pending operation that needs to be synchronized with the server.
 * Used for offline-first functionality to queue operations when the device is offline.
 */
@Entity(tableName = "pending_operations")
data class PendingOperationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "account_id")
    val accountId: String,
    @ColumnInfo(name = "task_id")
    val taskId: String,
    @ColumnInfo(name = "operation_type")
    val operationType: String,
    @ColumnInfo(name = "payload")
    val payload: String,
    @ColumnInfo(name = "created_at")
    val createdAt: Instant,
    @ColumnInfo(name = "retry_count")
    val retryCount: Int = 0,
    @ColumnInfo(name = "last_error")
    val lastError: String? = null,
) {
    companion object {
        const val OPERATION_UPDATE = "UPDATE"
        const val OPERATION_CREATE = "CREATE"
        const val OPERATION_DELETE = "DELETE"
    }
}
