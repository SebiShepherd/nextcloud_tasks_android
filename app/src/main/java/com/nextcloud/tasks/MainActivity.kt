@file:Suppress("TooManyFunctions")

package com.nextcloud.tasks

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.RemoveCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.SubdirectoryArrowRight
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
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
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.nextcloud.tasks.auth.LoginFlowUiState
import com.nextcloud.tasks.auth.LoginFlowViewModel
import com.nextcloud.tasks.auth.ServerInputScreen
import com.nextcloud.tasks.data.caldav.service.CalDavHttpException
import com.nextcloud.tasks.detail.TaskDetailScreen
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
import com.nextcloud.tasks.ui.theme.NextcloudTasksTheme
import com.nextcloud.tasks.ui.theme.NextcloudWarning
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val taskListViewModel: TaskListViewModel by viewModels()
    private val loginFlowViewModel: LoginFlowViewModel by viewModels()

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Edge-to-edge so the main window delivers the IME as an animated inset. The create sheet is
        // an in-window overlay pinned to the bottom with imePadding, so it rides the keyboard up in
        // one motion (see CreateTaskOverlay) instead of a separate dialog window lagging behind.
        enableEdgeToEdge()

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
                // Overlays any screen: prompts to trust an untrusted server certificate.
                com.nextcloud.tasks.cert
                    .CertTrustDialog()
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
    val refreshErrorDetail by taskListViewModel.refreshErrorDetail.collectAsState()
    val collapsedIds by taskListViewModel.collapsedIds.collectAsState()
    val selectionMode by taskListViewModel.selectionMode.collectAsState()
    val selectedIds by taskListViewModel.selectedIds.collectAsState()
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
            collapsedIds = collapsedIds,
            selectionMode = selectionMode,
            selectedIds = selectedIds,
            onEnterSelection = taskListViewModel::enterSelection,
            onToggleSelection = taskListViewModel::toggleSelection,
            onClearSelection = taskListViewModel::clearSelection,
            onSelectAll = taskListViewModel::selectAll,
            onCompleteSelected = taskListViewModel::completeSelected,
            onDetachSelected = taskListViewModel::detachSelected,
            onMoveSelectedToList = taskListViewModel::moveSelectedToList,
            onStageDeleteSelected = taskListViewModel::stageDeleteSelected,
            onReorderTasks = taskListViewModel::reorderTasks,
            anySelectedIsChild = taskListViewModel::anySelectedIsChild,
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
            onCreateTask = { input ->
                taskListViewModel.createTask(input)
                showCreateDialog = false
            },
            onToggleFavorite = taskListViewModel::toggleFavorite,
            onToggleTaskCollapsed = taskListViewModel::toggleCollapsed,
            onApplyCompletion = taskListViewModel::applyCompletion,
            onStageDelete = taskListViewModel::stageDelete,
            onUndoDelete = taskListViewModel::undoDelete,
            onCommitDelete = taskListViewModel::commitDelete,
            onAddAccount = { forceShowLogin = true },
            onOpenSettings = { showSettings = true },
            refreshError = refreshError,
            refreshErrorDetail = refreshErrorDetail,
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
    collapsedIds: Set<String>,
    selectionMode: Boolean,
    selectedIds: Set<String>,
    onEnterSelection: (String) -> Unit,
    onToggleSelection: (String) -> Unit,
    onClearSelection: () -> Unit,
    onSelectAll: (Collection<String>) -> Unit,
    onCompleteSelected: () -> Unit,
    onDetachSelected: () -> Unit,
    onMoveSelectedToList: (String) -> Unit,
    onStageDeleteSelected: () -> Deletion,
    onReorderTasks: (List<Pair<String, String?>>) -> Unit,
    anySelectedIsChild: () -> Boolean,
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
    onCreateTask: (NewTaskInput) -> Unit,
    onToggleFavorite: (Task) -> Unit,
    onToggleTaskCollapsed: (String) -> Unit,
    onApplyCompletion: (Task) -> Unit,
    onStageDelete: (Task, Boolean) -> Deletion,
    onUndoDelete: (Deletion) -> Unit,
    onCommitDelete: (Deletion) -> Unit,
    onAddAccount: () -> Unit,
    onOpenSettings: () -> Unit,
    refreshError: RefreshError? = null,
    refreshErrorDetail: String? = null,
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

    RefreshErrorEffect(refreshError, refreshErrorDetail, snackbarHostState, onClearRefreshError)
    CreateListErrorEffect(createListError, snackbarHostState, onClearCreateListError)
    EditListErrorEffect(editListError, snackbarHostState, onClearEditListError)
    DeleteListErrorEffect(deleteListError, snackbarHostState, onClearDeleteListError)

    // Swipe actions: complete/delete with an undo snackbar. Deleting a task that has sub-tasks
    // first asks whether to delete the whole subtree or free the children.
    val undoLabel = stringResource(R.string.action_undo)
    val deletedMsg = stringResource(R.string.task_deleted)
    var deleteDialogTask by remember { mutableStateOf<Task?>(null) }
    var subtaskParent by remember { mutableStateOf<Task?>(null) }

    val performDelete: (Task, Boolean) -> Unit = { task, keepChildren ->
        val deletion = onStageDelete(task, keepChildren)
        scope.launch {
            val result = snackbarHostState.showSnackbar(deletedMsg, undoLabel, duration = SnackbarDuration.Short)
            if (result == SnackbarResult.ActionPerformed) onUndoDelete(deletion) else onCommitDelete(deletion)
        }
    }
    val onSwipeDelete: (Task, Boolean) -> Unit = { task, hasChildren ->
        if (hasChildren) deleteDialogTask = task else performDelete(task, false)
    }
    // Bulk delete from selection mode: no per-task dialog (children are freed automatically), one undo.
    val performBulkDelete: () -> Unit = {
        val deletion = onStageDeleteSelected()
        if (deletion.deleteIds.isNotEmpty()) {
            scope.launch {
                val result = snackbarHostState.showSnackbar(deletedMsg, undoLabel, duration = SnackbarDuration.Short)
                if (result == SnackbarResult.ActionPerformed) onUndoDelete(deletion) else onCommitDelete(deletion)
            }
        }
    }
    BackHandler(enabled = selectionMode) { onClearSelection() }
    // Unified completion for both the checkbox and the swipe: mark done/reopen (cascading).
    // No snackbar — re-tapping the checkbox already undoes it, so a toast would just be noise.
    val completeWithUndo: (Task) -> Unit = { task -> onApplyCompletion(task) }

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
                        if (selectionMode) {
                            SelectionTopBar(
                                count = selectedIds.size,
                                canDetach = anySelectedIsChild(),
                                canAddSubtask = selectedIds.size == 1,
                                lists = taskLists.filter { it.shareAccess != ShareAccess.READ },
                                onExit = onClearSelection,
                                onComplete = onCompleteSelected,
                                onAddSubtask = {
                                    subtaskParent = tasks.firstOrNull { it.id in selectedIds }
                                    onClearSelection()
                                },
                                onMove = onMoveSelectedToList,
                                onSelectAll = { onSelectAll(tasks.map { it.id }) },
                                onDetach = onDetachSelected,
                                onDelete = performBulkDelete,
                            )
                        } else {
                            UnifiedSearchBar(
                                state = state,
                                searchQuery = searchQuery,
                                onSearchQueryChange = onSetSearchQuery,
                                onOpenDrawer =
                                    if (isExpandedScreen) null else ({ scope.launch { drawerState.open() } }),
                                onSwitchAccount = onSwitchAccount,
                                onLogout = onLogout,
                                taskSort = taskSort,
                                onSetSort = onSetSort,
                                onAddAccount = onAddAccount,
                            )
                        }

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
                                collapsedIds = collapsedIds,
                                selectionMode = selectionMode,
                                selectedIds = selectedIds,
                                onEnterSelection = onEnterSelection,
                                onToggleSelection = onToggleSelection,
                                onReorder = onReorderTasks,
                                isExpandedScreen = isExpandedScreen,
                                onSetFilter = onSetFilter,
                                onSetSort = onSetSort,
                                onToggleTaskComplete = { task ->
                                    if (!isReadOnly) {
                                        completeWithUndo(task)
                                        if (!isOnline) {
                                            showOfflineSnackbar = true
                                        }
                                    }
                                },
                                onToggleFavorite = { task ->
                                    if (!isReadOnly) {
                                        onToggleFavorite(task)
                                        if (!isOnline) {
                                            showOfflineSnackbar = true
                                        }
                                    }
                                },
                                onToggleTaskCollapsed = onToggleTaskCollapsed,
                                onSwipeDelete = onSwipeDelete,
                                onShowCreateListDialog = onShowCreateListDialog,
                                onOpenTask = { taskId -> navController.navigate("task/$taskId") },
                            )
                        }
                    }
                }
            }
            composable("task/{taskId}") {
                TaskDetailScreen(
                    onNavigateBack = { navController.navigateUp() },
                    onOpenTask = { taskId -> navController.navigate("task/$taskId") },
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

    // The create sheet is an in-window overlay (see CreateTaskOverlay), so it must be a sibling of
    // the drawer inside one full-screen Box that draws it on top.
    Box(modifier = Modifier.fillMaxSize()) {
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

        // Create sheet — only shown when at least one writable list exists.
        if (showCreateDialog && hasWritableLists) {
            CreateTaskOverlay(
                taskLists = taskLists,
                tasks = tasks,
                initialListId = selectedListId ?: taskLists.first().id,
                onDismiss = onDismissCreateDialog,
                onCreate = { input ->
                    onCreateTask(input)
                    if (!isOnline) {
                        showOfflineSnackbar = true
                    }
                },
            )
        }

        // Add-sub-task from the selection bar: the create sheet opens with the parent pre-set.
        subtaskParent?.let { parent ->
            CreateTaskOverlay(
                taskLists = taskLists,
                tasks = tasks,
                initialListId = parent.listId,
                initialParentUid = parent.uid,
                onDismiss = { subtaskParent = null },
                onCreate = { input ->
                    onCreateTask(input)
                    subtaskParent = null
                    if (!isOnline) {
                        showOfflineSnackbar = true
                    }
                },
            )
        }
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

    // Delete-with-sub-tasks choice dialog (shown when swiping a parent away)
    deleteDialogTask?.let { task ->
        DeleteWithChildrenDialog(
            onDeleteAll = {
                performDelete(task, false)
                deleteDialogTask = null
            },
            onKeepChildren = {
                performDelete(task, true)
                deleteDialogTask = null
            },
            onDismiss = { deleteDialogTask = null },
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
    refreshErrorDetail: String?,
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
                RefreshError.SERVER_ERROR -> withErrorDetail(serverErrorMsg, refreshErrorDetail)
                RefreshError.NETWORK_ERROR -> networkErrorMsg
                RefreshError.UNKNOWN -> withErrorDetail(unknownErrorMsg, refreshErrorDetail)
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
                    is CreateListError.Failed -> withErrorDetail(failedMsg, error.detail)
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
                    is EditListError.Failed -> withErrorDetail(failedMsg, error.detail)
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
                    is DeleteListError.Failed -> withErrorDetail(failedMsg, error.detail)
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
                // 72 dp box with 8 dp vertical padding → 56 dp pill (Material 3 SearchBar input height).
                .height(72.dp),
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
            shape = RoundedCornerShape(if (isSearchActive) 0.dp else 28.dp),
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
                    text = stringResource(R.string.sort_by_manual),
                    isSelected = currentSort == com.nextcloud.tasks.domain.model.TaskSort.MANUAL,
                    onClick = {
                        onSetSort(com.nextcloud.tasks.domain.model.TaskSort.MANUAL)
                        onDismiss()
                    },
                )
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

/** Everything the create sheet collects for a new task. */
data class NewTaskInput(
    val title: String,
    val description: String?,
    val listId: String,
    val parentUid: String?,
    val due: java.time.Instant?,
    val starred: Boolean,
)

/**
 * A staged, still-undoable delete. Rows in [hiddenIds] vanish from the list immediately; on commit
 * [freeIds] are detached from their parent (parentUid=null) and [deleteIds] are removed from the
 * server. Undo just un-hides — nothing was written yet.
 */
data class Deletion(
    val hiddenIds: Set<String>,
    val deleteIds: List<String>,
    val freeIds: List<String>,
)

/** A flattened sub-task tree row: the task plus its display depth and sub-task chip data. */
internal data class TaskRow(
    val task: Task,
    val depth: Int,
    val hasChildren: Boolean,
    val subtaskDone: Int,
    val subtaskTotal: Int,
    val isCollapsed: Boolean,
)

/** Deepest indent level rendered; nesting beyond this renders flat at this depth (data untouched). */
internal const val MAX_DISPLAY_DEPTH = 4

/**
 * Flattens open tasks of one list into an ordered list of [TaskRow]s: each parent is followed by its
 * children, indented one level deeper. Display depth is capped at [MAX_DISPLAY_DEPTH] (deeper nesting
 * from the web renders flat at that level). Cycles are broken via a visited set.
 *
 * @param listTasks all tasks of the list, already in the desired sibling order.
 * @param childCounts parentUid → (done, total) across ALL tasks in the list, for the collapse chip.
 * @param collapsedUids UIDs of parents whose children are hidden.
 */
internal fun buildOpenTaskRows(
    listTasks: List<Task>,
    childCounts: Map<String, Pair<Int, Int>>,
    collapsedUids: Set<String>,
    done: Boolean = false,
): List<TaskRow> {
    val byParentUid = listTasks.groupBy { it.parentUid }
    val uidsInList = listTasks.mapNotNull { it.uid }.toSet()
    // Roots: top-level (or orphaned) tasks matching the requested done state. An open tree renders its
    // whole subtree INCLUDING done children (struck through in place); the done tree only nests done
    // children so an open child of a done parent still surfaces in the open section, not here.
    val roots =
        listTasks.filter {
            (it.parentUid == null || it.parentUid !in uidsInList) && it.isEffectivelyDone == done
        }
    val rows = mutableListOf<TaskRow>()
    val visited = mutableSetOf<String>()

    fun emit(
        task: Task,
        depth: Int,
    ) {
        if (!visited.add(task.id)) return
        val (childDone, total) = task.uid?.let { childCounts[it] } ?: (0 to 0)
        val collapsed = task.uid != null && task.uid in collapsedUids
        rows.add(TaskRow(task, depth.coerceAtMost(MAX_DISPLAY_DEPTH), total > 0, childDone, total, collapsed))
        if (!collapsed) {
            task.uid
                ?.let { byParentUid[it] }
                ?.filter { !done || it.isEffectivelyDone }
                ?.forEach { emit(it, depth + 1) }
        }
    }
    roots.forEach { emit(it, 0) }
    // Tasks stuck in a parent cycle with no reachable root — surface at top level so they aren't lost.
    listTasks.forEach {
        if (it.isEffectivelyDone == done && it.id !in visited && it.parentUid !in visited) emit(it, 0)
    }
    return rows
}

/** Comparator for the active [sort]; MANUAL orders by the drag-assigned sortOrder (nulls last). */
internal fun taskComparator(sort: com.nextcloud.tasks.domain.model.TaskSort): Comparator<Task> =
    when (sort) {
        com.nextcloud.tasks.domain.model.TaskSort.MANUAL ->
            compareBy(nullsLast()) { t: Task -> t.sortOrder }.thenByDescending { t: Task -> t.updatedAt }
        com.nextcloud.tasks.domain.model.TaskSort.DUE_DATE -> compareBy(nullsLast()) { t: Task -> t.due }
        com.nextcloud.tasks.domain.model.TaskSort.PRIORITY -> compareBy(nullsLast()) { t: Task -> t.priority }
        com.nextcloud.tasks.domain.model.TaskSort.TITLE -> compareBy { t: Task -> t.title }
        com.nextcloud.tasks.domain.model.TaskSort.UPDATED_AT -> compareByDescending { t: Task -> t.updatedAt }
    }

/** Live drag-reorder state for the open list, produced by [rememberManualReorder]. */
internal class ManualReorder(
    val enabled: Boolean,
    val lazyListState: androidx.compose.foundation.lazy.LazyListState,
    val reorderState: sh.calvin.reorderable.ReorderableLazyListState,
    val manualIds: List<String>,
    val rowById: Map<String, TaskRow>,
    val persist: () -> Unit,
)

/**
 * Builds the drag-reorder state: a live id order the drag mutates, kept in sync with [rows] while not
 * dragging, plus a [persist] that writes the new order (id → parentUid) back through [onReorder].
 */
@Composable
private fun rememberManualReorder(
    taskSort: com.nextcloud.tasks.domain.model.TaskSort,
    openListIds: List<String>,
    selectionMode: Boolean,
    treeByList: Map<String, List<TaskRow>>,
    onReorder: (List<Pair<String, String?>>) -> Unit,
): ManualReorder {
    // Drag & drop only in "My order" while a single list is shown and not selecting — reordering across
    // list sections or against a field sort has no meaning.
    val enabled =
        taskSort == com.nextcloud.tasks.domain.model.TaskSort.MANUAL && openListIds.size == 1 && !selectionMode
    val rows = if (enabled) openListIds.firstOrNull()?.let { treeByList[it] }.orEmpty() else emptyList()
    val lazyListState = rememberLazyListState()
    val rowById = remember(rows) { rows.associateBy { it.task.id } }
    val manualIds = remember { mutableStateListOf<String>() }
    LaunchedEffect(rows.map { it.task.id }) {
        manualIds.clear()
        manualIds.addAll(rows.map { it.task.id })
    }
    val reorderState =
        rememberReorderableLazyListState(lazyListState) { from, to ->
            val f = manualIds.indexOf(from.key)
            val t = manualIds.indexOf(to.key)
            if (f in manualIds.indices && t in manualIds.indices) {
                manualIds.add(t, manualIds.removeAt(f))
            }
        }
    return ManualReorder(
        enabled = enabled,
        lazyListState = lazyListState,
        reorderState = reorderState,
        manualIds = manualIds,
        rowById = rowById,
        persist = { onReorder(manualIds.map { id -> id to rowById[id]?.task?.parentUid }) },
    )
}

/** The per-row callbacks, bundled so the list rendering doesn't thread seven lambdas through. */
@Suppress("LongParameterList")
class TaskRowCallbacks(
    val onToggleTaskComplete: (Task) -> Unit,
    val onSwipeDelete: (Task, Boolean) -> Unit,
    val onToggleFavorite: (Task) -> Unit,
    val onToggleTaskCollapsed: (String) -> Unit,
    val onOpenTask: (String) -> Unit,
    val onEnterSelection: (String) -> Unit,
    val onToggleSelection: (String) -> Unit,
)

/** parentUid → (done, total) child counts across [tasks], for the sub-task collapse chip. */
internal fun subtaskChildCounts(tasks: List<Task>): Map<String, Pair<Int, Int>> =
    tasks
        .groupBy { it.parentUid }
        .entries
        .mapNotNull { (parentUid, kids) ->
            parentUid?.let { it to (kids.count(Task::isEffectivelyDone) to kids.size) }
        }.toMap()

/** Nested rows for the completed section: done parents followed by their done children, indented. */
internal fun buildCompletedTaskRows(
    completedTasks: List<Task>,
    collapsedUids: Set<String>,
): List<TaskRow> = buildOpenTaskRows(completedTasks, subtaskChildCounts(completedTasks), collapsedUids, done = true)

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
    collapsedIds: Set<String>,
    selectionMode: Boolean,
    selectedIds: Set<String>,
    onEnterSelection: (String) -> Unit,
    onToggleSelection: (String) -> Unit,
    onReorder: (List<Pair<String, String?>>) -> Unit,
    isExpandedScreen: Boolean = false,
    onSetFilter: (com.nextcloud.tasks.domain.model.TaskFilter) -> Unit,
    onSetSort: (com.nextcloud.tasks.domain.model.TaskSort) -> Unit,
    onToggleTaskComplete: (Task) -> Unit,
    onToggleFavorite: (Task) -> Unit,
    onToggleTaskCollapsed: (String) -> Unit,
    onSwipeDelete: (Task, Boolean) -> Unit,
    onShowCreateListDialog: () -> Unit = {},
    onOpenTask: (String) -> Unit = {},
) {
    var showCompletedTasks by remember { mutableStateOf(false) }

    // Create a map for quick lookup of task list info
    val taskListMap = remember(taskLists) { taskLists.associateBy { it.id } }

    // Group tasks by completion status, filtering out tasks with unknown lists
    // (can happen briefly during account switch before refresh completes)
    val knownTasks = remember(tasks, taskListMap) { tasks.filter { it.listId in taskListMap } }

    // Build the open sub-task tree per list. Subtree + chip counts include done tasks, so a done
    // child stays nested (struck through) under its still-open parent; only done ROOTS drop into the
    // completed section below.
    val treeByList =
        knownTasks.groupBy { it.listId }.mapValues { (_, listTasks) ->
            buildOpenTaskRows(listTasks, subtaskChildCounts(listTasks), collapsedIds)
        }
    val emittedIds = treeByList.values.flatten().mapTo(mutableSetOf()) { it.task.id }
    // Lists that actually have an open tree, in first-seen order.
    val openListIds = knownTasks.map { it.listId }.distinct().filter { treeByList[it]?.isNotEmpty() == true }
    val completedTasks = knownTasks.filter { it.isEffectivelyDone && it.id !in emittedIds }
    // Keep the sub-task tree in the completed section too (done parent → done children, indented),
    // instead of flattening everything to depth 0.
    val completedRows = buildCompletedTaskRows(completedTasks, collapsedIds)

    // On expanded screens, constrain max content width for readability
    val contentModifier =
        if (isExpandedScreen) {
            Modifier.padding(padding).widthIn(max = 720.dp).fillMaxWidth()
        } else {
            Modifier.padding(padding)
        }

    val reorder =
        rememberManualReorder(
            taskSort = taskSort,
            openListIds = openListIds,
            selectionMode = selectionMode,
            treeByList = treeByList,
            onReorder = onReorder,
        )
    val rowCallbacks =
        TaskRowCallbacks(
            onToggleTaskComplete = onToggleTaskComplete,
            onSwipeDelete = onSwipeDelete,
            onToggleFavorite = onToggleFavorite,
            onToggleTaskCollapsed = onToggleTaskCollapsed,
            onOpenTask = onOpenTask,
            onEnterSelection = onEnterSelection,
            onToggleSelection = onToggleSelection,
        )

    LazyColumn(
        state = reorder.lazyListState,
        modifier = contentModifier,
        contentPadding =
            PaddingValues(
                start = if (isExpandedScreen) 24.dp else 16.dp,
                end = if (isExpandedScreen) 24.dp else 16.dp,
                top = 16.dp,
                bottom = 16.dp,
            ),
    ) {
        if (openListIds.isEmpty() && completedTasks.isEmpty()) {
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
            if (openListIds.isNotEmpty()) {
                openListIds.forEach { listId ->
                    // Get list info from map
                    val taskList = taskListMap[listId]

                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier =
                                Modifier.padding(
                                    top = if (openListIds.first() != listId) 16.dp else 0.dp,
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

                    openListRows(
                        reorderable = reorder.enabled,
                        rows = treeByList.getValue(listId),
                        manualIds = reorder.manualIds,
                        rowById = reorder.rowById,
                        reorderState = reorder.reorderState,
                        persistOrder = reorder.persist,
                        taskListMap = taskListMap,
                        selectionMode = selectionMode,
                        selectedIds = selectedIds,
                        callbacks = rowCallbacks,
                    )
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
                    items(completedRows, key = { it.task.id }) { row ->
                        TaskRowItem(
                            row = row,
                            taskListMap = taskListMap,
                            selectionMode = selectionMode,
                            isSelected = row.task.id in selectedIds,
                            callbacks = rowCallbacks,
                            modifier = Modifier.animateItem(),
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
    Column(
        modifier =
            Modifier
                .fillMaxHeight()
                .padding(16.dp),
    ) {
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

        // Only the list section scrolls; the header above and the create/settings
        // footer below stay pinned. weight(1f) lets it take the remaining height.
        LazyColumn(modifier = Modifier.weight(1f)) {
            if (taskLists.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.no_task_lists_sidebar),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            } else {
                items(taskLists, key = { it.id }) { taskList ->
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
 * A task row: sub-task indent rail + [TaskCard] + bottom spacing. Add/remove/move is animated by the
 * enclosing LazyColumn via Modifier.animateItem() — this composable must NOT run its own size
 * animation, because collapsing to height 0 inside a SwipeToDismissBox makes the swipe anchors settle
 * and fire a spurious dismiss (was double-firing delete on a plain checkbox tap).
 */
@Suppress("LongParameterList")
@Composable
private fun SimpleAnimatedTaskCard(
    task: Task,
    isReadOnly: Boolean = false,
    depth: Int = 0,
    hasChildren: Boolean = false,
    subtaskDone: Int = 0,
    subtaskTotal: Int = 0,
    isCollapsed: Boolean = false,
    isStarred: Boolean = false,
    isSelected: Boolean = false,
    onToggleComplete: () -> Unit,
    onToggleCollapsed: () -> Unit = {},
    onToggleFavorite: () -> Unit = {},
    onOpenTask: () -> Unit = {},
    onLongPress: () -> Unit = {},
    dragHandle: (@Composable () -> Unit)? = null,
) {
    Column {
        // Sub-task rows: rail margin + 2 dp guide line + gap before the card. Rail steps 16 dp per
        // level (Ebene 1: 9 + 2 + 16 dp, Ebene 2: 25 + 2 + 12 dp, then +16 dp each deeper level).
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        ) {
            if (depth > 0) {
                Spacer(modifier = Modifier.width((9 + (depth - 1) * 16).dp))
                Box(
                    modifier =
                        Modifier
                            .width(2.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.outlineVariant),
                )
                Spacer(modifier = Modifier.width(if (depth == 1) 16.dp else 12.dp))
            }
            Box(modifier = Modifier.weight(1f)) {
                TaskCard(
                    task = task,
                    isReadOnly = isReadOnly,
                    depth = depth,
                    hasChildren = hasChildren,
                    subtaskDone = subtaskDone,
                    subtaskTotal = subtaskTotal,
                    isCollapsed = isCollapsed,
                    isStarred = isStarred,
                    isSelected = isSelected,
                    onToggleComplete = onToggleComplete,
                    onToggleCollapsed = onToggleCollapsed,
                    onToggleFavorite = onToggleFavorite,
                    onOpenTask = onOpenTask,
                    onLongPress = onLongPress,
                    dragHandle = dragHandle,
                )
            }
        }
        Spacer(modifier = Modifier.height(if (depth > 0) 8.dp else 12.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Suppress("LongParameterList", "LongMethod", "CyclomaticComplexMethod")
@Composable
private fun TaskCard(
    task: Task,
    isReadOnly: Boolean = false,
    depth: Int = 0,
    hasChildren: Boolean = false,
    subtaskDone: Int = 0,
    subtaskTotal: Int = 0,
    isCollapsed: Boolean = false,
    isStarred: Boolean = false,
    isSelected: Boolean = false,
    onToggleComplete: () -> Unit,
    onToggleCollapsed: () -> Unit = {},
    onToggleFavorite: () -> Unit = {},
    onOpenTask: () -> Unit = {},
    onLongPress: () -> Unit = {},
    dragHandle: (@Composable () -> Unit)? = null,
) {
    val isChild = depth > 0
    val hasDescription = !isChild && task.description != null
    val hasDueOrTags = task.due != null || task.tags.isNotEmpty()
    val hasAdditionalContent = hasDescription || hasDueOrTags || hasChildren
    // CANCELLED tasks are treated as completed for display purposes.
    val isCancelledTask = task.status?.uppercase() == "CANCELLED"
    val localCompleted = task.completed || isCancelledTask
    val titleStyle = if (isChild) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.titleMedium
    val starHit = if (isChild) 32.dp else 36.dp
    val starIconSize = if (isChild) 20.dp else 22.dp
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    val shortDateFormatter =
        remember(locale) {
            val pattern =
                android.text.format.DateFormat
                    .getBestDateTimePattern(locale, "MMMMd")
            java.time.format.DateTimeFormatter
                .ofPattern(pattern, locale)
        }

    val borderColor =
        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val containerColor =
        if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
        shape = MaterialTheme.shapes.medium,
        // Long-press enters selection mode; a tap opens the task or toggles selection (decided by caller).
        modifier =
            Modifier.combinedClickable(
                onClick = onOpenTask,
                onLongClick = onLongPress,
            ),
    ) {
        Row(
            modifier = Modifier.padding(if (isChild) 10.dp else 12.dp).fillMaxWidth(),
            verticalAlignment = if (hasAdditionalContent) Alignment.Top else Alignment.CenterVertically,
        ) {
            // Checkbox — CANCELLED tasks are displayed as checked (like the web UI)
            Checkbox(
                checked = localCompleted,
                onCheckedChange =
                    if (isReadOnly) {
                        null
                    } else {
                        { onToggleComplete() }
                    },
            )

            // Task content
            Column(modifier = Modifier.weight(1f).padding(start = 8.dp, end = 8.dp)) {
                Text(
                    text = task.title,
                    style =
                        titleStyle.copy(
                            textDecoration =
                                if (isCancelledTask) {
                                    androidx.compose.ui.text.style.TextDecoration.LineThrough
                                } else {
                                    androidx.compose.ui.text.style.TextDecoration.None
                                },
                        ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth(),
                )

                if (hasDescription) {
                    Text(
                        text = task.description.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                // Meta row: collapse chip (if the task has sub-tasks), due date, tags
                if (hasChildren || hasDueOrTags) {
                    Row(
                        modifier = Modifier.padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (hasChildren) {
                            Surface(
                                onClick = onToggleCollapsed,
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.height(24.dp),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(start = 4.dp, end = 8.dp),
                                ) {
                                    Icon(
                                        imageVector =
                                            if (isCollapsed) Icons.Filled.ExpandMore else Icons.Filled.ExpandLess,
                                        contentDescription = stringResource(R.string.subtasks_label),
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        text = "$subtaskDone/$subtaskTotal",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        }
                        task.due?.let { due ->
                            Text(
                                text =
                                    stringResource(
                                        R.string.task_due_label,
                                        shortDateFormatter.format(due.atZone(java.time.ZoneId.systemDefault())),
                                    ),
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

            // Favourite star (writes PRIORITY) — vertically centred regardless of row alignment
            Box(
                modifier =
                    Modifier
                        .align(Alignment.CenterVertically)
                        .size(starHit)
                        .clip(CircleShape)
                        .clickable(enabled = !isReadOnly, onClick = onToggleFavorite),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isStarred) Icons.Filled.Star else Icons.Filled.StarBorder,
                    contentDescription = stringResource(R.string.favorite_description),
                    modifier = Modifier.size(starIconSize),
                    tint = if (isStarred) NextcloudWarning else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Drag handle (My order only) — provided by the reorderable item scope.
            dragHandle?.let { handle ->
                Box(
                    modifier = Modifier.align(Alignment.CenterVertically).padding(start = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    handle()
                }
            }
        }
    }
}

/**
 * One list row: the swipe wrapper plus the card. Swipe is disabled in selection mode; a tap then
 * toggles selection instead of opening, and a long-press enters selection mode.
 */
@Composable
private fun TaskRowItem(
    row: TaskRow,
    taskListMap: Map<String, com.nextcloud.tasks.domain.model.TaskList>,
    selectionMode: Boolean,
    isSelected: Boolean,
    callbacks: TaskRowCallbacks,
    modifier: Modifier = Modifier,
    dragHandle: (@Composable () -> Unit)? = null,
) {
    val task = row.task
    val taskIsReadOnly = taskListMap[task.listId]?.shareAccess == ShareAccess.READ
    SwipeableTaskRow(
        enabled = !taskIsReadOnly && !selectionMode,
        hasChildren = row.hasChildren,
        bottomInset = if (row.depth > 0) 8.dp else 12.dp,
        onComplete = { callbacks.onToggleTaskComplete(task) },
        onDelete = { hasChildren -> callbacks.onSwipeDelete(task, hasChildren) },
        modifier = modifier,
    ) {
        SimpleAnimatedTaskCard(
            task = task,
            isReadOnly = taskIsReadOnly,
            depth = row.depth,
            hasChildren = row.hasChildren,
            subtaskDone = row.subtaskDone,
            subtaskTotal = row.subtaskTotal,
            isCollapsed = row.isCollapsed,
            isStarred = task.isStarred,
            isSelected = isSelected,
            onToggleComplete = { callbacks.onToggleTaskComplete(task) },
            onToggleFavorite = { callbacks.onToggleFavorite(task) },
            onToggleCollapsed = { task.uid?.let(callbacks.onToggleTaskCollapsed) },
            onOpenTask = {
                if (selectionMode) callbacks.onToggleSelection(task.id) else callbacks.onOpenTask(task.id)
            },
            onLongPress = { callbacks.onEnterSelection(task.id) },
            dragHandle = dragHandle,
        )
    }
}

/** Renders one list's open rows — draggable (My order) or plain. */
@Suppress("LongParameterList")
private fun LazyListScope.openListRows(
    reorderable: Boolean,
    rows: List<TaskRow>,
    manualIds: List<String>,
    rowById: Map<String, TaskRow>,
    reorderState: sh.calvin.reorderable.ReorderableLazyListState,
    persistOrder: () -> Unit,
    taskListMap: Map<String, com.nextcloud.tasks.domain.model.TaskList>,
    selectionMode: Boolean,
    selectedIds: Set<String>,
    callbacks: TaskRowCallbacks,
) {
    if (reorderable) {
        items(manualIds, key = { it }) { id ->
            val row = rowById[id] ?: return@items
            ReorderableItem(reorderState, key = id) { _ ->
                TaskRowItem(
                    row = row,
                    taskListMap = taskListMap,
                    selectionMode = selectionMode,
                    isSelected = row.task.id in selectedIds,
                    callbacks = callbacks,
                    dragHandle = {
                        Icon(
                            imageVector = Icons.Filled.DragIndicator,
                            contentDescription = stringResource(R.string.reorder_handle),
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.draggableHandle(onDragStopped = { persistOrder() }),
                        )
                    },
                )
            }
        }
    } else {
        items(rows, key = { it.task.id }) { row ->
            TaskRowItem(
                row = row,
                taskListMap = taskListMap,
                selectionMode = selectionMode,
                isSelected = row.task.id in selectedIds,
                callbacks = callbacks,
                modifier = Modifier.animateItem(),
            )
        }
    }
}

/**
 * Wraps a task row so swiping right completes it and swiping left deletes it. The gesture fires the
 * action and snaps back (returns false from confirmValueChange) — the action removes the row itself,
 * which keeps the swipe reusable if an undo brings the row back. Disabled on read-only lists.
 */
@Composable
private fun SwipeableTaskRow(
    enabled: Boolean,
    hasChildren: Boolean,
    bottomInset: androidx.compose.ui.unit.Dp,
    onComplete: () -> Unit,
    onDelete: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    if (!enabled) {
        Box(modifier = modifier) { content() }
        return
    }
    // Always snap back (return false): the action drives removal itself — complete moves the row to
    // the done section, delete hides it via the pending set — and the LazyColumn animates it out with
    // animateItem(). Returning true would leave the box in a dismissed state showing its coloured
    // background; on the delete-with-children path (which only opens a dialog) that background then
    // sticks forever if the dialog is cancelled.
    val state =
        rememberSwipeToDismissBoxState(
            confirmValueChange = { value ->
                when (value) {
                    SwipeToDismissBoxValue.StartToEnd -> onComplete()
                    SwipeToDismissBoxValue.EndToStart -> onDelete(hasChildren)
                    SwipeToDismissBoxValue.Settled -> Unit
                }
                false
            },
        )
    SwipeToDismissBox(
        state = state,
        modifier = modifier,
        // dismissDirection follows the drag offset immediately, so the colour + icon reveal as the
        // row moves (targetValue only flips past the settle threshold, leaving a blank gap on a
        // partial swipe).
        backgroundContent = { SwipeActionBackground(state.dismissDirection, bottomInset) },
        content = { content() },
    )
}

@Composable
private fun SwipeActionBackground(
    direction: SwipeToDismissBoxValue,
    bottomInset: androidx.compose.ui.unit.Dp,
) {
    val completing = direction == SwipeToDismissBoxValue.StartToEnd
    val color =
        when (direction) {
            SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.primary
            SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.error
            SwipeToDismissBoxValue.Settled -> androidx.compose.ui.graphics.Color.Transparent
        }
    val onColor = if (completing) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onError
    Box(
        modifier =
            Modifier
                // Exclude the row's bottom gap so the coloured pill matches the card height exactly.
                .fillMaxSize()
                .padding(bottom = bottomInset)
                .clip(MaterialTheme.shapes.medium)
                .background(color)
                .padding(horizontal = 20.dp),
        contentAlignment = if (completing) Alignment.CenterStart else Alignment.CenterEnd,
    ) {
        if (direction != SwipeToDismissBoxValue.Settled) {
            val label =
                if (completing) {
                    stringResource(R.string.swipe_complete)
                } else {
                    stringResource(R.string.delete)
                }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = if (completing) Icons.Filled.Check else Icons.Default.Delete,
                    contentDescription = label,
                    tint = onColor,
                )
                Text(text = label, style = MaterialTheme.typography.labelSmall, color = onColor)
            }
        }
    }
}

@Composable
private fun DeleteWithChildrenDialog(
    onDeleteAll: () -> Unit,
    onKeepChildren: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_subtasks_title)) },
        text = { Text(stringResource(R.string.delete_subtasks_message)) },
        confirmButton = {
            TextButton(onClick = onDeleteAll) {
                Text(
                    stringResource(R.string.delete_subtasks_all),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onKeepChildren) {
                Text(stringResource(R.string.delete_subtasks_keep))
            }
        },
    )
}

/**
 * Contextual top bar shown while the selection mode is active — replaces the search bar. Back exits,
 * the count sits left, then bulk-complete and an overflow (select all, detach, delete).
 */
@Suppress("LongParameterList", "LongMethod")
@Composable
private fun SelectionTopBar(
    count: Int,
    canDetach: Boolean,
    canAddSubtask: Boolean,
    lists: List<com.nextcloud.tasks.domain.model.TaskList>,
    onExit: () -> Unit,
    onComplete: () -> Unit,
    onAddSubtask: () -> Unit,
    onMove: (String) -> Unit,
    onSelectAll: () -> Unit,
    onDetach: () -> Unit,
    onDelete: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }
    var showMoveMenu by remember { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxWidth().height(72.dp).padding(horizontal = 16.dp, vertical = 8.dp)) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
            ) {
                IconButton(onClick = onExit) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.close))
                }
                // Per the reference design the bar shows just the number; the full "N selected"
                // stays as the accessibility label so TalkBack still announces the context.
                val countLabel = stringResource(R.string.selection_count, count)
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    modifier =
                        Modifier
                            .weight(1f)
                            .padding(start = 4.dp, end = 8.dp)
                            .semantics { contentDescription = countLabel },
                )
                IconButton(onClick = onComplete) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = stringResource(R.string.mark_complete))
                }
                // Add sub-task — only meaningful for a single selection; dimmed otherwise.
                IconButton(onClick = onAddSubtask, enabled = canAddSubtask) {
                    Icon(
                        Icons.Filled.SubdirectoryArrowRight,
                        contentDescription = stringResource(R.string.add_subtask),
                    )
                }
                Box {
                    IconButton(onClick = { showMoveMenu = true }) {
                        Icon(Icons.Filled.DriveFileMove, contentDescription = stringResource(R.string.move_to_list))
                    }
                    androidx.compose.material3.DropdownMenu(
                        expanded = showMoveMenu,
                        onDismissRequest = { showMoveMenu = false },
                    ) {
                        lists.forEach { list ->
                            DropdownMenuItem(
                                text = { Text(list.name) },
                                onClick = {
                                    showMoveMenu = false
                                    onMove(list.id)
                                },
                            )
                        }
                    }
                }
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.more_options))
                    }
                    androidx.compose.material3.DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.select_all)) },
                            leadingIcon = { Icon(Icons.Filled.Checklist, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onSelectAll()
                            },
                        )
                        if (canDetach) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.selection_detach)) },
                                leadingIcon = { Icon(Icons.Filled.LinkOff, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onDetach()
                                },
                            )
                        }
                        androidx.compose.material3.HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.Delete,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            },
                            onClick = {
                                showMenu = false
                                onDelete()
                            },
                        )
                    }
                }
            }
        }
    }
}

