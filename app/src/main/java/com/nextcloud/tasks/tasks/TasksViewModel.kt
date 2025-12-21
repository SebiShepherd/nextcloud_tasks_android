package com.nextcloud.tasks.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nextcloud.tasks.domain.model.Tag
import com.nextcloud.tasks.domain.model.Task
import com.nextcloud.tasks.domain.model.TaskDraft
import com.nextcloud.tasks.domain.model.TaskList
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class TaskStatusFilter {
    ALL,
    OPEN,
    COMPLETED,
}

enum class PriorityFilter {
    ANY,
    HIGH,
    MEDIUM,
    LOW,
}

enum class TaskSortOption {
    DUE_DATE,
    PRIORITY,
    TITLE,
}

data class TaskListFilters(
    val query: String = "",
    val status: TaskStatusFilter = TaskStatusFilter.ALL,
    val priority: PriorityFilter = PriorityFilter.ANY,
    val tagIds: Set<String> = emptySet(),
    val sortOption: TaskSortOption = TaskSortOption.DUE_DATE,
)

data class TaskListUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val tasks: List<Task> = emptyList(),
    val filteredTasks: List<Task> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val lists: List<TaskList> = emptyList(),
    val filters: TaskListFilters = TaskListFilters(),
)

data class TaskDetailUiState(
    val isLoading: Boolean = false,
    val task: Task? = null,
    val errorMessage: String? = null,
)

data class TaskEditorUiState(
    val taskId: String? = null,
    val isEditing: Boolean = false,
    val title: String = "",
    val description: String = "",
    val completed: Boolean = false,
    val priority: Int = 0,
    val due: Instant? = null,
    val listId: String? = null,
    val tagIds: Set<String> = emptySet(),
    val titleError: String? = null,
    val dueError: String? = null,
    val isSaving: Boolean = false,
)

