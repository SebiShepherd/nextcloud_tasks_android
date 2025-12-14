package com.nextcloud.tasks

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nextcloud.tasks.domain.model.Account
import com.nextcloud.tasks.domain.model.AuthType
import com.nextcloud.tasks.domain.model.Task
import com.nextcloud.tasks.domain.usecase.LoadTasksUseCase
import com.nextcloud.tasks.domain.usecase.LogoutUseCase
import com.nextcloud.tasks.domain.usecase.ObserveAccountsUseCase
import com.nextcloud.tasks.domain.usecase.ObserveActiveAccountUseCase
import com.nextcloud.tasks.domain.usecase.PerformBasicLoginUseCase
import com.nextcloud.tasks.domain.usecase.PerformOAuthLoginUseCase
import com.nextcloud.tasks.domain.usecase.SwitchAccountUseCase
import com.nextcloud.tasks.domain.usecase.ValidateServerUrlUseCase
import com.nextcloud.tasks.ui.theme.NextcloudTasksTheme
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val taskListViewModel: TaskListViewModel by viewModels()
    private val sessionViewModel: SessionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NextcloudTasksTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    MainScreen(taskListViewModel = taskListViewModel, sessionViewModel = sessionViewModel)
                }
            }
        }
    }
}

@HiltViewModel
class TaskListViewModel @Inject constructor(private val loadTasksUseCase: LoadTasksUseCase) : ViewModel() {
    val tasks =
        loadTasksUseCase()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch { loadTasksUseCase.seedSample() }
    }
}

data class SessionUiState(
    val serverUrl: String = "",
    val username: String = "",
    val password: String = "",
    val accessToken: String = "",
    val refreshToken: String = "",
    val displayName: String = "",
    val authType: AuthType = AuthType.BASIC,
    val isProcessing: Boolean = false,
    val errorMessage: String? = null,
    val certificateWarning: String? = null,
    val accounts: List<Account> = emptyList(),
    val activeAccount: Account? = null,
)

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val performBasicLoginUseCase: PerformBasicLoginUseCase,
    private val performOAuthLoginUseCase: PerformOAuthLoginUseCase,
    private val observeAccountsUseCase: ObserveAccountsUseCase,
    private val observeActiveAccountUseCase: ObserveActiveAccountUseCase,
    private val switchAccountUseCase: SwitchAccountUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val validateServerUrlUseCase: ValidateServerUrlUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            observeActiveAccountUseCase().collect { account ->
                _uiState.update { it.copy(activeAccount = account, errorMessage = null) }
            }
        }
        viewModelScope.launch { observeAccountsUseCase().collect { accounts -> _uiState.update { it.copy(accounts = accounts) } } }
    }

    fun updateServerUrl(value: String) {
        _uiState.update { it.copy(serverUrl = value, certificateWarning = null, errorMessage = null) }
    }

    fun updateUsername(value: String) {
        _uiState.update { it.copy(username = value, errorMessage = null) }
    }

    fun updatePassword(value: String) {
        _uiState.update { it.copy(password = value, errorMessage = null) }
    }

    fun updateAuthType(type: AuthType) {
        _uiState.update { it.copy(authType = type, errorMessage = null) }
    }

    fun updateAccessToken(value: String) {
        _uiState.update { it.copy(accessToken = value, errorMessage = null) }
    }

    fun updateRefreshToken(value: String) {
        _uiState.update { it.copy(refreshToken = value, errorMessage = null) }
    }

    fun updateDisplayName(value: String) {
        _uiState.update { it.copy(displayName = value, errorMessage = null) }
    }

    fun submitLogin() {
        val state = uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, errorMessage = null) }
            val validation = validateServerUrlUseCase(state.serverUrl)
            val normalizedServer = validation.getOrNull()
            val warning = normalizedServer?.let { server -> if (!server.isHttps) stringResourceHttpsWarning(server.normalizedUrl) else null }
            val preferredDisplayName = state.displayName.ifBlank { normalizedServer?.normalizedUrl ?: state.serverUrl }
            val result =
                when (state.authType) {
                    AuthType.BASIC ->
                        performBasicLoginUseCase(
                            serverUrl = state.serverUrl,
                            username = state.username,
                            password = state.password,
                        )

                    AuthType.OAUTH ->
                        performOAuthLoginUseCase(
                            serverUrl = state.serverUrl,
                            accessToken = state.accessToken,
                            refreshToken = state.refreshToken.ifBlank { null },
                            displayName = preferredDisplayName,
                        )
                }

            result
                .onSuccess { account ->
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            activeAccount = account,
                            certificateWarning = warning,
                            password = "",
                            accessToken = "",
                            refreshToken = "",
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isProcessing = false, errorMessage = error.message ?: "Login failed") }
                }
        }
    }

    private fun stringResourceHttpsWarning(url: String) =
        "${url} " + "is not using HTTPS. Certificates will be strictly validated."

    fun switchAccount(accountId: String) {
        viewModelScope.launch {
            switchAccountUseCase(accountId).onFailure { error ->
                _uiState.update { it.copy(errorMessage = error.message ?: "Unable to switch account") }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            logoutUseCase()
            _uiState.update { it.copy(password = "", accessToken = "", refreshToken = "") }
        }
    }
}

