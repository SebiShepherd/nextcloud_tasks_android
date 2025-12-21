package com.nextcloud.tasks.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nextcloud.tasks.domain.model.Tag
import com.nextcloud.tasks.domain.model.Task
import com.nextcloud.tasks.domain.model.TaskDraft
import com.nextcloud.tasks.domain.model.TaskList
import com.nextcloud.tasks.domain.model.TaskPriority
import com.nextcloud.tasks.domain.usecase.CreateTaskUseCase
import com.nextcloud.tasks.domain.usecase.DeleteTaskUseCase
import com.nextcloud.tasks.domain.usecase.LoadTasksUseCase
import com.nextcloud.tasks.domain.usecase.ObserveTagsUseCase
import com.nextcloud.tasks.domain.usecase.ObserveTaskListsUseCase
import com.nextcloud.tasks.domain.usecase.RefreshTasksUseCase
import com.nextcloud.tasks.domain.usecase.UpdateTaskUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class TaskFilterState(
    val status: TaskStatusFilter = TaskStatusFilter.ALL,
    val listId: String? = null,
    val tagId: String? = null,
)

enum class TaskStatusFilter {
    ALL,
    OPEN,
    COMPLETED,
}

enum class TaskSortOption {
    DUE,
    PRIORITY,
    TITLE,
}

enum class TaskEditorMode {
    Hidden,
    Create,
    Edit,
}

enum class TaskValidationError {
    TITLE_REQUIRED,
    DUE_INVALID,
    DUE_IN_PAST,
}

data class TaskEditorState(
    val mode: TaskEditorMode = TaskEditorMode.Hidden,
    val title: String = "",
    val description: String = "",
    val listId: String? = null,
    val dueInput: String = "",
    val dueDate: LocalDate? = null,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val completed: Boolean = false,
    val tagIds: Set<String> = emptySet(),
    val isSaving: Boolean = false,
    val validationError: TaskValidationError? = null,
)

