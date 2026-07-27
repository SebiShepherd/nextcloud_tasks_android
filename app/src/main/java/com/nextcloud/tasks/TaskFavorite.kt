package com.nextcloud.tasks

import com.nextcloud.tasks.domain.model.Task

/**
 * A task is a favourite ("starred") when its iCalendar PRIORITY is 1-4. This mirrors the Nextcloud
 * web client (`priority > 0 && priority < 5`), so favourites round-trip without a schema change.
 * The star in the list and the priority picker in the detail screen write the same [Task.priority]
 * field and must both read starred-ness from here so they never drift apart.
 */
val Task.isStarred: Boolean
    get() = priority?.let { it in 1..4 } ?: false

/** Priority value written when starring a task (matches the web client's `toggleStarred`). */
const val STARRED_PRIORITY = 1

/**
 * Whether the task counts as "done" for display and filtering. CANCELLED is treated like COMPLETED
 * (the web UI lists it under completed tasks with a strikethrough).
 */
val Task.isEffectivelyDone: Boolean
    get() = completed || status?.uppercase() == "CANCELLED"
