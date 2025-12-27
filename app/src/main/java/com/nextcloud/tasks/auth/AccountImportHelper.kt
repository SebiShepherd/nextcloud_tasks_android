package com.nextcloud.tasks.auth

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
            val ssoAccounts = AccountImporter.findAccounts(context)

            Timber.d("Found ${ssoAccounts.size} Nextcloud accounts via SSO")

            ssoAccounts.map { ssoAccount ->
                Timber.d("Account: ${ssoAccount.name} @ ${ssoAccount.url}")
                NextcloudFileAccount(
                    name = ssoAccount.name,
                    url = ssoAccount.url,
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to query Nextcloud accounts via SSO")
            emptyList()
        }
    }
}
