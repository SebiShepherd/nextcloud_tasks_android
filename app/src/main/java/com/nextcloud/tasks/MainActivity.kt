package com.nextcloud.tasks

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.nextcloud.tasks.auth.LoginFlowUiState
import com.nextcloud.tasks.auth.LoginFlowViewModel
import com.nextcloud.tasks.auth.ServerInputScreen
import com.nextcloud.tasks.domain.model.NextcloudAccount
import com.nextcloud.tasks.domain.model.Task
import com.nextcloud.tasks.domain.usecase.LoadTasksUseCase
import com.nextcloud.tasks.ui.theme.NextcloudTasksTheme
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val taskListViewModel: TaskListViewModel by viewModels()
    private val loginFlowViewModel: LoginFlowViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Pre-initialize AppCompatDelegate to prevent first-time recreation
        // This reads any saved locale without triggering a configuration change
        AppCompatDelegate.getApplicationLocales()

        setContent {
            NextcloudTasksTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    NextcloudTasksApp(
                        loginFlowViewModel = loginFlowViewModel,
                        taskListViewModel = taskListViewModel,
                    )
                }
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Compose automatically recomposes when configuration changes
        // This includes locale/language changes, so all stringResource() calls
        // will pick up the new language without needing to recreate the activity
    }
}

