package com.nextcloud.tasks.auth

import android.accounts.AccountManager
import android.content.Context
import com.nextcloud.android.sso.AccountImporter
import timber.log.Timber

/**
 * Helper for querying Nextcloud accounts from the Android AccountManager.
 * Used to import accounts from the Nextcloud Files app.
 */
object AccountImportHelper {
    /**
     * Query all Nextcloud accounts registered in the system using Android-SingleSignOn.
     * These accounts are typically added by the Nextcloud Files app.
     */
    fun getNextcloudAccounts(context: Context): List<NextcloudFileAccount> {
        return try {
            Timber.d("Querying Nextcloud accounts via Android-SingleSignOn library")

            // Use Android-SingleSignOn library to find accounts
            // This properly handles Android 8.0+ account visibility restrictions
            val accounts = AccountImporter.findAccounts(context)

            Timber.d("Found ${accounts.size} Nextcloud accounts via SSO")

            // Convert Android Account objects to NextcloudFileAccount
            val accountManager = AccountManager.get(context)
            accounts.mapNotNull { account ->
                // Since we got these accounts from SSO, we should be able to read their data
                val serverUrl = accountManager.getUserData(account, "server_url")
                    ?: accountManager.getUserData(account, "oc_base_url")

                if (serverUrl != null) {
                    Timber.d("Account: ${account.name} @ $serverUrl")
                    NextcloudFileAccount(
                        name = account.name,
                        url = serverUrl,
                    )
                } else {
                    Timber.w("Account ${account.name} found via SSO but has no server URL")
                    null
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to query Nextcloud accounts via SSO")
            emptyList()
        }
    }
}
