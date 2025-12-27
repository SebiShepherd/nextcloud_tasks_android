package com.nextcloud.tasks.data.api.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Response DTO for initiating Login Flow v2.
 * Example response:
 * {
 *   "poll": {
 *     "token": "abc123",
 *     "endpoint": "https://cloud.example.com/login/v2/poll"
 *   },
 *   "login": "https://cloud.example.com/login/v2/flow/abc123"
 * }
 */
@JsonClass(generateAdapter = true)
data class LoginFlowV2InitiationDto(
    @Json(name = "poll")
    val poll: PollDto,
    @Json(name = "login")
    val login: String,
)

@JsonClass(generateAdapter = true)
data class PollDto(
    @Json(name = "token")
    val token: String,
    @Json(name = "endpoint")
    val endpoint: String,
)

/**
 * Response DTO for successful Login Flow v2 polling.
 * Example response (HTTP 200):
 * {
 *   "server": "https://cloud.example.com",
 *   "loginName": "username",
 *   "appPassword": "generated-app-password"
 * }
 */
@JsonClass(generateAdapter = true)
data class LoginFlowV2CredentialsDto(
    @Json(name = "server")
    val server: String,
    @Json(name = "loginName")
    val loginName: String,
    @Json(name = "appPassword")
    val appPassword: String,
)
