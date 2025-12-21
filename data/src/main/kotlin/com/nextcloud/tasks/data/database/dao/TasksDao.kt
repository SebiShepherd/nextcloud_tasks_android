package com.nextcloud.tasks.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.nextcloud.tasks.data.database.entity.TaskEntity
import com.nextcloud.tasks.data.database.entity.TaskTagCrossRef
import com.nextcloud.tasks.data.database.model.TaskWithRelations
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface TasksDao {
    @Transaction
    @Query("SELECT * FROM tasks")
    fun observeTasks(): Flow<List<TaskWithRelations>>

    @Transaction
    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskWithRelations(taskId: String): TaskWithRelations?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTask(task: TaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTasks(tasks: List<TaskEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTaskTagCrossRefs(crossRefs: List<TaskTagCrossRef>)

    @Query("DELETE FROM task_tag_cross_ref WHERE task_id = :taskId")
    suspend fun clearTagsForTask(taskId: String)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTask(taskId: String)

    @Query("DELETE FROM tasks")
    suspend fun clearTasks()

    @Query("DELETE FROM task_tag_cross_ref")
    suspend fun clearTaskTagCrossRefs()

    @Query("SELECT updated_at FROM tasks WHERE id = :taskId LIMIT 1")
    suspend fun getTaskUpdatedAt(taskId: String): Instant?

    @Query("SELECT COUNT(*) FROM tasks")
    suspend fun countTasks(): Int
}
