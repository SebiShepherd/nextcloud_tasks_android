@file:Suppress("TooManyFunctions")

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
import androidx.compose.animation.expandVertically
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
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
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import com.nextcloud.tasks.data.caldav.service.CalDavHttpException
import com.nextcloud.tasks.domain.model.NextcloudAccount
import com.nextcloud.tasks.domain.model.ShareAccess
import com.nextcloud.tasks.domain.model.Sharee
import com.nextcloud.tasks.domain.model.ShareeSearchResult
import com.nextcloud.tasks.domain.model.ShareeType
import com.nextcloud.tasks.domain.model.Task
import com.nextcloud.tasks.domain.usecase.GetShareesUseCase
import com.nextcloud.tasks.domain.usecase.LoadTasksUseCase
import com.nextcloud.tasks.domain.usecase.SearchShareesUseCase
import com.nextcloud.tasks.domain.usecase.ShareListUseCase
import com.nextcloud.tasks.domain.usecase.UnshareListUseCase
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nextcloud.tasks.detail.TaskDetailScreen
import com.nextcloud.tasks.ui.theme.NextcloudTasksTheme
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val taskListViewModel: TaskListViewModel by viewModels()
    private val loginFlowViewModel: LoginFlowViewModel by viewModels()

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Pre-initialize AppCompatDelegate to prevent first-time recreation
        // This reads any saved locale without triggering a configuration change
        AppCompatDelegate.getApplicationLocales()

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val isExpandedScreen = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded

            NextcloudTasksTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    NextcloudTasksApp(
                        loginFlowViewModel = loginFlowViewModel,
                        taskListViewModel = taskListViewModel,
                        isExpandedScreen = isExpandedScreen,
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
    isExpandedScreen: Boolean = false,
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
    val refreshError by taskListViewModel.refreshError.collectAsState()
    val animatingEntryTaskIds by taskListViewModel.animatingEntryTaskIds.collectAsState()
    val createListError by taskListViewModel.createListError.collectAsState()
    val editListError by taskListViewModel.editListError.collectAsState()
    val deleteListError by taskListViewModel.deleteListError.collectAsState()

    // Sharing state
    val sharingListId by taskListViewModel.sharingListId.collectAsState()
    val sharees by taskListViewModel.sharees.collectAsState()
    val shareeSearchResults by taskListViewModel.shareeSearchResults.collectAsState()
    val shareeSearchQuery by taskListViewModel.shareeSearchQuery.collectAsState()
    val isLoadingSharees by taskListViewModel.isLoadingSharees.collectAsState()
    val shareError by taskListViewModel.shareError.collectAsState()
    val shareSuccess by taskListViewModel.shareSuccess.collectAsState()
    val shareActionInProgress by taskListViewModel.shareActionInProgress.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var showCreateListDialog by remember { mutableStateOf(false) }
    var listToEdit by remember { mutableStateOf<com.nextcloud.tasks.domain.model.TaskList?>(null) }
    var listToDelete by remember { mutableStateOf<com.nextcloud.tasks.domain.model.TaskList?>(null) }
    var forceShowLogin by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    // Track whether we've loaded tasks for the current account
    // This ensures refresh happens after initial login when account becomes active
    // Using rememberSaveable to survive configuration changes (e.g. rotation)
    var lastLoadedAccountId by rememberSaveable { mutableStateOf<String?>(null) }

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
            animatingEntryTaskIds = animatingEntryTaskIds,
            showCreateDialog = showCreateDialog,
            isExpandedScreen = isExpandedScreen,
            onLogout = loginFlowViewModel::onLogout,
            onSwitchAccount = handleSwitchAccount,
            onSelectList = taskListViewModel::selectList,
            onSetFilter = taskListViewModel::setFilter,
            onSetSort = taskListViewModel::setSort,
            onSetSearchQuery = taskListViewModel::setSearchQuery,
            onRefresh = taskListViewModel::refresh,
            onShowCreateDialog = { showCreateDialog = true },
            onDismissCreateDialog = { showCreateDialog = false },
            onCreateTask = { title, description, listId ->
                taskListViewModel.createTask(title, description, listId)
                showCreateDialog = false
            },
            onToggleTaskComplete = taskListViewModel::toggleTaskComplete,
            onDeleteTask = taskListViewModel::deleteTask,
            onClearAnimatingEntryTaskId = taskListViewModel::clearAnimatingEntryTaskId,
            onAddAccount = { forceShowLogin = true },
            onOpenSettings = { showSettings = true },
            refreshError = refreshError,
            onClearRefreshError = taskListViewModel::clearRefreshError,
            showCreateListDialog = showCreateListDialog,
            onShowCreateListDialog = { showCreateListDialog = true },
            onDismissCreateListDialog = { showCreateListDialog = false },
            onCreateList = { name, color ->
                taskListViewModel.createTaskList(name, color)
                showCreateListDialog = false
            },
            createListError = createListError,
            onClearCreateListError = taskListViewModel::clearCreateListError,
            listToEdit = listToEdit,
            onShowEditListDialog = { list -> listToEdit = list },
            onDismissEditListDialog = { listToEdit = null },
            onEditList = { listId, name, color ->
                taskListViewModel.editTaskList(listId, name, color)
                listToEdit = null
            },
            editListError = editListError,
            onClearEditListError = taskListViewModel::clearEditListError,
            listToDelete = listToDelete,
            onShowDeleteListDialog = { list -> listToDelete = list },
            onDismissDeleteListDialog = { listToDelete = null },
            onDeleteList = { listId ->
                taskListViewModel.deleteTaskList(listId)
                listToDelete = null
            },
            deleteListError = deleteListError,
            onClearDeleteListError = taskListViewModel::clearDeleteListError,
            sharingListId = sharingListId,
            sharees = sharees,
            shareeSearchResults = shareeSearchResults,
            shareeSearchQuery = shareeSearchQuery,
            isLoadingSharees = isLoadingSharees,
            shareError = shareError,
            shareSuccess = shareSuccess,
            onOpenShareSheet = taskListViewModel::openShareSheet,
            onCloseShareSheet = taskListViewModel::closeShareSheet,
            onSearchSharees = taskListViewModel::searchSharees,
            onAddSharee = { id, type, access -> taskListViewModel.addSharee(id, type, access) },
            onRemoveSharee = { id, type -> taskListViewModel.removeSharee(id, type) },
            onUpdateShareeAccess = { id, type, access -> taskListViewModel.updateShareeAccess(id, type, access) },
            shareActionInProgress = shareActionInProgress,
            onClearShareError = taskListViewModel::clearShareError,
            onClearShareSuccess = taskListViewModel::clearShareSuccess,
        )
    }
}

