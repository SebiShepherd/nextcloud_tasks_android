package com.nextcloud.tasks.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.nextcloud.tasks.data.local.entity.TaskListEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskListDao {
    @Query("SELECT * FROM task_lists ORDER BY name")
    fun observeLists(): Flow<List<TaskListEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLists(lists: List<TaskListEntity>)

    @Query("DELETE FROM task_lists")
    suspend fun deleteAll()

    @Query("DELETE FROM task_lists WHERE id NOT IN (:ids)")
    suspend fun deleteMissing(ids: List<String>)

    @Query("DELETE FROM task_lists WHERE id = :listId")
    suspend fun deleteList(listId: String)

    @Transaction
    suspend fun replaceAll(lists: List<TaskListEntity>) {
        if (lists.isEmpty()) {
            deleteAll()
        } else {
            upsertLists(lists)
            deleteMissing(lists.map { it.id })
        }
    }
}
