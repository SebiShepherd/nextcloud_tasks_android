package com.nextcloud.tasks.auth

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.nextcloud.tasks.R
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerInputScreen(
    onLoginSuccess: () -> Unit,
    onBack: () -> Unit = {},
    viewModel: LoginFlowViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

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

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        ServerInputContent(
            padding = padding,
            state = state,
            onServerUrlChange = viewModel::updateServerUrl,
            onSubmit = viewModel::startLoginFlow,
            onCancel = viewModel::cancelLogin,
            onImportAccount = {
                // Use system account picker via SSO library
                activity?.let { act ->
                    AccountImportHelper.pickAccount(
                        activity = act,
                        onAccountSelected = { account ->
                            // Extract username from account name (typically "username@server.com")
                            val username = account.name.substringBefore("@")
                            viewModel.startLoginFlowWithPrefill(
                                serverUrl = account.url,
                                username = username,
                            )
                        },
                        onError = { error ->
                            scope.launch {
                                snackbarHostState.showSnackbar(error)
                            }
                        },
                    )
                }
            },
        )
    }
}

@Composable
private fun ServerInputContent(
    padding: PaddingValues,
    state: LoginFlowUiState,
    onServerUrlChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onCancel: () -> Unit,
    onImportAccount: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Nextcloud logo
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = stringResource(R.string.app_name),
            modifier =
                Modifier
                    .size(120.dp)
                    .align(Alignment.CenterHorizontally),
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Server URL input
        OutlinedTextField(
            value = state.serverUrl,
            onValueChange = onServerUrlChange,
            label = { Text(stringResource(R.string.server_url_hint)) },
            placeholder = { Text("cloud.example.com") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !state.isLoading,
            trailingIcon = {
                if (!state.isLoading) {
                    IconButton(onClick = onSubmit, enabled = state.serverUrl.isNotBlank()) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = stringResource(R.string.login_action),
                        )
                    }
                }
            },
        )

        // Help text
        Text(
            text = stringResource(R.string.server_url_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Start,
        )

        // Account import button
        if (!state.isLoading) {
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = onImportAccount,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.import_account))
            }
        }

        // Error message
        state.error?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // Loading state
        if (state.isLoading) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CircularProgressIndicator()
                Text(
                    text = stringResource(R.string.waiting_for_login),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
                Button(onClick = onCancel) {
                    Icon(Icons.Default.Close, contentDescription = null)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(stringResource(R.string.cancel))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
