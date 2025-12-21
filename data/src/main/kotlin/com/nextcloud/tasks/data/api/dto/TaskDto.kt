package com.nextcloud.tasks.data.api.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TaskDto(
    @Json(name = "id")
    val id: String,
    @Json(name = "list_id")
    val listId: String,
    val title: String,
    val description: String?,
    val completed: Boolean,
    val priority: Int = 0,
    val due: Long?,
    @Json(name = "updated_at")
    val updatedAt: Long,
    @Json(name = "tag_ids")
    val tagIds: List<String> = emptyList(),
)
