package com.nextcloud.tasks.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nextcloud.tasks.R
import com.nextcloud.tasks.ui.theme.NextcloudBlue

@Composable
fun ServerInputScreen(
    onLoginSuccess: () -> Unit,
    onBack: () -> Unit = {},
    viewModel: LoginFlowViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    // Handle back button - navigate back instead of closing app
    androidx.activity.compose.BackHandler {
        onBack()
    }

    // Reset loginSuccess when screen is disposed (not when it appears!)
    // This prevents stale loginSuccess=true from previous login
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            viewModel.resetLoginSuccess()
        }
    }

    // Navigate away on successful login
    if (state.loginSuccess) {
        onLoginSuccess()
    }

    // Full-screen NC Blue background
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(NextcloudBlue),
    ) {
        ServerInputContent(
            state = state,
            onServerUrlChange = viewModel::updateServerUrl,
            onSubmit = viewModel::startLoginFlow,
            onCancel = viewModel::cancelLogin,
        )
    }
}

@Composable
private fun ServerInputContent(
    state: LoginFlowUiState,
    onServerUrlChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Nextcloud Tasks Icon (centered)
        Image(
            painter = painterResource(R.mipmap.ic_launcher_foreground),
            contentDescription = stringResource(R.string.app_name),
            modifier = Modifier.size(120.dp),
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Server URL input with white styling
        OutlinedTextField(
            value = state.serverUrl,
            onValueChange = onServerUrlChange,
            label = { Text(stringResource(R.string.server_url_hint), color = Color.White) },
            placeholder = {
                Text(
                    stringResource(R.string.server_url_placeholder),
                    color = Color.White.copy(alpha = 0.6f),
                )
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !state.isLoading,
            colors =
                OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    disabledTextColor = Color.White.copy(alpha = 0.6f),
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.7f),
                    cursorColor = Color.White,
                ),
            trailingIcon = {
                if (!state.isLoading) {
                    IconButton(onClick = onSubmit, enabled = state.serverUrl.isNotBlank()) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = stringResource(R.string.login_action),
                            tint = Color.White,
                        )
                    }
                }
            },
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Help text in white
        Text(
            text = stringResource(R.string.server_url_description),
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.9f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

        // Error message
        state.error?.let { error ->
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = error,
                color = Color(0xFFFFCDD2), // Light red on blue background
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }

        // Loading state
        if (state.isLoading) {
            Spacer(modifier = Modifier.height(32.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CircularProgressIndicator(color = Color.White)
                Text(
                    text = stringResource(R.string.waiting_for_login),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = Color.White,
                )
                Button(
                    onClick = onCancel,
                    colors =
                        ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.2f),
                            contentColor = Color.White,
                        ),
                ) {
                    Icon(Icons.Default.Close, contentDescription = null)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    }
}
