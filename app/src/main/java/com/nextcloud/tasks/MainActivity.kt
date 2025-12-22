package com.nextcloud.tasks

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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
    val taskLists by taskListViewModel.taskLists.collectAsState()
    val selectedListId by taskListViewModel.selectedListId.collectAsState()
    val taskFilter by taskListViewModel.taskFilter.collectAsState()
    val taskSort by taskListViewModel.taskSort.collectAsState()
    val isRefreshing by taskListViewModel.isRefreshing.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }

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
            taskLists = taskLists,
            selectedListId = selectedListId,
            taskFilter = taskFilter,
            taskSort = taskSort,
            isRefreshing = isRefreshing,
            onLogout = loginViewModel::logout,
            onSwitchAccount = loginViewModel::switchAccount,
            onSelectList = taskListViewModel::selectList,
            onSetFilter = taskListViewModel::setFilter,
            onSetSort = taskListViewModel::setSort,
            onRefresh = taskListViewModel::refresh,
            onCreateTask = { showCreateDialog = true },
            onToggleTaskComplete = taskListViewModel::toggleTaskComplete,
            onDeleteTask = taskListViewModel::deleteTask,
        )

        // Create task dialog
        if (showCreateDialog) {
            val defaultListId = selectedListId ?: taskLists.firstOrNull()?.id
            if (defaultListId != null) {
                CreateTaskDialog(
                    listId = defaultListId,
                    onDismiss = { showCreateDialog = false },
                    onCreate = { title, description ->
                        taskListViewModel.createTask(title, description, defaultListId)
                        showCreateDialog = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthenticatedHome(
    state: LoginUiState,
    tasks: List<Task>,
    taskLists: List<com.nextcloud.tasks.domain.model.TaskList>,
    selectedListId: String?,
    taskFilter: com.nextcloud.tasks.domain.model.TaskFilter,
    taskSort: com.nextcloud.tasks.domain.model.TaskSort,
    isRefreshing: Boolean,
    onLogout: (String) -> Unit,
    onSwitchAccount: (String) -> Unit,
    onSelectList: (String?) -> Unit,
    onSetFilter: (com.nextcloud.tasks.domain.model.TaskFilter) -> Unit,
    onSetSort: (com.nextcloud.tasks.domain.model.TaskSort) -> Unit,
    onRefresh: () -> Unit,
    onCreateTask: () -> Unit,
    onToggleTaskComplete: (Task) -> Unit,
    onDeleteTask: (String) -> Unit,
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                TaskListsDrawer(
                    taskLists = taskLists,
                    selectedListId = selectedListId,
                    onSelectList = onSelectList,
                    onCloseDrawer = { scope.launch { drawerState.close() } },
                )
            }
        },
    ) {
        Scaffold(
            topBar = {
                AuthenticatedTopBar(
                    state = state,
                    onSwitchAccount = onSwitchAccount,
                    onLogout = onLogout,
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                    onRefresh = onRefresh,
                    isRefreshing = isRefreshing,
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = onCreateTask) {
                    Text("+", style = MaterialTheme.typography.headlineMedium)
                }
            },
        ) { padding ->
            TasksContent(
                padding = padding,
                state = state,
                tasks = tasks,
                taskFilter = taskFilter,
                taskSort = taskSort,
                onSetFilter = onSetFilter,
                onSetSort = onSetSort,
                onToggleTaskComplete = onToggleTaskComplete,
                onDeleteTask = onDeleteTask,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedTopBar(
    state: LoginUiState,
    onSwitchAccount: (String) -> Unit,
    onLogout: (String) -> Unit,
    onOpenDrawer: () -> Unit,
    onRefresh: () -> Unit,
    isRefreshing: Boolean,
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
        navigationIcon = {
            IconButton(onClick = onOpenDrawer) {
                Icon(
                    painter = painterResource(android.R.drawable.ic_menu_sort_by_size),
                    contentDescription = "Menu",
                )
            }
        },
        actions = {
            IconButton(onClick = onRefresh, enabled = !isRefreshing) {
                Icon(
                    painter = painterResource(android.R.drawable.ic_menu_rotate),
                    contentDescription = "Refresh",
                )
            }
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
    taskFilter: com.nextcloud.tasks.domain.model.TaskFilter,
    taskSort: com.nextcloud.tasks.domain.model.TaskSort,
    onSetFilter: (com.nextcloud.tasks.domain.model.TaskFilter) -> Unit,
    onSetSort: (com.nextcloud.tasks.domain.model.TaskSort) -> Unit,
    onToggleTaskComplete: (Task) -> Unit,
    onDeleteTask: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        state.activeAccount?.let { account ->
            item { AccountSummaryCard(account = account) }
        }

        item {
            FilterAndSortControls(
                taskFilter = taskFilter,
                taskSort = taskSort,
                onSetFilter = onSetFilter,
                onSetSort = onSetSort,
            )
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
                    text = "Tasks (${tasks.size})",
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            // Group tasks by completion status
            val openTasks = tasks.filter { !it.completed }
            val completedTasks = tasks.filter { it.completed }

            if (openTasks.isNotEmpty()) {
                item {
                    Text(
                        text = "Open (${openTasks.size})",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                items(openTasks) { task ->
                    TaskCard(
                        task = task,
                        onToggleComplete = { onToggleTaskComplete(task) },
                        onDelete = { onDeleteTask(task.id) },
                    )
                }
            }

            if (completedTasks.isNotEmpty()) {
                item {
                    Text(
                        text = "Completed (${completedTasks.size})",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }
                items(completedTasks) { task ->
                    TaskCard(
                        task = task,
                        onToggleComplete = { onToggleTaskComplete(task) },
                        onDelete = { onDeleteTask(task.id) },
                    )
                }
            }
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
private fun TaskListsDrawer(
    taskLists: List<com.nextcloud.tasks.domain.model.TaskList>,
    selectedListId: String?,
    onSelectList: (String?) -> Unit,
    onCloseDrawer: () -> Unit,
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "Task Lists",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp),
        )

        NavigationDrawerItem(
            label = { Text("All Tasks") },
            selected = selectedListId == null,
            onClick = {
                onSelectList(null)
                onCloseDrawer()
            },
        )

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(8.dp))

        taskLists.forEach { taskList ->
            NavigationDrawerItem(
                label = { Text(taskList.name) },
                selected = selectedListId == taskList.id,
                onClick = {
                    onSelectList(taskList.id)
                    onCloseDrawer()
                },
                badge = taskList.color?.let {
                    {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(it)),
                            modifier = Modifier.padding(4.dp),
                        ) {
                            Spacer(modifier = Modifier.width(16.dp))
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun FilterAndSortControls(
    taskFilter: com.nextcloud.tasks.domain.model.TaskFilter,
    taskSort: com.nextcloud.tasks.domain.model.TaskSort,
    onSetFilter: (com.nextcloud.tasks.domain.model.TaskFilter) -> Unit,
    onSetSort: (com.nextcloud.tasks.domain.model.TaskSort) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Filter controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Filter:",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.align(Alignment.CenterVertically),
            )
            FilterChip(
                selected = taskFilter == com.nextcloud.tasks.domain.model.TaskFilter.ALL,
                onClick = { onSetFilter(com.nextcloud.tasks.domain.model.TaskFilter.ALL) },
                label = { Text("All") },
            )
            FilterChip(
                selected = taskFilter == com.nextcloud.tasks.domain.model.TaskFilter.CURRENT,
                onClick = { onSetFilter(com.nextcloud.tasks.domain.model.TaskFilter.CURRENT) },
                label = { Text("Current") },
            )
            FilterChip(
                selected = taskFilter == com.nextcloud.tasks.domain.model.TaskFilter.COMPLETED,
                onClick = { onSetFilter(com.nextcloud.tasks.domain.model.TaskFilter.COMPLETED) },
                label = { Text("Completed") },
            )
        }

        // Sort controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Sort:",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.align(Alignment.CenterVertically),
            )
            FilterChip(
                selected = taskSort == com.nextcloud.tasks.domain.model.TaskSort.DUE_DATE,
                onClick = { onSetSort(com.nextcloud.tasks.domain.model.TaskSort.DUE_DATE) },
                label = { Text("Due") },
            )
            FilterChip(
                selected = taskSort == com.nextcloud.tasks.domain.model.TaskSort.PRIORITY,
                onClick = { onSetSort(com.nextcloud.tasks.domain.model.TaskSort.PRIORITY) },
                label = { Text("Priority") },
            )
            FilterChip(
                selected = taskSort == com.nextcloud.tasks.domain.model.TaskSort.TITLE,
                onClick = { onSetSort(com.nextcloud.tasks.domain.model.TaskSort.TITLE) },
                label = { Text("Title") },
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
private fun TaskCard(
    task: Task,
    onToggleComplete: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.medium,
        shadowElevation = 0.5.dp,
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            // Checkbox
            Checkbox(
                checked = task.completed,
                onCheckedChange = { onToggleComplete() },
            )

            // Task content
            Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    task.priority?.let { priority ->
                        Text(
                            text = "P$priority",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }

                task.description?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                // Show due date and tags
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    task.due?.let { due ->
                        Text(
                            text = "Due: ${java.time.format.DateTimeFormatter.ofPattern("MMM dd").format(due.atZone(java.time.ZoneId.systemDefault()))}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (task.tags.isNotEmpty()) {
                        Text(
                            text = task.tags.joinToString { it.name },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                }
            }

            // Delete button
            IconButton(onClick = onDelete) {
                Icon(
                    painter = painterResource(android.R.drawable.ic_menu_delete),
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun CreateTaskDialog(
    listId: String,
    onDismiss: () -> Unit,
    onCreate: (String, String?) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Task") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onCreate(title.trim(), description.ifBlank { null })
                    }
                },
                enabled = title.isNotBlank(),
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
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
        private val tasksRepository: com.nextcloud.tasks.domain.repository.TasksRepository,
    ) : ViewModel() {
        // Raw tasks from repository
        private val allTasks =
            loadTasksUseCase()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

        // Task lists from repository
        val taskLists =
            tasksRepository.observeLists()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

        // UI state for selected list, filter, and sort
        private val _selectedListId = MutableStateFlow<String?>(null)
        val selectedListId = _selectedListId.asStateFlow()

        private val _taskFilter = MutableStateFlow(com.nextcloud.tasks.domain.model.TaskFilter.ALL)
        val taskFilter = _taskFilter.asStateFlow()

        private val _taskSort = MutableStateFlow(com.nextcloud.tasks.domain.model.TaskSort.DUE_DATE)
        val taskSort = _taskSort.asStateFlow()

        private val _isRefreshing = MutableStateFlow(false)
        val isRefreshing = _isRefreshing.asStateFlow()

        // Filtered and sorted tasks
        val tasks =
            combine(allTasks, selectedListId, taskFilter, taskSort) { tasks, listId, filter, sort ->
                tasks
                    .filter { task ->
                        // Filter by selected list
                        (listId == null || task.listId == listId)
                    }.filter { task ->
                        // Filter by task status
                        when (filter) {
                            com.nextcloud.tasks.domain.model.TaskFilter.ALL -> true
                            com.nextcloud.tasks.domain.model.TaskFilter.CURRENT -> !task.completed
                            com.nextcloud.tasks.domain.model.TaskFilter.COMPLETED -> task.completed
                        }
                    }.sortedWith(
                        when (sort) {
                            com.nextcloud.tasks.domain.model.TaskSort.DUE_DATE ->
                                compareBy(
                                    nullsLast()
                                ) { it.due }
                            com.nextcloud.tasks.domain.model.TaskSort.PRIORITY ->
                                compareBy(
                                    nullsLast()
                                ) { it.priority }
                            com.nextcloud.tasks.domain.model.TaskSort.TITLE -> compareBy { it.title }
                            com.nextcloud.tasks.domain.model.TaskSort.UPDATED_AT ->
                                compareByDescending {
                                    it.updatedAt
                                }
                        },
                    )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

        init {
            viewModelScope.launch {
                loadTasksUseCase.seedSample()
                // Auto-refresh on app start to fetch tasks from server
                refresh()
            }
        }

        fun selectList(listId: String?) {
            _selectedListId.value = listId
        }

        fun setFilter(filter: com.nextcloud.tasks.domain.model.TaskFilter) {
            _taskFilter.value = filter
        }

        fun setSort(sort: com.nextcloud.tasks.domain.model.TaskSort) {
            _taskSort.value = sort
        }

        fun refresh() {
            viewModelScope.launch {
                _isRefreshing.value = true
                try {
                    tasksRepository.refresh()
                } catch (e: Exception) {
                    timber.log.Timber.e(e, "Failed to refresh tasks")
                } finally {
                    _isRefreshing.value = false
                }
            }
        }

        fun createTask(
            title: String,
            description: String? = null,
            listId: String,
        ) {
            viewModelScope.launch {
                try {
                    val draft =
                        com.nextcloud.tasks.domain.model.TaskDraft(
                            listId = listId,
                            title = title,
                            description = description,
                            completed = false,
                            due = null,
                            tagIds = emptyList(),
                        )
                    tasksRepository.createTask(draft)
                    timber.log.Timber.d("Task created successfully")
                } catch (e: Exception) {
                    timber.log.Timber.e(e, "Failed to create task")
                }
            }
        }

        fun toggleTaskComplete(task: Task) {
            viewModelScope.launch {
                try {
                    val updated =
                        task.copy(
                            completed = !task.completed,
                            completedAt = if (!task.completed) java.time.Instant.now() else null,
                            status = if (!task.completed) "COMPLETED" else "NEEDS-ACTION",
                        )
                    tasksRepository.updateTask(updated)
                    timber.log.Timber.d("Task completion toggled")
                } catch (e: Exception) {
                    timber.log.Timber.e(e, "Failed to toggle task completion")
                }
            }
        }

        fun deleteTask(taskId: String) {
            viewModelScope.launch {
                try {
                    tasksRepository.deleteTask(taskId)
                    timber.log.Timber.d("Task deleted successfully")
                } catch (e: Exception) {
                    timber.log.Timber.e(e, "Failed to delete task")
                }
            }
        }
    }
