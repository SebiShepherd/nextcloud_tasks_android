package com.nextcloud.tasks.auth

data class LoginCallbacks(
    val onServerUrlChange: (String) -> Unit,
    val onUsernameChange: (String) -> Unit,
    val onPasswordChange: (String) -> Unit,
    val onAuthorizationCodeChange: (String) -> Unit,
    val onRedirectUriChange: (String) -> Unit,
    val onAuthMethodChange: (AuthUiMethod) -> Unit,
    val onSubmit: () -> Unit,
)
