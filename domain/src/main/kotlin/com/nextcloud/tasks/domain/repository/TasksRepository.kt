package com.nextcloud.tasks.domain.repository

import com.nextcloud.tasks.domain.model.Tag
import com.nextcloud.tasks.domain.model.Task
import com.nextcloud.tasks.domain.model.TaskDraft
import com.nextcloud.tasks.domain.model.TaskList
import kotlinx.coroutines.flow.Flow

@Suppress("TooManyFunctions")
interface TasksRepository {
    fun observeTasks(): Flow<List<Task>>

    fun observeLists(): Flow<List<TaskList>>

    fun observeTags(): Flow<List<Tag>>

    /**
     * Observes the network connectivity status.
     * Emits true when online, false when offline.
     */
    fun observeIsOnline(): Flow<Boolean>

    /**
     * Observes whether there are pending changes that need to be synced.
     * Emits true when there are pending operations, false otherwise.
     */
    fun observeHasPendingChanges(): Flow<Boolean>

    /**
     * Checks if the device is currently online.
     */
    fun isCurrentlyOnline(): Boolean

    suspend fun getTask(id: String): Task?

    suspend fun createTask(draft: TaskDraft): Task

    suspend fun updateTask(task: Task): Task

    suspend fun deleteTask(taskId: String)

    suspend fun refresh()

    suspend fun syncOnAppStart()

    suspend fun addSampleTasksIfEmpty()

    suspend fun clearAccountData(accountId: String)
}