@Suppress("CyclomaticComplexMethod")
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
    animatingEntryTaskIds: Set<String>,
    showCreateDialog: Boolean,
    isExpandedScreen: Boolean = false,
    onLogout: (String) -> Unit,
    onSwitchAccount: (String) -> Unit,
    onSelectList: (String?) -> Unit,
    onSetFilter: (com.nextcloud.tasks.domain.model.TaskFilter) -> Unit,
    onSetSort: (com.nextcloud.tasks.domain.model.TaskSort) -> Unit,
    onSetSearchQuery: (String) -> Unit,
    onRefresh: () -> Unit,
    onShowCreateDialog: () -> Unit,
    onDismissCreateDialog: () -> Unit,
    onCreateTask: (String, String?, String) -> Unit,
    onToggleTaskComplete: (Task) -> Unit,
    onDeleteTask: (String) -> Unit,
    onClearAnimatingEntryTaskId: (String) -> Unit,
    onAddAccount: () -> Unit,
    onOpenSettings: () -> Unit,
    refreshError: RefreshError? = null,
    onClearRefreshError: () -> Unit = {},
    showCreateListDialog: Boolean = false,
    onShowCreateListDialog: () -> Unit = {},
    onDismissCreateListDialog: () -> Unit = {},
    onCreateList: (String, String?) -> Unit = { _, _ -> },
    createListError: CreateListError? = null,
    onClearCreateListError: () -> Unit = {},
    listToEdit: com.nextcloud.tasks.domain.model.TaskList? = null,
    onShowEditListDialog: (com.nextcloud.tasks.domain.model.TaskList) -> Unit = {},
    onDismissEditListDialog: () -> Unit = {},
    onEditList: (String, String, String?) -> Unit = { _, _, _ -> },
    editListError: EditListError? = null,
    onClearEditListError: () -> Unit = {},
    listToDelete: com.nextcloud.tasks.domain.model.TaskList? = null,
    onShowDeleteListDialog: (com.nextcloud.tasks.domain.model.TaskList) -> Unit = {},
    onDismissDeleteListDialog: () -> Unit = {},
    onDeleteList: (String) -> Unit = {},
    deleteListError: DeleteListError? = null,
    onClearDeleteListError: () -> Unit = {},
    // Sharing
    sharingListId: String? = null,
    sharees: List<Sharee> = emptyList(),
    shareeSearchResults: List<ShareeSearchResult> = emptyList(),
    shareeSearchQuery: String = "",
    isLoadingSharees: Boolean = false,
    shareError: String? = null,
    shareSuccess: Boolean = false,
    onOpenShareSheet: (String) -> Unit = {},
    onCloseShareSheet: () -> Unit = {},
    onSearchSharees: (String) -> Unit = {},
    onAddSharee: (String, ShareeType, ShareAccess) -> Unit = { _, _, _ -> },
    onRemoveSharee: (String, ShareeType) -> Unit = { _, _ -> },
    onUpdateShareeAccess: (String, ShareeType, ShareAccess) -> Unit = { _, _, _ -> },
    shareActionInProgress: String? = null,
    onClearShareError: () -> Unit = {},
    onClearShareSuccess: () -> Unit = {},
) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val offlineMessage = stringResource(R.string.offline_message)
    val readOnlyHintMsg = stringResource(R.string.list_read_only_hint)

    // Show snackbar when offline and user performs an action
    var showOfflineSnackbar by remember { mutableStateOf(false) }

    LaunchedEffect(showOfflineSnackbar, isOnline) {
        if (showOfflineSnackbar && !isOnline) {
            snackbarHostState.showSnackbar(offlineMessage)
            showOfflineSnackbar = false
        }
    }

    RefreshErrorEffect(refreshError, snackbarHostState, onClearRefreshError)
    CreateListErrorEffect(createListError, snackbarHostState, onClearCreateListError)
    EditListErrorEffect(editListError, snackbarHostState, onClearEditListError)
    DeleteListErrorEffect(deleteListError, snackbarHostState, onClearDeleteListError)

    // Share errors and success are shown in the bottom sheet only (no duplicate snackbar)

    // Determine if selected list is read-only
    val selectedListAccess = taskLists.find { it.id == selectedListId }?.shareAccess
    val isReadOnly = selectedListAccess == ShareAccess.READ
    val hasWritableLists = taskLists.any { it.shareAccess != ShareAccess.READ }

    val mainContent: @Composable () -> Unit = {
        NavHost(navController = navController, startDestination = "tasks") {
            composable("tasks") {
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
                        if (hasWritableLists && !isReadOnly) {
                            FloatingActionButton(onClick = onShowCreateDialog) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = stringResource(R.string.create_task_description),
                                )
                            }
                        }
                    },
                ) { padding ->
                    Column(modifier = Modifier.padding(padding)) {
                        UnifiedSearchBar(
                            state = state,
                            searchQuery = searchQuery,
                            onSearchQueryChange = onSetSearchQuery,
                            onOpenDrawer = if (isExpandedScreen) null else ({ scope.launch { drawerState.open() } }),
                            onSwitchAccount = onSwitchAccount,
                            onLogout = onLogout,
                            taskSort = taskSort,
                            onSetSort = onSetSort,
                            onAddAccount = onAddAccount,
                        )

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
                                animatingEntryTaskIds = animatingEntryTaskIds,
                                isExpandedScreen = isExpandedScreen,
                                onSetFilter = onSetFilter,
                                onSetSort = onSetSort,
                                onToggleTaskComplete = { task ->
                                    if (!isReadOnly) {
                                        onToggleTaskComplete(task)
                                        if (!isOnline) {
                                            showOfflineSnackbar = true
                                        }
                                    }
                                },
                                onDeleteTask = { taskId ->
                                    if (!isReadOnly) {
                                        onDeleteTask(taskId)
                                        if (!isOnline) {
                                            showOfflineSnackbar = true
                                        }
                                    }
                                },
                                onClearAnimatingEntryTaskId = onClearAnimatingEntryTaskId,
                                onShowCreateListDialog = onShowCreateListDialog,
                                onOpenTask = { taskId -> navController.navigate("task/$taskId") },
                            )
                        }
                    }
                }
            }
            composable("task/{taskId}") {
                TaskDetailScreen(
                    onNavigateBack = { navController.popBackStack() },
                )
            }
        }
    }

    val drawerContent: @Composable () -> Unit = {
        TaskListsDrawer(
            taskLists = taskLists,
            selectedListId = selectedListId,
            onSelectList = onSelectList,
            onCloseDrawer = { if (!isExpandedScreen) scope.launch { drawerState.close() } },
            onOpenSettings = {
                onOpenSettings()
                if (!isExpandedScreen) scope.launch { drawerState.close() }
            },
            hasPendingChanges = hasPendingChanges,
            onShowCreateListDialog = {
                onShowCreateListDialog()
                if (!isExpandedScreen) scope.launch { drawerState.close() }
            },
            onOpenShareSheet = { listId ->
                onOpenShareSheet(listId)
                if (!isExpandedScreen) scope.launch { drawerState.close() }
            },
            onEditList = { list ->
                onShowEditListDialog(list)
                if (!isExpandedScreen) scope.launch { drawerState.close() }
            },
            onDeleteList = { list ->
                onShowDeleteListDialog(list)
                if (!isExpandedScreen) scope.launch { drawerState.close() }
            },
        )
    }

    if (isExpandedScreen) {
        PermanentNavigationDrawer(
            drawerContent = {
                PermanentDrawerSheet { drawerContent() }
            },
            content = mainContent,
        )
    } else {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet { drawerContent() }
            },
            content = mainContent,
        )
    }

    // Create task dialog — only shown when at least one writable list exists.
    if (showCreateDialog && hasWritableLists) {
        CreateTaskDialog(
            taskLists = taskLists,
            initialListId = selectedListId ?: taskLists.first().id,
            showListSelector = selectedListId == null,
            onDismiss = onDismissCreateDialog,
            onCreate = { title, description, listId ->
                onCreateTask(title, description, listId)
                if (!isOnline) {
                    showOfflineSnackbar = true
                }
            },
        )
    }

    // Create list dialog
    if (showCreateListDialog) {
        CreateTaskListDialog(
            onDismiss = onDismissCreateListDialog,
            onCreate = onCreateList,
        )
    }

    // Edit list dialog
    if (listToEdit != null) {
        EditTaskListDialog(
            taskList = listToEdit!!,
            onDismiss = onDismissEditListDialog,
            onEdit = { name, color -> onEditList(listToEdit!!.id, name, color) },
        )
    }

    // Delete list confirmation dialog
    if (listToDelete != null) {
        DeleteListConfirmationDialog(
            listName = listToDelete!!.name,
            onDismiss = onDismissDeleteListDialog,
            onConfirm = { onDeleteList(listToDelete!!.id) },
        )
    }

    // Share list bottom sheet
    if (sharingListId != null) {
        ShareListBottomSheet(
            serverUrl = state.activeAccount?.serverUrl ?: "",
            sharees = sharees,
            searchResults = shareeSearchResults,
            searchQuery = shareeSearchQuery,
            isLoading = isLoadingSharees,
            shareError = shareError,
            shareSuccess = shareSuccess,
            actionInProgress = shareActionInProgress,
            onSearchQueryChange = onSearchSharees,
            onAddSharee = { id, type, access -> onAddSharee(id, type, access) },
            onRemoveSharee = onRemoveSharee,
            onUpdateAccess = onUpdateShareeAccess,
            onDismiss = onCloseShareSheet,
            onClearShareError = onClearShareError,
            onClearShareSuccess = onClearShareSuccess,
        )
    }

    // Read-only hint for shared lists
    if (isReadOnly) {
        LaunchedEffect(selectedListId) {
            snackbarHostState.showSnackbar(readOnlyHintMsg)
        }
    }
}

