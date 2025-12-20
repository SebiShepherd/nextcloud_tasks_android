package com.nextcloud.tasks.data.repository

import androidx.room.withTransaction
import com.nextcloud.tasks.data.api.NextcloudTasksApi
import com.nextcloud.tasks.data.api.dto.TaskDto
import com.nextcloud.tasks.data.database.NextcloudTasksDatabase
import com.nextcloud.tasks.data.database.entity.TagEntity
import com.nextcloud.tasks.data.database.entity.TaskEntity
import com.nextcloud.tasks.data.database.entity.TaskListEntity
import com.nextcloud.tasks.data.mapper.TagMapper
import com.nextcloud.tasks.data.mapper.TaskListMapper
import com.nextcloud.tasks.data.mapper.TaskMapper
import com.nextcloud.tasks.domain.model.Tag
import com.nextcloud.tasks.domain.model.Task
import com.nextcloud.tasks.domain.model.TaskDraft
import com.nextcloud.tasks.domain.model.TaskList
import com.nextcloud.tasks.domain.repository.TasksRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.Instant
import javax.inject.Inject

@Suppress("TooManyFunctions")
class DefaultTasksRepository
    @Inject
    constructor(
        private val api: NextcloudTasksApi,
        private val database: NextcloudTasksDatabase,
        private val taskMapper: TaskMapper,
        private val taskListMapper: TaskListMapper,
        private val tagMapper: TagMapper,
        private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    ) : TasksRepository {
        private val tasksDao get() = database.tasksDao()
        private val taskListsDao get() = database.taskListsDao()
        private val tagsDao get() = database.tagsDao()

        override fun observeTasks(): Flow<List<Task>> =
            tasksDao.observeTasks().map { tasks ->
                tasks.map(taskMapper::toDomain)
            }

        override fun observeLists(): Flow<List<TaskList>> =
            taskListsDao.observeTaskLists().map { lists ->
                lists.map(taskListMapper::toDomain)
            }

        override fun observeTags(): Flow<List<Tag>> =
            tagsDao.observeTags().map { tags ->
                tags.map(tagMapper::toDomain)
            }

        override suspend fun getTask(id: String): Task? =
            withContext(ioDispatcher) {
                tasksDao.getTaskWithRelations(id)?.let(taskMapper::toDomain)
            }

        override suspend fun createTask(draft: TaskDraft): Task =
            withContext(ioDispatcher) {
                val now = Instant.now()
                val response = api.createTask(taskMapper.toRequest(draft, now))
                upsertFromRemote(listOf(response))
                getTask(response.id) ?: error("Created task missing from local database")
            }

        override suspend fun updateTask(task: Task): Task =
            withContext(ioDispatcher) {
                val response = api.updateTask(task.id, taskMapper.toRequest(task))
                upsertFromRemote(listOf(response))
                getTask(task.id) ?: error("Updated task missing from local database")
            }

        override suspend fun deleteTask(taskId: String) =
            withContext(ioDispatcher) {
                api.deleteTask(taskId)
                database.withTransaction {
                    tasksDao.clearTagsForTask(taskId)
                    tasksDao.deleteTask(taskId)
                }
            }

        override suspend fun refresh() =
            withContext(ioDispatcher) {
                val remoteLists = api.getTaskLists()
                val remoteTags = api.getTags()
                val remoteTasks = api.getTasks()

                database.withTransaction {
                    upsertTaskLists(remoteLists.map(taskListMapper::toEntity))
                    upsertTags(remoteTags.map(tagMapper::toEntity))
                    upsertFromRemote(remoteTasks)
                }
            }

        override suspend fun syncOnAppStart() = refresh()

        override suspend fun addSampleTasksIfEmpty() =
            withContext(ioDispatcher) {
                if (tasksDao.countTasks() > 0) {
                    return@withContext
                }

                val now = Instant.now()
                val defaultList =
                    TaskListEntity(
                        id = "inbox",
                        name = "Inbox",
                        color = null,
                        updatedAt = now,
                    )
                val sampleTag =
                    TagEntity(
                        id = "personal",
                        name = "Personal",
                        updatedAt = now,
                    )
                val sampleTask =
                    TaskEntity(
                        id = "sample-task",
                        listId = defaultList.id,
                        title = "Welcome to Nextcloud Tasks",
                        description = "Use the refresh action to fetch tasks from your Nextcloud server.",
                        completed = false,
                        due = null,
                        updatedAt = now,
                    )

                database.withTransaction {
                    taskListsDao.upsertTaskList(defaultList)
                    tagsDao.upsertTag(sampleTag)
                    tasksDao.upsertTask(sampleTask)
                    tasksDao.upsertTaskTagCrossRefs(
                        taskMapper.crossRefs(sampleTask.id, listOf(sampleTag.id)),
                    )
                }
            }

        private suspend fun upsertFromRemote(tasks: List<TaskDto>) {
            tasks.forEach { dto ->
                val entity = taskMapper.toEntity(dto)
                if (shouldReplaceTask(entity)) {
                    tasksDao.upsertTask(entity)
                    tasksDao.clearTagsForTask(entity.id)
                    if (dto.tagIds.isNotEmpty()) {
                        tasksDao.upsertTaskTagCrossRefs(taskMapper.crossRefs(entity.id, dto.tagIds))
                    }
                } else {
                    Timber.d("Skipped task %s because local version is newer", entity.id)
                }
            }
        }

        private suspend fun upsertTaskLists(lists: List<TaskListEntity>) {
            lists.forEach { list ->
                if (shouldReplaceList(list)) {
                    taskListsDao.upsertTaskList(list)
                } else {
                    Timber.d("Skipped list %s because local version is newer", list.id)
                }
            }
        }

        private suspend fun upsertTags(tags: List<TagEntity>) {
            tags.forEach { tag ->
                if (shouldReplaceTag(tag)) {
                    tagsDao.upsertTag(tag)
                } else {
                    Timber.d("Skipped tag %s because local version is newer", tag.id)
                }
            }
        }

        private suspend fun shouldReplaceTask(task: TaskEntity): Boolean {
            val currentUpdatedAt = tasksDao.getTaskUpdatedAt(task.id)
            return currentUpdatedAt == null || !currentUpdatedAt.isAfter(task.updatedAt)
        }

        private suspend fun shouldReplaceList(list: TaskListEntity): Boolean {
            val currentUpdatedAt = taskListsDao.getUpdatedAt(list.id)
            return currentUpdatedAt == null || !currentUpdatedAt.isAfter(list.updatedAt)
        }

        private suspend fun shouldReplaceTag(tag: TagEntity): Boolean {
            val currentUpdatedAt = tagsDao.getUpdatedAt(tag.id)
            return currentUpdatedAt == null || !currentUpdatedAt.isAfter(tag.updatedAt)
        }
    }
