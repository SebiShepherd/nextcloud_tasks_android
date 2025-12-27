package com.nextcloud.tasks

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class TasksApp : Application() {
    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Note: Locale initialization is NOT needed here.
        // AppCompatDelegate automatically persists and restores the selected
        // locale in SharedPreferences. It will be applied automatically when
        // MainActivity starts.
    }
}
