package com.nextcloud.tasks.browser

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import timber.log.Timber

/**
 * Helper object for opening URLs in Chrome Custom Tabs.
 * Provides a seamless in-app browser experience for the Login Flow v2.
 */
object CustomTabsHelper {
    /**
     * Opens the given login URL in Chrome Custom Tabs.
     * Falls back to the default browser if Custom Tabs are not available.
     *
     * @param context Android context
     * @param loginUrl The URL to open (typically the Login Flow v2 login URL)
     */
    fun openLoginUrl(
        context: Context,
        loginUrl: String,
    ) {
        Timber.d("Opening login URL in browser: %s", loginUrl)

        val customTabsIntent =
            CustomTabsIntent
                .Builder()
                .setColorScheme(CustomTabsIntent.COLOR_SCHEME_SYSTEM)
                .setShowTitle(true)
                .setUrlBarHidingEnabled(false)
                .build()

        runCatching {
            customTabsIntent.launchUrl(context, Uri.parse(loginUrl))
            Timber.i("Launched Custom Tab successfully")
        }.onFailure { throwable ->
            // Fallback to regular browser
            Timber.w(throwable, "Failed to launch Custom Tab, falling back to default browser")
            fallbackToBrowser(context, loginUrl)
        }
    }

    /**
     * Fallback method to open URL in the default system browser.
     */
    private fun fallbackToBrowser(
        context: Context,
        url: String,
    ) {
        runCatching {
            val intent =
                Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            context.startActivity(intent)
            Timber.i("Opened URL in default browser")
        }.onFailure { throwable ->
            Timber.e(throwable, "Failed to open URL in any browser")
        }
    }
}
