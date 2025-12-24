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

                // Parse RELATED-TO property for sub-task support
                val relatedTo = vtodo.getProperty<net.fortuna.ical4j.model.property.RelatedTo>("RELATED-TO")
                val parentUid = relatedTo?.value

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
                    parentUid = parentUid,
                )
            } catch (e: Exception) {
                // Log error and return null for malformed data
                timber.log.Timber.w(e, "Failed to parse VTODO")
                null
            }
        }

        /**
         * Parse multiple VTODOs from iCalendar data
         * Handles both single VCALENDAR with multiple VTODOs and multiple VCALENDAR blocks
         */
        fun parseVTodos(
            icalData: String,
            listId: String,
            href: String,
            etag: String,
        ): List<TaskEntity> {
            // First try to parse as a single calendar with multiple VTODOs
            try {
                val builder = CalendarBuilder()
                val calendar = builder.build(StringReader(icalData))

                @Suppress("UNCHECKED_CAST")
                val vtodos = calendar.getComponents<VToDo>("VTODO") as? List<VToDo> ?: emptyList()

                val tasks =
                    vtodos.mapNotNull { vtodo ->
                        parseVTodoComponent(vtodo, listId, href, etag)
                    }

                if (tasks.isNotEmpty()) {
                    return tasks
                }
            } catch (e: Exception) {
                timber.log.Timber.d(e, "Failed to parse as single calendar, trying to split into multiple calendars")
            }

            // Fallback: Split into separate VCALENDAR blocks and parse each
            return try {
                val calendarBlocks = splitVCalendarBlocks(icalData)
                timber.log.Timber.d("Split iCalendar data into ${calendarBlocks.size} blocks")

                calendarBlocks.flatMap { block ->
                    try {
                        val builder = CalendarBuilder()
                        val calendar = builder.build(StringReader(block))

                        @Suppress("UNCHECKED_CAST")
                        val vtodos = calendar.getComponents<VToDo>("VTODO") as? List<VToDo> ?: emptyList()

                        vtodos.mapNotNull { vtodo ->
                            parseVTodoComponent(vtodo, listId, href, etag)
                        }
                    } catch (e: Exception) {
                        timber.log.Timber.w(e, "Failed to parse calendar block")
                        emptyList()
                    }
                }
            } catch (e: Exception) {
                timber.log.Timber.w(e, "Failed to parse VTODOs with fallback")
                emptyList()
            }
        }

        /**
         * Extract VTODO blocks from iCalendar data and wrap each in a minimal VCALENDAR
         * This handles nested/complex VCALENDAR structures
         */
        private fun splitVCalendarBlocks(icalData: String): List<String> {
            val vtodoBlocks = mutableListOf<String>()
            val lines = icalData.lines()
            var currentVTodo = mutableListOf<String>()
            var inVTodo = false
            var depth = 0

            for (line in lines) {
                when {
                    line.startsWith("BEGIN:VTODO") -> {
                        inVTodo = true
                        depth++
                        currentVTodo.add(line)
                    }

                    line.startsWith("END:VTODO") -> {
                        currentVTodo.add(line)
                        depth--
                        if (depth == 0 && inVTodo) {
                            // Wrap VTODO in minimal VCALENDAR
                            val wrappedVTodo =
                                """
                                BEGIN:VCALENDAR
                                VERSION:2.0
                                PRODID:-//Nextcloud Tasks Android//EN
                                ${currentVTodo.joinToString("\n")}
                                END:VCALENDAR
                                """.trimIndent()
                            vtodoBlocks.add(wrappedVTodo)
                            currentVTodo = mutableListOf()
                            inVTodo = false
                        }
                    }

                    inVTodo -> {
                        currentVTodo.add(line)
                    }
                }
            }

            timber.log.Timber.d("Extracted ${vtodoBlocks.size} VTODO blocks from iCalendar data")
            return vtodoBlocks
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

                // Parse RELATED-TO property for sub-task support
                val relatedTo = vtodo.getProperty<net.fortuna.ical4j.model.property.RelatedTo>("RELATED-TO")
                val parentUid = relatedTo?.value

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
                    parentUid = parentUid,
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
