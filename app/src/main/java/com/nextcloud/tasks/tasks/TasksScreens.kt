package com.nextcloud.tasks.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.nextcloud.tasks.R
import com.nextcloud.tasks.domain.model.Tag
import com.nextcloud.tasks.domain.model.TaskList
import com.nextcloud.tasks.domain.model.Task
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val dateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault())

object TaskDestinations {
    const val LIST = "tasks/list"
    const val DETAIL = "tasks/detail/{taskId}"
    const val CREATE = "tasks/create"
    const val EDIT = "tasks/edit/{taskId}"

    fun detail(taskId: String) = "tasks/detail/$taskId"

    fun edit(taskId: String) = "tasks/edit/$taskId"
}

@Composable
fun TasksNavHost(
    navController: NavHostController,
    padding: PaddingValues,
    listState: TaskListUiState,
    detailState: TaskDetailUiState,
    editorState: TaskEditorUiState,
    viewModel: TasksViewModel,
    onTaskSaved: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = TaskDestinations.LIST,
        modifier = modifier.padding(padding),
    ) {
        composable(TaskDestinations.LIST) {
            TaskListScreen(
                state = listState,
                onTaskClick = { taskId ->
                    navController.navigate(TaskDestinations.detail(taskId))
                },
                onCreateTask = {
                    viewModel.prepareEditorForCreate(listState.lists.firstOrNull()?.id)
                    navController.navigate(TaskDestinations.CREATE)
                },
                onQueryChange = viewModel::setQuery,
                onStatusFilterChange = viewModel::setStatusFilter,
                onPriorityFilterChange = viewModel::setPriorityFilter,
                onTagToggle = viewModel::toggleTagFilter,
                onSortChange = viewModel::setSortOption,
                onListSelect = viewModel::setSelectedList,
                onRefresh = viewModel::refresh,
            )
        }
        composable(
            TaskDestinations.DETAIL,
            arguments = listOf(navArgument("taskId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getString("taskId")
            if (taskId != null) {
                LaunchedEffect(taskId) { viewModel.loadTaskDetail(taskId) }
                TaskDetailScreen(
                    state = detailState,
                    onEdit = { navController.navigate(TaskDestinations.edit(taskId)) },
                    onBack = { navController.popBackStack() },
                    onDelete = {
                        viewModel.deleteTask(taskId) {
                            navController.popBackStack()
                        }
                    },
                )
            }
        }
        composable(TaskDestinations.CREATE) {
            TaskEditorScreen(
                state = editorState,
                lists = listState.lists,
                tags = listState.tags,
                onTitleChange = viewModel::updateEditorTitle,
                onDescriptionChange = viewModel::updateEditorDescription,
                onCompletedChange = viewModel::updateEditorCompleted,
                onPriorityChange = viewModel::updateEditorPriority,
                onDueChange = viewModel::updateEditorDue,
                onListChange = viewModel::updateEditorList,
                onTagToggle = viewModel::toggleEditorTag,
                onSave = {
                    viewModel.saveTask {
                        onTaskSaved()
                        navController.popBackStack()
                    }
                },
                onCancel = { navController.popBackStack() },
            )
        }
        composable(
            TaskDestinations.EDIT,
            arguments = listOf(navArgument("taskId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getString("taskId")
            if (taskId != null) {
                LaunchedEffect(taskId) { viewModel.prepareEditorForEdit(taskId) }
                TaskEditorScreen(
                    state = editorState.copy(isEditing = true),
                    lists = listState.lists,
                    tags = listState.tags,
                    onTitleChange = viewModel::updateEditorTitle,
                    onDescriptionChange = viewModel::updateEditorDescription,
                    onCompletedChange = viewModel::updateEditorCompleted,
                    onPriorityChange = viewModel::updateEditorPriority,
                    onDueChange = viewModel::updateEditorDue,
                    onListChange = viewModel::updateEditorList,
                    onTagToggle = viewModel::toggleEditorTag,
                    onSave = {
                        viewModel.saveTask {
                            onTaskSaved()
                            navController.popBackStack()
                        }
                    },
                    onCancel = { navController.popBackStack() },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun TaskListScreen(
    state: TaskListUiState,
    onTaskClick: (String) -> Unit,
    onCreateTask: () -> Unit,
    onQueryChange: (String) -> Unit,
    onStatusFilterChange: (TaskStatusFilter) -> Unit,
    onPriorityFilterChange: (PriorityFilter) -> Unit,
    onTagToggle: (String) -> Unit,
    onSortChange: (TaskSortOption) -> Unit,
    onListSelect: (String?) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listNames = remember(state.lists) { state.lists.associate { it.id to it.name } }
    val pullRefreshState =
        rememberPullRefreshState(
            refreshing = state.isRefreshing,
            onRefresh = onRefresh,
        )

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .pullRefresh(pullRefreshState),
    ) {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                TaskSearchBar(
                    query = state.filters.query,
                    onQueryChange = onQueryChange,
                )
            }
            item {
                ListSelector(
                    lists = state.lists,
                    selectedListId = state.selectedListId,
                    onListSelect = onListSelect,
                )
            }
            item {
                FilterSection(
                    state = state,
                    onStatusFilterChange = onStatusFilterChange,
                    onPriorityFilterChange = onPriorityFilterChange,
                    onTagToggle = onTagToggle,
                )
            }
            item {
                SortRow(
                    sortOption = state.filters.sortOption,
                    onSortChange = onSortChange,
                )
            }

            if (state.errorMessage != null) {
                item {
                    ErrorCard(
                        message = state.errorMessage,
                        onRetry = onRefresh,
                    )
                }
            }

            if (state.isLoading && state.filteredTasks.isEmpty()) {
                item {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth(0.6f))
                    }
                }
            } else if (state.filteredTasks.isEmpty()) {
                item {
                    EmptyTasksState(onCreateTask = onCreateTask)
                }
            } else {
                items(state.filteredTasks) { task ->
                    TaskRow(
                        task = task,
                        listName = listNames[task.listId],
                        onClick = { onTaskClick(task.id) },
                    )
                }
                item { Spacer(modifier = Modifier.height(64.dp)) }
            }
        }

        PullRefreshIndicator(
            refreshing = state.isRefreshing,
            state = pullRefreshState,
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp),
        )

        FloatingActionButton(
            onClick = onCreateTask,
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp),
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Icon(
                painter = painterResource(id = android.R.drawable.ic_input_add),
                contentDescription = stringResource(id = R.string.add_task),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun ListSelector(
    lists: List<TaskList>,
    selectedListId: String?,
    onListSelect: (String?) -> Unit,
) {
    if (lists.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(id = R.string.filter_lists_label),
            style = MaterialTheme.typography.titleMedium,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = selectedListId == null,
                onClick = { onListSelect(null) },
                label = { Text(text = stringResource(id = R.string.filter_list_all)) },
            )
            lists.forEach { list ->
                FilterChip(
                    selected = selectedListId == list.id,
                    onClick = { onListSelect(list.id) },
                    label = { Text(text = list.name) },
                )
            }
        }
    }
}

@Composable
private fun TaskSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        leadingIcon = {
            Icon(
                painter = painterResource(id = android.R.drawable.ic_menu_search),
                contentDescription = null,
            )
        },
        placeholder = { Text(text = stringResource(id = R.string.search_tasks)) },
        singleLine = true,
        keyboardOptions =
            KeyboardOptions(
                imeAction = ImeAction.Search,
                keyboardType = KeyboardType.Text,
            ),
        keyboardActions = KeyboardActions(onSearch = { }),
    )
}

@Composable
private fun FilterSection(
    state: TaskListUiState,
    onStatusFilterChange: (TaskStatusFilter) -> Unit,
    onPriorityFilterChange: (PriorityFilter) -> Unit,
    onTagToggle: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(id = R.string.filters_title),
            style = MaterialTheme.typography.titleMedium,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TaskStatusFilter.entries.forEach { filter ->
                FilterChip(
                    selected = state.filters.status == filter,
                    onClick = { onStatusFilterChange(filter) },
                    label = {
                        Text(
                            text =
                                when (filter) {
                                    TaskStatusFilter.ALL -> stringResource(id = R.string.filter_status_all)
                                    TaskStatusFilter.OPEN -> stringResource(id = R.string.filter_status_open)
                                    TaskStatusFilter.COMPLETED -> stringResource(id = R.string.filter_status_completed)
                                },
                        )
                    },
                )
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PriorityFilter.entries.forEach { filter ->
                FilterChip(
                    selected = state.filters.priority == filter,
                    onClick = { onPriorityFilterChange(filter) },
                    label = {
                        Text(
                            text =
                                when (filter) {
                                    PriorityFilter.ANY -> stringResource(id = R.string.filter_priority_any)
                                    PriorityFilter.HIGH -> stringResource(id = R.string.filter_priority_high)
                                    PriorityFilter.MEDIUM -> stringResource(id = R.string.filter_priority_medium)
                                    PriorityFilter.LOW -> stringResource(id = R.string.filter_priority_low)
                                },
                        )
                    },
                )
            }
        }

        if (state.tags.isNotEmpty()) {
            Text(
                text = stringResource(id = R.string.filter_tags_label),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = 4.dp),
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                state.tags.forEach { tag ->
                    FilterChip(
                        selected = state.filters.tagIds.contains(tag.id),
                        onClick = { onTagToggle(tag.id) },
                        label = { Text(text = tag.name) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SortRow(
    sortOption: TaskSortOption,
    onSortChange: (TaskSortOption) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(id = R.string.sort_label),
            style = MaterialTheme.typography.titleSmall,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SortButton(
                selected = sortOption == TaskSortOption.DUE_DATE,
                label = stringResource(id = R.string.sort_due_date),
                onClick = { onSortChange(TaskSortOption.DUE_DATE) },
            )
            SortButton(
                selected = sortOption == TaskSortOption.PRIORITY,
                label = stringResource(id = R.string.sort_priority),
                onClick = { onSortChange(TaskSortOption.PRIORITY) },
            )
            SortButton(
                selected = sortOption == TaskSortOption.TITLE,
                label = stringResource(id = R.string.sort_title),
                onClick = { onSortChange(TaskSortOption.TITLE) },
            )
        }
    }
}

@Composable
private fun SortButton(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
) {
    TextButton(onClick = onClick) {
        Icon(
            painter = painterResource(id = android.R.drawable.ic_menu_sort_by_size),
            contentDescription = null,
            tint =
                if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
        )
        Text(
            text = label,
            color =
                if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

@Composable
private fun TaskRow(
    task: Task,
    listName: String?,
    onClick: () -> Unit,
) {
    Card(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onClick() },
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                StatusPill(completed = task.completed)
            }
            if (!task.description.isNullOrBlank()) {
                Text(
                    text = task.description.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PriorityBadge(priority = task.priority)
                task.due?.let { due ->
                    Text(
                        text = stringResource(id = R.string.due_date_label, dateFormatter.format(due)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                listName?.takeIf { it.isNotBlank() }?.let { name ->
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (task.tags.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    task.tags.forEach { tag ->
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Text(
                                text = tag.name,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusPill(completed: Boolean) {
    Surface(
        color =
            if (completed) {
                MaterialTheme.colorScheme.tertiaryContainer
            } else {
                MaterialTheme.colorScheme.primaryContainer
            },
        shape = RoundedCornerShape(12.dp),
    ) {
        Text(
            text =
                if (completed) {
                    stringResource(id = R.string.status_completed)
                } else {
                    stringResource(id = R.string.status_open)
                },
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color =
                if (completed) {
                    MaterialTheme.colorScheme.onTertiaryContainer
                } else {
                    MaterialTheme.colorScheme.onPrimaryContainer
                },
        )
    }
}

@Composable
private fun PriorityBadge(priority: Int) {
    val (label, color) =
        when {
            priority >= 7 -> stringResource(id = R.string.priority_high) to MaterialTheme.colorScheme.errorContainer
            priority in 3..6 -> stringResource(id = R.string.priority_medium) to MaterialTheme.colorScheme.secondaryContainer
            else -> stringResource(id = R.string.priority_low) to MaterialTheme.colorScheme.surfaceVariant
        }
    Surface(
        color = color,
        shape = RoundedCornerShape(12.dp),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun ErrorCard(
    message: String,
    onRetry: () -> Unit,
) {
    ElevatedCard(
        colors =
            CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
            ),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onRetry) {
                Text(text = stringResource(id = R.string.retry))
            }
        }
    }
}

@Composable
private fun EmptyTasksState(onCreateTask: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(id = R.string.empty_task_hint),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        TextButton(onClick = onCreateTask) {
            Text(text = stringResource(id = R.string.add_task))
        }
    }
}

@Composable
fun TaskDetailScreen(
    state: TaskDetailUiState,
    onEdit: () -> Unit,
    onBack: () -> Unit,
    onDelete: () -> Unit,
) {
    var confirmDelete by remember { mutableStateOf(false) }
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TextButton(onClick = onBack) {
            Icon(
                painter = painterResource(id = android.R.drawable.ic_media_previous),
                contentDescription = null,
            )
            Text(text = stringResource(id = R.string.back))
        }

        if (state.isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        state.errorMessage?.let { error ->
            ErrorCard(
                message = error,
                onRetry = onBack,
            )
        }

        state.task?.let { task ->
            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(text = task.title, style = MaterialTheme.typography.headlineSmall)
                    Text(
                        text =
                            stringResource(
                                id = R.string.detail_status_and_priority,
                                if (task.completed) {
                                    stringResource(id = R.string.status_completed)
                                } else {
                                    stringResource(id = R.string.status_open)
                                },
                                task.priority,
                            ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    task.due?.let { due ->
                        Text(
                            text = stringResource(id = R.string.due_date_label, dateFormatter.format(due)),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    if (task.tags.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            task.tags.forEach { tag ->
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(16.dp),
                                ) {
                                    Text(
                                        text = tag.name,
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    )
                                }
                            }
                        }
                    }
                    if (!task.description.isNullOrBlank()) {
                        SelectionContainer {
                            Text(
                                text = task.description.orEmpty(),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(text = stringResource(id = R.string.edit_task))
                }
                Button(
                    onClick = { confirmDelete = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) {
                    Text(text = stringResource(id = R.string.delete_task))
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        onDelete()
                    },
                ) {
                    Text(text = stringResource(id = R.string.delete_task))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(text = stringResource(id = R.string.cancel))
                }
            },
            title = { Text(text = stringResource(id = R.string.delete_task)) },
            text = { Text(text = stringResource(id = R.string.confirm_delete_message)) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditorScreen(
    state: TaskEditorUiState,
    lists: List<TaskList>,
    tags: List<Tag>,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onCompletedChange: (Boolean) -> Unit,
    onPriorityChange: (Int) -> Unit,
    onDueChange: (Instant?) -> Unit,
    onListChange: (String) -> Unit,
    onTagToggle: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    var isDuePickerOpen by remember { mutableStateOf(false) }
    val datePickerState =
        rememberDatePickerState(initialSelectedDateMillis = state.due?.toEpochMilli())
    LaunchedEffect(state.due) {
        datePickerState.selectedDateMillis = state.due?.toEpochMilli()
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text =
                if (state.isEditing) {
                    stringResource(id = R.string.edit_task)
                } else {
                    stringResource(id = R.string.create_task)
                },
            style = MaterialTheme.typography.headlineSmall,
        )

        OutlinedTextField(
            value = state.title,
            onValueChange = onTitleChange,
            label = { Text(text = stringResource(id = R.string.task_title_label)) },
            isError = state.titleError != null,
            supportingText =
                state.titleError?.let {
                    { Text(text = it, color = MaterialTheme.colorScheme.error) }
                },
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = state.description,
            onValueChange = onDescriptionChange,
            label = { Text(text = stringResource(id = R.string.task_description_label)) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = state.completed,
                    onCheckedChange = onCompletedChange,
                )
                Text(text = stringResource(id = R.string.mark_completed))
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(text = stringResource(id = R.string.priority_label, state.priority))
                Slider(
                    value = state.priority.toFloat(),
                    onValueChange = { onPriorityChange(it.toInt()) },
                    valueRange = 0f..10f,
                    steps = 9,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text =
                        state.due?.let { stringResource(id = R.string.due_date_label, dateFormatter.format(it)) }
                            ?: stringResource(id = R.string.no_due_date),
                    style = MaterialTheme.typography.bodyMedium,
                )
                state.dueError?.let { error ->
                    Text(text = error, color = MaterialTheme.colorScheme.error)
                }
            }
            Row {
                TextButton(onClick = { isDuePickerOpen = true }) {
                    Text(text = stringResource(id = R.string.pick_due_date))
                }
                if (state.due != null) {
                    TextButton(onClick = { onDueChange(null) }) {
                        Text(text = stringResource(id = R.string.clear_due_date))
                    }
                }
            }
        }

        Divider()

        if (lists.isNotEmpty()) {
            var expanded by remember { mutableStateOf(false) }
            val selectedListName = lists.firstOrNull { it.id == state.listId }?.name.orEmpty()
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
            ) {
                OutlinedTextField(
                    value = selectedListName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(text = stringResource(id = R.string.task_list_label)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier =
                        Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                )
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    lists.forEach { list ->
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text(text = list.name) },
                            onClick = {
                                expanded = false
                                onListChange(list.id)
                            },
                        )
                    }
                }
            }
        }

        if (tags.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = stringResource(id = R.string.attach_tags_label))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    tags.forEach { tag ->
                        FilterChip(
                            selected = state.tagIds.contains(tag.id),
                            onClick = { onTagToggle(tag.id) },
                            label = { Text(text = tag.name) },
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
            ) {
                Text(text = stringResource(id = R.string.cancel))
            }
            Button(
                onClick = onSave,
                modifier = Modifier.weight(1f),
                enabled = !state.isSaving,
            ) {
                Text(text = stringResource(id = R.string.save_task))
            }
        }
    }

    if (isDuePickerOpen) {
        DatePickerDialog(
            onDismissRequest = { isDuePickerOpen = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val millis = datePickerState.selectedDateMillis
                        if (millis != null) {
                            onDueChange(Instant.ofEpochMilli(millis))
                        }
                        isDuePickerOpen = false
                    },
                ) {
                    Text(text = stringResource(id = R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { isDuePickerOpen = false }) {
                    Text(text = stringResource(id = R.string.cancel))
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