@Composable
private fun RefreshErrorEffect(
    refreshError: RefreshError?,
    snackbarHostState: SnackbarHostState,
    onClearRefreshError: () -> Unit,
) {
    val rateLimitedMsg = stringResource(R.string.error_rate_limited)
    val authFailedMsg = stringResource(R.string.error_auth_failed_refresh)
    val serverErrorMsg = stringResource(R.string.error_server)
    val networkErrorMsg = stringResource(R.string.error_network)
    val unknownErrorMsg = stringResource(R.string.error_unknown)

    LaunchedEffect(refreshError) {
        val message =
            when (refreshError) {
                RefreshError.RATE_LIMITED -> rateLimitedMsg
                RefreshError.AUTH_FAILED -> authFailedMsg
                RefreshError.SERVER_ERROR -> serverErrorMsg
                RefreshError.NETWORK_ERROR -> networkErrorMsg
                RefreshError.UNKNOWN -> unknownErrorMsg
                null -> null
            }
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            onClearRefreshError()
        }
    }
}

@Composable
private fun CreateListErrorEffect(
    error: CreateListError?,
    snackbarHostState: SnackbarHostState,
    onClearError: () -> Unit,
) {
    val offlineMsg = stringResource(R.string.error_create_list_offline)
    val failedMsg = stringResource(R.string.error_create_list_failed)

    LaunchedEffect(error) {
        if (error != null) {
            val message =
                when (error) {
                    is CreateListError.Offline -> offlineMsg
                    is CreateListError.Failed -> failedMsg
                }
            snackbarHostState.showSnackbar(message)
            onClearError()
        }
    }
}

@Composable
private fun EditListErrorEffect(
    error: EditListError?,
    snackbarHostState: SnackbarHostState,
    onClearError: () -> Unit,
) {
    val offlineMsg = stringResource(R.string.error_edit_list_offline)
    val failedMsg = stringResource(R.string.error_edit_list_failed)

    LaunchedEffect(error) {
        if (error != null) {
            val message =
                when (error) {
                    is EditListError.Offline -> offlineMsg
                    is EditListError.Failed -> failedMsg
                }
            snackbarHostState.showSnackbar(message)
            onClearError()
        }
    }
}

@Composable
private fun DeleteListErrorEffect(
    error: DeleteListError?,
    snackbarHostState: SnackbarHostState,
    onClearError: () -> Unit,
) {
    val offlineMsg = stringResource(R.string.error_delete_list_offline)
    val failedMsg = stringResource(R.string.error_delete_list_failed)

    LaunchedEffect(error) {
        if (error != null) {
            val message =
                when (error) {
                    is DeleteListError.Offline -> offlineMsg
                    is DeleteListError.Failed -> failedMsg
                }
            snackbarHostState.showSnackbar(message)
            onClearError()
        }
    }
}

