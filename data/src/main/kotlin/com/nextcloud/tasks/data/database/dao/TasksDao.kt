package com.nextcloud.tasks.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.nextcloud.tasks.data.database.entity.TaskEntity
import com.nextcloud.tasks.data.database.entity.TaskTagCrossRef
import com.nextcloud.tasks.data.database.model.TaskWithRelations
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Suppress("TooManyFunctions")
@Dao
interface TasksDao {
    @Transaction
    @Query("SELECT * FROM tasks WHERE account_id = :accountId")
    fun observeTasks(accountId: String): Flow<List<TaskWithRelations>>

    @Transaction
    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskWithRelations(taskId: String): TaskWithRelations?

    // @Upsert (not @Insert(REPLACE)): REPLACE deletes and re-inserts the row, giving it a new
    // rowid so tasks jump position under a stable sort with equal keys (#101), and it would trigger
    // ON DELETE CASCADE on any child rows. @Upsert updates in place, preserving rowid and relations.
    @Upsert
    suspend fun upsertTask(task: TaskEntity)

    @Upsert
    suspend fun upsertTasks(tasks: List<TaskEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTaskTagCrossRefs(crossRefs: List<TaskTagCrossRef>)

    @Query("DELETE FROM task_tag_cross_ref WHERE task_id = :taskId")
    suspend fun clearTagsForTask(taskId: String)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTask(taskId: String)

    @Query("DELETE FROM tasks WHERE href IS NULL")
    suspend fun deleteTasksWithoutHref()

    @Query("DELETE FROM tasks WHERE href IS NULL AND id NOT IN (:excludeIds)")
    suspend fun deleteTasksWithoutHrefExcluding(excludeIds: List<String>)

    @Query("DELETE FROM tasks WHERE account_id = :accountId")
    suspend fun deleteTasksByAccount(accountId: String)

    @Query("SELECT updated_at FROM tasks WHERE id = :taskId LIMIT 1")
    suspend fun getTaskUpdatedAt(taskId: String): Instant?

    @Query("SELECT * FROM tasks WHERE id = :taskId LIMIT 1")
    suspend fun getTaskEntity(taskId: String): TaskEntity?

    @Query("SELECT COUNT(*) FROM tasks")
    suspend fun countTasks(): Int

    @Query(
        "DELETE FROM tasks WHERE account_id = :accountId AND list_id NOT IN (:listIds) AND id NOT IN (:protectedIds)",
    )
    suspend fun deleteTasksForRemovedLists(
        accountId: String,
        listIds: List<String>,
        protectedIds: List<String>,
    )

    @Query("DELETE FROM tasks WHERE account_id = :accountId AND list_id NOT IN (:listIds)")
    suspend fun deleteTasksForRemovedListsAll(
        accountId: String,
        listIds: List<String>,
    )

    @Query("DELETE FROM tasks WHERE account_id = :accountId AND href IS NOT NULL")
    suspend fun deleteSyncedTasksByAccount(accountId: String)

    @Query("DELETE FROM tasks WHERE account_id = :accountId AND href IS NOT NULL AND id NOT IN (:excludeIds)")
    suspend fun deleteSyncedTasksByAccountExcluding(
        accountId: String,
        excludeIds: List<String>,
    )

    @Query("DELETE FROM tasks WHERE list_id = :listId")
    suspend fun deleteTasksByListId(listId: String)

    @Query("DELETE FROM task_tag_cross_ref WHERE task_id IN (SELECT id FROM tasks WHERE list_id = :listId)")
    suspend fun deleteTagsByListId(listId: String)
}
