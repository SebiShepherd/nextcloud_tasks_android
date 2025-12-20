package com.nextcloud.tasks.data.network.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
data class TaskListDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "color") val color: String?,
    @Json(name = "last_modified") val lastModified: Long,
)

@JsonClass(generateAdapter = false)
data class TagDto(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "color") val color: String?,
)

@JsonClass(generateAdapter = false)
data class TaskDto(
    @Json(name = "id") val id: String,
    @Json(name = "title") val title: String,
    @Json(name = "description") val description: String?,
    @Json(name = "is_completed") val completed: Boolean,
    @Json(name = "list_id") val listId: String,
    @Json(name = "last_modified") val lastModified: Long,
    @Json(name = "due_date") val dueDate: Long?,
    @Json(name = "tags") val tags: List<TagDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class TaskRequest(
    @Json(name = "title") val title: String,
    @Json(name = "description") val description: String?,
    @Json(name = "is_completed") val completed: Boolean,
    @Json(name = "list_id") val listId: String,
    @Json(name = "due_date") val dueDate: Long?,
    @Json(name = "tags") val tags: List<String> = emptyList(),
)