@Composable
private fun UnifiedSearchBar(
    state: LoginFlowUiState,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onOpenDrawer: (() -> Unit)?,
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
                } else if (onOpenDrawer != null) {
                    // Hamburger menu in normal state (hidden on expanded screens with permanent drawer)
                    IconButton(onClick = onOpenDrawer) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = stringResource(R.string.menu_description),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    // Extra start padding on expanded screens where hamburger icon is absent
                    Spacer(Modifier.width(8.dp))
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
    animatingEntryTaskIds: Set<String>,
    isExpandedScreen: Boolean = false,
    onSetFilter: (com.nextcloud.tasks.domain.model.TaskFilter) -> Unit,
    onSetSort: (com.nextcloud.tasks.domain.model.TaskSort) -> Unit,
    onToggleTaskComplete: (Task) -> Unit,
    onDeleteTask: (String) -> Unit,
    onClearAnimatingEntryTaskId: (String) -> Unit,
    onShowCreateListDialog: () -> Unit = {},
    onOpenTask: (String) -> Unit = {},
) {
    var showCompletedTasks by remember { mutableStateOf(false) }

    // Create a map for quick lookup of task list info
    val taskListMap = remember(taskLists) { taskLists.associateBy { it.id } }

    // Group tasks by completion status, filtering out tasks with unknown lists
    // (can happen briefly during account switch before refresh completes)
    val knownTasks = remember(tasks, taskListMap) { tasks.filter { it.listId in taskListMap } }
    val openTasks = knownTasks.filter { !it.completed }
    val completedTasks = knownTasks.filter { it.completed }

    // Group open tasks by list
    val openTasksByList = openTasks.groupBy { it.listId }

    // On expanded screens, constrain max content width for readability
    val contentModifier =
        if (isExpandedScreen) {
            Modifier.padding(padding).widthIn(max = 720.dp).fillMaxWidth()
        } else {
            Modifier.padding(padding)
        }

    LazyColumn(
        modifier = contentModifier,
        contentPadding =
            PaddingValues(
                start = if (isExpandedScreen) 24.dp else 16.dp,
                end = if (isExpandedScreen) 24.dp else 16.dp,
                top = 16.dp,
                bottom = 16.dp,
            ),
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
                    } else if (taskLists.isEmpty()) {
                        NoListsEmptyState(onCreateList = onShowCreateListDialog)
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
                                    top = if (openTasksByList.keys.first() != listId) 16.dp else 0.dp,
                                    bottom = 8.dp,
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
                        val taskIsReadOnly =
                            taskListMap[task.listId]?.shareAccess == ShareAccess.READ
                        SimpleAnimatedTaskCard(
                            task = task,
                            isReadOnly = taskIsReadOnly,
                            animateEntry = task.id in animatingEntryTaskIds,
                            onToggleComplete = { onToggleTaskComplete(task) },
                            onDelete = { onDeleteTask(task.id) },
                            onEntryAnimationComplete = { onClearAnimatingEntryTaskId(task.id) },
                            onOpenTask = { onOpenTask(task.id) },
                        )
                    }
                }
            }

            // Button zum Aufklappen der erledigten Tasks
            if (completedTasks.isNotEmpty()) {
                item {
                    TextButton(
                        onClick = { showCompletedTasks = !showCompletedTasks },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
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
                        val taskIsReadOnly =
                            taskListMap[task.listId]?.shareAccess == ShareAccess.READ
                        SimpleAnimatedTaskCard(
                            task = task,
                            isReadOnly = taskIsReadOnly,
                            animateEntry = task.id in animatingEntryTaskIds,
                            onToggleComplete = { onToggleTaskComplete(task) },
                            onDelete = { onDeleteTask(task.id) },
                            onEntryAnimationComplete = { onClearAnimatingEntryTaskId(task.id) },
                            onOpenTask = { onOpenTask(task.id) },
                        )
                    }
                }
            }
        }
    }
}

