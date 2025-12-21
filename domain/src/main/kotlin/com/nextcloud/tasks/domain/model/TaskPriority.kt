package com.nextcloud.tasks.domain.model

enum class TaskPriority(val weight: Int) {
    LOW(0),
    MEDIUM(1),
    HIGH(2),
    URGENT(3),
    ;

    companion object {
        fun fromRaw(raw: String?): TaskPriority =
            entries.firstOrNull { priority ->
                priority.name.equals(raw, ignoreCase = true)
            } ?: MEDIUM
    }
}
