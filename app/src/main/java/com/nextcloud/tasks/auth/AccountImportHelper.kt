package com.nextcloud.tasks.auth

import android.app.Activity
import android.content.Context
import com.nextcloud.android.sso.AccountImporter
import com.nextcloud.android.sso.exceptions.AndroidGetAccountsPermissionNotGranted
import com.nextcloud.android.sso.exceptions.NextcloudFilesAppNotInstalledException
import com.nextcloud.android.sso.helper.SingleAccountHelper
import timber.log.Timber

/**
 * Helper for importing Nextcloud accounts from the Nextcloud Files app.
 * Uses Android-SingleSignOn library's system account picker.
 */
object AccountImportHelper {
    /**
     * Show system account picker to select a Nextcloud account.
     * This is the recommended approach for Android 8.0+ due to account visibility restrictions.
     *
     * Returns the selected account info or null if cancelled/failed.
     */
    fun pickAccount(
        activity: Activity,
        onAccountSelected: (NextcloudFileAccount) -> Unit,
        onError: (String) -> Unit,
    ) {
        try {
            Timber.d("Launching system account picker for Nextcloud accounts")

            // Use SSO library's account picker
            // This shows a system dialog and grants access to the selected account
            AccountImporter.pickNewAccount(activity) { account ->
                if (account != null) {
                    Timber.i("Account selected via picker: ${account.name}")
                    onAccountSelected(
                        NextcloudFileAccount(
                            name = account.name,
                            url = account.url,
                        ),
                    )
                } else {
                    Timber.d("Account picker cancelled by user")
                }
            }
        } catch (e: NextcloudFilesAppNotInstalledException) {
            val message = "Nextcloud Files app not installed"
            Timber.e(e, message)
            onError(message)
        } catch (e: AndroidGetAccountsPermissionNotGranted) {
            val message = "Permission to access accounts not granted"
            Timber.e(e, message)
            onError(message)
        } catch (e: Exception) {
            val message = "Failed to pick account: ${e.message}"
            Timber.e(e, message)
            onError(message)
        }
    }
}