@Suppress("UnusedParameter", "ForbiddenComment")
@Composable
private fun TaskListsDrawer(
    taskLists: List<com.nextcloud.tasks.domain.model.TaskList>,
    selectedListId: String?,
    onSelectList: (String?) -> Unit,
    onCloseDrawer: () -> Unit,
    onOpenSettings: () -> Unit,
    hasPendingChanges: Boolean = false,
    onShowCreateListDialog: () -> Unit = {},
    onOpenShareSheet: (String) -> Unit = {},
    onEditList: (com.nextcloud.tasks.domain.model.TaskList) -> Unit = {},
    onDeleteList: (com.nextcloud.tasks.domain.model.TaskList) -> Unit = {},
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        ) {
            Text(
                text = stringResource(R.string.task_lists_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            // TODO: Temporarily disabled - the `hasPendingChanges` indicator was added recently
            // but appears to be unreliable / incorrectly reported in the UI. Hide it for now
            // to avoid confusing users. Re-enable once the root cause is investigated and
            // fixed (restore the CircularProgressIndicator block below).
            //
            // if (hasPendingChanges) {
            //     CircularProgressIndicator(
            //         modifier = Modifier.size(16.dp),
            //         strokeWidth = 2.dp,
            //         color = MaterialTheme.colorScheme.primary,
            //     )
            // }
        }

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

        if (taskLists.isEmpty()) {
            Text(
                text = stringResource(R.string.no_task_lists_sidebar),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )
        } else {
            taskLists.forEach { taskList ->
                TaskListDrawerItem(
                    taskList = taskList,
                    isSelected = selectedListId == taskList.id,
                    onSelect = {
                        onSelectList(taskList.id)
                        onCloseDrawer()
                    },
                    onOpenShareSheet = { onOpenShareSheet(taskList.id) },
                    onEditList = { onEditList(taskList) },
                    onDeleteList = { onDeleteList(taskList) },
                )
            }
        }

        TextButton(
            onClick = onShowCreateListDialog,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.create_new_list))
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

@Composable
private fun TaskListDrawerItem(
    taskList: com.nextcloud.tasks.domain.model.TaskList,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onOpenShareSheet: () -> Unit,
    onEditList: () -> Unit = {},
    onDeleteList: () -> Unit = {},
) {
    var showMenu by remember { mutableStateOf(false) }

    NavigationDrawerItem(
        label = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
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
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Text(
                    text = taskList.name,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.width(8.dp))
                // Share/access icon
                when {
                    // Owner who shared the list → People icon
                    taskList.shareAccess == ShareAccess.OWNER && taskList.isShared -> {
                        Icon(
                            imageVector = Icons.Default.People,
                            contentDescription = stringResource(R.string.shared_by_you),
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    // Sharee with edit access → Edit icon
                    taskList.shareAccess == ShareAccess.READ_WRITE -> {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.share_access_edit),
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    // Sharee with read-only access → Eye icon
                    taskList.shareAccess == ShareAccess.READ -> {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = stringResource(R.string.share_access_read_only),
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                }
                // 3-dot menu (for owner lists and sharees with edit access)
                if (taskList.shareAccess == ShareAccess.OWNER ||
                    taskList.shareAccess == ShareAccess.READ_WRITE
                ) {
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = stringResource(R.string.list_options),
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        androidx.compose.material3.DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                        ) {
                            if (taskList.shareAccess == ShareAccess.OWNER) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.share_list)) },
                                    onClick = {
                                        showMenu = false
                                        onOpenShareSheet()
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.Share, contentDescription = null)
                                    },
                                )
                            }
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.edit_list)) },
                                onClick = {
                                    showMenu = false
                                    onEditList()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Edit, contentDescription = null)
                                },
                            )
                            if (taskList.shareAccess == ShareAccess.OWNER) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = stringResource(R.string.delete_list),
                                            color = MaterialTheme.colorScheme.error,
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        onDeleteList()
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                        )
                                    },
                                )
                            }
                        }
                    }
                }
            }
        },
        selected = isSelected,
        onClick = onSelect,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShareListBottomSheet(
    serverUrl: String,
    sharees: List<Sharee>,
    searchResults: List<ShareeSearchResult>,
    searchQuery: String,
    isLoading: Boolean,
    shareError: String? = null,
    shareSuccess: Boolean = false,
    actionInProgress: String? = null,
    onSearchQueryChange: (String) -> Unit,
    onAddSharee: (String, ShareeType, ShareAccess) -> Unit,
    onRemoveSharee: (String, ShareeType) -> Unit,
    onUpdateAccess: (String, ShareeType, ShareAccess) -> Unit,
    onDismiss: () -> Unit,
    onClearShareError: () -> Unit = {},
    onClearShareSuccess: () -> Unit = {},
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp),
        ) {
            Text(
                text = stringResource(R.string.share_list_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            // Search field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                label = { Text(stringResource(R.string.share_with_user_or_group)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            // Share error banner
            if (shareError != null) {
                val errorText =
                    when (shareError) {
                        "share_forbidden" -> stringResource(R.string.error_share_forbidden)
                        "load_sharees_failed" -> stringResource(R.string.error_load_sharees_failed)
                        else -> stringResource(R.string.error_share_failed)
                    }
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = errorText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(
                            onClick = onClearShareError,
                            modifier = Modifier.size(24.dp),
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }

            // Share success banner
            if (shareSuccess) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.share_success),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(
                            onClick = onClearShareSuccess,
                            modifier = Modifier.size(24.dp),
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }

            // Search results
            if (searchResults.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                val existingKeys = sharees.map { "${it.id}:${it.type}" }.toSet()
                searchResults.filter { "${it.id}:${it.type}" !in existingKeys }.forEach { result ->
                    ShareeSearchResultItem(
                        result = result,
                        serverUrl = serverUrl,
                        isLoading = actionInProgress == "add:${result.id}:${result.type}",
                        onAdd = { onAddSharee(result.id, result.type, ShareAccess.READ) },
                    )
                }
            }

            if (searchQuery.length >= 2 && searchResults.isEmpty() && !isLoading) {
                Text(
                    text = stringResource(R.string.no_sharees_found),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }

            // Loading indicator for sharees
            if (isLoading && sharees.isEmpty() && searchQuery.length < 2) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }

            // Current sharees
            if (sharees.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                sharees.forEach { sharee ->
                    CurrentShareeItem(
                        sharee = sharee,
                        serverUrl = serverUrl,
                        isActionLoading =
                            actionInProgress == "remove:${sharee.id}:${sharee.type}" ||
                                actionInProgress == "access:${sharee.id}:${sharee.type}",
                        isAccessLoading = actionInProgress == "access:${sharee.id}:${sharee.type}",
                        onRemove = { onRemoveSharee(sharee.id, sharee.type) },
                        onUpdateAccess = { newAccess ->
                            onUpdateAccess(sharee.id, sharee.type, newAccess)
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ShareeSearchResultItem(
    result: ShareeSearchResult,
    serverUrl: String,
    isLoading: Boolean = false,
    onAdd: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ShareeAvatar(userId = result.id, serverUrl = serverUrl, isGroup = result.type == ShareeType.GROUP)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = result.displayName, style = MaterialTheme.typography.bodyLarge)
            if (result.type == ShareeType.GROUP) {
                Text(
                    text = stringResource(R.string.sharee_type_group),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (isLoading) {
            Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            }
        } else {
            IconButton(onClick = onAdd) {
                Icon(Icons.Default.PersonAdd, contentDescription = stringResource(R.string.share_list))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrentShareeItem(
    sharee: Sharee,
    serverUrl: String,
    isActionLoading: Boolean = false,
    isAccessLoading: Boolean = false,
    onRemove: () -> Unit,
    onUpdateAccess: (ShareAccess) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val readOnlyLabel = stringResource(R.string.share_access_read_only)
    val editLabel = stringResource(R.string.share_access_edit)
    val currentLabel = if (sharee.access == ShareAccess.READ_WRITE) editLabel else readOnlyLabel

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ShareeAvatar(userId = sharee.id, serverUrl = serverUrl, isGroup = sharee.type == ShareeType.GROUP)
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = sharee.displayName,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.width(8.dp))
        // Permission dropdown or loading spinner
        if (isAccessLoading) {
            Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            }
        } else {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it },
            ) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.menuAnchor(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val accessIcon =
                            if (sharee.access == ShareAccess.READ_WRITE) {
                                Icons.Default.Edit
                            } else {
                                Icons.Default.Visibility
                            }
                        Icon(
                            imageVector = accessIcon,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = currentLabel,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.widthIn(min = 160.dp),
                ) {
                    DropdownMenuItem(
                        text = { Text(readOnlyLabel) },
                        leadingIcon = {
                            Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        onClick = {
                            expanded = false
                            if (sharee.access != ShareAccess.READ) {
                                onUpdateAccess(ShareAccess.READ)
                            }
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(editLabel) },
                        leadingIcon = {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        onClick = {
                            expanded = false
                            if (sharee.access != ShareAccess.READ_WRITE) {
                                onUpdateAccess(ShareAccess.READ_WRITE)
                            }
                        },
                    )
                }
            }
        }
        // Remove button or loading spinner
        if (isActionLoading && !isAccessLoading) {
            Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            }
        } else {
            IconButton(onClick = onRemove, enabled = !isActionLoading) {
                Icon(
                    Icons.Default.RemoveCircle,
                    contentDescription = stringResource(R.string.remove_share),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun ShareeAvatar(
    userId: String,
    serverUrl: String,
    isGroup: Boolean,
) {
    if (isGroup) {
        Box(
            modifier = Modifier.size(36.dp).background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.Group,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    } else {
        AsyncImage(
            model = "$serverUrl/index.php/avatar/$userId/64",
            contentDescription = null,
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            modifier =
                Modifier
                    .size(
                        36.dp,
                    ).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
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
    isReadOnly: Boolean = false,
    animateEntry: Boolean = false,
    onToggleComplete: () -> Unit,
    onDelete: () -> Unit,
    onEntryAnimationComplete: () -> Unit = {},
    onOpenTask: () -> Unit = {},
) {
    // Only start invisible if this task should animate entry (recently toggled)
    var isVisible by remember { mutableStateOf(!animateEntry) }
    var isAnimating by remember { mutableStateOf(false) }
    // Local checkbox state to show change before animation
    var localCompleted by remember(task.id) { mutableStateOf(task.completed) }
    val scope = rememberCoroutineScope()

    // Trigger entry animation only for recently toggled tasks
    LaunchedEffect(animateEntry) {
        if (animateEntry && !isVisible) {
            isVisible = true
            // Clear the animating flag after a short delay so it doesn't retrigger
            delay(250)
            onEntryAnimationComplete()
        }
    }

    // Sync local state when task changes (e.g., from server sync)
    LaunchedEffect(task.completed) {
        if (!isAnimating) {
            localCompleted = task.completed
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter =
            expandVertically(
                animationSpec = tween(durationMillis = 200),
            ) + fadeIn(animationSpec = tween(durationMillis = 200)),
        exit =
            shrinkVertically(
                animationSpec = tween(durationMillis = 200),
            ) + fadeOut(animationSpec = tween(durationMillis = 150)),
    ) {
        // Column includes bottom spacing so it animates with shrinkVertically
        Column {
            TaskCard(
                task = task.copy(completed = localCompleted),
                isReadOnly = isReadOnly,
                onToggleComplete = {
                    if (!isAnimating) {
                        isAnimating = true
                        scope.launch {
                            // Show checkbox change first
                            localCompleted = !localCompleted
                            // Wait for user to see the change
                            delay(200)
                            // Then start fade/shrink animation
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
                            // Start fade/shrink animation
                            isVisible = false
                            // Wait for animation to complete
                            delay(250)
                            // Then delete
                            onDelete()
                        }
                    }
                },
                onOpenTask = onOpenTask,
            )
            // Bottom spacing - animates with shrinkVertically
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun TaskCard(
    task: Task,
    isReadOnly: Boolean = false,
    onToggleComplete: () -> Unit,
    onDelete: () -> Unit,
    onOpenTask: () -> Unit = {},
) {
    // Dynamische vertikale Ausrichtung basierend auf Inhalt
    val hasDescription = task.description != null
    val hasDueOrTags = task.due != null || task.tags.isNotEmpty()
    val hasAdditionalContent = hasDescription || hasDueOrTags

    Card(
        onClick = onOpenTask,
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
                onCheckedChange =
                    if (isReadOnly) {
                        null
                    } else {
                        { onToggleComplete() }
                    },
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

            // Delete button (hidden for read-only lists, but space is preserved for uniform height)
            if (!isReadOnly) {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.delete_description),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(48.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateTaskDialog(
    taskLists: List<com.nextcloud.tasks.domain.model.TaskList>,
    initialListId: String,
    showListSelector: Boolean = false,
    onDismiss: () -> Unit,
    onCreate: (String, String?, String) -> Unit,
) {
    val writableLists = taskLists.filter { it.shareAccess != ShareAccess.READ }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedListId by remember {
        mutableStateOf(
            if (writableLists.any { it.id == initialListId }) {
                initialListId
            } else {
                writableLists.firstOrNull()?.id ?: initialListId
            },
        )
    }
    var listDropdownExpanded by remember { mutableStateOf(false) }
    val selectedList = writableLists.firstOrNull { it.id == selectedListId } ?: writableLists.firstOrNull() ?: return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.create_task_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (showListSelector) {
                    ExposedDropdownMenuBox(
                        expanded = listDropdownExpanded,
                        onExpandedChange = { listDropdownExpanded = it },
                    ) {
                        OutlinedTextField(
                            value = selectedList.name,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.task_list_label)) },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(
                                    expanded = listDropdownExpanded,
                                )
                            },
                            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable),
                        )
                        ExposedDropdownMenu(
                            expanded = listDropdownExpanded,
                            onDismissRequest = { listDropdownExpanded = false },
                        ) {
                            writableLists.forEach { list ->
                                DropdownMenuItem(
                                    text = { Text(list.name) },
                                    onClick = {
                                        selectedListId = list.id
                                        listDropdownExpanded = false
                                    },
                                    leadingIcon =
                                        list.color?.let { colorHex ->
                                            {
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
                                        },
                                )
                            }
                        }
                    }
                }
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
                        onCreate(title.trim(), description.ifBlank { null }, selectedListId)
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

private val TASK_LIST_COLORS =
    listOf(
        "#E9322D", // Red
        "#ECA700", // Orange
        "#FFD800", // Yellow
        "#46BA61", // Green
        "#4DA8DA", // Light blue
        "#0082C9", // Nextcloud blue
        "#8C00C9", // Purple
        "#C9007A", // Pink
    )

@Composable
private fun CreateTaskListDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String?) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf<String?>(TASK_LIST_COLORS.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.create_list_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.list_name_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = stringResource(R.string.list_color_label),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    TASK_LIST_COLORS.forEach { colorHex ->
                        val color =
                            androidx.compose.ui.graphics.Color(
                                android.graphics.Color.parseColor(colorHex),
                            )
                        val isSelected = selectedColor == colorHex
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier =
                                Modifier
                                    .size(28.dp)
                                    .background(color, CircleShape)
                                    .selectable(
                                        selected = isSelected,
                                        onClick = { selectedColor = colorHex },
                                        role = Role.RadioButton,
                                    ).semantics {
                                        contentDescription = colorHex
                                    },
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = androidx.compose.ui.graphics.Color.White,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onCreate(name.trim(), selectedColor)
                    }
                },
                enabled = name.isNotBlank(),
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
private fun EditTaskListDialog(
    taskList: com.nextcloud.tasks.domain.model.TaskList,
    onDismiss: () -> Unit,
    onEdit: (String, String?) -> Unit,
) {
    // Build the color palette, prepending a custom color swatch if the current color is not in the list
    val currentColor = taskList.color
    val paletteColors =
        if (currentColor != null && !TASK_LIST_COLORS.contains(currentColor)) {
            listOf(currentColor) + TASK_LIST_COLORS
        } else {
            TASK_LIST_COLORS
        }

    var name by remember { mutableStateOf(taskList.name) }
    var selectedColor by remember { mutableStateOf<String?>(currentColor) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_list_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.list_name_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = stringResource(R.string.list_color_label),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    paletteColors.forEach { colorHex ->
                        val color =
                            androidx.compose.ui.graphics.Color(
                                android.graphics.Color.parseColor(colorHex),
                            )
                        val isSelected = selectedColor == colorHex
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier =
                                Modifier
                                    .size(28.dp)
                                    .background(color, CircleShape)
                                    .selectable(
                                        selected = isSelected,
                                        onClick = { selectedColor = colorHex },
                                        role = Role.RadioButton,
                                    ).semantics {
                                        contentDescription = colorHex
                                    },
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = androidx.compose.ui.graphics.Color.White,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onEdit(name.trim(), selectedColor)
                    }
                },
                enabled = name.isNotBlank(),
            ) {
                Text(stringResource(R.string.save))
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
private fun DeleteListConfirmationDialog(
    listName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_list_dialog_title)) },
        text = {
            Text(stringResource(R.string.delete_list_dialog_message, listName))
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors =
                    androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
            ) {
                Text(stringResource(R.string.delete_list_confirm))
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
fun NoListsEmptyState(
    onCreateList: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(id = R.string.no_task_lists_title),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(id = R.string.no_task_lists_hint),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
        Button(
            onClick = onCreateList,
            modifier = Modifier.padding(top = 16.dp),
        ) {
            Text(stringResource(id = R.string.create_first_list))
        }
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

enum class RefreshError {
    RATE_LIMITED,
    AUTH_FAILED,
    SERVER_ERROR,
    NETWORK_ERROR,
    UNKNOWN,
}

sealed class CreateListError {
    data object Offline : CreateListError()

    data object Failed : CreateListError()
}

sealed class EditListError {
    data object Offline : EditListError()

    data object Failed : EditListError()
}

sealed class DeleteListError {
    data object Offline : DeleteListError()

    data object Failed : DeleteListError()
}

@HiltViewModel
@Suppress("LongParameterList")
class TaskListViewModel
    @Inject
    constructor(
        private val loadTasksUseCase: LoadTasksUseCase,
        private val tasksRepository: com.nextcloud.tasks.domain.repository.TasksRepository,
        private val observeActiveAccountUseCase: com.nextcloud.tasks.domain.usecase.ObserveActiveAccountUseCase,
        private val getShareesUseCase: GetShareesUseCase,
        private val shareListUseCase: ShareListUseCase,
        private val unshareListUseCase: UnshareListUseCase,
        private val searchShareesUseCase: SearchShareesUseCase,
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

        // Track task IDs that were recently toggled and should animate entry in their new section
        private val _animatingEntryTaskIds = MutableStateFlow<Set<String>>(emptySet())
        val animatingEntryTaskIds = _animatingEntryTaskIds.asStateFlow()

        fun clearAnimatingEntryTaskId(taskId: String) {
            _animatingEntryTaskIds.update { it - taskId }
        }

        // Network status and pending changes
        val isOnline =
            tasksRepository
                .observeIsOnline()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

        val hasPendingChanges =
            tasksRepository
                .observeHasPendingChanges()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

        private val _refreshError = MutableStateFlow<RefreshError?>(null)
        val refreshError = _refreshError.asStateFlow()

        fun clearRefreshError() {
            _refreshError.value = null
        }

        private val _createListError = MutableStateFlow<CreateListError?>(null)
        val createListError = _createListError.asStateFlow()

        fun clearCreateListError() {
            _createListError.value = null
        }

        fun createTaskList(
            name: String,
            color: String? = null,
        ) {
            viewModelScope.launch {
                try {
                    val newList = tasksRepository.createTaskList(name, color)
                    _selectedListId.value = newList.id
                } catch (
                    @Suppress("TooGenericExceptionCaught") e: Exception,
                ) {
                    timber.log.Timber.e(e, "Failed to create task list")
                    _createListError.value =
                        if (!tasksRepository.isCurrentlyOnline()) {
                            CreateListError.Offline
                        } else {
                            CreateListError.Failed
                        }
                }
            }
        }

        private val _editListError = MutableStateFlow<EditListError?>(null)
        val editListError = _editListError.asStateFlow()

        fun clearEditListError() {
            _editListError.value = null
        }

        fun editTaskList(
            listId: String,
            name: String,
            color: String?,
        ) {
            viewModelScope.launch {
                try {
                    tasksRepository.updateTaskList(listId, name, color)
                } catch (
                    @Suppress("TooGenericExceptionCaught") e: Exception,
                ) {
                    timber.log.Timber.e(e, "Failed to edit task list")
                    _editListError.value =
                        if (!tasksRepository.isCurrentlyOnline()) {
                            EditListError.Offline
                        } else {
                            EditListError.Failed
                        }
                }
            }
        }

        private val _deleteListError = MutableStateFlow<DeleteListError?>(null)
        val deleteListError = _deleteListError.asStateFlow()

        fun clearDeleteListError() {
            _deleteListError.value = null
        }

        fun deleteTaskList(listId: String) {
            viewModelScope.launch {
                try {
                    tasksRepository.deleteTaskList(listId)
                    if (_selectedListId.value == listId) {
                        _selectedListId.value = null
                    }
                } catch (
                    @Suppress("TooGenericExceptionCaught") e: Exception,
                ) {
                    timber.log.Timber.e(e, "Failed to delete task list")
                    _deleteListError.value =
                        if (!tasksRepository.isCurrentlyOnline()) {
                            DeleteListError.Offline
                        } else {
                            DeleteListError.Failed
                        }
                }
            }
        }

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
                _refreshError.value = null
                try {
                    tasksRepository.refresh()
                } catch (e: com.nextcloud.tasks.data.caldav.service.CalDavHttpException) {
                    timber.log.Timber.e(e, "Failed to refresh tasks (HTTP ${e.statusCode})")
                    _refreshError.value =
                        when (e.statusCode) {
                            429 -> RefreshError.RATE_LIMITED
                            401, 403 -> RefreshError.AUTH_FAILED
                            in 500..599 -> RefreshError.SERVER_ERROR
                            else -> RefreshError.SERVER_ERROR
                        }
                } catch (e: java.net.UnknownHostException) {
                    timber.log.Timber.e(e, "Failed to refresh tasks (DNS)")
                    _refreshError.value = RefreshError.NETWORK_ERROR
                } catch (e: java.net.ConnectException) {
                    timber.log.Timber.e(e, "Failed to refresh tasks (connection)")
                    _refreshError.value = RefreshError.NETWORK_ERROR
                } catch (e: java.net.SocketTimeoutException) {
                    timber.log.Timber.e(e, "Failed to refresh tasks (timeout)")
                    _refreshError.value = RefreshError.NETWORK_ERROR
                } catch (
                    @Suppress("TooGenericExceptionCaught") e: Exception,
                ) {
                    timber.log.Timber.e(e, "Failed to refresh tasks")
                    _refreshError.value = RefreshError.UNKNOWN
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
            // Mark task for entry animation in the new section
            _animatingEntryTaskIds.update { it + task.id }
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

        // --- Sharing state ---
        private val _sharingListId = MutableStateFlow<String?>(null)
        val sharingListId = _sharingListId.asStateFlow()

        private val _sharees = MutableStateFlow<List<Sharee>>(emptyList())
        val sharees = _sharees.asStateFlow()

        private val _shareeSearchResults = MutableStateFlow<List<ShareeSearchResult>>(emptyList())
        val shareeSearchResults = _shareeSearchResults.asStateFlow()

        private val _shareeSearchQuery = MutableStateFlow("")
        val shareeSearchQuery = _shareeSearchQuery.asStateFlow()

        private val _shareError = MutableStateFlow<String?>(null)
        val shareError = _shareError.asStateFlow()

        private val _shareSuccess = MutableStateFlow(false)
        val shareSuccess = _shareSuccess.asStateFlow()

        private val _isLoadingSharees = MutableStateFlow(false)
        val isLoadingSharees = _isLoadingSharees.asStateFlow()

        private val _shareActionInProgress = MutableStateFlow<String?>(null)
        val shareActionInProgress = _shareActionInProgress.asStateFlow()

        fun openShareSheet(listId: String) {
            _sharingListId.value = listId
            _shareeSearchQuery.value = ""
            _shareeSearchResults.value = emptyList()
            _shareError.value = null
            _shareSuccess.value = false
            viewModelScope.launch { loadSharees(listId) }
        }

        fun closeShareSheet() {
            _sharingListId.value = null
            _sharees.value = emptyList()
            _shareeSearchResults.value = emptyList()
            _shareeSearchQuery.value = ""
            _shareError.value = null
            _shareSuccess.value = false
        }

        private suspend fun loadSharees(listId: String) {
            _isLoadingSharees.value = true
            try {
                _sharees.value = getShareesUseCase(listId)
            } catch (
                @Suppress("TooGenericExceptionCaught") e: Exception,
            ) {
                timber.log.Timber.e(e, "Failed to load sharees")
                _shareError.value = "load_sharees_failed"
            } finally {
                _isLoadingSharees.value = false
            }
        }

        private var searchJob: kotlinx.coroutines.Job? = null

        fun searchSharees(query: String) {
            _shareeSearchQuery.value = query
            searchJob?.cancel()
            if (query.length < 2) {
                _shareeSearchResults.value = emptyList()
                return
            }
            searchJob =
                viewModelScope.launch {
                    delay(300)
                    try {
                        _shareeSearchResults.value = searchShareesUseCase(query)
                    } catch (
                        @Suppress("TooGenericExceptionCaught") e: Exception,
                    ) {
                        timber.log.Timber.e(e, "Failed to search sharees")
                    }
                }
        }

        fun addSharee(
            shareeId: String,
            type: ShareeType,
            access: ShareAccess = ShareAccess.READ,
        ) {
            val listId = _sharingListId.value ?: return
            _shareActionInProgress.value = "add:$shareeId:$type"
            viewModelScope.launch {
                try {
                    shareListUseCase(listId, shareeId, type, access)
                    loadSharees(listId)
                    _shareSuccess.value = true
                    viewModelScope.launch {
                        delay(3000)
                        _shareSuccess.value = false
                    }
                } catch (
                    @Suppress("TooGenericExceptionCaught") e: Exception,
                ) {
                    timber.log.Timber.e(e, "Failed to share list")
                    val cause = e.cause ?: e
                    _shareError.value =
                        if (cause is CalDavHttpException && cause.statusCode == 403) {
                            "share_forbidden"
                        } else {
                            "share_failed"
                        }
                } finally {
                    _shareActionInProgress.value = null
                }
            }
        }

        fun clearShareSuccess() {
            _shareSuccess.value = false
        }

        fun removeSharee(
            shareeId: String,
            type: ShareeType,
        ) {
            val listId = _sharingListId.value ?: return
            _shareActionInProgress.value = "remove:$shareeId:$type"
            viewModelScope.launch {
                try {
                    unshareListUseCase(listId, shareeId, type)
                    loadSharees(listId)
                } catch (
                    @Suppress("TooGenericExceptionCaught") e: Exception,
                ) {
                    timber.log.Timber.e(e, "Failed to remove sharee")
                    _shareError.value = "share_failed"
                } finally {
                    _shareActionInProgress.value = null
                }
            }
        }

        fun updateShareeAccess(
            shareeId: String,
            type: ShareeType,
            access: ShareAccess,
        ) {
            val listId = _sharingListId.value ?: return
            _shareActionInProgress.value = "access:$shareeId:$type"
            viewModelScope.launch {
                try {
                    shareListUseCase(listId, shareeId, type, access)
                    loadSharees(listId)
                } catch (
                    @Suppress("TooGenericExceptionCaught") e: Exception,
                ) {
                    timber.log.Timber.e(e, "Failed to update sharee access")
                    _shareError.value = "share_failed"
                } finally {
                    _shareActionInProgress.value = null
                }
            }
        }

        fun clearShareError() {
            _shareError.value = null
        }
    }
