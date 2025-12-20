package com.nextcloud.tasks.domain.repository

import com.nextcloud.tasks.domain.model.Task
import com.nextcloud.tasks.domain.model.TaskList
import kotlinx.coroutines.flow.Flow

interface TasksRepository {
    fun observeTasks(): Flow<List<Task>>

    fun observeLists(): Flow<List<TaskList>>

    suspend fun refresh()

    suspend fun createTask(task: Task): Task

    suspend fun updateTask(task: Task): Task

    suspend fun deleteTask(taskId: String)

    suspend fun upsertList(list: TaskList): TaskList

    suspend fun deleteList(listId: String)

    suspend fun addSampleTasksIfEmpty()
}