data class TaskListUiState(
    val allTasks: List<Task> = emptyList(),
    val visibleTasks: List<Task> = emptyList(),
    val lists: List<TaskList> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val searchQuery: String = "",
    val filter: TaskFilterState = TaskFilterState(),
    val sortOption: TaskSortOption = TaskSortOption.DUE,
    val selectedTask: Task? = null,
    val editor: TaskEditorState = TaskEditorState(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
)

@HiltViewModel
class TaskListViewModel
    @Inject
    constructor(
        private val refreshTasksUseCase: RefreshTasksUseCase,
        private val createTaskUseCase: CreateTaskUseCase,
        private val updateTaskUseCase: UpdateTaskUseCase,
        private val deleteTaskUseCase: DeleteTaskUseCase,
        loadTasksUseCase: LoadTasksUseCase,
        observeTaskListsUseCase: ObserveTaskListsUseCase,
        observeTagsUseCase: ObserveTagsUseCase,
    ) : ViewModel() {
        private val mutableState = MutableStateFlow(TaskListUiState())

        private val tasksFlow =
            loadTasksUseCase()
                .onEach { tasks ->
                    mutableState.update { state ->
                        state.copy(
                            isLoading = false,
                            errorMessage = null,
                            allTasks = tasks,
                            visibleTasks = applyFilters(tasks, state),
                        )
                    }
                }

        private val listsFlow = observeTaskListsUseCase()
        private val tagsFlow = observeTagsUseCase()

        val uiState: StateFlow<TaskListUiState> =
            combine(
                mutableState,
                tasksFlow,
                listsFlow,
                tagsFlow,
            ) { state, tasks, lists, tags ->
                val selectedTask = state.selectedTask?.let { previous ->
                    tasks.find { it.id == previous.id }
                }

                state.copy(
                    allTasks = tasks,
                    visibleTasks = applyFilters(tasks, state),
                    lists = lists,
                    tags = tags,
                    selectedTask = selectedTask,
                    editor = state.editor.ensureListSelection(lists),
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = TaskListUiState(isLoading = true),
            )

        init {
            viewModelScope.launch { loadTasksUseCase.seedSample() }
            refresh()
        }

        fun refresh() {
            viewModelScope.launch {
                mutableState.update { it.copy(isRefreshing = true, isLoading = it.allTasks.isEmpty()) }
                runCatching { refreshTasksUseCase() }
                    .onFailure { throwable ->
                        mutableState.update { state ->
                            state.copy(
                                errorMessage = throwable.message ?: "Could not refresh tasks.",
                            )
                        }
                    }
                mutableState.update { it.copy(isRefreshing = false, isLoading = false) }
            }
        }

        fun consumeError() {
            mutableState.update { it.copy(errorMessage = null) }
        }

        fun updateSearchQuery(query: String) {
            mutableState.update { state ->
                state.copy(
                    searchQuery = query,
                    visibleTasks = applyFilters(state.allTasks, state.copy(searchQuery = query)),
                )
            }
        }

        fun updateStatusFilter(filter: TaskStatusFilter) {
            mutableState.update { state ->
                val nextState = state.copy(filter = state.filter.copy(status = filter))
                nextState.copy(visibleTasks = applyFilters(state.allTasks, nextState))
            }
        }

        fun updateSortOption(option: TaskSortOption) {
            mutableState.update { state ->
                val nextState = state.copy(sortOption = option)
                nextState.copy(visibleTasks = applyFilters(state.allTasks, nextState))
            }
        }

        fun updateListFilter(listId: String?) {
            mutableState.update { state ->
                val nextState = state.copy(filter = state.filter.copy(listId = listId))
                nextState.copy(visibleTasks = applyFilters(state.allTasks, nextState))
            }
        }

        fun updateTagFilter(tagId: String?) {
            mutableState.update { state ->
                val nextState = state.copy(filter = state.filter.copy(tagId = tagId))
                nextState.copy(visibleTasks = applyFilters(state.allTasks, nextState))
            }
        }

        fun openDetails(task: Task) {
            mutableState.update { state ->
                state.copy(selectedTask = task, editor = TaskEditorState())
            }
        }

        fun closeSheet() {
            mutableState.update { it.copy(selectedTask = null, editor = TaskEditorState()) }
        }

        fun startCreate() {
            mutableState.update { state ->
                state.copy(
                    selectedTask = null,
                    editor =
                        TaskEditorState(
                            mode = TaskEditorMode.Create,
                            listId = state.lists.firstOrNull()?.id,
                        ),
                )
            }
        }

        fun startEdit(task: Task) {
            mutableState.update {
                it.copy(
                    selectedTask = task,
                    editor = task.toEditorState(),
                )
            }
        }

        fun updateEditorTitle(title: String) = updateEditor { it.copy(title = title, validationError = null) }

        fun updateEditorDescription(description: String) = updateEditor { it.copy(description = description) }

        fun updateEditorList(listId: String) = updateEditor { it.copy(listId = listId) }

        fun updateEditorPriority(priority: TaskPriority) = updateEditor { it.copy(priority = priority) }

        fun toggleEditorCompleted() = updateEditor { it.copy(completed = !it.completed) }

        fun toggleEditorTag(tagId: String) =
            updateEditor { editor ->
                val nextTags =
                    if (editor.tagIds.contains(tagId)) {
                        editor.tagIds - tagId
                    } else {
                        editor.tagIds + tagId
                    }
                editor.copy(tagIds = nextTags)
            }

        fun updateEditorDueInput(raw: String) {
            val sanitized = raw.trim()
            val parsedDate = sanitized.toLocalDateOrNull()

            updateEditor {
                it.copy(
                    dueInput = sanitized,
                    dueDate = parsedDate,
                    validationError = null,
                )
            }
        }

        fun submitEditor(onSuccess: (() -> Unit)? = null) {
            val currentState = mutableState.value
            val editor = currentState.editor

            if (editor.title.isBlank()) {
                mutableState.update { it.copy(editor = editor.copy(validationError = TaskValidationError.TITLE_REQUIRED)) }
                return
            }

            if (editor.dueInput.isNotEmpty() && editor.dueDate == null) {
                mutableState.update {
                    it.copy(
                        editor = editor.copy(validationError = TaskValidationError.DUE_INVALID),
                    )
                }
                return
            }

            if (editor.dueDate != null && editor.dueDate.isBefore(LocalDate.now())) {
                mutableState.update {
                    it.copy(editor = editor.copy(validationError = TaskValidationError.DUE_IN_PAST))
                }
                return
            }

            val dueInstant = editor.dueDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()
            val listId = editor.listId ?: currentState.lists.firstOrNull()?.id ?: return

            when (editor.mode) {
                TaskEditorMode.Create -> createTask(listId, editor, dueInstant, onSuccess)
                TaskEditorMode.Edit -> updateTask(editor, dueInstant, onSuccess)
                TaskEditorMode.Hidden -> Unit
            }
        }

        fun deleteSelectedTask(onSuccess: (() -> Unit)? = null) {
            val taskId = mutableState.value.selectedTask?.id ?: return
            viewModelScope.launch {
                runCatching { deleteTaskUseCase(taskId) }
                    .onSuccess {
                        mutableState.update { it.copy(selectedTask = null, editor = TaskEditorState()) }
                        onSuccess?.invoke()
                    }.onFailure { throwable ->
                        mutableState.update { state ->
                            state.copy(
                                errorMessage = throwable.message ?: "Unable to delete task.",
                            )
                        }
                    }
            }
        }

        private fun createTask(
            listId: String,
            editor: TaskEditorState,
            dueInstant: Instant?,
            onSuccess: (() -> Unit)?,
        ) {
            viewModelScope.launch {
                mutableState.update { it.copy(editor = editor.copy(isSaving = true, validationError = null)) }
                runCatching {
                    createTaskUseCase(
                        TaskDraft(
                            listId = listId,
                            title = editor.title.trim(),
                            description = editor.description.takeIf(String::isNotBlank),
                            completed = editor.completed,
                            priority = editor.priority,
                            due = dueInstant,
                            tagIds = editor.tagIds.toList(),
                        ),
                    )
                }.onSuccess {
                    mutableState.update { it.copy(editor = TaskEditorState(), selectedTask = null) }
                    onSuccess?.invoke()
                }.onFailure { throwable ->
                    mutableState.update { state ->
                        state.copy(
                            editor = state.editor.copy(isSaving = false),
                            errorMessage = throwable.message ?: "Unable to create task.",
                        )
                    }
                }
            }
        }

        private fun updateTask(
            editor: TaskEditorState,
            dueInstant: Instant?,
            onSuccess: (() -> Unit)?,
        ) {
            val selectedTask = mutableState.value.selectedTask ?: return
            val tagsById = mutableState.value.tags.associateBy(Tag::id)
            val updatedTags = editor.tagIds.mapNotNull { tagsById[it] }

            viewModelScope.launch {
                mutableState.update { it.copy(editor = editor.copy(isSaving = true, validationError = null)) }
                runCatching {
                    updateTaskUseCase(
                        selectedTask.copy(
                            title = editor.title.trim(),
                            description = editor.description.takeIf(String::isNotBlank),
                            completed = editor.completed,
                            priority = editor.priority,
                            listId = editor.listId ?: selectedTask.listId,
                            due = dueInstant,
                            updatedAt = Instant.now(),
                            tags = updatedTags,
                        ),
                    )
                }.onSuccess {
                    mutableState.update { it.copy(editor = TaskEditorState()) }
                    onSuccess?.invoke()
                }.onFailure { throwable ->
                    mutableState.update { state ->
                        state.copy(
                            editor = state.editor.copy(isSaving = false),
                            errorMessage = throwable.message ?: "Unable to update task.",
                        )
                    }
                }
            }
        }

        private fun applyFilters(
            tasks: List<Task>,
            state: TaskListUiState,
        ): List<Task> {
            val normalizedQuery = state.searchQuery.trim().lowercase()

            val filteredByStatus =
                tasks.filter { task ->
                    when (state.filter.status) {
                        TaskStatusFilter.ALL -> true
                        TaskStatusFilter.OPEN -> !task.completed
                        TaskStatusFilter.COMPLETED -> task.completed
                    }
                }

            val filteredByList =
                filteredByStatus.filter { task ->
                    state.filter.listId?.let { listId ->
                        task.listId == listId
                    } ?: true
                }

            val filteredByTag =
                filteredByList.filter { task ->
                    state.filter.tagId?.let { tagId ->
                        task.tags.any { it.id == tagId }
                    } ?: true
                }

            val filteredByQuery =
                filteredByTag.filter { task ->
                    if (normalizedQuery.isBlank()) return@filter true
                    val tagNames = task.tags.joinToString(separator = " ") { it.name }.lowercase()
                    val description = task.description?.lowercase().orEmpty()
                    task.title.lowercase().contains(normalizedQuery) ||
                        description.contains(normalizedQuery) ||
                        tagNames.contains(normalizedQuery)
                }

            return filteredByQuery.sortedWith(comparatorFor(state.sortOption))
        }

        private fun comparatorFor(sortOption: TaskSortOption): Comparator<Task> =
            when (sortOption) {
                TaskSortOption.DUE ->
                    compareBy<Task> { it.due ?: Instant.MAX }
                        .thenByDescending { it.priority.weight }
                        .thenBy { it.title.lowercase() }
                TaskSortOption.PRIORITY ->
                    compareByDescending<Task> { it.priority.weight }
                        .thenBy { it.due ?: Instant.MAX }
                        .thenBy { it.title.lowercase() }
                TaskSortOption.TITLE ->
                    compareBy<Task> { it.title.lowercase() }
                        .thenBy { it.due ?: Instant.MAX }
            }

        private fun TaskEditorState.ensureListSelection(lists: List<TaskList>): TaskEditorState {
            if (mode == TaskEditorMode.Hidden) return this
            if (listId != null) return this
            val defaultList = lists.firstOrNull() ?: return this
            return copy(listId = defaultList.id)
        }

        private fun Task.toEditorState(): TaskEditorState {
            val localDate = due?.atZone(ZoneId.systemDefault())?.toLocalDate()
            return TaskEditorState(
                mode = TaskEditorMode.Edit,
                title = title,
                description = description.orEmpty(),
                listId = listId,
                dueInput = localDate?.toString().orEmpty(),
                dueDate = localDate,
                priority = priority,
                completed = completed,
                tagIds = tags.map(Tag::id).toSet(),
            )
        }

        private fun updateEditor(transform: (TaskEditorState) -> TaskEditorState) {
            mutableState.update { state ->
                state.copy(editor = transform(state.editor))
            }
        }

        private fun String.toLocalDateOrNull(): LocalDate? =
            runCatching { LocalDate.parse(this) }.getOrNull()
    }
