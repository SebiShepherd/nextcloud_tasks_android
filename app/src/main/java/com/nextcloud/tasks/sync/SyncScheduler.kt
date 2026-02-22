package com.nextcloud.tasks.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schedules periodic background synchronization using WorkManager.
 */
@Singleton
class SyncScheduler
    @Inject
    constructor() {
        fun schedulePeriodicSync(context: Context) {
            val constraints =
                Constraints
                    .Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

            val syncRequest =
                PeriodicWorkRequestBuilder<SyncWorker>(
                    SYNC_INTERVAL_MINUTES,
                    TimeUnit.MINUTES,
                ).setConstraints(constraints)
                    .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                SyncWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest,
            )

            Timber.d("Periodic background sync scheduled (every $SYNC_INTERVAL_MINUTES minutes)")
        }

        fun cancelPeriodicSync(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(SyncWorker.WORK_NAME)
            Timber.d("Periodic background sync cancelled")
        }

        companion object {
            private const val SYNC_INTERVAL_MINUTES = 15L
        }
    }
