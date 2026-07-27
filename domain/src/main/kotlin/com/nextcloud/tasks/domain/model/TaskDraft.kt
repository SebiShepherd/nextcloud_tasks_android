package com.nextcloud.tasks.domain.model

import java.time.Instant

data class TaskDraft(
    val listId: String,
    val title: String,
    val description: String? = null,
    val completed: Boolean = false,
    val due: Instant? = null,
    val tagIds: List<String> = emptyList(),
    /** UID of the parent task when creating a sub-task (RELATED-TO). */
    val parentUid: String? = null,
    /** iCalendar PRIORITY (1-9); 1-4 renders as a favourite/star. */
    val priority: Int? = null,
)
