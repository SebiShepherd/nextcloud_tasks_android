package com.nextcloud.tasks.data.database.model

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.nextcloud.tasks.data.database.entity.TagEntity
import com.nextcloud.tasks.data.database.entity.TaskEntity
import com.nextcloud.tasks.data.database.entity.TaskListEntity
import com.nextcloud.tasks.data.database.entity.TaskTagCrossRef

data class TaskWithRelations(
    @Embedded
    val task: TaskEntity,
    @Relation(
        parentColumn = "list_id",
        entityColumn = "id",
    )
    val list: TaskListEntity?,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = TaskTagCrossRef::class,
            parentColumn = "task_id",
            entityColumn = "tag_id",
        ),
    )
    val tags: List<TagEntity>,
)
