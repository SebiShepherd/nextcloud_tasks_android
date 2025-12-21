package com.nextcloud.tasks.domain.model

/**
 * Sort options for displaying tasks
 */
enum class TaskSort {
    /** Sort by due date (earliest first) */
    DUE_DATE,

    /** Sort by priority (highest first) */
    PRIORITY,

    /** Sort by title (alphabetically) */
    TITLE,

    /** Sort by creation/update time (newest first) */
    UPDATED_AT,
}
