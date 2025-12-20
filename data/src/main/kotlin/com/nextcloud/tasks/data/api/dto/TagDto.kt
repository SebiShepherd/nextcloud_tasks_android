package com.nextcloud.tasks.data.api.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TagDto(
    @Json(name = "id")
    val id: String,
    val name: String,
    @Json(name = "updated_at")
    val updatedAt: Long,
)
