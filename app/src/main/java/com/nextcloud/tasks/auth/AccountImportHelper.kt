package com.nextcloud.tasks.auth

import android.accounts.AccountManager
import android.content.Context
import timber.log.Timber

/**
 * Helper for importing Nextcloud accounts from the Nextcloud Files app.
 * Based on the approach used by Nextcloud Talk app.
 *
 * NOTE: Currently not used. Account import only works for apps signed with the
 * same certificate as Nextcloud Files app (official NC apps like Talk, Notes).
 * Kept for future development if app becomes officially signed.
 */
object AccountImportHelper {
    private const val ACCOUNT_TYPE_NEXTCLOUD = "nextcloud"

    /**
     * Find available Nextcloud accounts on the device.
     *
     * Note: This approach works for apps signed with the same certificate as
     * Nextcloud Files app. For other apps, this may return empty list due to
     * Android 8.0+ account visibility restrictions.
     */
    fun findAvailableAccounts(context: Context): List<NextcloudFileAccount> {
        return try {
            val accountManager = AccountManager.get(context)
            val accounts = accountManager.getAccountsByType(ACCOUNT_TYPE_NEXTCLOUD)

            Timber.d("Found ${accounts.size} Nextcloud accounts")

            accounts.mapNotNull { account ->
                // Extract account information like Talk app does
                val lastAtPos = account.name.lastIndexOf("@")
                if (lastAtPos > 0) {
                    val serverUrl = account.name.substring(lastAtPos + 1)
                    val normalizedUrl = if (serverUrl.endsWith("/")) {
                        serverUrl.substring(0, serverUrl.length - 1)
                    } else {
                        serverUrl
                    }

                    // Add https:// if no protocol specified
                    val fullUrl = if (!normalizedUrl.startsWith("http")) {
                        "https://$normalizedUrl"
                    } else {
                        normalizedUrl
                    }

                    Timber.d("Account: ${account.name} -> $fullUrl")
                    NextcloudFileAccount(
                        name = account.name,
                        url = fullUrl,
                    )
                } else {
                    Timber.w("Account ${account.name} has invalid format (missing @)")
                    null
                }
            }
        } catch (e: SecurityException) {
            Timber.e(e, "Permission denied - app not signed with Nextcloud certificate")
            emptyList()
        } catch (e: Exception) {
            Timber.e(e, "Failed to query Nextcloud accounts")
            emptyList()
        }
    }
}
