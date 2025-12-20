package com.nextcloud.tasks.domain.repository

import com.nextcloud.tasks.domain.model.Task
import com.nextcloud.tasks.domain.model.TaskDraft
import com.nextcloud.tasks.domain.model.TaskList
import com.nextcloud.tasks.domain.model.Tag
import kotlinx.coroutines.flow.Flow

interface TasksRepository {
    fun observeTasks(): Flow<List<Task>>

    fun observeLists(): Flow<List<TaskList>>

    fun observeTags(): Flow<List<Tag>>

    suspend fun getTask(id: String): Task?

    suspend fun createTask(draft: TaskDraft): Task

    suspend fun updateTask(task: Task): Task

    suspend fun deleteTask(taskId: String)

    suspend fun refresh()

    suspend fun syncOnAppStart()

    suspend fun addSampleTasksIfEmpty()
}
