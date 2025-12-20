package com.nextcloud.tasks

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nextcloud.tasks.auth.LoginCallbacks
import com.nextcloud.tasks.auth.LoginScreen
import com.nextcloud.tasks.auth.LoginUiState
import com.nextcloud.tasks.auth.LoginViewModel
import com.nextcloud.tasks.domain.model.AuthType
import com.nextcloud.tasks.domain.model.NextcloudAccount
import com.nextcloud.tasks.domain.model.Task
import com.nextcloud.tasks.domain.usecase.LoadTasksUseCase
import com.nextcloud.tasks.ui.theme.NextcloudTasksTheme
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val taskListViewModel: TaskListViewModel by viewModels()
    private val loginViewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NextcloudTasksTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    NextcloudTasksApp(
                        loginViewModel = loginViewModel,
                        taskListViewModel = taskListViewModel,
                    )
                }
            }
        }
    }
}

@Composable
fun NextcloudTasksApp(
    loginViewModel: LoginViewModel,
    taskListViewModel: TaskListViewModel,
) {
    val loginState by loginViewModel.uiState.collectAsState()
    val tasks by taskListViewModel.tasks.collectAsState()

    if (loginState.activeAccount == null) {
        LoginScreen(
            state = loginState,
            callbacks =
                LoginCallbacks(
                    onServerUrlChange = loginViewModel::updateServerUrl,
                    onUsernameChange = loginViewModel::updateUsername,
                    onPasswordChange = loginViewModel::updatePassword,
                    onAuthorizationCodeChange = loginViewModel::updateAuthorizationCode,
                    onRedirectUriChange = loginViewModel::updateRedirectUri,
                    onAuthMethodChange = loginViewModel::switchAuthMethod,
                    onSubmit = loginViewModel::submit,
                ),
        )
    } else {
        AuthenticatedHome(
            state = loginState,
            tasks = tasks,
            onLogout = loginViewModel::logout,
            onSwitchAccount = loginViewModel::switchAccount,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthenticatedHome(
    state: LoginUiState,
    tasks: List<Task>,
    onLogout: (String) -> Unit,
    onSwitchAccount: (String) -> Unit,
) {
    Scaffold(
        topBar = {
            AuthenticatedTopBar(
                state = state,
                onSwitchAccount = onSwitchAccount,
                onLogout = onLogout,
            )
        },
    ) { padding ->
        TasksContent(
            padding = padding,
            state = state,
            tasks = tasks,
        )
    }
}

@Composable
private fun AuthenticatedTopBar(
    state: LoginUiState,
    onSwitchAccount: (String) -> Unit,
    onLogout: (String) -> Unit,
) {
    TopAppBar(
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = stringResource(id = R.string.app_name))
                state.activeAccount?.let {
                    Text(
                        text = it.serverUrl,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        actions = {
            state.activeAccount?.let { account ->
                AccountDropdown(
                    activeAccount = account,
                    accounts = state.accounts,
                    onSwitchAccount = onSwitchAccount,
                    onLogout = onLogout,
                )
            }
        },
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
            ),
    )
}

@Composable
private fun TasksContent(
    padding: PaddingValues,
    state: LoginUiState,
    tasks: List<Task>,
) {
    LazyColumn(
        modifier = Modifier.padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        state.activeAccount?.let { account ->
            item { AccountSummaryCard(account = account) }
        }

        if (tasks.isEmpty()) {
            item {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    EmptyState()
                }
            }
        } else {
            item {
                Text(
                    text = stringResource(id = R.string.task_list_title),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            items(tasks) { task -> TaskCard(task = task) }
        }
    }
}

@Composable
private fun AccountSummaryCard(account: NextcloudAccount) {
    Card(
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp),
            ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = stringResource(id = R.string.active_account),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = account.displayName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = account.serverUrl,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text =
                    if (account.authType == AuthType.PASSWORD) {
                        stringResource(id = R.string.login_method_password)
                    } else {
                        stringResource(id = R.string.login_method_oauth)
                    },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AccountDropdown(
    activeAccount: NextcloudAccount,
    accounts: List<NextcloudAccount>,
    onSwitchAccount: (String) -> Unit,
    onLogout: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        TextButton(onClick = { expanded = true }) {
            Icon(
                painter = painterResource(android.R.drawable.ic_menu_manage),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = activeAccount.displayName,
                modifier = Modifier.padding(start = 8.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            accounts.forEach { account ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(text = account.displayName)
                            Text(
                                text = account.serverUrl,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                    onClick = {
                        expanded = false
                        onSwitchAccount(account.id)
                    },
                    trailingIcon =
                        if (account.id == activeAccount.id) {
                            {
                                Icon(
                                    painter = painterResource(android.R.drawable.checkbox_on_background),
                                    contentDescription = null,
                                )
                            }
                        } else {
                            null
                        },
                )
            }
            DropdownMenuItem(
                text = { Text(text = stringResource(id = R.string.logout)) },
                onClick = {
                    expanded = false
                    onLogout(activeAccount.id)
                },
            )
        }
    }
}

@Composable
private fun TaskCard(task: Task) {
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

@Composable
fun EmptyState(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(),
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
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
    }
}

@HiltViewModel
class TaskListViewModel
    @Inject
    constructor(
        private val loadTasksUseCase: LoadTasksUseCase,
    ) : ViewModel() {
        val tasks =
            loadTasksUseCase()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

        init {
            viewModelScope.launch { loadTasksUseCase.seedSample() }
        }
    }
