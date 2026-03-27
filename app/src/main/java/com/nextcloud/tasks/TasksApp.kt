package com.nextcloud.tasks

import android.app.Application
import androidx.work.Configuration
import com.nextcloud.tasks.data.auth.AuthTokenProvider
import com.nextcloud.tasks.data.sync.PushSyncManager
import com.nextcloud.tasks.sync.SyncScheduler
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class TasksApp :
    Application(),
    Configuration.Provider {
    @Inject
    lateinit var workerFactory: androidx.hilt.work.HiltWorkerFactory

    @Inject
    lateinit var syncScheduler: SyncScheduler

    @Inject
    lateinit var authTokenProvider: AuthTokenProvider

    @Inject
    lateinit var pushSyncManager: PushSyncManager

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Schedule periodic background synchronization (always active as fallback)
        syncScheduler.schedulePeriodicSync(this)

        // Start real-time push sync manager directly (avoids ForegroundServiceStartNotAllowedException
        // when the app is started in the background on Android 12+). The foreground service with its
        // persistent notification is started separately by MainActivity when the UI is visible.
        if (authTokenProvider.activeAccountId() != null) {
            pushSyncManager.start()
        }

        // Note: Locale initialization is NOT needed here.
        // AppCompatDelegate automatically persists and restores the selected
        // locale in SharedPreferences. It will be applied automatically when
        // MainActivity starts.
    }

    override val workManagerConfiguration: Configuration
        get() =
            Configuration
                .Builder()
                .setWorkerFactory(workerFactory)
                .build()
}
