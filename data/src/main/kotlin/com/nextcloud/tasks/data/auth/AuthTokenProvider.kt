package com.nextcloud.tasks.data.auth

interface AuthTokenProvider {
    fun activeToken(): AuthToken?

    fun activeServerUrl(): String?

    fun activeUsername(): String?
}