@HiltViewModel
class TasksViewModel
    @Inject
    constructor(
        private val useCases: TasksUseCases,
    ) : ViewModel() {
        private val filters = MutableStateFlow(TaskListFilters())
        private val _listState = MutableStateFlow(TaskListUiState())
        val listState: StateFlow<TaskListUiState> = _listState.asStateFlow()

        private val _detailState = MutableStateFlow(TaskDetailUiState())
        val detailState: StateFlow<TaskDetailUiState> = _detailState.asStateFlow()

        private val _editorState = MutableStateFlow(TaskEditorUiState())
        val editorState: StateFlow<TaskEditorUiState> = _editorState.asStateFlow()

        init {
            observeTasks()
            syncOnStart()
        }

        fun refresh() {
            viewModelScope.launch {
                _listState.update { it.copy(isRefreshing = true, errorMessage = null) }
                runCatching { useCases.refresh() }
                    .onFailure { throwable ->
                        _listState.update { current ->
                            current.copy(
                                errorMessage = throwable.message ?: "Unable to refresh tasks.",
                            )
                        }
                    }
                _listState.update { it.copy(isRefreshing = false) }
            }
        }

        fun setQuery(query: String) {
            filters.update { it.copy(query = query) }
        }

        fun setStatusFilter(filter: TaskStatusFilter) {
            filters.update { it.copy(status = filter) }
        }

        fun setPriorityFilter(filter: PriorityFilter) {
            filters.update { it.copy(priority = filter) }
        }

        fun setSortOption(sortOption: TaskSortOption) {
            filters.update { it.copy(sortOption = sortOption) }
        }

        fun toggleTagFilter(tagId: String) {
            filters.update { current ->
                val tagIds =
                    if (current.tagIds.contains(tagId)) {
                        current.tagIds - tagId
                    } else {
                        current.tagIds + tagId
                    }
                current.copy(tagIds = tagIds)
            }
        }

        fun loadTaskDetail(taskId: String) {
            viewModelScope.launch {
                _detailState.update { it.copy(isLoading = true, errorMessage = null) }
                runCatching { useCases.getTask(taskId) }
                    .onSuccess { task ->
                        _detailState.update {
                            it.copy(
                                isLoading = false,
                                task = task,
                                errorMessage = if (task == null) "Task not found" else null,
                            )
                        }
                    }.onFailure { throwable ->
                        _detailState.update {
                            it.copy(isLoading = false, errorMessage = throwable.message)
                        }
                    }
            }
        }

        fun prepareEditorForCreate(defaultListId: String?) {
            _editorState.value =
                TaskEditorUiState(
                    listId = defaultListId ?: _listState.value.lists.firstOrNull()?.id,
                )
        }

        fun prepareEditorForEdit(taskId: String) {
            viewModelScope.launch {
                val task = useCases.getTask(taskId)
                if (task != null) {
                    _editorState.value =
                        TaskEditorUiState(
                            taskId = task.id,
                            isEditing = true,
                            title = task.title,
                            description = task.description.orEmpty(),
                            completed = task.completed,
                            priority = task.priority,
                            due = task.due,
                            listId = task.listId,
                            tagIds = task.tags.map(Tag::id).toSet(),
                        )
                    _detailState.update { it.copy(task = task, errorMessage = null) }
                } else {
                    _editorState.update { it.copy(titleError = "Task not found") }
                }
            }
        }

        fun updateEditorTitle(title: String) {
            _editorState.update { it.copy(title = title, titleError = null) }
        }

        fun updateEditorDescription(description: String) {
            _editorState.update { it.copy(description = description) }
        }

        fun updateEditorCompleted(completed: Boolean) {
            _editorState.update { it.copy(completed = completed) }
        }

        fun updateEditorPriority(priority: Int) {
            _editorState.update { it.copy(priority = priority) }
        }

        fun updateEditorDue(due: Instant?) {
            _editorState.update { it.copy(due = due, dueError = null) }
        }

        fun updateEditorList(listId: String) {
            _editorState.update { it.copy(listId = listId) }
        }

        fun toggleEditorTag(tagId: String) {
            _editorState.update { current ->
                val updated =
                    if (current.tagIds.contains(tagId)) {
                        current.tagIds - tagId
                    } else {
                        current.tagIds + tagId
                    }
                current.copy(tagIds = updated)
            }
        }

        fun saveTask(onComplete: () -> Unit = {}) {
            val editorState = _editorState.value
            val titleError =
                if (editorState.title.isBlank()) {
                    "A title is required"
                } else {
                    null
                }
            val dueError =
                if (editorState.due != null && editorState.due.isBefore(Instant.now())) {
                    "Due date cannot be in the past"
                } else {
                    null
                }

            if (titleError != null || dueError != null) {
                _editorState.update { it.copy(titleError = titleError, dueError = dueError) }
                return
            }

            viewModelScope.launch {
                val selectedListId = editorState.listId ?: _listState.value.lists.firstOrNull()?.id
                if (selectedListId.isNullOrBlank()) {
                    _listState.update { it.copy(errorMessage = "Please select a list before saving.") }
                    return@launch
                }
                _editorState.update { it.copy(isSaving = true, titleError = null, dueError = null) }
                runCatching {
                    if (editorState.taskId == null) {
                        val draft =
                            TaskDraft(
                                listId = selectedListId,
                                title = editorState.title.trim(),
                                description = editorState.description.ifBlank { null },
                                completed = editorState.completed,
                                priority = editorState.priority,
                                due = editorState.due,
                                tagIds = editorState.tagIds.toList(),
                            )
                        useCases.create(draft)
                    } else {
                        val currentTask = useCases.getTask(editorState.taskId)
                        val tags =
                            _listState.value.tags.filter { tag ->
                                editorState.tagIds.contains(tag.id)
                            }
                        val updated =
                            currentTask?.copy(
                                title = editorState.title.trim(),
                                description = editorState.description.ifBlank { null },
                                completed = editorState.completed,
                                priority = editorState.priority,
                                due = editorState.due,
                                listId = selectedListId ?: currentTask.listId,
                                tags = tags,
                                updatedAt = Instant.now(),
                            ) ?: return@launch
                        useCases.update(updated)
                        _detailState.update { it.copy(task = updated) }
                    }
                }.onSuccess {
                    onComplete()
                    _editorState.update { TaskEditorUiState(listId = it.listId) }
                }.onFailure { throwable ->
                    _listState.update { state ->
                        state.copy(errorMessage = throwable.message ?: "Unable to save task.")
                    }
                }
                _editorState.update { it.copy(isSaving = false) }
            }
        }

        fun deleteTask(taskId: String, onComplete: () -> Unit) {
            viewModelScope.launch {
                _detailState.update { it.copy(isLoading = true, errorMessage = null) }
                runCatching { useCases.delete(taskId) }
                    .onSuccess {
                        _detailState.update { TaskDetailUiState() }
                        onComplete()
                    }.onFailure { throwable ->
                        _detailState.update {
                            it.copy(isLoading = false, errorMessage = throwable.message)
                        }
                    }
            }
        }

        private fun observeTasks() {
            viewModelScope.launch {
                combine(
                    useCases.loadTasks(),
                    useCases.observeTags(),
                    useCases.observeLists(),
                    filters,
                ) { tasks, tags, lists, filters ->
                    val filteredTasks = applyFilters(tasks, filters)
                    Triple(
                        Triple(tasks, filteredTasks, filters),
                        tags,
                        lists,
                    )
                }.collect { (taskData, tags, lists) ->
                    val (tasks, filteredTasks, currentFilters) = taskData
                    _listState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = null,
                            tasks = tasks,
                            filteredTasks = filteredTasks,
                            filters = currentFilters,
                            tags = tags,
                            lists = lists,
                        )
                    }

                    if (_editorState.value.listId == null && lists.isNotEmpty()) {
                        _editorState.update { editor ->
                            editor.copy(listId = lists.first().id)
                        }
                    }
                }
            }
        }

        private fun syncOnStart() {
            viewModelScope.launch {
                _listState.update { it.copy(isLoading = true) }
                runCatching { useCases.syncOnStart() }
                    .onFailure { throwable ->
                        _listState.update { state ->
                            state.copy(errorMessage = throwable.message ?: "Unable to sync tasks.")
                        }
                    }
                runCatching { useCases.loadTasks.seedSample() }
                _listState.update { it.copy(isLoading = false) }
            }
        }

        private fun applyFilters(
            tasks: List<Task>,
            filters: TaskListFilters,
        ): List<Task> {
            val filteredByStatus =
                tasks.filter { task ->
                    when (filters.status) {
                        TaskStatusFilter.ALL -> true
                        TaskStatusFilter.OPEN -> !task.completed
                        TaskStatusFilter.COMPLETED -> task.completed
                    }
                }

            val filteredByPriority =
                filteredByStatus.filter { task ->
                    filters.priority == PriorityFilter.ANY || priorityBucket(task.priority) == filters.priority
                }

            val filteredByTags =
                filteredByPriority.filter { task ->
                    filters.tagIds.isEmpty() || task.tags.any { tag -> filters.tagIds.contains(tag.id) }
                }

            val filteredByQuery =
                filteredByTags.filter { task ->
                    filters.query.isBlank() ||
                        task.title.contains(filters.query, ignoreCase = true) ||
                        (task.description?.contains(filters.query, ignoreCase = true) ?: false)
                }

            return filteredByQuery.sortedWith(sortComparator(filters.sortOption))
        }

        private fun sortComparator(sortOption: TaskSortOption): Comparator<Task> =
            when (sortOption) {
                TaskSortOption.DUE_DATE ->
                    compareBy<Task> { it.due ?: Instant.MAX }
                        .thenBy { it.title.lowercase() }
                TaskSortOption.PRIORITY ->
                    compareByDescending<Task> { it.priority }
                        .thenBy { it.title.lowercase() }
                TaskSortOption.TITLE -> compareBy { it.title.lowercase() }
            }

        private fun priorityBucket(priority: Int): PriorityFilter =
            when {
                priority >= 7 -> PriorityFilter.HIGH
                priority in 3..6 -> PriorityFilter.MEDIUM
                priority <= 2 -> PriorityFilter.LOW
                else -> PriorityFilter.ANY
            }
    }
