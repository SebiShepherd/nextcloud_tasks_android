package com.nextcloud.tasks.auth

import android.accounts.AccountManager
import android.content.Context
import timber.log.Timber

/**
 * Helper for querying Nextcloud accounts from the Android AccountManager.
 * Used to import accounts from the Nextcloud Files app.
 */
object AccountImportHelper {
    // Try multiple account types as different versions of NC Files app might use different types
    private val ACCOUNT_TYPES = listOf(
        "nextcloud",
        "com.nextcloud",
        "owncloud",
        "com.owncloud",
    )

    // Try multiple possible keys for server URL
    private val SERVER_URL_KEYS = listOf(
        "oc_base_url",
        "server_url",
        "url",
        "baseUrl",
        "serverUrl",
    )

    /**
     * Query all Nextcloud accounts registered in the system.
     * These accounts are typically added by the Nextcloud Files app.
     */
    fun getNextcloudAccounts(context: Context): List<NextcloudFileAccount> {
        return try {
            val accountManager = AccountManager.get(context)
            val results = mutableListOf<NextcloudFileAccount>()

            // Log all available account types for debugging
            val allAccounts = accountManager.accounts
            Timber.d("Total accounts on device: ${allAccounts.size}")
            allAccounts.groupBy { it.type }.forEach { (type, accounts) ->
                Timber.d("Account type '$type': ${accounts.size} accounts")
            }

            // Try each possible account type
            for (accountType in ACCOUNT_TYPES) {
                Timber.d("Querying accounts with type: $accountType")
                val accounts = accountManager.getAccountsByType(accountType)
                Timber.d("Found ${accounts.size} accounts with type '$accountType'")

                accounts.forEach { account ->
                    Timber.d("Processing account: ${account.name} (type: ${account.type})")

                    // Try to find server URL using different keys
                    var serverUrl: String? = null
                    for (key in SERVER_URL_KEYS) {
                        serverUrl = accountManager.getUserData(account, key)
                        if (serverUrl != null) {
                            Timber.d("Found server URL for ${account.name} using key '$key': $serverUrl")
                            break
                        }
                    }

                    if (serverUrl != null) {
                        results.add(
                            NextcloudFileAccount(
                                name = account.name,
                                url = serverUrl,
                            ),
                        )
                    } else {
                        // Log all available user data keys for debugging
                        Timber.w("Account ${account.name} has no server URL in expected keys")
                        Timber.w("This might help: tried keys: ${SERVER_URL_KEYS.joinToString()}")
                    }
                }
            }

            Timber.i("Found ${results.size} importable Nextcloud accounts")
            results

        } catch (e: SecurityException) {
            Timber.e(e, "Permission denied to access accounts - check GET_ACCOUNTS permission")
            emptyList()
        } catch (e: Exception) {
            Timber.e(e, "Failed to query Nextcloud accounts")
            emptyList()
        }
    }
}
