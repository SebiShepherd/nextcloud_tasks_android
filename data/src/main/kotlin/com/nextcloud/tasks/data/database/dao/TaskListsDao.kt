package com.nextcloud.tasks.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nextcloud.tasks.data.database.entity.TaskListEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface TaskListsDao {
    @Query("SELECT * FROM task_lists")
    fun observeTaskLists(): Flow<List<TaskListEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTaskLists(entities: List<TaskListEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTaskList(entity: TaskListEntity)

    @Query("DELETE FROM task_lists WHERE href IS NULL")
    suspend fun deleteListsWithoutHref()

    @Query("SELECT updated_at FROM task_lists WHERE id = :listId LIMIT 1")
    suspend fun getUpdatedAt(listId: String): Instant?
}