private val DUE_DATE_FORMATTER: java.time.format.DateTimeFormatter =
    java.time.format.DateTimeFormatter
        .ofLocalizedDate(java.time.format.FormatStyle.MEDIUM)

@Composable
private fun sheetFieldColors() =
    TextFieldDefaults.colors(
        focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
        unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
        focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
        unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
    )

/**
 * Bottom sheet for creating a task or sub-task. Title is the only required field; description, due
 * date, favourite and a parent task are opt-in via the icon row. Picking a parent hides the list row
 * (the list follows the parent).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("LongMethod", "CyclomaticComplexMethod")
@Composable
private fun CreateTaskOverlay(
    taskLists: List<com.nextcloud.tasks.domain.model.TaskList>,
    tasks: List<Task>,
    initialListId: String,
    onDismiss: () -> Unit,
    onCreate: (NewTaskInput) -> Unit,
    initialParentUid: String? = null,
) {
    val writableLists = taskLists.filter { it.shareAccess != ShareAccess.READ }
    if (writableLists.isEmpty()) return

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var showDescription by remember { mutableStateOf(false) }
    var due by remember { mutableStateOf<java.time.Instant?>(null) }
    var starred by remember { mutableStateOf(false) }
    var parentUid by remember { mutableStateOf(initialParentUid) }
    // A pre-set parent forces the list to follow it (a sub-task lives in its parent's list).
    val parentListId = initialParentUid?.let { uid -> tasks.firstOrNull { it.uid == uid }?.listId }
    var selectedListId by remember {
        val start = parentListId ?: initialListId
        mutableStateOf(writableLists.firstOrNull { it.id == start }?.id ?: writableLists.first().id)
    }
    var listDropdownExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showParentPicker by remember { mutableStateOf(false) }

    val parentTask = parentUid?.let { uid -> tasks.firstOrNull { it.uid == uid } }
    val selectedList = writableLists.firstOrNull { it.id == selectedListId } ?: writableLists.first()
    val titleFocus = remember { FocusRequester() }
    // Focus immediately so the sheet and keyboard animate up together. Deferring the focus by a frame
    // made the sheet rest on the nav bar first and then overshoot, showing a large gap on every open.
    LaunchedEffect(Unit) { titleFocus.requestFocus() }
    BackHandler(enabled = true) { onDismiss() }

    // In-window sheet (NOT a ModalBottomSheet): a scrim plus a bottom-pinned Surface with imePadding.
    // Because it lives in the main window, its vertical position IS the keyboard top, so the sheet and
    // keyboard rise in a single motion — the separate dialog window of ModalBottomSheet could not.
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        androidx.compose.ui.graphics.Color.Black
                            .copy(alpha = 0.32f),
                    ).clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onDismiss() },
        )
        // Sheet-coloured filler under the sheet, exactly as tall as the inset the sheet is lifted
        // by. When a cancelled hide/show IME animation makes the inset lead the keyboard's visual
        // position for a few frames, the gap shows this (sheet briefly looks taller) instead of the
        // black window background.
        Box(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .windowInsetsBottomHeight(WindowInsets.ime.union(WindowInsets.navigationBars))
                    .background(MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)),
        )
        Surface(
            // Content-sized sheet pinned to the bottom, lifted by the IME inset so it rides the
            // keyboard frame-by-frame (requires windowSoftInputMode=adjustResize + edge-to-edge —
            // without adjustResize the system pans the window and the inset jumps instead of
            // animating). Union with the nav bar inset so the sheet clears the gesture pill
            // whenever the keyboard is not (yet) up.
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars)),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(top = 12.dp, bottom = 4.dp)
                            .size(width = 32.dp, height = 4.dp)
                            .background(
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                RoundedCornerShape(2.dp),
                            ),
                )
                parentTask?.let { parent ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier =
                            Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
                    ) {
                        Icon(
                            Icons.Default.SubdirectoryArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = stringResource(R.string.create_subtask_of, parent.title),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).padding(start = 8.dp),
                        )
                        IconButton(onClick = { parentUid = null }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = stringResource(R.string.cancel),
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }

                TextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text(stringResource(R.string.task_title_label)) },
                    singleLine = true,
                    colors = sheetFieldColors(),
                    textStyle = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.fillMaxWidth().focusRequester(titleFocus),
                )

                if (showDescription) {
                    TextField(
                        value = description,
                        onValueChange = { description = it },
                        placeholder = { Text(stringResource(R.string.task_description_label)) },
                        colors = sheetFieldColors(),
                        textStyle = MaterialTheme.typography.bodyMedium,
                        maxLines = 4,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                due?.let { dueInstant ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = DUE_DATE_FORMATTER.format(dueInstant.atZone(java.time.ZoneId.systemDefault())),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f).padding(start = 12.dp),
                        )
                        IconButton(onClick = { due = null }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = stringResource(R.string.cancel),
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }

                if (parentTask == null) {
                    ExposedDropdownMenuBox(
                        expanded = listDropdownExpanded,
                        onExpandedChange = { listDropdownExpanded = it },
                        modifier = Modifier.padding(horizontal = 16.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier =
                                Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp),
                        ) {
                            selectedList.color?.let { colorHex ->
                                Box(
                                    modifier =
                                        Modifier
                                            .size(10.dp)
                                            .background(
                                                androidx.compose.ui.graphics.Color(
                                                    android.graphics.Color.parseColor(colorHex),
                                                ),
                                                CircleShape,
                                            ),
                                )
                            }
                            Text(
                                selectedList.name,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f).padding(start = 12.dp),
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
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
                                                            .size(10.dp)
                                                            .background(
                                                                androidx.compose.ui.graphics.Color(
                                                                    android.graphics.Color.parseColor(colorHex),
                                                                ),
                                                                CircleShape,
                                                            ),
                                                )
                                            }
                                        },
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 8.dp, bottom = 8.dp),
                ) {
                    val active = MaterialTheme.colorScheme.primary
                    val muted = MaterialTheme.colorScheme.onSurfaceVariant
                    IconButton(onClick = { showDescription = !showDescription }) {
                        Icon(
                            Icons.Default.Notes,
                            contentDescription = stringResource(R.string.task_description_label),
                            tint = if (showDescription) active else muted,
                        )
                    }
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(
                            Icons.Default.Schedule,
                            contentDescription = stringResource(R.string.pick_due_date),
                            tint = if (due != null) active else muted,
                        )
                    }
                    IconButton(onClick = { starred = !starred }) {
                        Icon(
                            if (starred) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = stringResource(R.string.favorite_description),
                            tint = if (starred) NextcloudWarning else muted,
                        )
                    }
                    IconButton(onClick = { showParentPicker = true }) {
                        Icon(
                            Icons.Default.SubdirectoryArrowRight,
                            contentDescription = stringResource(R.string.select_parent_task),
                            tint = if (parentTask != null) active else muted,
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(
                        enabled = title.isNotBlank(),
                        onClick = {
                            onCreate(
                                NewTaskInput(
                                    title = title.trim(),
                                    description = if (showDescription) description.ifBlank { null } else null,
                                    listId = selectedListId,
                                    parentUid = parentUid,
                                    due = due,
                                    starred = starred,
                                ),
                            )
                        },
                    ) { Text(stringResource(R.string.save)) }
                }
            }
        }
    }

    if (showDatePicker) {
        val dateState = rememberDatePickerState(initialSelectedDateMillis = due?.toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    showDatePicker = false
                    dateState.selectedDateMillis?.let { millis ->
                        // DatePickerState reports midnight UTC; convert via UTC so the date can't slip a day.
                        val localDate =
                            java.time.Instant
                                .ofEpochMilli(
                                    millis,
                                ).atZone(java.time.ZoneOffset.UTC)
                                .toLocalDate()
                        due = localDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()
                    }
                }) { Text(stringResource(android.R.string.ok)) }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDatePicker = false },
                ) { Text(stringResource(R.string.cancel)) }
            },
        ) { DatePicker(state = dateState) }
    }

    if (showParentPicker) {
        ParentPickerSheet(
            taskLists = writableLists,
            tasks = tasks,
            selectedParentUid = parentUid,
            onDismiss = { showParentPicker = false },
            onSelect = { picked ->
                parentUid = picked?.uid
                picked?.let { selectedListId = it.listId }
                showParentPicker = false
            },
        )
    }
}

/**
 * Bottom sheet to pick a parent task. Reuses [buildOpenTaskRows] to order and indent candidates per
 * list; only open tasks can be parents. "None" clears the parent.
 * ponytail: no search field — parent lists are short; add one if lists grow large.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ParentPickerSheet(
    taskLists: List<com.nextcloud.tasks.domain.model.TaskList>,
    tasks: List<Task>,
    selectedParentUid: String?,
    onDismiss: () -> Unit,
    onSelect: (Task?) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val perList =
        remember(tasks, taskLists) {
            taskLists
                .map { list ->
                    list to
                        buildOpenTaskRows(tasks.filter { it.listId == list.id }, emptyMap(), emptySet())
                            .filter { !it.task.isEffectivelyDone && it.task.uid != null }
                }.filter { it.second.isNotEmpty() }
        }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp).padding(bottom = 16.dp)) {
            item {
                Text(
                    stringResource(R.string.select_parent_task),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                )
            }
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelect(
                                    null,
                                )
                            }.padding(horizontal = 20.dp, vertical = 14.dp),
                ) {
                    Text(
                        stringResource(R.string.parent_none),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    if (selectedParentUid == null) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            perList.forEach { (list, rows) ->
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 4.dp),
                    ) {
                        list.color?.let { colorHex ->
                            Box(
                                modifier =
                                    Modifier
                                        .size(8.dp)
                                        .background(
                                            androidx.compose.ui.graphics.Color(
                                                android.graphics.Color.parseColor(colorHex),
                                            ),
                                            CircleShape,
                                        ),
                            )
                        }
                        Text(
                            list.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
                items(rows, key = { it.task.id }) { row ->
                    val candidate = row.task
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(candidate) }
                                .padding(start = (20 + row.depth * 16).dp, end = 20.dp, top = 12.dp, bottom = 12.dp),
                    ) {
                        Text(
                            candidate.title,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        if (candidate.uid == selectedParentUid) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }
    }
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
            text = stringResource(id = R.string.empty_list_title),
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

    data class Failed(
        val detail: String? = null,
    ) : CreateListError()
}

sealed class EditListError {
    data object Offline : EditListError()

    data class Failed(
        val detail: String? = null,
    ) : EditListError()
}

sealed class DeleteListError {
    data object Offline : DeleteListError()

    data class Failed(
        val detail: String? = null,
    ) : DeleteListError()
}

/**
 * Extracts a short, user-presentable technical detail from a failure so the UI can
 * append it to an otherwise-generic error message (e.g. "HTTP 405"). This turns
 * opaque "please try again" snackbars into something a self-hoster can act on and
 * report back. Returns null when there is nothing useful to add.
 */
