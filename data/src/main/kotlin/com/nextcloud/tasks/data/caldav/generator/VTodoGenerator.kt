package com.nextcloud.tasks.data.caldav.generator

import com.nextcloud.tasks.domain.model.Task
import net.fortuna.ical4j.model.Calendar
import net.fortuna.ical4j.model.Date
import net.fortuna.ical4j.model.DateTime
import net.fortuna.ical4j.model.component.VToDo
import net.fortuna.ical4j.model.property.CalScale
import net.fortuna.ical4j.model.property.Completed
import net.fortuna.ical4j.model.property.DtStamp
import net.fortuna.ical4j.model.property.ProdId
import net.fortuna.ical4j.model.property.Status
import net.fortuna.ical4j.model.property.Uid
import net.fortuna.ical4j.model.property.Version
import java.util.UUID
import javax.inject.Inject

/**
 * Generates iCalendar VTODO components from Task domain objects
 */
class VTodoGenerator
    @Inject
    constructor() {
        /**
         * Generate a complete iCalendar with VTODO for a task
         */
        fun generateVTodo(task: Task): String {
            val calendar = Calendar()
            calendar.properties.add(ProdId("-//Nextcloud Tasks Android//EN"))
            calendar.properties.add(Version.VERSION_2_0)
            calendar.properties.add(CalScale.GREGORIAN)

            val vtodo = VToDo()

            // UID - use existing or generate new
            val uid = task.uid ?: UUID.randomUUID().toString()
            vtodo.properties.add(Uid(uid))

            // DTSTAMP - current timestamp
            vtodo.properties.add(DtStamp(DateTime()))

            // SUMMARY - task title (always include for CalDAV compatibility)
            vtodo.properties.add(net.fortuna.ical4j.model.property.Summary(task.title))

            // DESCRIPTION
            task.description?.let {
                vtodo.properties.add(net.fortuna.ical4j.model.property.Description(it))
            }

            // STATUS and COMPLETED
            if (task.completed) {
                vtodo.properties.add(Status.VTODO_COMPLETED)
                task.completedAt?.let { completedAt ->
                    vtodo.properties.add(Completed(DateTime(java.util.Date.from(completedAt))))
                }
            } else {
                vtodo.properties.add(Status.VTODO_NEEDS_ACTION)
            }

            // PRIORITY
            task.priority?.let { priority ->
                vtodo.properties.add(net.fortuna.ical4j.model.property.Priority(priority))
            }

            // DUE
            task.due?.let { due ->
                vtodo.properties.add(net.fortuna.ical4j.model.property.Due(DateTime(java.util.Date.from(due))))
            }

            // CATEGORIES (tags)
            if (task.tags.isNotEmpty()) {
                val categories = net.fortuna.ical4j.model.property.Categories()
                task.tags.forEach { tag ->
                    categories.categories.add(tag.name)
                }
                vtodo.properties.add(categories)
            }

            calendar.components.add(vtodo)
            return calendar.toString()
        }

        /**
         * Generate a filename for a new task
         */
        fun generateFilename(uid: String): String = "$uid.ics"
    }
