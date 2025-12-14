package com.nextcloud.tasks.domain.repository

import com.nextcloud.tasks.domain.model.Task
import kotlinx.coroutines.flow.Flow

interface TasksRepository {
    fun observeTasks(): Flow<List<Task>>

    suspend fun addSampleTasksIfEmpty()
}
