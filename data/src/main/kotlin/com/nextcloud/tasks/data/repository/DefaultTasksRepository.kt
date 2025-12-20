package com.nextcloud.tasks.data.repository

import com.nextcloud.tasks.data.auth.AuthTokenProvider
import com.nextcloud.tasks.data.local.dao.TaskDao
import com.nextcloud.tasks.data.local.dao.TaskListDao
import com.nextcloud.tasks.data.local.toDomain
import com.nextcloud.tasks.data.local.toEntity
import com.nextcloud.tasks.data.local.toPayload
import com.nextcloud.tasks.data.network.NextcloudClientFactory
import com.nextcloud.tasks.data.network.toDomain
import com.nextcloud.tasks.data.network.toRequest
import com.nextcloud.tasks.data.network.model.TaskListDto
import com.nextcloud.tasks.domain.model.Task
import com.nextcloud.tasks.domain.model.TaskList
import com.nextcloud.tasks.domain.repository.TasksRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultTasksRepository
    @Inject
    constructor(
        private val taskDao: TaskDao,
        private val taskListDao: TaskListDao,
        private val clientFactory: NextcloudClientFactory,
        private val tokenProvider: AuthTokenProvider,
    ) : TasksRepository {
        override fun observeTasks(): Flow<List<Task>> = taskDao.observeTasks().map { tasks -> tasks.map { it.toDomain() } }

        override fun observeLists(): Flow<List<TaskList>> = taskListDao.observeLists().map { lists -> lists.map { it.toDomain() } }

        override suspend fun refresh() {
            val serverUrl =
                tokenProvider.activeServerUrl()
                    ?: run {
                        Timber.i("Skipping refresh because no active account is set")
                        return
                    }
            val service = clientFactory.create(serverUrl)
            val lists = service.fetchTaskLists().body.data.map { it.toDomain() }
            taskListDao.replaceAll(lists.map { it.toEntity() })

            if (lists.isEmpty()) {
                taskDao.clearAllRefs()
                taskDao.clearTags()
                taskDao.clearTasks()
                return
            }

            lists.forEach { list ->
                val remoteTasks = service.fetchTasks(list.id).body.data.map { it.toDomain() }
                when {
                    remoteTasks.isEmpty() -> taskDao.deleteByList(list.id)
                    else -> {
                        val existingTasks = taskDao.findByIds(remoteTasks.map { it.id }).associateBy { it.id }
                        val toPersist =
                            remoteTasks.filter { remote ->
                                val local = existingTasks[remote.id]
                                local == null || remote.lastModified >= local.lastModified
                            }
                        if (toPersist.isNotEmpty()) {
                            taskDao.upsertTasksWithTags(toPersist.map { it.toPayload() })
                        }
                        taskDao.deleteMissing(list.id, remoteTasks.map { it.id })
                    }
                }
            }
        }

        override suspend fun createTask(task: Task): Task {
            val service = requireService()
            val created =
                service
                    .createTask(task.listId, task.toRequest())
                    .body
                    .data
                    .toDomain()
            taskDao.upsertTasksWithTags(listOf(created.toPayload()))
            return created
        }

        override suspend fun updateTask(task: Task): Task {
            val service = requireService()
            val updated =
                service
                    .updateTask(task.id, task.toRequest())
                    .body
                    .data
                    .toDomain()
            taskDao.upsertTasksWithTags(listOf(updated.toPayload()))
            return updated
        }

        override suspend fun deleteTask(taskId: String) {
            val service = requireService()
            service.deleteTask(taskId)
            taskDao.deleteTask(taskId)
        }

        override suspend fun upsertList(list: TaskList): TaskList {
            val service = requireService()
            val dto = TaskListDto(id = list.id, name = list.name, color = list.color, lastModified = list.lastModified)
            val saved =
                if (list.id.isNotBlank()) {
                    service.updateTaskList(list.id, dto).body.data.toDomain()
                } else {
                    service.createTaskList(dto).body.data.toDomain()
                }
            taskListDao.upsertLists(listOf(saved.toEntity()))
            return saved
        }

        override suspend fun deleteList(listId: String) {
            val service = requireService()
            service.deleteTaskList(listId)
            taskListDao.deleteList(listId)
        }

        override suspend fun addSampleTasksIfEmpty() {
            val hasData = taskDao.countTasks() > 0
            if (hasData) return

            Timber.i("Seeding sample tasks locally")
            val listId = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()
            val list =
                TaskList(
                    id = listId,
                    name = "Getting started",
                    color = "#1E9CEF",
                    lastModified = now,
                )
            taskListDao.upsertLists(listOf(list.toEntity()))
            val sampleTasks =
                listOf(
                    Task(
                        id = UUID.randomUUID().toString(),
                        title = "Check Nextcloud tasks",
                        description = "Sync tasks with your server",
                        completed = false,
                        listId = listId,
                        lastModified = now,
                    ),
                    Task(
                        id = UUID.randomUUID().toString(),
                        title = "Enable notifications",
                        description = "Turn on reminders for due dates",
                        completed = false,
                        listId = listId,
                        lastModified = now,
                    ),
                    Task(
                        id = UUID.randomUUID().toString(),
                        title = "Invite collaborators",
                        description = "Share lists with your team",
                        completed = false,
                        listId = listId,
                        lastModified = now,
                    ),
                )
            taskDao.upsertTasksWithTags(sampleTasks.map { it.toPayload() })
        }

        private fun requireService() =
            clientFactory.create(
                tokenProvider.activeServerUrl() ?: error("No active account configured for tasks API"),
            )
    }
