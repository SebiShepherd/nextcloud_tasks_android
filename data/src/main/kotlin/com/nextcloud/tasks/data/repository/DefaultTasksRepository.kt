package com.nextcloud.tasks.data.repository

import com.nextcloud.tasks.domain.model.Task
import com.nextcloud.tasks.domain.repository.TasksRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class DefaultTasksRepository @Inject constructor() : TasksRepository {
    private val tasks = MutableStateFlow<List<Task>>(emptyList())

    override fun observeTasks(): Flow<List<Task>> = tasks.asStateFlow()

    override suspend fun addSampleTasksIfEmpty() {
        if (tasks.value.isNotEmpty()) return
        tasks.value = listOf(
            Task(id = "1", title = "Check Nextcloud tasks", description = "Sync tasks with your server"),
            Task(id = "2", title = "Enable notifications", description = "Turn on reminders for due dates"),
            Task(id = "3", title = "Invite collaborators", description = "Share lists with your team"),
        )
    }
}
