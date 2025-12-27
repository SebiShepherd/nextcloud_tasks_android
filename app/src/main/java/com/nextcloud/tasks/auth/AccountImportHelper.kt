package com.nextcloud.tasks.auth

import android.accounts.AccountManager
import android.content.Context
import timber.log.Timber

/**
 * Helper for querying Nextcloud accounts from the Android AccountManager.
 * Used to import accounts from the Nextcloud Files app.
 */
object AccountImportHelper {
    private const val ACCOUNT_TYPE_NEXTCLOUD = "nextcloud"

    /**
     * Query all Nextcloud accounts registered in the system.
     * These accounts are typically added by the Nextcloud Files app.
     */
    fun getNextcloudAccounts(context: Context): List<NextcloudFileAccount> {
        return try {
            val accountManager = AccountManager.get(context)
            val accounts = accountManager.getAccountsByType(ACCOUNT_TYPE_NEXTCLOUD)

            accounts.mapNotNull { account ->
                // Account name is typically "username@server.com"
                // We need to extract server URL from account data
                val serverUrl = accountManager.getUserData(account, "server_url")
                    ?: accountManager.getUserData(account, "oc_base_url")

                if (serverUrl != null) {
                    NextcloudFileAccount(
                        name = account.name,
                        url = serverUrl,
                    )
                } else {
                    Timber.w("Account ${account.name} has no server URL, skipping")
                    null
                }
            }
        } catch (e: SecurityException) {
            Timber.e(e, "Permission denied to access accounts")
            emptyList()
        } catch (e: Exception) {
            Timber.e(e, "Failed to query Nextcloud accounts")
            emptyList()
        }
    }
}
