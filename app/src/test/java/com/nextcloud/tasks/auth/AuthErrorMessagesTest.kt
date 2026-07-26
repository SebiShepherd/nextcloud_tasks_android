package com.nextcloud.tasks.auth

import com.nextcloud.tasks.R
import com.nextcloud.tasks.domain.model.AuthFailure
import com.nextcloud.tasks.domain.model.ServerUrlError
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals

class AuthErrorMessagesTest {
    @Test
    fun `server url errors map to their string resource`() {
        assertEquals(R.string.error_server_url_empty, serverUrlErrorMessageRes(ServerUrlError.EMPTY))
        assertEquals(R.string.error_server_url_invalid, serverUrlErrorMessageRes(ServerUrlError.INVALID))
        assertEquals(R.string.error_server_url_insecure, serverUrlErrorMessageRes(ServerUrlError.INSECURE_HTTP))
    }

    @Test
    fun `invalid server url failure delegates to the server-url mapping`() {
        assertEquals(
            R.string.error_server_url_insecure,
            authErrorMessageRes(AuthFailure.InvalidServerUrl(ServerUrlError.INSECURE_HTTP)),
        )
    }

    @Test
    fun `invalid credentials maps to credentials string`() {
        assertEquals(R.string.error_invalid_credentials, authErrorMessageRes(AuthFailure.InvalidCredentials))
    }

    @Test
    fun `network failure with status code maps to server error`() {
        assertEquals(R.string.error_server, authErrorMessageRes(AuthFailure.Network(statusCode = 500)))
    }

    @Test
    fun `network failure without status code maps to network error`() {
        assertEquals(R.string.error_network, authErrorMessageRes(AuthFailure.Network()))
    }

    @Test
    fun `certificate failure maps to invalid certificate string`() {
        assertEquals(R.string.invalid_certificate, authErrorMessageRes(AuthFailure.Certificate))
    }

    @Test
    fun `import-not-supported maps to its string`() {
        assertEquals(R.string.error_import_not_supported, authErrorMessageRes(AuthFailure.ImportNotSupported))
    }

    @Test
    fun `unknown auth failure and arbitrary throwables fall back to generic login-failed`() {
        assertEquals(R.string.error_login_failed, authErrorMessageRes(AuthFailure.Unknown))
        assertEquals(R.string.error_login_failed, authErrorMessageRes(IOException("boom")))
    }
}
