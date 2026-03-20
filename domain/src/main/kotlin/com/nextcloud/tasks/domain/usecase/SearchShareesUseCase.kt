package com.nextcloud.tasks.domain.usecase

import com.nextcloud.tasks.domain.model.ShareeSearchResult
import com.nextcloud.tasks.domain.repository.TasksRepository

class SearchShareesUseCase(
    private val repository: TasksRepository,
) {
    suspend operator fun invoke(query: String): List<ShareeSearchResult> = repository.searchSharees(query)
}