@Composable
fun NextcloudTasksApp(
    loginFlowViewModel: LoginFlowViewModel,
    taskListViewModel: TaskListViewModel,
) {
    val loginState by loginFlowViewModel.uiState.collectAsState()
    val tasks by taskListViewModel.tasks.collectAsState()
    val taskLists by taskListViewModel.taskLists.collectAsState()
    val selectedListId by taskListViewModel.selectedListId.collectAsState()
    val taskFilter by taskListViewModel.taskFilter.collectAsState()
    val taskSort by taskListViewModel.taskSort.collectAsState()
    val isRefreshing by taskListViewModel.isRefreshing.collectAsState()
    val searchQuery by taskListViewModel.searchQuery.collectAsState()
    val isOnline by taskListViewModel.isOnline.collectAsState()
    val hasPendingChanges by taskListViewModel.hasPendingChanges.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var forceShowLogin by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    // Track whether we've loaded tasks for the current account
    // This ensures refresh happens after initial login when account becomes active
    var lastLoadedAccountId by remember { mutableStateOf<String?>(null) }

    // Auto-refresh when account becomes active (initial login or account switch from another source)
    LaunchedEffect(loginState.activeAccount?.id) {
        val currentAccountId = loginState.activeAccount?.id
        if (currentAccountId != null && currentAccountId != lastLoadedAccountId) {
            taskListViewModel.refresh()
            lastLoadedAccountId = currentAccountId
        }
    }

    // Handle account switching with automatic refresh
    val handleSwitchAccount: (String) -> Unit = { accountId ->
        loginFlowViewModel.onSwitchAccount(accountId)
        forceShowLogin = false
        // Refresh will be triggered by LaunchedEffect above
    }

    if (loginState.activeAccount == null || forceShowLogin) {
        ServerInputScreen(
            onLoginSuccess = {
                forceShowLogin = false
                // Refresh will be triggered by LaunchedEffect above when account becomes active
            },
            onBack = {
                // Only allow back if there's an existing account
                if (loginState.activeAccount != null) {
                    forceShowLogin = false
                }
            },
        )
    } else if (showSettings) {
        // Settings Screen
        com.nextcloud.tasks.settings.SettingsScreen(
            onNavigateBack = { showSettings = false },
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
            searchQuery = searchQuery,
            isOnline = isOnline,
            hasPendingChanges = hasPendingChanges,
            onLogout = loginFlowViewModel::onLogout,
            onSwitchAccount = handleSwitchAccount,
            onSelectList = taskListViewModel::selectList,
            onSetFilter = taskListViewModel::setFilter,
            onSetSort = taskListViewModel::setSort,
            onSetSearchQuery = taskListViewModel::setSearchQuery,
            onRefresh = taskListViewModel::refresh,
            onCreateTask = { showCreateDialog = true },
            onToggleTaskComplete = taskListViewModel::toggleTaskComplete,
            onDeleteTask = taskListViewModel::deleteTask,
            onAddAccount = { forceShowLogin = true },
            onOpenSettings = { showSettings = true },
        )

        // Create task dialog
        if (showCreateDialog) {
            val defaultListId = selectedListId ?: taskLists.firstOrNull()?.id
            if (defaultListId != null) {
                CreateTaskDialog(
                    listId = defaultListId,
                    onDismiss = { showCreateDialog = false },
                    onCreate = { title, description, listId ->
                        taskListViewModel.createTask(title, description, listId)
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
    state: LoginFlowUiState,
    tasks: List<Task>,
    taskLists: List<com.nextcloud.tasks.domain.model.TaskList>,
    selectedListId: String?,
    taskFilter: com.nextcloud.tasks.domain.model.TaskFilter,
    taskSort: com.nextcloud.tasks.domain.model.TaskSort,
    isRefreshing: Boolean,
    searchQuery: String,
    isOnline: Boolean,
    hasPendingChanges: Boolean,
    onLogout: (String) -> Unit,
    onSwitchAccount: (String) -> Unit,
    onSelectList: (String?) -> Unit,
    onSetFilter: (com.nextcloud.tasks.domain.model.TaskFilter) -> Unit,
    onSetSort: (com.nextcloud.tasks.domain.model.TaskSort) -> Unit,
    onSetSearchQuery: (String) -> Unit,
    onRefresh: () -> Unit,
    onCreateTask: () -> Unit,
    onToggleTaskComplete: (Task) -> Unit,
    onDeleteTask: (String) -> Unit,
    onAddAccount: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val offlineMessage = stringResource(R.string.offline_message)

    // Show snackbar when offline and user performs an action
    var showOfflineSnackbar by remember { mutableStateOf(false) }

    LaunchedEffect(showOfflineSnackbar, isOnline) {
        if (showOfflineSnackbar && !isOnline) {
            snackbarHostState.showSnackbar(offlineMessage)
            showOfflineSnackbar = false
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                TaskListsDrawer(
                    taskLists = taskLists,
                    selectedListId = selectedListId,
                    onSelectList = onSelectList,
                    onCloseDrawer = { scope.launch { drawerState.close() } },
                    onOpenSettings = {
                        onOpenSettings()
                        scope.launch { drawerState.close() }
                    },
                )
            }
        },
    ) {
        Scaffold(
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState) { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            floatingActionButton = {
                FloatingActionButton(onClick = onCreateTask) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.create_task_description),
                    )
                }
            },
        ) { padding ->
            Column(modifier = Modifier.padding(padding)) {
                // Durchgehende Search Bar mit allen Elementen
                UnifiedSearchBar(
                    state = state,
                    searchQuery = searchQuery,
                    onSearchQueryChange = onSetSearchQuery,
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                    onSwitchAccount = onSwitchAccount,
                    onLogout = onLogout,
                    taskSort = taskSort,
                    onSetSort = onSetSort,
                    onAddAccount = onAddAccount,
                )

                // Pull-to-Refresh Content
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = onRefresh,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    TasksContent(
                        padding = PaddingValues(0.dp),
                        state = state,
                        tasks = tasks,
                        taskLists = taskLists,
                        taskFilter = taskFilter,
                        taskSort = taskSort,
                        searchQuery = searchQuery,
                        isOnline = isOnline,
                        onSetFilter = onSetFilter,
                        onSetSort = onSetSort,
                        onToggleTaskComplete = { task ->
                            onToggleTaskComplete(task)
                            if (!isOnline) {
                                showOfflineSnackbar = true
                            }
                        },
                        onDeleteTask = { taskId ->
                            onDeleteTask(taskId)
                            if (!isOnline) {
                                showOfflineSnackbar = true
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun UnifiedSearchBar(
    state: LoginFlowUiState,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onOpenDrawer: () -> Unit,
    onSwitchAccount: (String) -> Unit,
    onLogout: (String) -> Unit,
    taskSort: com.nextcloud.tasks.domain.model.TaskSort,
    onSetSort: (com.nextcloud.tasks.domain.model.TaskSort) -> Unit,
    onAddAccount: () -> Unit,
) {
    var showSortDialog by remember { mutableStateOf(false) }
    var showAccountSheet by remember { mutableStateOf(false) }
    var isSearchActive by rememberSaveable { mutableStateOf(false) }
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    // System back should close the search when it's active (same behavior as the in-UI back icon)
    BackHandler(enabled = isSearchActive) {
        isSearchActive = false
        onSearchQueryChange("")
        focusManager.clearFocus()
    }

    // Container with fixed height to prevent layout shift
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(64.dp), // Fixed total height
    ) {
        // Surface adapts based on search state
        Surface(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = if (isSearchActive) 0.dp else 16.dp,
                        vertical = if (isSearchActive) 0.dp else 8.dp,
                    ).fillMaxHeight(),
            shape = RoundedCornerShape(if (isSearchActive) 0.dp else 24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (isSearchActive) {
                    // Back button when search is active
                    IconButton(
                        onClick = {
                            isSearchActive = false
                            onSearchQueryChange("")
                            focusManager.clearFocus()
                        },
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.close),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    // Hamburger menu in normal state
                    IconButton(onClick = onOpenDrawer) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = stringResource(R.string.menu_description),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // Search text field
                androidx.compose.foundation.text.BasicTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier =
                        Modifier
                            .weight(1f)
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused && isSearchActive == false) {
                                    isSearchActive = true
                                }
                            },
                    textStyle =
                        MaterialTheme.typography.bodyLarge.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    singleLine = true,
                    cursorBrush =
                        androidx.compose.ui.graphics
                            .SolidColor(
                                androidx.compose.ui.graphics
                                    .Color(0xFF0082C9),
                            ),
                    decorationBox = { innerTextField ->
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = stringResource(R.string.search_all_notes),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        innerTextField()
                    },
                )

                // Sort icon and profile picture only visible when search is not active
                if (isSearchActive == false) {
                    IconButton(onClick = { showSortDialog = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Sort,
                            contentDescription = stringResource(R.string.sort_description),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    ProfilePicture(
                        account = state.activeAccount,
                        size = 32.dp,
                        onClick = { showAccountSheet = true },
                    )
                }
            }
        }
    }

    // Account Bottom Sheet
    if (showAccountSheet) {
        state.activeAccount?.let { account ->
            AccountBottomSheet(
                activeAccount = account,
                accounts = state.accounts,
                onSwitchAccount = onSwitchAccount,
                onLogout = onLogout,
                onAddAccount = onAddAccount,
                onDismiss = { showAccountSheet = false },
            )
        }
    }

    // Sort-Dialog
    if (showSortDialog) {
        SortDialog(
            currentSort = taskSort,
            onSetSort = onSetSort,
            onDismiss = { showSortDialog = false },
        )
    }
}

@Composable
private fun SortDialog(
    currentSort: com.nextcloud.tasks.domain.model.TaskSort,
    onSetSort: (com.nextcloud.tasks.domain.model.TaskSort) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.sort_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SortOption(
                    text = stringResource(R.string.sort_by_due_date),
                    isSelected = currentSort == com.nextcloud.tasks.domain.model.TaskSort.DUE_DATE,
                    onClick = {
                        onSetSort(com.nextcloud.tasks.domain.model.TaskSort.DUE_DATE)
                        onDismiss()
                    },
                )
                SortOption(
                    text = stringResource(R.string.sort_by_priority),
                    isSelected = currentSort == com.nextcloud.tasks.domain.model.TaskSort.PRIORITY,
                    onClick = {
                        onSetSort(com.nextcloud.tasks.domain.model.TaskSort.PRIORITY)
                        onDismiss()
                    },
                )
                SortOption(
                    text = stringResource(R.string.sort_by_title),
                    isSelected = currentSort == com.nextcloud.tasks.domain.model.TaskSort.TITLE,
                    onClick = {
                        onSetSort(com.nextcloud.tasks.domain.model.TaskSort.TITLE)
                        onDismiss()
                    },
                )
                SortOption(
                    text = stringResource(R.string.sort_by_updated),
                    isSelected = currentSort == com.nextcloud.tasks.domain.model.TaskSort.UPDATED_AT,
                    onClick = {
                        onSetSort(com.nextcloud.tasks.domain.model.TaskSort.UPDATED_AT)
                        onDismiss()
                    },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        },
    )
}

@Composable
private fun SortOption(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        androidx.compose.material3.RadioButton(
            selected = isSelected,
            onClick = onClick,
        )
        Text(
            text = text,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Suppress("UnusedParameter", "LongParameterList")
@Composable
private fun TasksContent(
    padding: PaddingValues,
    state: LoginFlowUiState,
    tasks: List<Task>,
    taskLists: List<com.nextcloud.tasks.domain.model.TaskList>,
    taskFilter: com.nextcloud.tasks.domain.model.TaskFilter,
    taskSort: com.nextcloud.tasks.domain.model.TaskSort,
    searchQuery: String,
    isOnline: Boolean,
    onSetFilter: (com.nextcloud.tasks.domain.model.TaskFilter) -> Unit,
    onSetSort: (com.nextcloud.tasks.domain.model.TaskSort) -> Unit,
    onToggleTaskComplete: (Task) -> Unit,
    onDeleteTask: (String) -> Unit,
) {
    var showCompletedTasks by remember { mutableStateOf(false) }

    // Create a map for quick lookup of task list info
    val taskListMap = remember(taskLists) { taskLists.associateBy { it.id } }

    // Group tasks by completion status
    val openTasks = tasks.filter { !it.completed }
    val completedTasks = tasks.filter { it.completed }

    // Group open tasks by list
    val openTasksByList = openTasks.groupBy { it.listId }

    LazyColumn(
        modifier = Modifier.padding(padding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (openTasks.isEmpty() && completedTasks.isEmpty()) {
            item {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (searchQuery.isNotBlank()) {
                        NoSearchResultsState()
                    } else {
                        EmptyState()
                    }
                }
            }
        } else {
            // Offene Tasks gruppiert nach Listen
            if (openTasks.isNotEmpty()) {
                openTasksByList.forEach { (listId, listTasks) ->
                    // Get list info from map
                    val taskList = taskListMap[listId]

                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier =
                                Modifier.padding(
                                    top = if (openTasksByList.keys.first() != listId) 8.dp else 0.dp,
                                ),
                        ) {
                            // Color dot
                            taskList?.color?.let { colorHex ->
                                Box(
                                    modifier =
                                        Modifier
                                            .size(8.dp)
                                            .background(
                                                color =
                                                    androidx.compose.ui.graphics.Color(
                                                        android.graphics.Color.parseColor(colorHex),
                                                    ),
                                                shape = CircleShape,
                                            ),
                                )
                            }
                            Text(
                                text = taskList?.name ?: stringResource(R.string.unknown_list),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    items(listTasks, key = { it.id }) { task ->
                        SimpleAnimatedTaskCard(
                            task = task,
                            onToggleComplete = { onToggleTaskComplete(task) },
                            onDelete = { onDeleteTask(task.id) },
                        )
                    }
                }
            }

            // Button zum Aufklappen der erledigten Tasks
            if (completedTasks.isNotEmpty()) {
                item {
                    TextButton(
                        onClick = { showCompletedTasks = !showCompletedTasks },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text =
                                if (showCompletedTasks) {
                                    stringResource(R.string.hide_completed_tasks, completedTasks.size)
                                } else {
                                    stringResource(R.string.show_completed_tasks, completedTasks.size)
                                },
                        )
                    }
                }

                // Erledigte Tasks (wenn aufgeklappt)
                if (showCompletedTasks) {
                    items(completedTasks, key = { it.id }) { task ->
                        SimpleAnimatedTaskCard(
                            task = task,
                            onToggleComplete = { onToggleTaskComplete(task) },
                            onDelete = { onDeleteTask(task.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskListsDrawer(
    taskLists: List<com.nextcloud.tasks.domain.model.TaskList>,
    selectedListId: String?,
    onSelectList: (String?) -> Unit,
    onCloseDrawer: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = stringResource(R.string.task_lists_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp),
        )

        NavigationDrawerItem(
            label = { Text(stringResource(R.string.all_tasks)) },
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
                label = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        // Color indicator (dot)
                        taskList.color?.let { colorHex ->
                            Box(
                                modifier =
                                    Modifier
                                        .size(12.dp)
                                        .background(
                                            color =
                                                androidx.compose.ui.graphics.Color(
                                                    android.graphics.Color.parseColor(colorHex),
                                                ),
                                            shape = CircleShape,
                                        ),
                            )
                        }
                        Text(taskList.name)
                    }
                },
                selected = selectedListId == taskList.id,
                onClick = {
                    onSelectList(taskList.id)
                    onCloseDrawer()
                },
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(8.dp))

        // Settings
        NavigationDrawerItem(
            label = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                    )
                    Text(stringResource(R.string.settings_title))
                }
            },
            selected = false,
            onClick = onOpenSettings,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountBottomSheet(
    activeAccount: NextcloudAccount,
    accounts: List<NextcloudAccount>,
    onSwitchAccount: (String) -> Unit,
    onLogout: (String) -> Unit,
    onAddAccount: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    var showManageMenu by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 16.dp),
        ) {
            // Account-Liste
            accounts.forEach { account ->
                AccountItem(
                    account = account,
                    isActive = account.id == activeAccount.id,
                    onClick = {
                        if (account.id != activeAccount.id) {
                            onSwitchAccount(account.id)
                        }
                        onDismiss()
                    },
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Konto hinzufügen
            Surface(
                onClick = {
                    onDismiss()
                    onAddAccount()
                },
                color = MaterialTheme.colorScheme.surface,
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = stringResource(R.string.add_account_description),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.add_account),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            // Konten verwalten
            Surface(
                onClick = { showManageMenu = true },
                color = MaterialTheme.colorScheme.surface,
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = stringResource(R.string.manage_accounts_description),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.manage_accounts),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Manage Account Dialog
    if (showManageMenu) {
        AlertDialog(
            onDismissRequest = { showManageMenu = false },
            title = { Text(stringResource(R.string.manage_accounts_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.account_info, activeAccount.displayName))
                    Text(stringResource(R.string.server_info, activeAccount.serverUrl))
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showManageMenu = false
                        onLogout(activeAccount.id)
                        onDismiss()
                    },
                ) {
                    Text(stringResource(R.string.logout_button), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showManageMenu = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
private fun ProfilePicture(
    account: NextcloudAccount?,
    size: androidx.compose.ui.unit.Dp,
    onClick: (() -> Unit)? = null,
) {
    val avatarUrl =
        account?.let {
            "${it.serverUrl}/index.php/avatar/${it.username}/64"
        }

    Box(
        modifier =
            Modifier
                .size(size)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        if (avatarUrl != null) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = stringResource(R.string.profile_picture_description),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier =
                    Modifier
                        .size(size)
                        .clip(CircleShape)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape,
                        ),
            )
        } else {
            Box(
                modifier =
                    Modifier
                        .size(size)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape,
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = account?.displayName?.firstOrNull()?.uppercase() ?: "?",
                    style =
                        if (size > 40.dp) {
                            MaterialTheme.typography.titleLarge
                        } else {
                            MaterialTheme.typography.bodyMedium
                        },
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}

@Composable
private fun AccountItem(
    account: NextcloudAccount,
    isActive: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Profilbild
            ProfilePicture(
                account = account,
                size = 48.dp,
            )

            // Name und Server
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = account.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = account.serverUrl,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Checkmark für aktiven Account
            if (isActive) {
                Box(
                    modifier =
                        Modifier
                            .size(32.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary,
                                shape = CircleShape,
                            ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = stringResource(R.string.active_account_description),
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

/**
 * Simple animated wrapper for TaskCard.
 * - Toggle complete: Fade out animation, then update
 * - Delete: Fade out animation, then delete
 *
 * IMPORTANT: Animation must complete BEFORE data changes,
 * otherwise the composable is removed from composition immediately.
 */
@Composable
private fun SimpleAnimatedTaskCard(
    task: Task,
    onToggleComplete: () -> Unit,
    onDelete: () -> Unit,
) {
    var isVisible by remember { mutableStateOf(true) }
    var isAnimating by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(durationMillis = 200)),
        exit = shrinkVertically(
            animationSpec = tween(durationMillis = 200),
        ) + fadeOut(animationSpec = tween(durationMillis = 150)),
    ) {
        TaskCard(
            task = task,
            onToggleComplete = {
                if (!isAnimating) {
                    isAnimating = true
                    scope.launch {
                        // First fade out
                        isVisible = false
                        // Wait for animation to complete
                        delay(250)
                        // Then trigger the data change
                        onToggleComplete()
                    }
                }
            },
            onDelete = {
                if (!isAnimating) {
                    isAnimating = true
                    scope.launch {
                        // First fade out
                        isVisible = false
                        // Wait for animation to complete
                        delay(250)
                        // Then delete
                        onDelete()
                    }
                }
            },
        )
    }
}

@Composable
private fun TaskCard(
    task: Task,
    onToggleComplete: () -> Unit,
    onDelete: () -> Unit,
) {
    // Dynamische vertikale Ausrichtung basierend auf Inhalt
    val hasDescription = task.description != null
    val hasDueOrTags = task.due != null || task.tags.isNotEmpty()
    val hasAdditionalContent = hasDescription || hasDueOrTags

    Card(
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = 0.dp,
            ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = if (hasAdditionalContent) Alignment.Top else Alignment.CenterVertically,
        ) {
            // Checkbox
            Checkbox(
                checked = task.completed,
                onCheckedChange = { onToggleComplete() },
            )

            // Task content
            Column(modifier = Modifier.weight(1f).padding(start = 8.dp, end = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
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

                // Show due date and tags only if they exist
                if (hasDueOrTags) {
                    Row(
                        modifier = Modifier.padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        task.due?.let { due ->
                            Text(
                                text = "Due: ${java.time.format.DateTimeFormatter.ofPattern(
                                    "MMM dd",
                                ).format(due.atZone(java.time.ZoneId.systemDefault()))}",
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
            }

            // Delete button
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.delete_description),
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
    onCreate: (String, String?, String) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.create_task_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.task_title_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.task_description_label)) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onCreate(title.trim(), description.ifBlank { null }, listId)
                    }
                },
                enabled = title.isNotBlank(),
            ) {
                Text(stringResource(R.string.create))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
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

@Composable
fun NoSearchResultsState(
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
            text = stringResource(id = R.string.no_search_results_title),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(id = R.string.no_search_results_hint),
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
        private val observeActiveAccountUseCase: com.nextcloud.tasks.domain.usecase.ObserveActiveAccountUseCase,
    ) : ViewModel() {
        // Raw tasks from repository
        private val allTasks =
            loadTasksUseCase()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

        // Task lists from repository
        val taskLists =
            tasksRepository
                .observeLists()
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

        private val _searchQuery = MutableStateFlow("")
        val searchQuery = _searchQuery.asStateFlow()

        // Frozen tasks during sync to prevent UI flicker
        private val frozenTasksForSync = MutableStateFlow<List<Task>?>(null)

        // Network status and pending changes
        val isOnline =
            tasksRepository
                .observeIsOnline()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

        val hasPendingChanges =
            tasksRepository
                .observeHasPendingChanges()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

        // Internal filtered and sorted tasks (before freezing logic)
        private val filteredTasks =
            combine(
                allTasks,
                _selectedListId,
                _taskFilter,
                _taskSort,
                _searchQuery,
            ) { tasks, listId, filter, sort, query ->
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
                    }.filter { task ->
                        // Filter by search query (case-insensitive)
                        if (query.isBlank()) {
                            true
                        } else {
                            val searchLower = query.lowercase()
                            task.title.lowercase().contains(searchLower) ||
                                task.description?.lowercase()?.contains(searchLower) == true
                        }
                    }.sortedWith(
                        when (sort) {
                            com.nextcloud.tasks.domain.model.TaskSort.DUE_DATE ->
                                compareBy(
                                    nullsLast(),
                                ) { it.due }
                            com.nextcloud.tasks.domain.model.TaskSort.PRIORITY ->
                                compareBy(
                                    nullsLast(),
                                ) { it.priority }
                            com.nextcloud.tasks.domain.model.TaskSort.TITLE -> compareBy { it.title }
                            com.nextcloud.tasks.domain.model.TaskSort.UPDATED_AT ->
                                compareByDescending {
                                    it.updatedAt
                                }
                        },
                    )
            }

        // Public tasks flow that respects freezing during sync
        val tasks =
            combine(filteredTasks, frozenTasksForSync) { filtered, frozen ->
                // Use frozen tasks during refresh to prevent UI flicker
                frozen ?: filtered
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

        init {
            // Auto-refresh is handled by LaunchedEffect in NextcloudTasksApp
            // when account becomes active (after login or account switch)

            // Reset selected list when account changes
            viewModelScope.launch {
                var previousAccountId: String? = null
                observeActiveAccountUseCase().collect { account ->
                    val currentAccountId = account?.id
                    if (previousAccountId != null && previousAccountId != currentAccountId) {
                        // Account changed, reset to "All Tasks" view
                        _selectedListId.value = null
                    }
                    previousAccountId = currentAccountId
                }
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

        fun setSearchQuery(query: String) {
            _searchQuery.value = query
        }

        fun refresh() {
            viewModelScope.launch {
                // Freeze the current task list to prevent UI flicker during sync
                frozenTasksForSync.value = tasks.value
                _isRefreshing.value = true
                try {
                    tasksRepository.refresh()
                } catch (ignored: Exception) {
                    timber.log.Timber.e(ignored, "Failed to refresh tasks")
                } finally {
                    _isRefreshing.value = false
                    // Unfreeze - show the updated list
                    frozenTasksForSync.value = null
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
                } catch (ignored: Exception) {
                    timber.log.Timber.e(ignored, "Failed to create task")
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
                } catch (ignored: Exception) {
                    timber.log.Timber.e(ignored, "Failed to toggle task completion")
                }
            }
        }

        fun deleteTask(taskId: String) {
            viewModelScope.launch {
                try {
                    tasksRepository.deleteTask(taskId)
                    timber.log.Timber.d("Task deleted successfully")
                } catch (ignored: Exception) {
                    timber.log.Timber.e(ignored, "Failed to delete task")
                }
            }
        }
    }
