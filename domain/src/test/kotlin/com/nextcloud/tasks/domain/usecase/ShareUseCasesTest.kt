package com.nextcloud.tasks.domain.usecase

import com.nextcloud.tasks.domain.model.ShareAccess
import com.nextcloud.tasks.domain.model.Sharee
import com.nextcloud.tasks.domain.model.ShareeSearchResult
import com.nextcloud.tasks.domain.model.ShareeType
import com.nextcloud.tasks.domain.repository.TasksRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ShareUseCasesTest {
    private val repository = mockk<TasksRepository>(relaxed = true)

    // ── GetShareesUseCase ─────────────────────────────────────────────────────

    @Test
    fun `GetShareesUseCase delegates to repository`() =
        runTest {
            val sharees =
                listOf(
                    Sharee("alice", "Alice", ShareAccess.READ_WRITE, ShareeType.USER),
                    Sharee("bob", "Bob", ShareAccess.READ, ShareeType.USER),
                )
            coEvery { repository.getSharees("list-1") } returns sharees

            val result = GetShareesUseCase(repository)("list-1")

            assertEquals(sharees, result)
            coVerify { repository.getSharees("list-1") }
        }

    // ── ShareListUseCase ──────────────────────────────────────────────────────

    @Test
    fun `ShareListUseCase delegates to repository with correct parameters`() =
        runTest {
            ShareListUseCase(repository)("list-1", "alice", ShareeType.USER, ShareAccess.READ_WRITE)

            coVerify { repository.shareList("list-1", "alice", ShareeType.USER, ShareAccess.READ_WRITE) }
        }

    @Test
    fun `ShareListUseCase works with GROUP sharee type`() =
        runTest {
            ShareListUseCase(repository)("list-2", "dev-team", ShareeType.GROUP, ShareAccess.READ)

            coVerify { repository.shareList("list-2", "dev-team", ShareeType.GROUP, ShareAccess.READ) }
        }

    // ── UnshareListUseCase ────────────────────────────────────────────────────

    @Test
    fun `UnshareListUseCase delegates to repository with correct parameters`() =
        runTest {
            UnshareListUseCase(repository)("list-1", "alice", ShareeType.USER)

            coVerify { repository.unshareList("list-1", "alice", ShareeType.USER) }
        }

    @Test
    fun `UnshareListUseCase works with GROUP sharee type`() =
        runTest {
            UnshareListUseCase(repository)("list-2", "dev-team", ShareeType.GROUP)

            coVerify { repository.unshareList("list-2", "dev-team", ShareeType.GROUP) }
        }

    // ── SearchShareesUseCase ──────────────────────────────────────────────────

    @Test
    fun `SearchShareesUseCase delegates to repository`() =
        runTest {
            val searchResults =
                listOf(
                    ShareeSearchResult("alice", "Alice Smith", ShareeType.USER),
                    ShareeSearchResult("bob", "Bob Jones", ShareeType.USER),
                )
            coEvery { repository.searchSharees("ali") } returns searchResults

            val result = SearchShareesUseCase(repository)("ali")

            assertEquals(searchResults, result)
            coVerify { repository.searchSharees("ali") }
        }

    @Test
    fun `SearchShareesUseCase returns empty list when no matches`() =
        runTest {
            coEvery { repository.searchSharees("xyz") } returns emptyList()

            val result = SearchShareesUseCase(repository)("xyz")

            assertEquals(emptyList(), result)
        }
}
