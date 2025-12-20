package com.nextcloud.tasks.data.network.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
data class OcsResponse<T>(
    @Json(name = "ocs") val body: OcsBody<T>,
)

@JsonClass(generateAdapter = false)
data class OcsBody<T>(
    @Json(name = "meta") val meta: OcsMeta,
    @Json(name = "data") val data: T,
)

@JsonClass(generateAdapter = false)
data class OcsMeta(
    @Json(name = "statuscode") val statusCode: Int,
    @Json(name = "message") val message: String?,
)

@JsonClass(generateAdapter = false)
data class UserResponse(
    @Json(name = "display-name") val displayName: String?,
    @Json(name = "id") val id: String?,
    @Json(name = "email") val email: String?,
)

@JsonClass(generateAdapter = false)
data class OAuthTokenResponse(
    @Json(name = "access_token") val accessToken: String,
    @Json(name = "refresh_token") val refreshToken: String?,
    @Json(name = "token_type") val tokenType: String?,
    @Json(name = "expires_in") val expiresIn: Long?,
)
