package com.nextcloud.tasks.domain.model

/**
 * Filter options for displaying tasks
 */
enum class TaskFilter {
    /** Show all tasks (open and completed) */
    ALL,

    /** Show only current/open tasks */
    CURRENT,

    /** Show only completed tasks */
    COMPLETED,
}
