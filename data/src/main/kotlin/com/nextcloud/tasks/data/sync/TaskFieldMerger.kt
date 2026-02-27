package com.nextcloud.tasks.data.sync

import com.nextcloud.tasks.data.database.entity.TaskEntity
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import timber.log.Timber
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Snapshot of a task's mergeable fields, stored as JSON in the base_snapshot column.
 * Represents the last known server state at the time of the last successful sync.
 */
data class TaskSnapshot(
    val title: String,
    val description: String?,
    val completed: Boolean,
    val due: Long?,
    val priority: Int?,
    val status: String?,
    val completedAt: Long?,
    val parentUid: String?,
)

/**
 * Performs field-level merging of tasks during sync.
 *
 * Instead of simple last-write-wins (which can lose changes to different fields),
 * this merger compares each field individually against a base snapshot (the last
 * known server state). This way, local changes to one field are preserved even
 * when the server changed a different field.
 *
 * When both sides changed the same field to different values (a true conflict),
 * the server value wins.
 */
@Singleton
class TaskFieldMerger
    @Inject
    constructor() {
        private val moshi =
            Moshi
                .Builder()
                .add(KotlinJsonAdapterFactory())
                .build()
        private val snapshotAdapter = moshi.adapter(TaskSnapshot::class.java)

        /**
         * Creates a JSON snapshot string from a TaskEntity's mergeable fields.
         */
        fun createSnapshot(task: TaskEntity): String =
            snapshotAdapter.toJson(
                TaskSnapshot(
                    title = task.title,
                    description = task.description,
                    completed = task.completed,
                    due = task.due?.toEpochMilli(),
                    priority = task.priority,
                    status = task.status,
                    completedAt = task.completedAt?.toEpochMilli(),
                    parentUid = task.parentUid,
                ),
            )

        /**
         * Parses a JSON snapshot string back into a TaskSnapshot.
         */
        private fun parseSnapshot(json: String): TaskSnapshot? =
            try {
                snapshotAdapter.fromJson(json)
            } catch (
                @Suppress("TooGenericExceptionCaught") e: Exception,
            ) {
                Timber.w(e, "Failed to parse base snapshot")
                null
            }

        /**
         * Merges a server task with the local task using field-level comparison.
         *
         * For each field:
         * - If only the server changed it (relative to base): take server value
         * - If only local changed it (relative to base): keep local value
         * - If both changed to the same value: no conflict, use that value
         * - If both changed to different values: server wins (conflict resolved)
         * - If neither changed: take server value (they're the same anyway)
         *
         * @param serverTask The task as received from the server
         * @param localTask The current local task
         * @return The merged task entity with updated base_snapshot, or null if no merge needed
         *         (server task should be used as-is)
         */
        fun mergeTask(
            serverTask: TaskEntity,
            localTask: TaskEntity,
        ): TaskEntity {
            val baseSnapshot = localTask.baseSnapshot?.let { parseSnapshot(it) }

            // If no base snapshot exists, we can't do field-level merge — fall back to server wins
            if (baseSnapshot == null) {
                Timber.d("No base snapshot for task ${localTask.id}, using server version")
                return serverTask.copy(baseSnapshot = createSnapshot(serverTask))
            }

            // Merge each field individually
            val mergedTitle = mergeField("title", baseSnapshot.title, localTask.title, serverTask.title)
            val mergedDescription =
                mergeField("description", baseSnapshot.description, localTask.description, serverTask.description)
            val mergedCompleted =
                mergeField("completed", baseSnapshot.completed, localTask.completed, serverTask.completed)
            val mergedDue = mergeField("due", fromEpoch(baseSnapshot.due), localTask.due, serverTask.due)
            val mergedPriority =
                mergeField("priority", baseSnapshot.priority, localTask.priority, serverTask.priority)
            val mergedStatus = mergeField("status", baseSnapshot.status, localTask.status, serverTask.status)
            val mergedCompletedAt =
                mergeField(
                    "completedAt",
                    fromEpoch(baseSnapshot.completedAt),
                    localTask.completedAt,
                    serverTask.completedAt,
                )
            val mergedParentUid =
                mergeField("parentUid", baseSnapshot.parentUid, localTask.parentUid, serverTask.parentUid)

            // Build merged entity — use server's etag/href/updatedAt as canonical
            val merged =
                serverTask.copy(
                    title = mergedTitle,
                    description = mergedDescription,
                    completed = mergedCompleted,
                    due = mergedDue,
                    priority = mergedPriority,
                    status = mergedStatus,
                    completedAt = mergedCompletedAt,
                    parentUid = mergedParentUid,
                    baseSnapshot = createSnapshot(serverTask),
                )

            return merged
        }

        /**
         * Three-way merge for a single field.
         * Returns the value that should be used after merge.
         */
        private fun <T> mergeField(
            fieldName: String,
            base: T,
            local: T,
            server: T,
        ): T {
            val localChanged = local != base
            val serverChanged = server != base

            return when {
                !localChanged && !serverChanged -> server // Neither changed
                !localChanged && serverChanged -> server // Only server changed
                localChanged && !serverChanged -> local // Only local changed — preserve local edit
                local == server -> server // Both changed to same value
                else -> {
                    // True conflict: both changed to different values — server wins
                    Timber.d("Field-level conflict on '$fieldName': local='$local', server='$server' → server wins")
                    server
                }
            }
        }

        private fun fromEpoch(epochMilli: Long?): Instant? = epochMilli?.let { Instant.ofEpochMilli(it) }
    }
