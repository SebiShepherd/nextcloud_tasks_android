package com.nextcloud.tasks.data.repository

import androidx.room.withTransaction
import com.nextcloud.tasks.data.auth.AuthTokenProvider
import com.nextcloud.tasks.data.caldav.generator.VTodoGenerator
import com.nextcloud.tasks.data.caldav.parser.VTodoParser
import com.nextcloud.tasks.data.caldav.service.CalDavService
import com.nextcloud.tasks.data.database.NextcloudTasksDatabase
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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.IOException
import java.time.Instant
import javax.inject.Inject

@Suppress("TooManyFunctions", "LongParameterList")
class DefaultTasksRepository
    @Inject
    constructor(
        private val database: NextcloudTasksDatabase,
        private val taskMapper: TaskMapper,
        private val taskListMapper: TaskListMapper,
        private val tagMapper: TagMapper,
        private val calDavService: CalDavService,
        private val vTodoParser: VTodoParser,
        private val vTodoGenerator: VTodoGenerator,
        private val authTokenProvider: AuthTokenProvider,
        private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    ) : TasksRepository {
        private val tasksDao get() = database.tasksDao()
        private val taskListsDao get() = database.taskListsDao()
        private val tagsDao get() = database.tagsDao()

        override fun observeTasks(): Flow<List<Task>> =
            authTokenProvider
                .observeActiveAccountId()
                .flatMapLatest { accountId ->
                    if (accountId != null) {
                        tasksDao.observeTasks(accountId).map { tasks ->
                            tasks.map(taskMapper::toDomain)
                        }
                    } else {
                        flowOf(emptyList())
                    }
                }

        override fun observeLists(): Flow<List<TaskList>> =
            authTokenProvider
                .observeActiveAccountId()
                .flatMapLatest { accountId ->
                    if (accountId != null) {
                        taskListsDao.observeTaskLists(accountId).map { lists ->
                            lists.map(taskListMapper::toDomain)
                        }
                    } else {
                        flowOf(emptyList())
                    }
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
                val baseUrl = authTokenProvider.activeServerUrl() ?: throw IOException("No active server URL")
                val accountId = authTokenProvider.activeAccountId() ?: throw IOException("No active account")

                // Generate UID and create task domain model
                val uid =
                    java.util.UUID
                        .randomUUID()
                        .toString()
                val now = Instant.now()
                val task =
                    Task(
                        id = uid, // Use UID as ID for consistency
                        listId = draft.listId,
                        title = draft.title,
                        description = draft.description,
                        completed = draft.completed,
                        due = draft.due,
                        updatedAt = now,
                        tags = emptyList(), // Tags will be set separately if needed
                        priority = null,
                        status = if (draft.completed) "COMPLETED" else "NEEDS-ACTION",
                        completedAt = if (draft.completed) now else null,
                        uid = uid,
                        etag = null,
                        href = null,
                        parentUid = null,
                    )

                // Generate iCalendar VTODO
                val icalData = vTodoGenerator.generateVTodo(task)
                val filename = vTodoGenerator.generateFilename(uid)

                // Upload to server
                val createResult = calDavService.createTodo(baseUrl, draft.listId, filename, icalData)
                if (createResult.isFailure) {
                    throw createResult.exceptionOrNull() ?: IOException("Failed to create task")
                }

                val etag = createResult.getOrThrow()
                val href = "${draft.listId}/$filename"

                // Save to local database
                val taskEntity =
                    TaskEntity(
                        id = uid, // Use UID as ID for consistency
                        accountId = accountId,
                        listId = draft.listId,
                        title = draft.title,
                        description = draft.description,
                        completed = draft.completed,
                        due = draft.due,
                        updatedAt = now,
                        priority = null,
                        status = if (draft.completed) "COMPLETED" else "NEEDS-ACTION",
                        completedAt = if (draft.completed) now else null,
                        uid = uid,
                        etag = etag,
                        href = href,
                        parentUid = null,
                    )

                database.withTransaction {
                    tasksDao.upsertTask(taskEntity)
                }

                getTask(taskEntity.id) ?: error("Created task missing from local database")
            }

        override suspend fun updateTask(task: Task): Task =
            withContext(ioDispatcher) {
                val baseUrl = authTokenProvider.activeServerUrl() ?: throw IOException("No active server URL")
                val accountId = authTokenProvider.activeAccountId() ?: throw IOException("No active account")

                val href = task.href ?: throw IOException("Cannot update task without href")

                // Generate updated iCalendar VTODO
                val icalData = vTodoGenerator.generateVTodo(task)

                // Upload to server with ETag for optimistic locking
                val updateResult = calDavService.updateTodo(baseUrl, href, icalData, task.etag)
                if (updateResult.isFailure) {
                    throw updateResult.exceptionOrNull() ?: IOException("Failed to update task")
                }

                val newEtag = updateResult.getOrThrow()

                // Update local database
                val taskEntity =
                    TaskEntity(
                        id = task.id,
                        accountId = accountId,
                        listId = task.listId,
                        title = task.title,
                        description = task.description,
                        completed = task.completed,
                        due = task.due,
                        updatedAt = Instant.now(),
                        priority = task.priority,
                        status = task.status,
                        completedAt = task.completedAt,
                        uid = task.uid,
                        etag = newEtag,
                        href = task.href,
                        parentUid = task.parentUid,
                    )

                database.withTransaction {
                    tasksDao.upsertTask(taskEntity)
                }

                getTask(task.id) ?: error("Updated task missing from local database")
            }

        override suspend fun deleteTask(taskId: String) =
            withContext(ioDispatcher) {
                val baseUrl = authTokenProvider.activeServerUrl()
                val task = getTask(taskId)
                val taskHref = task?.href

                if (baseUrl != null && taskHref != null) {
                    // Delete from server
                    val deleteResult = calDavService.deleteTodo(baseUrl, taskHref, task.etag)
                    if (deleteResult.isFailure) {
                        Timber.w(deleteResult.exceptionOrNull(), "Failed to delete task from server, deleting locally")
                    }
                }

                // Delete from local database
                database.withTransaction {
                    tasksDao.clearTagsForTask(taskId)
                    tasksDao.deleteTask(taskId)
                }
            }

        @Suppress("LongMethod")
        override suspend fun refresh() =
            withContext(ioDispatcher) {
                val baseUrl =
                    authTokenProvider.activeServerUrl() ?: run {
                        Timber.w("No active server URL, skipping refresh")
                        return@withContext
                    }

                val accountId =
                    authTokenProvider.activeAccountId() ?: run {
                        Timber.w("No active account, skipping refresh")
                        return@withContext
                    }

                // CalDAV discovery
                val principalResult = calDavService.discoverPrincipal(baseUrl)
                if (principalResult.isFailure) {
                    Timber.e(principalResult.exceptionOrNull(), "Failed to discover principal")
                    return@withContext
                }

                val principal = principalResult.getOrThrow()
                val calendarHomeResult = calDavService.discoverCalendarHome(baseUrl, principal.principalUrl)
                if (calendarHomeResult.isFailure) {
                    Timber.e(calendarHomeResult.exceptionOrNull(), "Failed to discover calendar home")
                    return@withContext
                }

                val calendarHome = calendarHomeResult.getOrThrow()
                val collectionsResult =
                    calDavService.enumerateCalendarCollections(baseUrl, calendarHome.calendarHomeUrl)
                if (collectionsResult.isFailure) {
                    Timber.e(collectionsResult.exceptionOrNull(), "Failed to enumerate collections")
                    return@withContext
                }

                val collections = collectionsResult.getOrThrow()
                Timber.d("Found ${collections.size} calendar collections with VTODO support (before filtering)")

                // Convert collections to TaskListEntity
                val taskLists =
                    collections.map { collection ->
                        TaskListEntity(
                            id = collection.href,
                            accountId = accountId,
                            name = collection.displayName,
                            color = collection.color,
                            updatedAt = Instant.now(),
                            etag = collection.etag,
                            href = collection.href,
                            order = collection.order,
                        )
                    }

                // Fetch tasks from each collection
                val allTasks = mutableListOf<TaskEntity>()
                collections.forEach { collection ->
                    val todosResult = calDavService.fetchTodosFromCollection(baseUrl, collection.href)
                    if (todosResult.isSuccess) {
                        val todos = todosResult.getOrThrow()
                        Timber.d("Found ${todos.size} todos in ${collection.displayName}")

                        todos.forEach { todo ->
                            // Each server response contains one complete VCALENDAR with one VTODO
                            val taskEntity =
                                vTodoParser.parseVTodo(
                                    icalData = todo.calendarData,
                                    accountId = accountId,
                                    listId = collection.href,
                                    href = todo.href,
                                    etag = todo.etag,
                                )
                            if (taskEntity != null) {
                                allTasks.add(taskEntity)
                            }
                        }
                    } else {
                        Timber.w(todosResult.exceptionOrNull(), "Failed to fetch todos from ${collection.displayName}")
                    }
                }

                Timber.d("Fetched total ${allTasks.size} tasks from CalDAV")

                // Update database
                database.withTransaction {
                    // Delete local-only (demo) tasks and lists before syncing
                    tasksDao.deleteTasksWithoutHref()
                    taskListsDao.deleteListsWithoutHref()

                    upsertTaskLists(taskLists)
                    upsertTasksFromCalDav(allTasks)
                }
            }

        override suspend fun syncOnAppStart() = refresh()

        override suspend fun addSampleTasksIfEmpty() =
            withContext(ioDispatcher) {
                // Sample tasks are disabled in multi-account mode
                // Users need to log in with a real account and sync from server
                Timber.d("Sample tasks disabled - multi-account mode requires real login")
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

        private suspend fun shouldReplaceTask(task: TaskEntity): Boolean {
            val currentUpdatedAt = tasksDao.getTaskUpdatedAt(task.id)
            return currentUpdatedAt == null || !currentUpdatedAt.isAfter(task.updatedAt)
        }

        private suspend fun shouldReplaceList(list: TaskListEntity): Boolean {
            val currentUpdatedAt = taskListsDao.getUpdatedAt(list.id)
            return currentUpdatedAt == null || !currentUpdatedAt.isAfter(list.updatedAt)
        }

        private suspend fun upsertTasksFromCalDav(tasks: List<TaskEntity>) {
            tasks.forEach { task ->
                if (shouldReplaceTask(task)) {
                    tasksDao.upsertTask(task)
                    // Clear existing tag associations
                    tasksDao.clearTagsForTask(task.id)
                    // Note: CalDAV tags will be handled separately if needed
                } else {
                    Timber.d("Skipped task %s because local version is newer", task.id)
                }
            }
        }

        override suspend fun clearAccountData(accountId: String) =
            withContext(ioDispatcher) {
                database.withTransaction {
                    tasksDao.deleteTasksByAccount(accountId)
                    taskListsDao.deleteListsByAccount(accountId)
                    Timber.d("Cleared all data for account: $accountId")
                }
            }
    }
