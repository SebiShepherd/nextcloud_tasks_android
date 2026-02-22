package com.nextcloud.tasks

import android.app.Application
import androidx.work.Configuration
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

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Schedule periodic background synchronization
        syncScheduler.schedulePeriodicSync(this)

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
