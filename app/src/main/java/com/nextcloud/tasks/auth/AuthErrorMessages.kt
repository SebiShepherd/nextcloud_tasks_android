package com.nextcloud.tasks.auth

import androidx.annotation.StringRes
import com.nextcloud.tasks.R
import com.nextcloud.tasks.domain.model.AuthFailure
import com.nextcloud.tasks.domain.model.ServerUrlError

/**
 * Maps a typed [AuthFailure] (or any other throwable) to a user-facing string resource.
 * Pure and Context-free so the mapping can be unit-tested; the ViewModel resolves the id.
 */
@StringRes
internal fun authErrorMessageRes(throwable: Throwable): Int =
    when (throwable) {
        is AuthFailure.InvalidServerUrl -> serverUrlErrorMessageRes(throwable.error)
        is AuthFailure.InvalidCredentials -> R.string.error_invalid_credentials
        is AuthFailure.Network ->
            if (throwable.statusCode != null) R.string.error_server else R.string.error_network

        is AuthFailure.Certificate -> R.string.invalid_certificate
        is AuthFailure.ImportNotSupported -> R.string.error_import_not_supported
        else -> R.string.error_login_failed
    }

@StringRes
internal fun serverUrlErrorMessageRes(error: ServerUrlError): Int =
    when (error) {
        ServerUrlError.EMPTY -> R.string.error_server_url_empty
        ServerUrlError.INVALID -> R.string.error_server_url_invalid
        ServerUrlError.INSECURE_HTTP -> R.string.error_server_url_insecure
    }
