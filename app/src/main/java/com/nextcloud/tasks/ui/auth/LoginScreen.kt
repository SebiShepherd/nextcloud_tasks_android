package com.nextcloud.tasks.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import com.nextcloud.tasks.auth.AuthUiMethod
import com.nextcloud.tasks.auth.LoginActions
import com.nextcloud.tasks.auth.LoginUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    state: LoginUiState,
    actions: LoginActions,
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text(text = stringResource(id = R.string.app_name)) })
        },
    ) { padding ->
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
            ServerUrlField(state, actions.onServerUrlChange)
            AuthMethodSelector(state.authMethod, actions.onAuthMethodChange)
            AuthFields(state, actions)
            ValidationMessage(state.validationMessage)
            ErrorMessage(state.error)
            SubmitButton(state.isLoading, actions.onSubmit)
            CertificateWarning(state.error)
        }
    }
}

@Composable
private fun ServerUrlField(
    state: LoginUiState,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = state.serverUrl,
        onValueChange = onChange,
        label = { Text(stringResource(id = R.string.server_url_label)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun AuthFields(
    state: LoginUiState,
    actions: LoginActions,
) {
    when (state.authMethod) {
        AuthUiMethod.PASSWORD -> CredentialsFields(state, actions.onUsernameChange, actions.onPasswordChange)
        AuthUiMethod.OAUTH -> OAuthFields(state, actions.onAuthorizationCodeChange, actions.onRedirectUriChange)
    }
}

@Composable
private fun ValidationMessage(message: String?) {
    if (message == null) return

    Text(
        text = message,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
private fun ErrorMessage(message: String?) {
    if (message == null) return

    Text(
        text = message,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodyMedium,
    )
}

@Composable
private fun SubmitButton(
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
private fun CertificateWarning(error: String?) {
    val showCertificateWarning = error?.contains("certificate", ignoreCase = true) == true
    if (!showCertificateWarning) return

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
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = state.username,
        onValueChange = onUsernameChange,
        label = { Text(stringResource(id = R.string.username_label)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = state.password,
        onValueChange = onPasswordChange,
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
    onCodeChange: (String) -> Unit,
    onRedirectUriChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = state.authorizationCode,
        onValueChange = onCodeChange,
        label = { Text(stringResource(id = R.string.authorization_code_label)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = state.redirectUri,
        onValueChange = onRedirectUriChange,
        label = { Text(stringResource(id = R.string.redirect_uri_label)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}
