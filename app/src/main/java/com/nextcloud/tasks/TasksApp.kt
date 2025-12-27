package com.nextcloud.tasks

import android.app.Application
import com.nextcloud.tasks.preferences.LocaleHelper
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class TasksApp : Application() {
    @Inject
    lateinit var localeHelper: LocaleHelper

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Initialize locale/language preferences
        localeHelper.initialize()
    }
}
