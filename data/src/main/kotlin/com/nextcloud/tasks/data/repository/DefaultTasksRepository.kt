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
import com.nextcloud.tasks.data.network.NetworkMonitor
import com.nextcloud.tasks.data.sync.PendingOperationsManager
import com.nextcloud.tasks.data.sync.TaskFieldMerger
import com.nextcloud.tasks.domain.model.Tag
import com.nextcloud.tasks.domain.model.Task
import com.nextcloud.tasks.domain.model.TaskDraft
import com.nextcloud.tasks.domain.model.TaskList
import com.nextcloud.tasks.domain.repository.TasksRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
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
        private val networkMonitor: NetworkMonitor,
        private val pendingOperationsManager: PendingOperationsManager,
        private val taskFieldMerger: TaskFieldMerger,
        private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    ) : TasksRepository {
        private val backgroundScope = CoroutineScope(SupervisorJob() + ioDispatcher)
        private val tasksDao get() = database.tasksDao()
        private val taskListsDao get() = database.taskListsDao()
        private val tagsDao get() = database.tagsDao()

        @OptIn(ExperimentalCoroutinesApi::class)
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

        @OptIn(ExperimentalCoroutinesApi::class)
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

        override fun observeIsOnline(): Flow<Boolean> = networkMonitor.isOnline

        override fun observeHasPendingChanges(): Flow<Boolean> = pendingOperationsManager.hasPendingOperations()

        override fun isCurrentlyOnline(): Boolean = networkMonitor.isCurrentlyOnline()

        override suspend fun getTask(id: String): Task? =
            withContext(ioDispatcher) {
                tasksDao.getTaskWithRelations(id)?.let(taskMapper::toDomain)
            }

        /**
         * Creates a task with optimistic UI update.
         * The task is saved locally immediately and server sync happens in the background.
         * If offline, the operation is queued for later sync.
         */
        override suspend fun createTask(draft: TaskDraft): Task =
            withContext(ioDispatcher) {
                val accountId = authTokenProvider.activeAccountId() ?: throw IOException("No active account")

                // Generate UID and create task entity
                val uid =
                    java.util.UUID
                        .randomUUID()
                        .toString()
                val now = Instant.now()

                // Save to local database immediately (optimistic update)
                // Note: href is null initially - will be set after server sync
                val taskEntity =
                    TaskEntity(
                        id = uid,
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
                        etag = null,
                        href = null, // Will be set after server sync
                        parentUid = null,
                    )

                database.withTransaction {
                    tasksDao.upsertTask(taskEntity)
                }

                Timber.d("Task $uid created locally (optimistic)")

                // Sync with server in background
                if (networkMonitor.isCurrentlyOnline()) {
                    backgroundScope.launch {
                        syncCreateToServer(taskEntity, draft.listId)
                    }
                } else {
                    // Queue for later sync when offline
                    pendingOperationsManager.queueCreateOperation(taskEntity, draft.listId)
                    Timber.d("Task $uid queued for sync (offline)")
                }

                getTask(taskEntity.id) ?: error("Created task missing from local database")
            }

        /**
         * Syncs a create operation to the server.
         */
        private suspend fun syncCreateToServer(
            taskEntity: TaskEntity,
            listId: String,
        ) {
            try {
                val baseUrl = authTokenProvider.activeServerUrl() ?: return

                // Build task domain model for generator
                val task =
                    Task(
                        id = taskEntity.id,
                        listId = taskEntity.listId,
                        title = taskEntity.title,
                        description = taskEntity.description,
                        completed = taskEntity.completed,
                        due = taskEntity.due,
                        updatedAt = taskEntity.updatedAt,
                        tags = emptyList(),
                        priority = taskEntity.priority,
                        status = taskEntity.status,
                        completedAt = taskEntity.completedAt,
                        uid = taskEntity.uid,
                        etag = null,
                        href = null,
                        parentUid = null,
                    )

                // Generate iCalendar VTODO
                val icalData = vTodoGenerator.generateVTodo(task)
                val filename = vTodoGenerator.generateFilename(taskEntity.uid ?: taskEntity.id)

                // Upload to server
                val createResult = calDavService.createTodo(baseUrl, listId, filename, icalData)

                if (createResult.isSuccess) {
                    val etag = createResult.getOrThrow()
                    val href = "$listId/$filename"

                    // Update local database with server data (etag, href, and base snapshot)
                    val syncedEntity = taskEntity.copy(etag = etag, href = href)
                    database.withTransaction {
                        tasksDao.upsertTask(
                            syncedEntity.copy(baseSnapshot = taskFieldMerger.createSnapshot(syncedEntity)),
                        )
                    }
                    Timber.d("Task ${taskEntity.id} synced to server successfully")
                } else {
                    // Queue for retry if sync failed
                    Timber.w(createResult.exceptionOrNull(), "Failed to sync task ${taskEntity.id}, queuing for retry")
                    pendingOperationsManager.queueCreateOperation(taskEntity, listId)
                }
            } catch (
                @Suppress("TooGenericExceptionCaught") e: Exception,
            ) {
                Timber.e(e, "Error syncing task ${taskEntity.id}")
                pendingOperationsManager.queueCreateOperation(taskEntity, listId)
            }
        }

        /**
         * Updates a task with optimistic UI update.
         * The local database is updated immediately, and server sync happens in the background.
         * If offline, the operation is queued for later sync.
         */
        override suspend fun updateTask(task: Task): Task =
            withContext(ioDispatcher) {
                val accountId = authTokenProvider.activeAccountId() ?: throw IOException("No active account")

                // Update local database immediately (optimistic update)
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
                        etag = task.etag,
                        href = task.href,
                        parentUid = task.parentUid,
                    )

                database.withTransaction {
                    tasksDao.upsertTask(taskEntity)
                }

                Timber.d("Task ${task.id} updated locally (optimistic)")

                // Sync with server in background
                if (networkMonitor.isCurrentlyOnline() && task.href != null) {
                    backgroundScope.launch {
                        syncTaskToServer(task, taskEntity)
                    }
                } else {
                    // Queue for later sync when offline or no href
                    if (task.href != null) {
                        pendingOperationsManager.queueUpdateOperation(taskEntity)
                        Timber.d("Task ${task.id} queued for sync (offline)")
                    }
                }

                getTask(task.id) ?: error("Updated task missing from local database")
            }

        /**
         * Syncs a single task to the server.
         */
        private suspend fun syncTaskToServer(
            task: Task,
            taskEntity: TaskEntity,
        ) {
            try {
                val baseUrl = authTokenProvider.activeServerUrl() ?: return
                val href = task.href ?: return

                val icalData = vTodoGenerator.generateVTodo(task)
                val updateResult = calDavService.updateTodo(baseUrl, href, icalData, task.etag)

                if (updateResult.isSuccess) {
                    val newEtag = updateResult.getOrThrow()
                    // Update etag and base snapshot in database
                    val syncedEntity = taskEntity.copy(etag = newEtag)
                    database.withTransaction {
                        tasksDao.upsertTask(
                            syncedEntity.copy(baseSnapshot = taskFieldMerger.createSnapshot(syncedEntity)),
                        )
                    }
                    Timber.d("Task ${task.id} synced to server successfully")
                } else {
                    // Queue for retry if sync failed
                    Timber.w(updateResult.exceptionOrNull(), "Failed to sync task ${task.id}, queuing for retry")
                    pendingOperationsManager.queueUpdateOperation(taskEntity)
                }
            } catch (
                @Suppress("TooGenericExceptionCaught") e: Exception,
            ) {
                Timber.e(e, "Error syncing task ${task.id}")
                pendingOperationsManager.queueUpdateOperation(taskEntity)
            }
        }

        /**
         * Deletes a task with optimistic UI update.
         * The local database is updated immediately, and server sync happens in the background.
         * If offline, the operation is queued for later sync.
         */
        override suspend fun deleteTask(taskId: String): Unit =
            withContext(ioDispatcher) {
                val task = getTask(taskId)
                val taskHref = task?.href
                val taskEtag = task?.etag

                // Delete from local database immediately (optimistic update)
                database.withTransaction {
                    tasksDao.clearTagsForTask(taskId)
                    tasksDao.deleteTask(taskId)
                }

                Timber.d("Task $taskId deleted locally (optimistic)")

                // Sync with server in background
                if (networkMonitor.isCurrentlyOnline() && taskHref != null) {
                    backgroundScope.launch {
                        syncDeleteToServer(taskId, taskHref, taskEtag)
                    }
                } else if (taskHref != null) {
                    // Queue for later sync when offline
                    pendingOperationsManager.queueDeleteOperation(taskId, taskHref, taskEtag)
                    Timber.d("Task $taskId delete queued for sync (offline)")
                }
                Unit
            }

        /**
         * Syncs a delete operation to the server.
         */
        private suspend fun syncDeleteToServer(
            taskId: String,
            href: String,
            etag: String?,
        ) {
            try {
                val baseUrl = authTokenProvider.activeServerUrl() ?: return

                val deleteResult = calDavService.deleteTodo(baseUrl, href, etag)
                if (deleteResult.isFailure) {
                    val error = deleteResult.exceptionOrNull()
                    // 404 means already deleted, which is fine
                    if (error?.message?.contains("404") != true) {
                        Timber.w(error, "Failed to delete task $taskId from server")
                        pendingOperationsManager.queueDeleteOperation(taskId, href, etag)
                    }
                } else {
                    Timber.d("Task $taskId deleted from server successfully")
                }
            } catch (
                @Suppress("TooGenericExceptionCaught") e: Exception,
            ) {
                Timber.e(e, "Error deleting task $taskId from server")
                pendingOperationsManager.queueDeleteOperation(taskId, href, etag)
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

                // IMPORTANT: Process pending operations FIRST before fetching from server
                // This ensures local offline changes are synced before we fetch server state
                if (networkMonitor.isCurrentlyOnline()) {
                    Timber.d("Processing pending operations before refresh")
                    pendingOperationsManager.processPendingOperations()
                }

                // CalDAV discovery — propagate errors to the caller so the UI can
                // show meaningful messages (429 rate limiting, 401 auth failure, etc.)
                val principalResult = calDavService.discoverPrincipal(baseUrl)
                if (principalResult.isFailure) {
                    val ex = principalResult.exceptionOrNull()!!
                    Timber.e(ex, "Failed to discover principal")
                    throw ex
                }

                val principal = principalResult.getOrThrow()
                val calendarHomeResult = calDavService.discoverCalendarHome(baseUrl, principal.principalUrl)
                if (calendarHomeResult.isFailure) {
                    val ex = calendarHomeResult.exceptionOrNull()!!
                    Timber.e(ex, "Failed to discover calendar home")
                    throw ex
                }

                val calendarHome = calendarHomeResult.getOrThrow()
                val collectionsResult =
                    calDavService.enumerateCalendarCollections(baseUrl, calendarHome.calendarHomeUrl)
                if (collectionsResult.isFailure) {
                    val ex = collectionsResult.exceptionOrNull()!!
                    Timber.e(ex, "Failed to enumerate collections")
                    throw ex
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

                // Get task IDs with pending CREATE operations to protect them from deletion
                val pendingCreateTaskIds = pendingOperationsManager.getTaskIdsWithPendingCreate()
                if (pendingCreateTaskIds.isNotEmpty()) {
                    Timber.d("Protecting ${pendingCreateTaskIds.size} offline-created tasks from deletion")
                }

                // Update database
                database.withTransaction {
                    // Delete local-only (demo) tasks and lists before syncing
                    // But exclude tasks that have pending CREATE operations (offline-created)
                    if (pendingCreateTaskIds.isEmpty()) {
                        tasksDao.deleteTasksWithoutHref()
                    } else {
                        tasksDao.deleteTasksWithoutHrefExcluding(pendingCreateTaskIds)
                    }
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

        private suspend fun shouldReplaceList(list: TaskListEntity): Boolean {
            val currentUpdatedAt = taskListsDao.getUpdatedAt(list.id)
            return currentUpdatedAt == null || !currentUpdatedAt.isAfter(list.updatedAt)
        }

        /**
         * Upserts tasks from CalDAV using field-level merge.
         *
         * For each server task:
         * - New task (not in local DB): insert with base_snapshot set
         * - Existing task with same etag: skip (no changes)
         * - Existing task with different etag: perform field-level merge
         */
        private suspend fun upsertTasksFromCalDav(tasks: List<TaskEntity>) {
            tasks.forEach { serverTask ->
                val localTask = tasksDao.getTaskEntity(serverTask.id)

                if (localTask == null) {
                    // New task from server — insert with base snapshot
                    val taskWithSnapshot =
                        serverTask.copy(
                            baseSnapshot = taskFieldMerger.createSnapshot(serverTask),
                        )
                    tasksDao.upsertTask(taskWithSnapshot)
                    tasksDao.clearTagsForTask(serverTask.id)
                } else if (localTask.etag != null && localTask.etag == serverTask.etag) {
                    // Same etag — no server changes
                    // But update account_id and listId if they changed (e.g. after re-login)
                    if (localTask.accountId != serverTask.accountId ||
                        localTask.listId != serverTask.listId
                    ) {
                        tasksDao.upsertTask(
                            localTask.copy(
                                accountId = serverTask.accountId,
                                listId = serverTask.listId,
                            ),
                        )
                    }
                    Timber.d("Skipped task %s (etag unchanged)", serverTask.id)
                } else {
                    // Server has changes — perform field-level merge
                    val merged = taskFieldMerger.mergeTask(serverTask, localTask)
                    tasksDao.upsertTask(merged)
                    tasksDao.clearTagsForTask(serverTask.id)
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
