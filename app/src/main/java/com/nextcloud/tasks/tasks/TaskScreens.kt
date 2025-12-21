package com.nextcloud.tasks.tasks

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nextcloud.tasks.R
import com.nextcloud.tasks.domain.model.AuthType
import com.nextcloud.tasks.domain.model.NextcloudAccount
import com.nextcloud.tasks.domain.model.Tag
import com.nextcloud.tasks.domain.model.Task
import com.nextcloud.tasks.domain.model.TaskList
import com.nextcloud.tasks.domain.model.TaskPriority
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun TaskHomeContent(
    padding: PaddingValues,
    account: NextcloudAccount?,
    state: TaskListUiState,
    onSearchQueryChange: (String) -> Unit,
    onStatusFilterChange: (TaskStatusFilter) -> Unit,
    onSortOptionChange: (TaskSortOption) -> Unit,
    onListFilterChange: (String?) -> Unit,
    onTagFilterChange: (String?) -> Unit,
    onRefresh: () -> Unit,
    onTaskSelected: (Task) -> Unit,
    onCreateTask: () -> Unit,
) {
    val pullToRefreshState = rememberPullToRefreshState()

    LaunchedEffect(pullToRefreshState.isRefreshing) {
        if (pullToRefreshState.isRefreshing) {
            onRefresh()
        }
    }

    LaunchedEffect(state.isRefreshing) {
        if (!state.isRefreshing && pullToRefreshState.isRefreshing) {
            pullToRefreshState.endRefresh()
        }
    }

    Box(
        modifier =
            Modifier
                .padding(padding)
                .fillMaxSize()
                .pullToRefreshContainer(pullToRefreshState),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            account?.let {
                item { AccountSummaryCard(account = it) }
            }

            stickyHeader {
                Surface(color = MaterialTheme.colorScheme.background) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        TaskSearchBar(
                            query = state.searchQuery,
                            onQueryChange = onSearchQueryChange,
                            onCreateTask = onCreateTask,
                        )
                        TaskFilters(
                            state = state,
                            onStatusFilterChange = onStatusFilterChange,
                            onSortOptionChange = onSortOptionChange,
                            onListFilterChange = onListFilterChange,
                            onTagFilterChange = onTagFilterChange,
                        )
                    }
                }
            }

            when {
                state.isLoading -> {
                    item {
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            CircularProgressIndicator()
                            Text(text = stringResource(id = R.string.task_loading))
                        }
                    }
                }

                state.visibleTasks.isEmpty() -> {
                    item {
                        val emptyMessage =
                            if (
                                state.searchQuery.isBlank() &&
                                state.filter.listId == null &&
                                state.filter.tagId == null &&
                                state.filter.status == TaskStatusFilter.ALL
                            ) {
                                stringResource(id = R.string.task_list_empty_description)
                            } else {
                                stringResource(id = R.string.task_list_empty_filtered)
                            }

                        EmptyState(
                            title = stringResource(id = R.string.task_list_empty_title),
                            message = emptyMessage,
                        )
                    }
                }

                else -> {
                    item {
                        Text(
                            text = stringResource(id = R.string.task_list_title),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                    items(state.visibleTasks, key = Task::id) { task ->
                        TaskCard(
                            task = task,
                            onClick = { onTaskSelected(task) },
                        )
                    }
                }
            }
        }

        PullToRefreshContainer(
            state = pullToRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        IconButton(
            onClick = onRefresh,
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
                    .background(MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(16.dp)),
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = stringResource(id = R.string.task_refresh_action),
                tint = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}

@Composable
private fun TaskSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onCreateTask: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.weight(1f),
            label = { Text(text = stringResource(id = R.string.task_search_label)) },
            placeholder = { Text(text = stringResource(id = R.string.task_search_placeholder)) },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
            trailingIcon =
                if (query.isNotEmpty()) {
                    {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = null)
                        }
                    }
                } else {
                    null
                },
            singleLine = true,
        )

        IconButton(
            onClick = onCreateTask,
            modifier =
                Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(12.dp)),
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(id = R.string.task_create),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TaskFilters(
    state: TaskListUiState,
    onStatusFilterChange: (TaskStatusFilter) -> Unit,
    onSortOptionChange: (TaskSortOption) -> Unit,
    onListFilterChange: (String?) -> Unit,
    onTagFilterChange: (String?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = state.filter.status == TaskStatusFilter.ALL,
                onClick = { onStatusFilterChange(TaskStatusFilter.ALL) },
                label = { Text(text = stringResource(id = R.string.filter_status_all)) },
            )
            FilterChip(
                selected = state.filter.status == TaskStatusFilter.OPEN,
                onClick = { onStatusFilterChange(TaskStatusFilter.OPEN) },
                label = { Text(text = stringResource(id = R.string.filter_status_open)) },
                leadingIcon = { Icon(imageVector = Icons.Default.Flag, contentDescription = null) },
            )
            FilterChip(
                selected = state.filter.status == TaskStatusFilter.COMPLETED,
                onClick = { onStatusFilterChange(TaskStatusFilter.COMPLETED) },
                label = { Text(text = stringResource(id = R.string.filter_status_completed)) },
                leadingIcon = { Icon(imageVector = Icons.Default.Done, contentDescription = null) },
            )
        }

        SortSelector(
            selected = state.sortOption,
            onSelectionChange = onSortOptionChange,
        )

        if (state.lists.isNotEmpty()) {
            Text(
                text = stringResource(id = R.string.task_filter_lists),
                style = MaterialTheme.typography.labelLarge,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = state.filter.listId == null,
                    onClick = { onListFilterChange(null) },
                    label = { Text(text = stringResource(id = R.string.filter_status_all)) },
                    leadingIcon = { Icon(imageVector = Icons.Default.FilterList, contentDescription = null) },
                )
                state.lists.forEach { list ->
                    FilterChip(
                        selected = state.filter.listId == list.id,
                        onClick = { onListFilterChange(list.id) },
                        label = { Text(text = list.name) },
                    )
                }
            }
        }

        if (state.tags.isNotEmpty()) {
            Text(
                text = stringResource(id = R.string.task_filter_tags),
                style = MaterialTheme.typography.labelLarge,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = state.filter.tagId == null,
                    onClick = { onTagFilterChange(null) },
                    label = { Text(text = stringResource(id = R.string.filter_status_all)) },
                )
                state.tags.forEach { tag ->
                    FilterChip(
                        selected = state.filter.tagId == tag.id,
                        onClick = { onTagFilterChange(tag.id) },
                        label = { Text(text = tag.name) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SortSelector(
    selected: TaskSortOption,
    onSelectionChange: (TaskSortOption) -> Unit,
) {
    SingleChoiceSegmentedButtonRow {
        TaskSortOption.entries.forEachIndexed { index, option ->
            SegmentedButton(
                selected = selected == option,
                onClick = { onSelectionChange(option) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = TaskSortOption.entries.size),
            ) {
                Text(
                    text =
                        when (option) {
                            TaskSortOption.DUE -> stringResource(id = R.string.sort_by_due)
                            TaskSortOption.PRIORITY -> stringResource(id = R.string.sort_by_priority)
                            TaskSortOption.TITLE -> stringResource(id = R.string.sort_by_title)
                        },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskCard(
    task: Task,
    onClick: () -> Unit,
) {
    val formatter = DateTimeFormatter.ISO_LOCAL_DATE
    Surface(
        tonalElevation = 1.dp,
        shape = MaterialTheme.shapes.medium,
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                PriorityChip(priority = task.priority)
            }
            if (!task.description.isNullOrBlank()) {
                Text(
                    text = task.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StatusBadge(task.completed)
                task.due?.let { due ->
                    Text(
                        text = due.atZone(java.time.ZoneId.systemDefault()).toLocalDate().format(formatter),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            if (task.tags.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    task.tags.forEach { tag ->
                        AssistChip(
                            onClick = {},
                            label = { Text(text = tag.name) },
                            colors =
                                AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(isCompleted: Boolean) {
    val label =
        if (isCompleted) {
            stringResource(id = R.string.task_detail_completed)
        } else {
            stringResource(id = R.string.task_detail_open)
        }

    val colors =
        if (isCompleted) {
            AssistChipDefaults.assistChipColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        } else {
            AssistChipDefaults.assistChipColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }

    AssistChip(
        onClick = {},
        label = { Text(text = label) },
        leadingIcon = {
            Icon(
                imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.Flag,
                contentDescription = null,
            )
        },
        colors = colors,
    )
}

@Composable
private fun PriorityChip(priority: TaskPriority) {
    val (container, content) =
        when (priority) {
            TaskPriority.LOW -> Pair(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
            TaskPriority.MEDIUM -> Pair(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer)
            TaskPriority.HIGH -> Pair(MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.onTertiaryContainer)
            TaskPriority.URGENT -> Pair(MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer)
        }

    AssistChip(
        onClick = {},
        label = {
            Text(
                text =
                    when (priority) {
                        TaskPriority.LOW -> stringResource(id = R.string.priority_low)
                        TaskPriority.MEDIUM -> stringResource(id = R.string.priority_medium)
                        TaskPriority.HIGH -> stringResource(id = R.string.priority_high)
                        TaskPriority.URGENT -> stringResource(id = R.string.priority_urgent)
                    },
            )
        },
        leadingIcon = { Icon(imageVector = Icons.Default.Flag, contentDescription = null) },
        colors =
            AssistChipDefaults.assistChipColors(
                containerColor = container,
                labelColor = content,
            ),
    )
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailSheet(
    task: Task,
    onEdit: (Task) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val formatter = DateTimeFormatter.ISO_LOCAL_DATE
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = stringResource(id = R.string.task_detail_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                )
                StatusBadge(isCompleted = task.completed)
            }
            Text(
                text = task.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            if (!task.description.isNullOrBlank()) {
                Text(
                    text = task.description,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            PriorityChip(priority = task.priority)
            task.due?.let { due ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.CalendarToday, contentDescription = null)
                    Text(
                        text = due.atZone(java.time.ZoneId.systemDefault()).toLocalDate().format(formatter),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
            if (task.tags.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    task.tags.forEach { tag ->
                        AssistChip(
                            onClick = {},
                            label = { Text(text = tag.name) },
                            leadingIcon = { Icon(painter = painterResource(android.R.drawable.ic_menu_agenda), contentDescription = null) },
                        )
                    }
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Button(
                    onClick = { onEdit(task) },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = null)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(text = stringResource(id = R.string.task_edit))
                }
                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(text = stringResource(id = R.string.task_delete))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TaskEditorSheet(
    state: TaskEditorState,
    tags: List<Tag>,
    lists: List<TaskList>,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onListChange: (String) -> Unit,
    onPriorityChange: (TaskPriority) -> Unit,
    onStatusToggle: () -> Unit,
    onTagToggle: (String) -> Unit,
    onDueChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text =
                    when (state.mode) {
                        TaskEditorMode.Create -> stringResource(id = R.string.task_create)
                        TaskEditorMode.Edit -> stringResource(id = R.string.task_edit)
                        TaskEditorMode.Hidden -> ""
                    },
                style = MaterialTheme.typography.titleLarge,
            )

            OutlinedTextField(
                value = state.title,
                onValueChange = onTitleChange,
                label = { Text(text = stringResource(id = R.string.task_form_title_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSaving,
            )

            OutlinedTextField(
                value = state.description,
                onValueChange = onDescriptionChange,
                label = { Text(text = stringResource(id = R.string.task_form_description_label)) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSaving,
            )

            if (lists.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(id = R.string.task_form_list_label),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.align(Alignment.CenterVertically),
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        lists.forEach { list ->
                            FilterChip(
                                selected = state.listId == list.id,
                                onClick = { onListChange(list.id) },
                                label = { Text(text = list.name) },
                                enabled = !state.isSaving,
                            )
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(id = R.string.task_form_priority_label),
                    style = MaterialTheme.typography.labelLarge,
                )
                SingleChoiceSegmentedButtonRow {
                    TaskPriority.entries.forEachIndexed { index, priority ->
                        SegmentedButton(
                            selected = state.priority == priority,
                            onClick = { onPriorityChange(priority) },
                            shape = SegmentedButtonDefaults.itemShape(index, TaskPriority.entries.size),
                            enabled = !state.isSaving,
                            icon = { Icon(imageVector = Icons.Default.Flag, contentDescription = null) },
                        ) {
                            Text(
                                text =
                                    when (priority) {
                                        TaskPriority.LOW -> stringResource(id = R.string.priority_low)
                                        TaskPriority.MEDIUM -> stringResource(id = R.string.priority_medium)
                                        TaskPriority.HIGH -> stringResource(id = R.string.priority_high)
                                        TaskPriority.URGENT -> stringResource(id = R.string.priority_urgent)
                                    },
                            )
                        }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(text = stringResource(id = R.string.task_form_status_label))
                AssistChip(
                    onClick = onStatusToggle,
                    label = {
                        Text(
                            text =
                                if (state.completed) {
                                    stringResource(id = R.string.task_detail_completed)
                                } else {
                                    stringResource(id = R.string.task_detail_open)
                                },
                        )
                    },
                    leadingIcon = { Icon(imageVector = Icons.Default.Done, contentDescription = null) },
                    colors =
                        AssistChipDefaults.assistChipColors(
                            containerColor = if (state.completed) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer,
                            labelColor = if (state.completed) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                )
            }

            OutlinedTextField(
                value = state.dueInput,
                onValueChange = onDueChange,
                label = { Text(text = stringResource(id = R.string.task_form_due_label)) },
                singleLine = true,
                enabled = !state.isSaving,
            )

            if (tags.isNotEmpty()) {
                Text(
                    text = stringResource(id = R.string.task_form_tags_label),
                    style = MaterialTheme.typography.labelLarge,
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    tags.forEach { tag ->
                        FilterChip(
                            selected = state.tagIds.contains(tag.id),
                            onClick = { onTagToggle(tag.id) },
                            label = { Text(text = tag.name) },
                            enabled = !state.isSaving,
                        )
                    }
                }
            }

            state.validationError?.let { error ->
                val message =
                    when (error) {
                        TaskValidationError.TITLE_REQUIRED -> stringResource(id = R.string.task_form_error_title_required)
                        TaskValidationError.DUE_INVALID -> stringResource(id = R.string.task_form_error_due_invalid)
                        TaskValidationError.DUE_IN_PAST -> stringResource(id = R.string.task_form_error_due_past)
                    }
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSaving,
            ) {
                Text(text = stringResource(id = R.string.task_save))
            }
        }
    }
}

@Composable
fun AccountSummaryCard(account: NextcloudAccount) {
    val loginMethod =
        if (account.authType == AuthType.PASSWORD) {
            stringResource(id = R.string.login_method_password)
        } else {
            stringResource(id = R.string.login_method_oauth)
        }

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
                text = loginMethod,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun EmptyState(
    title: String,
    message: String,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun Modifier.pullToRefreshContainer(state: PullToRefreshState): Modifier =
    this
        .nestedScroll(state.nestedScrollConnection)
        .background(Color.Transparent)
