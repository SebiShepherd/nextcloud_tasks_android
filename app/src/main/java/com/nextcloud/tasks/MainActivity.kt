package com.nextcloud.tasks

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nextcloud.tasks.auth.LoginCallbacks
import com.nextcloud.tasks.auth.LoginScreen
import com.nextcloud.tasks.auth.LoginUiState
import com.nextcloud.tasks.auth.LoginViewModel
import com.nextcloud.tasks.domain.model.NextcloudAccount
import com.nextcloud.tasks.tasks.TaskDetailSheet
import com.nextcloud.tasks.tasks.TaskEditorMode
import com.nextcloud.tasks.tasks.TaskEditorSheet
import com.nextcloud.tasks.tasks.TaskHomeContent
import com.nextcloud.tasks.tasks.TaskListViewModel
import com.nextcloud.tasks.ui.theme.NextcloudTasksTheme
import dagger.hilt.android.AndroidEntryPoint

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
    val taskState by taskListViewModel.uiState.collectAsState()

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
            loginState = loginState,
            taskState = taskState,
            taskListViewModel = taskListViewModel,
            onLogout = loginViewModel::logout,
            onSwitchAccount = loginViewModel::switchAccount,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthenticatedHome(
    loginState: LoginUiState,
    taskState: TaskListUiState,
    taskListViewModel: TaskListViewModel,
    onLogout: (String) -> Unit,
    onSwitchAccount: (String) -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(taskState.errorMessage) {
        taskState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
            taskListViewModel.consumeError()
        }
    }

    if (taskState.editor.mode != TaskEditorMode.Hidden) {
        TaskEditorSheet(
            state = taskState.editor,
            tags = taskState.tags,
            lists = taskState.lists,
            onTitleChange = taskListViewModel::updateEditorTitle,
            onDescriptionChange = taskListViewModel::updateEditorDescription,
            onListChange = taskListViewModel::updateEditorList,
            onPriorityChange = taskListViewModel::updateEditorPriority,
            onStatusToggle = taskListViewModel::toggleEditorCompleted,
            onTagToggle = taskListViewModel::toggleEditorTag,
            onDueChange = taskListViewModel::updateEditorDueInput,
            onSave = { taskListViewModel.submitEditor(taskListViewModel::closeSheet) },
            onDismiss = taskListViewModel::closeSheet,
        )
    } else if (taskState.selectedTask != null) {
        TaskDetailSheet(
            task = taskState.selectedTask,
            onEdit = taskListViewModel::startEdit,
            onDelete = { taskListViewModel.deleteSelectedTask(taskListViewModel::closeSheet) },
            onDismiss = taskListViewModel::closeSheet,
        )
    }

    Scaffold(
        topBar = {
            AuthenticatedTopBar(
                state = loginState,
                onSwitchAccount = onSwitchAccount,
                onLogout = onLogout,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        TaskHomeContent(
            padding = padding,
            account = loginState.activeAccount,
            state = taskState,
            onSearchQueryChange = taskListViewModel::updateSearchQuery,
            onStatusFilterChange = taskListViewModel::updateStatusFilter,
            onSortOptionChange = taskListViewModel::updateSortOption,
            onListFilterChange = taskListViewModel::updateListFilter,
            onTagFilterChange = taskListViewModel::updateTagFilter,
            onRefresh = taskListViewModel::refresh,
            onTaskSelected = taskListViewModel::openDetails,
            onCreateTask = taskListViewModel::startCreate,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
private fun AccountDropdown(
    activeAccount: NextcloudAccount,
    accounts: List<NextcloudAccount>,
    onSwitchAccount: (String) -> Unit,
    onLogout: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

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
            fontWeight = FontWeight.Medium,
        )
    }

    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        accounts.forEach { account ->
            DropdownMenuItem(
                text = {
                    Column {
                        Text(text = account.displayName, fontWeight = FontWeight.SemiBold)
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
                leadingIcon =
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
