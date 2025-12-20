package com.nextcloud.tasks.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.nextcloud.tasks.data.local.entity.TagEntity
import com.nextcloud.tasks.data.local.entity.TaskEntity
import com.nextcloud.tasks.data.local.entity.TaskTagCrossRef
import com.nextcloud.tasks.data.local.entity.TaskWithRelations
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Transaction
    @Query("SELECT * FROM tasks ORDER BY last_modified DESC")
    fun observeTasks(): Flow<List<TaskWithRelations>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTasks(tasks: List<TaskEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTags(tags: List<TagEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRefs(refs: List<TaskTagCrossRef>)

    @Query("DELETE FROM task_tag_cross_ref WHERE task_id = :taskId")
    suspend fun clearRefs(taskId: String)

    @Query("SELECT COUNT(*) FROM tasks")
    suspend fun countTasks(): Int

    @Query("DELETE FROM task_tag_cross_ref")
    suspend fun clearAllRefs()

    @Query("DELETE FROM tags")
    suspend fun clearTags()

    @Query("DELETE FROM tasks")
    suspend fun clearTasks()

    @Query("DELETE FROM tasks WHERE list_id = :listId AND id NOT IN (:taskIds)")
    suspend fun deleteMissing(listId: String, taskIds: List<String>)

    @Query("DELETE FROM tasks WHERE list_id = :listId")
    suspend fun deleteByList(listId: String)

    @Query("SELECT * FROM tasks WHERE id IN (:ids)")
    suspend fun findByIds(ids: List<String>): List<TaskEntity>

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTask(taskId: String)

    @Transaction
    suspend fun upsertTasksWithTags(tasks: List<TaskWithTagsPayload>) {
        upsertTasks(tasks.map { it.task })
        val allTags = tasks.flatMap { it.tags }
        if (allTags.isNotEmpty()) {
            upsertTags(allTags)
        }

        tasks.forEach { payload ->
            clearRefs(payload.task.id)
            val refs = payload.tags.map { tag -> TaskTagCrossRef(taskId = payload.task.id, tagId = tag.id) }
            if (refs.isNotEmpty()) {
                upsertRefs(refs)
            }
        }
    }
}

data class TaskWithTagsPayload(
    val task: TaskEntity,
    val tags: List<TagEntity>,
)
