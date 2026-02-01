package com.nextcloud.tasks.data.sync

import com.nextcloud.tasks.data.auth.AuthTokenProvider
import com.nextcloud.tasks.data.caldav.generator.VTodoGenerator
import com.nextcloud.tasks.data.caldav.service.CalDavService
import com.nextcloud.tasks.data.database.NextcloudTasksDatabase
import com.nextcloud.tasks.data.database.entity.PendingOperationEntity
import com.nextcloud.tasks.data.database.entity.TaskEntity
import com.nextcloud.tasks.data.network.NetworkMonitor
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
import javax.inject.Singleton

/**
 * Manages pending operations for offline-first functionality.
 * Queues operations when offline and processes them when network becomes available.
 */
@Suppress("TooManyFunctions")
@Singleton
class PendingOperationsManager
    @Inject
    constructor(
        private val database: NextcloudTasksDatabase,
        private val networkMonitor: NetworkMonitor,
        private val calDavService: CalDavService,
        private val vTodoGenerator: VTodoGenerator,
        private val authTokenProvider: AuthTokenProvider,
        private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    ) {
        private val pendingOperationsDao get() = database.pendingOperationsDao()
        private val tasksDao get() = database.tasksDao()

        private val moshi =
            Moshi
                .Builder()
                .add(KotlinJsonAdapterFactory())
                .add(InstantAdapter())
                .build()
        private val taskPayloadAdapter = moshi.adapter(TaskPayload::class.java)

        private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)

        init {
            // Start monitoring network changes and process pending operations when online
            scope.launch {
                networkMonitor.isOnline.collect { isOnline ->
                    if (isOnline) {
                        Timber.d("Network available, processing pending operations")
                        processPendingOperations()
                    }
                }
            }
        }

        /**
         * Queues an update operation to be synced later.
         */
        suspend fun queueUpdateOperation(task: TaskEntity) =
            withContext(ioDispatcher) {
                val accountId =
                    authTokenProvider.activeAccountId() ?: run {
                        Timber.w("No active account, cannot queue operation")
                        return@withContext
                    }

                // Remove any existing pending operations for this task to avoid conflicts
                pendingOperationsDao.deleteByTaskId(task.id)

                val payload =
                    TaskPayload(
                        id = task.id,
                        accountId = task.accountId,
                        listId = task.listId,
                        title = task.title,
                        description = task.description,
                        completed = task.completed,
                        due = task.due,
                        updatedAt = task.updatedAt,
                        priority = task.priority,
                        status = task.status,
                        completedAt = task.completedAt,
                        uid = task.uid,
                        etag = task.etag,
                        href = task.href,
                        parentUid = task.parentUid,
                    )

                val operation =
                    PendingOperationEntity(
                        accountId = accountId,
                        taskId = task.id,
                        operationType = PendingOperationEntity.OPERATION_UPDATE,
                        payload = taskPayloadAdapter.toJson(payload),
                        createdAt = Instant.now(),
                    )

                pendingOperationsDao.insert(operation)
                Timber.d("Queued update operation for task ${task.id}")
            }

        /**
         * Queues a delete operation to be synced later.
         */
        suspend fun queueDeleteOperation(
            taskId: String,
            href: String?,
            etag: String?,
        ) = withContext(ioDispatcher) {
            val accountId =
                authTokenProvider.activeAccountId() ?: run {
                    Timber.w("No active account, cannot queue operation")
                    return@withContext
                }

            // Remove any existing pending operations for this task
            pendingOperationsDao.deleteByTaskId(taskId)

            val payload =
                DeletePayload(
                    taskId = taskId,
                    href = href,
                    etag = etag,
                )

            val deletePayloadAdapter = moshi.adapter(DeletePayload::class.java)

            val operation =
                PendingOperationEntity(
                    accountId = accountId,
                    taskId = taskId,
                    operationType = PendingOperationEntity.OPERATION_DELETE,
                    payload = deletePayloadAdapter.toJson(payload),
                    createdAt = Instant.now(),
                )

            pendingOperationsDao.insert(operation)
            Timber.d("Queued delete operation for task $taskId")
        }

        /**
         * Observes the count of pending operations for the active account.
         */
        fun observePendingCount(): Flow<Int> =
            authTokenProvider.observeActiveAccountId().flatMapLatest { accountId ->
                if (accountId != null) {
                    pendingOperationsDao.observePendingCount(accountId)
                } else {
                    flowOf(0)
                }
            }

        /**
         * Checks if there are pending operations.
         */
        fun hasPendingOperations(): Flow<Boolean> = observePendingCount().map { it > 0 }

        /**
         * Processes all pending operations for the active account.
         * Called when network becomes available.
         */
        suspend fun processPendingOperations() =
            withContext(ioDispatcher) {
                val accountId =
                    authTokenProvider.activeAccountId() ?: run {
                        Timber.w("No active account, skipping pending operations")
                        return@withContext
                    }

                val baseUrl =
                    authTokenProvider.activeServerUrl() ?: run {
                        Timber.w("No active server URL, skipping pending operations")
                        return@withContext
                    }

                val operations = pendingOperationsDao.getPendingOperations(accountId)
                Timber.d("Processing ${operations.size} pending operations")

                for (operation in operations) {
                    try {
                        when (operation.operationType) {
                            PendingOperationEntity.OPERATION_UPDATE -> processUpdateOperation(baseUrl, operation)
                            PendingOperationEntity.OPERATION_DELETE -> processDeleteOperation(baseUrl, operation)
                            else -> {
                                Timber.w("Unknown operation type: ${operation.operationType}")
                                pendingOperationsDao.delete(operation.id)
                            }
                        }
                    } catch (e: IOException) {
                        Timber.e(e, "Failed to process operation ${operation.id}, will retry later")
                        pendingOperationsDao.incrementRetryCount(operation.id, e.message)

                        // If too many retries, give up
                        if (operation.retryCount >= MAX_RETRIES) {
                            Timber.w("Max retries reached for operation ${operation.id}, removing")
                            pendingOperationsDao.delete(operation.id)
                        }
                    }
                }
            }

        private suspend fun processUpdateOperation(
            baseUrl: String,
            operation: PendingOperationEntity,
        ) {
            val payload = taskPayloadAdapter.fromJson(operation.payload) ?: return

            val href = payload.href ?: return

            // Get fresh task from database to get latest etag
            val currentTask = tasksDao.getTaskWithRelations(payload.id)

            // Build task entity from payload but use current etag
            val task =
                TaskEntity(
                    id = payload.id,
                    accountId = payload.accountId,
                    listId = payload.listId,
                    title = payload.title,
                    description = payload.description,
                    completed = payload.completed,
                    due = payload.due,
                    updatedAt = payload.updatedAt,
                    priority = payload.priority,
                    status = payload.status,
                    completedAt = payload.completedAt,
                    uid = payload.uid,
                    etag = currentTask?.task?.etag ?: payload.etag,
                    href = href,
                    parentUid = payload.parentUid,
                )

            // Convert to domain model for generator
            val domainTask = taskEntityToDomain(task)
            val icalData = vTodoGenerator.generateVTodo(domainTask)

            // Upload to server
            val result = calDavService.updateTodo(baseUrl, href, icalData, task.etag)
            if (result.isSuccess) {
                // Update local database with new etag
                val newEtag = result.getOrNull()
                if (newEtag != null) {
                    tasksDao.upsertTask(task.copy(etag = newEtag))
                }
                pendingOperationsDao.delete(operation.id)
                Timber.d("Successfully synced update for task ${task.id}")
            } else {
                throw result.exceptionOrNull() ?: IOException("Unknown error during update")
            }
        }

        private suspend fun processDeleteOperation(
            baseUrl: String,
            operation: PendingOperationEntity,
        ) {
            val deletePayloadAdapter = moshi.adapter(DeletePayload::class.java)
            val payload = deletePayloadAdapter.fromJson(operation.payload) ?: return

            val href = payload.href
            if (href != null) {
                // Delete from server
                val result = calDavService.deleteTodo(baseUrl, href, payload.etag)
                if (result.isFailure) {
                    val error = result.exceptionOrNull()
                    // 404 means already deleted, which is fine
                    if (error?.message?.contains("404") != true) {
                        throw error ?: IOException("Unknown error during delete")
                    }
                }
            }

            pendingOperationsDao.delete(operation.id)
            Timber.d("Successfully synced delete for task ${payload.taskId}")
        }

        private fun taskEntityToDomain(entity: TaskEntity) =
            com.nextcloud.tasks.domain.model.Task(
                id = entity.id,
                listId = entity.listId,
                title = entity.title,
                description = entity.description,
                completed = entity.completed,
                due = entity.due,
                updatedAt = entity.updatedAt,
                tags = emptyList(),
                priority = entity.priority,
                status = entity.status,
                completedAt = entity.completedAt,
                uid = entity.uid,
                etag = entity.etag,
                href = entity.href,
                parentUid = entity.parentUid,
            )

        /**
         * Force sync all pending operations now.
         * Returns true if all operations were processed successfully.
         */
        suspend fun syncNow(): Boolean =
            withContext(ioDispatcher) {
                if (!networkMonitor.isCurrentlyOnline()) {
                    Timber.d("Cannot sync, device is offline")
                    return@withContext false
                }

                processPendingOperations()

                val accountId = authTokenProvider.activeAccountId()
                val remainingCount =
                    if (accountId != null) {
                        pendingOperationsDao.getPendingCount(accountId)
                    } else {
                        0
                    }

                remainingCount == 0
            }

        companion object {
            private const val MAX_RETRIES = 5
        }
    }

/**
 * Payload for task update operations.
 */
data class TaskPayload(
    val id: String,
    val accountId: String,
    val listId: String,
    val title: String,
    val description: String?,
    val completed: Boolean,
    val due: Instant?,
    val updatedAt: Instant,
    val priority: Int?,
    val status: String?,
    val completedAt: Instant?,
    val uid: String?,
    val etag: String?,
    val href: String?,
    val parentUid: String?,
)

/**
 * Payload for delete operations.
 */
data class DeletePayload(
    val taskId: String,
    val href: String?,
    val etag: String?,
)

/**
 * Moshi adapter for Instant.
 */
class InstantAdapter {
    @com.squareup.moshi.ToJson
    fun toJson(instant: Instant?): Long? = instant?.toEpochMilli()

    @com.squareup.moshi.FromJson
    fun fromJson(epochMilli: Long?): Instant? = epochMilli?.let { Instant.ofEpochMilli(it) }
}
