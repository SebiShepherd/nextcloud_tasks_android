package com.nextcloud.tasks.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.nextcloud.tasks.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    state: LoginUiState,
    callbacks: LoginCallbacks,
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text(text = stringResource(id = R.string.app_name)) })
        },
    ) { padding ->
        LoginContent(
            padding = padding,
            state = state,
            callbacks = callbacks,
        )
    }
}

@Composable
private fun LoginContent(
    padding: PaddingValues,
    state: LoginUiState,
    callbacks: LoginCallbacks,
) {
    Column(
        modifier =
            Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(id = R.string.welcome_message),
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = stringResource(id = R.string.empty_task_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
            value = state.serverUrl,
            onValueChange = callbacks.onServerUrlChange,
            label = { Text(stringResource(id = R.string.server_url_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        AuthMethodSelector(
            selected = state.authMethod,
            onSelected = callbacks.onAuthMethodChange,
        )

        when (state.authMethod) {
            AuthUiMethod.PASSWORD -> CredentialsFields(state, callbacks)
            AuthUiMethod.OAUTH -> OAuthFields(state, callbacks)
        }

        LoginMessages(state = state)

        LoginButton(
            isLoading = state.isLoading,
            onSubmit = callbacks.onSubmit,
        )

        CertificateWarning(show = state.error?.contains("certificate", ignoreCase = true) == true)
    }
}

@Composable
private fun LoginMessages(state: LoginUiState) {
    state.validationMessage?.let { message ->
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
        )
    }

    state.error?.let { error ->
        Text(
            text = error,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun LoginButton(
    isLoading: Boolean,
    onSubmit: () -> Unit,
) {
    Button(
        onClick = onSubmit,
        modifier = Modifier.fillMaxWidth(),
        enabled = !isLoading,
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.padding(end = 8.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
        Text(text = stringResource(id = R.string.login_action))
    }
}

@Composable
private fun CertificateWarning(show: Boolean) {
    if (!show) return

    Text(
        text = stringResource(id = R.string.invalid_certificate),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun AuthMethodSelector(
    selected: AuthUiMethod,
    onSelected: (AuthUiMethod) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = selected == AuthUiMethod.PASSWORD,
            onClick = { onSelected(AuthUiMethod.PASSWORD) },
            label = { Text(text = stringResource(id = R.string.login_method_password)) },
        )
        FilterChip(
            selected = selected == AuthUiMethod.OAUTH,
            onClick = { onSelected(AuthUiMethod.OAUTH) },
            label = { Text(text = stringResource(id = R.string.login_method_oauth)) },
        )
    }
}

@Composable
private fun CredentialsFields(
    state: LoginUiState,
    callbacks: LoginCallbacks,
) {
    OutlinedTextField(
        value = state.username,
        onValueChange = callbacks.onUsernameChange,
        label = { Text(stringResource(id = R.string.username_label)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = state.password,
        onValueChange = callbacks.onPasswordChange,
        label = { Text(stringResource(id = R.string.password_label)) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions =
            KeyboardOptions(
                imeAction = ImeAction.Done,
            ),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun OAuthFields(
    state: LoginUiState,
    callbacks: LoginCallbacks,
) {
    OutlinedTextField(
        value = state.authorizationCode,
        onValueChange = callbacks.onAuthorizationCodeChange,
        label = { Text(stringResource(id = R.string.authorization_code_label)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = state.redirectUri,
        onValueChange = callbacks.onRedirectUriChange,
        label = { Text(stringResource(id = R.string.redirect_uri_label)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}