internal fun throwableDetail(t: Throwable): String? =
    when (t) {
        is CalDavHttpException -> "HTTP ${t.statusCode}"
        else ->
            t.message
                ?.lineSequence()
                ?.firstOrNull()
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?.take(MAX_ERROR_DETAIL_LENGTH)
    }

internal const val MAX_ERROR_DETAIL_LENGTH = 140

/** Appends a technical detail in parentheses to a generic error message, if present. */
internal fun withErrorDetail(
    base: String,
    detail: String?,
): String = if (detail.isNullOrBlank()) base else "$base ($detail)"

@HiltViewModel
@Suppress("LongParameterList", "LargeClass")
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
        private val appPreferences: com.nextcloud.tasks.data.AppPreferences,
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

        val perListSortEnabled =
            appPreferences.perListSortEnabled
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

        // Active sort, persisted: per selected list when the "remember per list" toggle is on (falling
        // back to the global sort), otherwise the global sort.
        @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
        val taskSort =
            combine(_selectedListId, appPreferences.perListSortEnabled) { listId, perList -> listId to perList }
                .flatMapLatest { (listId, perList) ->
                    if (perList && listId != null) {
                        combine(appPreferences.listSort(listId), appPreferences.globalSort) { forList, global ->
                            forList ?: global
                        }
                    } else {
                        appPreferences.globalSort
                    }
                }.stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(5_000),
                    com.nextcloud.tasks.domain.model.TaskSort.DUE_DATE,
                )

        private val _isRefreshing = MutableStateFlow(false)
        val isRefreshing = _isRefreshing.asStateFlow()

        private val _searchQuery = MutableStateFlow("")
        val searchQuery = _searchQuery.asStateFlow()

        // Frozen tasks during sync to prevent UI flicker
        private val frozenTasksForSync = MutableStateFlow<List<Task>?>(null)

        // UIDs of parent tasks whose sub-tasks are collapsed in the list.
        // ponytail: kept in the ViewModel — survives config changes and in-session navigation, but
        // not process death. Persist to Room/DataStore if that matters (tracked as a follow-up issue).
        private val _collapsedIds = MutableStateFlow<Set<String>>(emptySet())
        val collapsedIds = _collapsedIds.asStateFlow()

        fun toggleCollapsed(taskUid: String) {
            _collapsedIds.update { if (taskUid in it) it - taskUid else it + taskUid }
        }

        /**
         * Persist a manual reorder/reparent. [ordered] is the new display order as (taskId, newParentUid)
         * pairs; each task gets sortOrder = its index and, if changed, the new parentUid. Only tasks that
         * actually changed are written. Used by drag & drop in the MANUAL sort mode.
         */
        fun reorderTasks(ordered: List<Pair<String, String?>>) {
            val byId = allTasks.value.associateBy { it.id }
            viewModelScope.launch {
                ordered.forEachIndexed { index, (id, newParentUid) ->
                    val task = byId[id] ?: return@forEachIndexed
                    val order = index.toLong()
                    if (task.sortOrder != order || task.parentUid != newParentUid) {
                        runCatching {
                            tasksRepository.updateTask(
                                task.copy(sortOrder = order, parentUid = newParentUid),
                            )
                        }
                    }
                }
            }
        }

        // --- Selection mode (long-press) --- ponytail: in-VM, survives config change; process death
        // is an acceptable loss for a transient selection.
        private val _selectionMode = MutableStateFlow(false)
        val selectionMode = _selectionMode.asStateFlow()
        private val _selectedIds = MutableStateFlow<Set<String>>(emptySet())
        val selectedIds = _selectedIds.asStateFlow()

        fun enterSelection(taskId: String) {
            _selectedIds.value = setOf(taskId)
            _selectionMode.value = true
        }

        fun toggleSelection(taskId: String) {
            _selectedIds.update { if (taskId in it) it - taskId else it + taskId }
            if (_selectedIds.value.isEmpty()) _selectionMode.value = false
        }

        fun selectAll(ids: Collection<String>) {
            _selectedIds.value = ids.toSet()
        }

        fun clearSelection() {
            _selectedIds.value = emptySet()
            _selectionMode.value = false
        }

        /** True if any selected task is still nested under a parent (enables "detach"). */
        fun anySelectedIsChild(): Boolean {
            val byId = allTasks.value.associateBy { it.id }
            return _selectedIds.value.any { byId[it]?.parentUid != null }
        }

        /** Bulk-complete every still-open selected task (cascading like the single toggle), then exit. */
        fun completeSelected() {
            val byId = allTasks.value.associateBy { it.id }
            _selectedIds.value
                .mapNotNull { byId[it] }
                .filter { !it.isEffectivelyDone }
                .forEach { applyCompletion(it) }
            clearSelection()
        }

        /** Move the selection (whole subtrees, so nesting stays valid) to [targetListId], then exit. */
        fun moveSelectedToList(targetListId: String) {
            val all = allTasks.value
            val selected = _selectedIds.value.mapNotNull { id -> all.firstOrNull { it.id == id } }
            val toMove = selected.flatMap { collectDescendants(it, all) }.distinctBy { it.id }
            clearSelection()
            viewModelScope.launch {
                toMove.forEach { runCatching { tasksRepository.moveTask(it.id, targetListId) } }
            }
        }

        /** Detach every selected task from its parent (parentUid=null), then exit. */
        fun detachSelected() {
            val byId = allTasks.value.associateBy { it.id }
            val toDetach = _selectedIds.value.mapNotNull { byId[it] }.filter { it.parentUid != null }
            clearSelection()
            viewModelScope.launch {
                toDetach.forEach { runCatching { tasksRepository.updateTask(it.copy(parentUid = null)) } }
            }
        }

        /**
         * Stage a bulk delete of the selection: delete each selected task, freeing any direct child
         * that isn't itself selected. Hidden immediately (undo window); committed via [commitDelete].
         */
        fun stageDeleteSelected(): Deletion {
            val all = allTasks.value
            val byId = all.associateBy { it.id }
            val deleteIds = _selectedIds.value.mapNotNull { byId[it]?.id }.toSet()
            val deletedUids = deleteIds.mapNotNull { byId[it]?.uid }.toSet()
            val freeIds = all.filter { it.parentUid in deletedUids && it.id !in deleteIds }.map { it.id }
            val deletion = Deletion(hiddenIds = deleteIds, deleteIds = deleteIds.toList(), freeIds = freeIds)
            pendingDeleteIds.update { it + deletion.hiddenIds }
            clearSelection()
            return deletion
        }

        // Ids of tasks swipe-deleted but not yet committed (undo window). Hidden from the list.
        private val pendingDeleteIds = MutableStateFlow<Set<String>>(emptySet())

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

        // Technical detail (e.g. "HTTP 405") accompanying the current refresh error, if any.
        private val _refreshErrorDetail = MutableStateFlow<String?>(null)
        val refreshErrorDetail = _refreshErrorDetail.asStateFlow()

        fun clearRefreshError() {
            _refreshError.value = null
            _refreshErrorDetail.value = null
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
                            CreateListError.Failed(throwableDetail(e))
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
                            EditListError.Failed(throwableDetail(e))
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
                            DeleteListError.Failed(throwableDetail(e))
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
                taskSort,
                _searchQuery,
            ) { tasks, listId, filter, sort, query ->
                tasks
                    .filter { task ->
                        // Filter by selected list
                        (listId == null || task.listId == listId)
                    }.filter { task ->
                        // Filter by task status — CANCELLED is treated the same as COMPLETED
                        // (it appears in the web UI under completed tasks with strikethrough).
                        val isEffectivelyDone = task.completed || task.status?.uppercase() == "CANCELLED"
                        when (filter) {
                            com.nextcloud.tasks.domain.model.TaskFilter.ALL -> true
                            com.nextcloud.tasks.domain.model.TaskFilter.CURRENT -> !isEffectivelyDone
                            com.nextcloud.tasks.domain.model.TaskFilter.COMPLETED -> isEffectivelyDone
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
                    }.sortedWith(taskComparator(sort))
            }

        // Public tasks flow that respects freezing during sync and hides pending swipe-deletes
        val tasks =
            combine(filteredTasks, frozenTasksForSync, pendingDeleteIds) { filtered, frozen, pending ->
                // Use frozen tasks during refresh to prevent UI flicker
                (frozen ?: filtered).filter { it.id !in pending }
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
            viewModelScope.launch {
                val listId = _selectedListId.value
                if (appPreferences.perListSortEnabled.first() && listId != null) {
                    appPreferences.setListSort(listId, sort)
                } else {
                    appPreferences.setGlobalSort(sort)
                }
            }
        }

        fun setPerListSortEnabled(enabled: Boolean) {
            viewModelScope.launch { appPreferences.setPerListSortEnabled(enabled) }
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
                _refreshErrorDetail.value = null
                try {
                    tasksRepository.refresh()
                } catch (e: com.nextcloud.tasks.data.caldav.service.CalDavHttpException) {
                    timber.log.Timber.e(e, "Failed to refresh tasks (HTTP ${e.statusCode})")
                    _refreshErrorDetail.value = throwableDetail(e)
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
                    _refreshErrorDetail.value = throwableDetail(e)
                    _refreshError.value = RefreshError.UNKNOWN
                } finally {
                    _isRefreshing.value = false
                    // Unfreeze - show the updated list
                    frozenTasksForSync.value = null
                }
            }
        }

        fun createTask(input: NewTaskInput) {
            viewModelScope.launch {
                try {
                    val draft =
                        com.nextcloud.tasks.domain.model.TaskDraft(
                            listId = input.listId,
                            title = input.title,
                            description = input.description,
                            completed = false,
                            due = input.due,
                            tagIds = emptyList(),
                            parentUid = input.parentUid,
                            priority = if (input.starred) STARRED_PRIORITY else null,
                        )
                    tasksRepository.createTask(draft)
                    timber.log.Timber.d("Task created successfully")
                } catch (ignored: Exception) {
                    timber.log.Timber.e(ignored, "Failed to create task")
                }
            }
        }

        fun toggleTaskComplete(task: Task) {
            applyCompletion(task)
        }

        /**
         * Toggle [task]'s completion, cascading down to descendants (complete) or up to ancestors
         * (reopen) — matching Todoist/Reminders/Google Tasks. Undo is a second tap, so nothing to return.
         */
        fun applyCompletion(task: Task) {
            val all = allTasks.value
            val readOnlyListIds =
                taskLists.value
                    .filter { it.shareAccess == ShareAccess.READ }
                    .map { it.id }
                    .toSet()
            val wasDone = task.isEffectivelyDone
            val affected =
                (if (wasDone) collectAncestors(task, all) else collectDescendants(task, all))
                    .filter { it.listId !in readOnlyListIds && it.isEffectivelyDone == wasDone }
            setCompletion(affected, done = !wasDone)
        }

        private fun setCompletion(
            tasks: List<Task>,
            done: Boolean,
        ) {
            viewModelScope.launch {
                try {
                    tasks.forEach { t ->
                        val updated =
                            if (done) {
                                t.copy(completed = true, completedAt = java.time.Instant.now(), status = "COMPLETED")
                            } else {
                                t.copy(completed = false, completedAt = null, status = "NEEDS-ACTION")
                            }
                        tasksRepository.updateTask(updated)
                    }
                    timber.log.Timber.d("Completion set to $done (${tasks.size} tasks)")
                } catch (ignored: Exception) {
                    timber.log.Timber.e(ignored, "Failed to set completion")
                }
            }
        }

        fun toggleFavorite(task: Task) {
            viewModelScope.launch {
                try {
                    val newPriority = if (task.isStarred) null else STARRED_PRIORITY
                    tasksRepository.updateTask(task.copy(priority = newPriority))
                } catch (ignored: Exception) {
                    timber.log.Timber.e(ignored, "Failed to toggle favorite for task ${task.id}")
                }
            }
        }

        /** [root] plus every descendant, walking parentUid→uid links and breaking cycles. */
        private fun collectDescendants(
            root: Task,
            all: List<Task>,
        ): List<Task> {
            val byParentUid = all.groupBy { it.parentUid }
            val result = mutableListOf<Task>()
            val visited = mutableSetOf<String>()

            fun visit(task: Task) {
                if (!visited.add(task.id)) return
                result.add(task)
                task.uid?.let { byParentUid[it] }?.forEach(::visit)
            }
            visit(root)
            return result
        }

        /** [leaf] plus every ancestor, walking parentUid→uid links and breaking cycles. */
        private fun collectAncestors(
            leaf: Task,
            all: List<Task>,
        ): List<Task> {
            val byUid = all.mapNotNull { t -> t.uid?.let { it to t } }.toMap()
            val result = mutableListOf(leaf)
            val visited = mutableSetOf(leaf.id)
            var parentUid = leaf.parentUid
            while (parentUid != null) {
                val parent = byUid[parentUid] ?: break
                if (!visited.add(parent.id)) break
                result.add(parent)
                parentUid = parent.parentUid
            }
            return result
        }

        /**
         * Stage a swipe-delete of [task]: hide the affected rows now (undo window), commit later.
         * [keepChildren] frees the task's direct children (parentUid=null) and deletes only [task];
         * otherwise the whole subtree is deleted. Nothing is written until [commitDelete].
         */
        fun stageDelete(
            task: Task,
            keepChildren: Boolean,
        ): Deletion {
            val all = allTasks.value
            val deletion =
                if (keepChildren) {
                    val childIds = all.filter { it.parentUid != null && it.parentUid == task.uid }.map { it.id }
                    Deletion(hiddenIds = setOf(task.id), deleteIds = listOf(task.id), freeIds = childIds)
                } else {
                    val subtree = collectDescendants(task, all).map { it.id }
                    Deletion(hiddenIds = subtree.toSet(), deleteIds = subtree, freeIds = emptyList())
                }
            pendingDeleteIds.update { it + deletion.hiddenIds }
            return deletion
        }

        fun undoDelete(deletion: Deletion) {
            pendingDeleteIds.update { it - deletion.hiddenIds }
        }

        fun commitDelete(deletion: Deletion) {
            viewModelScope.launch {
                try {
                    val byId = allTasks.value.associateBy { it.id }
                    deletion.freeIds.forEach { id ->
                        byId[id]?.let { tasksRepository.updateTask(it.copy(parentUid = null)) }
                    }
                    deletion.deleteIds.forEach { tasksRepository.deleteTask(it) }
                    timber.log.Timber.d("Delete committed (${deletion.deleteIds.size} removed)")
                } catch (ignored: Exception) {
                    timber.log.Timber.e(ignored, "Failed to commit delete")
                } finally {
                    pendingDeleteIds.update { it - deletion.hiddenIds }
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
