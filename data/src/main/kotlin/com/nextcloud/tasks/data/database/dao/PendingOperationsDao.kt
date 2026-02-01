package com.nextcloud.tasks.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.nextcloud.tasks.data.database.entity.PendingOperationEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for managing pending operations that need to be synchronized with the server.
 */
@Dao
interface PendingOperationsDao {
    @Query("SELECT * FROM pending_operations WHERE account_id = :accountId ORDER BY created_at ASC")
    fun observePendingOperations(accountId: String): Flow<List<PendingOperationEntity>>

    @Query("SELECT * FROM pending_operations WHERE account_id = :accountId ORDER BY created_at ASC")
    suspend fun getPendingOperations(accountId: String): List<PendingOperationEntity>

    @Query("SELECT * FROM pending_operations WHERE task_id = :taskId ORDER BY created_at DESC LIMIT 1")
    suspend fun getLatestOperationForTask(taskId: String): PendingOperationEntity?

    @Query("SELECT COUNT(*) FROM pending_operations WHERE account_id = :accountId")
    fun observePendingCount(accountId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM pending_operations WHERE account_id = :accountId")
    suspend fun getPendingCount(accountId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(operation: PendingOperationEntity): Long

    @Update
    suspend fun update(operation: PendingOperationEntity)

    @Query("DELETE FROM pending_operations WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM pending_operations WHERE task_id = :taskId")
    suspend fun deleteByTaskId(taskId: String)

    @Query("DELETE FROM pending_operations WHERE account_id = :accountId")
    suspend fun deleteByAccount(accountId: String)

    @Query("UPDATE pending_operations SET retry_count = retry_count + 1, last_error = :error WHERE id = :id")
    suspend fun incrementRetryCount(
        id: Long,
        error: String?,
    )
}
