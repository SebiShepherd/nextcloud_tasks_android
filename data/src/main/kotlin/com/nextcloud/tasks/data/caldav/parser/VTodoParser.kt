package com.nextcloud.tasks.data.caldav.parser

import com.nextcloud.tasks.data.database.entity.TaskEntity
import net.fortuna.ical4j.data.CalendarBuilder
import net.fortuna.ical4j.model.component.VToDo
import java.io.StringReader
import java.time.Instant
import javax.inject.Inject

/**
 * Parser for VTODO iCalendar components
 */
class VTodoParser
    @Inject
    constructor() {
        /**
         * Parse iCalendar data and convert VTODO to TaskEntity
         */
        fun parseVTodo(
            icalData: String,
            listId: String,
            href: String,
            etag: String,
        ): TaskEntity? {
            return try {
                val builder = CalendarBuilder()
                val calendar = builder.build(StringReader(icalData))

                @Suppress("UNCHECKED_CAST")
                val vtodo = calendar.getComponent<VToDo>("VTODO") as? VToDo ?: return null

                val uid = vtodo.uid?.value ?: return null
                val summary = vtodo.summary?.value ?: ""
                val description = vtodo.description?.value
                val status = vtodo.status?.value
                val priority = vtodo.priority?.level
                val completed = status == "COMPLETED"
                val completedAt = vtodo.dateCompleted?.date?.toInstant()
                val due = vtodo.due?.date?.toInstant()
                val lastModified =
                    vtodo.lastModified?.date?.toInstant() ?: Instant.now()

                TaskEntity(
                    id = generateTaskId(uid, listId),
                    listId = listId,
                    title = summary,
                    description = description,
                    completed = completed,
                    due = due,
                    updatedAt = lastModified,
                    priority = priority,
                    status = status,
                    completedAt = completedAt,
                    uid = uid,
                    etag = etag,
                    href = href,
                )
            } catch (e: Exception) {
                // Log error and return null for malformed data
                timber.log.Timber.w(e, "Failed to parse VTODO")
                null
            }
        }

        /**
         * Parse multiple VTODOs from iCalendar data
         */
        fun parseVTodos(
            icalData: String,
            listId: String,
            href: String,
            etag: String,
        ): List<TaskEntity> {
            return try {
                val builder = CalendarBuilder()
                val calendar = builder.build(StringReader(icalData))

                @Suppress("UNCHECKED_CAST")
                val vtodos = calendar.getComponents<VToDo>("VTODO") as? List<VToDo> ?: emptyList()

                vtodos.mapNotNull { vtodo ->
                    parseVTodoComponent(vtodo, listId, href, etag)
                }
            } catch (e: Exception) {
                timber.log.Timber.w(e, "Failed to parse VTODOs")
                emptyList()
            }
        }

        private fun parseVTodoComponent(
            vtodo: VToDo,
            listId: String,
            href: String,
            etag: String,
        ): TaskEntity? {
            return try {
                val uid = vtodo.uid?.value ?: return null
                val summary = vtodo.summary?.value ?: ""
                val description = vtodo.description?.value
                val status = vtodo.status?.value
                val priority = vtodo.priority?.level
                val completed = status == "COMPLETED"
                val completedAt = vtodo.dateCompleted?.date?.toInstant()
                val due = vtodo.due?.date?.toInstant()
                val lastModified =
                    vtodo.lastModified?.date?.toInstant() ?: Instant.now()

                TaskEntity(
                    id = generateTaskId(uid, listId),
                    listId = listId,
                    title = summary,
                    description = description,
                    completed = completed,
                    due = due,
                    updatedAt = lastModified,
                    priority = priority,
                    status = status,
                    completedAt = completedAt,
                    uid = uid,
                    etag = etag,
                    href = href,
                )
            } catch (e: Exception) {
                timber.log.Timber.w(e, "Failed to parse VTODO component")
                null
            }
        }

        private fun generateTaskId(
            uid: String,
            listId: String,
        ): String = "$listId/$uid"
    }