@Composable
fun MainScreen(taskListViewModel: TaskListViewModel, sessionViewModel: SessionViewModel) {
    val sessionState by sessionViewModel.uiState.collectAsState()

    if (sessionState.activeAccount == null) {
        LoginScreen(state = sessionState, onAction = sessionViewModel)
    } else {
        TaskListScreen(
            viewModel = taskListViewModel,
            sessionState = sessionState,
            onSwitchAccount = sessionViewModel::switchAccount,
            onLogout = sessionViewModel::logout,
        )
    }
}

@Composable
fun LoginScreen(state: SessionUiState, onAction: SessionViewModel) {
    val spacing = 16.dp
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = spacing, vertical = 24.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = stringResource(id = R.string.login_title), style = MaterialTheme.typography.headlineSmall)
        Text(
            text = stringResource(id = R.string.login_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
            textAlign = TextAlign.Center,
        )
        LoginForm(state = state, onAction = onAction)
    }
}

@Composable
fun LoginForm(state: SessionUiState, onAction: SessionViewModel) {
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = state.serverUrl,
                onValueChange = onAction::updateServerUrl,
                label = { Text(text = stringResource(id = R.string.label_server_url)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            AuthTypeSelector(selected = state.authType, onSelected = onAction::updateAuthType)

            when (state.authType) {
                AuthType.BASIC -> {
                    OutlinedTextField(
                        value = state.username,
                        onValueChange = onAction::updateUsername,
                        label = { Text(stringResource(id = R.string.label_username)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = state.password,
                        onValueChange = onAction::updatePassword,
                        label = { Text(stringResource(id = R.string.label_app_password)) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                AuthType.OAUTH -> {
                    OutlinedTextField(
                        value = state.displayName,
                        onValueChange = onAction::updateDisplayName,
                        label = { Text(stringResource(id = R.string.label_display_name)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = state.accessToken,
                        onValueChange = onAction::updateAccessToken,
                        label = { Text(stringResource(id = R.string.label_access_token)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = state.refreshToken,
                        onValueChange = onAction::updateRefreshToken,
                        label = { Text(stringResource(id = R.string.label_refresh_token_optional)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            if (state.errorMessage != null) {
                Text(
                    text = state.errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            if (state.certificateWarning != null) {
                Text(
                    text = state.certificateWarning,
                    color = MaterialTheme.colorScheme.tertiary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Button(
                onClick = onAction::submitLogin,
                enabled = !state.isProcessing,
                modifier = Modifier.fillMaxWidth(),
            ) {
                val label = if (state.authType == AuthType.BASIC) R.string.action_login_basic else R.string.action_login_oauth
                Text(text = stringResource(id = label))
            }
        }
    }
}

@Composable
fun AuthTypeSelector(selected: AuthType, onSelected: (AuthType) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = { onSelected(AuthType.BASIC) },
            enabled = selected != AuthType.BASIC,
        ) {
            Text(text = stringResource(id = R.string.auth_basic))
        }
        OutlinedButton(
            onClick = { onSelected(AuthType.OAUTH) },
            enabled = selected != AuthType.OAUTH,
        ) {
            Text(text = stringResource(id = R.string.auth_oauth))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    viewModel: TaskListViewModel,
    sessionState: SessionUiState,
    onSwitchAccount: (String) -> Unit,
    onLogout: () -> Unit,
) {
    val tasks by viewModel.tasks.collectAsState()
    var showLogoutConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { /* analytics hook */ }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = stringResource(id = R.string.app_name))
                        sessionState.activeAccount?.let {
                            Text(
                                text = stringResource(id = R.string.label_signed_in_as, it.displayName),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                actions = {
                    AccountMenu(sessionState = sessionState, onSwitchAccount = onSwitchAccount, onLogout = { showLogoutConfirm = true })
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
                    ),
            )
        },
    ) { padding ->
        if (tasks.isEmpty()) {
            EmptyState(padding)
        } else {
            TaskList(padding = padding, tasks = tasks)
        }
    }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text(text = stringResource(id = R.string.action_logout)) },
            text = { Text(text = stringResource(id = R.string.logout_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutConfirm = false
                    onLogout()
                }) {
                    Text(text = stringResource(id = R.string.action_logout))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) {
                    Text(text = stringResource(id = R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
fun AccountMenu(sessionState: SessionUiState, onSwitchAccount: (String) -> Unit, onLogout: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val otherAccounts = sessionState.accounts.filter { it.id != sessionState.activeAccount?.id }

    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { expanded = true }) { Icon(Icons.Default.ArrowDropDown, contentDescription = stringResource(id = R.string.action_switch_account)) }
    }

    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        otherAccounts.forEach { account ->
            DropdownMenuItem(
                text = { Text(account.displayName) },
                onClick = {
                    expanded = false
                    onSwitchAccount(account.id)
                },
                leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) },
            )
        }
        DropdownMenuItem(
            text = { Text(text = stringResource(id = R.string.action_logout)) },
            onClick = {
                expanded = false
                onLogout()
            },
            leadingIcon = { Icon(Icons.Default.Logout, contentDescription = null) },
        )
    }
}

@Composable
fun TaskList(padding: PaddingValues, tasks: List<Task>) {
    LazyColumn(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(tasks) { task ->
            Surface(
                tonalElevation = 1.dp,
                shape = MaterialTheme.shapes.medium,
                shadowElevation = 0.5.dp,
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.SemiBold,
                    )
                    task.description?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyState(padding: PaddingValues) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(padding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(id = R.string.welcome_message),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(id = R.string.empty_task_hint),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(id = R.string.empty_state_login_reminder),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
