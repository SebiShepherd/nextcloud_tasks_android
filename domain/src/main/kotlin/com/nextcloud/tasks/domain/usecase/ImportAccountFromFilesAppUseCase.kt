package com.nextcloud.tasks.domain.usecase

import com.nextcloud.tasks.domain.model.NextcloudAccount
import com.nextcloud.tasks.domain.repository.AuthRepository

/**
 * Use case for importing an account from the Nextcloud Files app.
 * This requires the Nextcloud Files app to be installed and uses the Android SingleSignOn library.
 */
class ImportAccountFromFilesAppUseCase(
    private val repository: AuthRepository,
) {
    /**
     * Imports an account using the account name from the Files app.
     *
     * @param accountName The account name from Android's AccountManager
     * @return The imported NextcloudAccount
     */
    suspend operator fun invoke(accountName: String): NextcloudAccount = repository.importAccountFromFilesApp(accountName)
}
