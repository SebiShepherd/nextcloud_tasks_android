package com.nextcloud.tasks

import android.app.Application
import com.nextcloud.tasks.data.sync.SyncManager
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class TasksApp : Application() {
    @Inject lateinit var syncManager: SyncManager

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        syncManager.onAppStart()
    }
}
