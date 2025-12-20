package com.nextcloud.tasks.network

import android.content.Context
import androidx.core.content.ContextCompat
import com.nextcloud.tasks.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject

class NetworkPermissionChecker
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        fun hasInternetPermission(): Boolean {
            val permission = android.Manifest.permission.INTERNET
            val granted =
                ContextCompat.checkSelfPermission(
                    context,
                    permission,
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED

            if (BuildConfig.DEBUG && !granted) {
                // Timbers are planted in debug builds; keep a small breadcrumb for devs.
                Timber.w("INTERNET permission missing from merged manifest")
            }

            return granted
        }
    }
