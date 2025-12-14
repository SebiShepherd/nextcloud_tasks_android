package com.nextcloud.tasks

import android.app.Application
import com.jakewharton.timber.log.Timber
import com.nextcloud.tasks.BuildConfig
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class TasksApp : Application() {
    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
