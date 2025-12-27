package com.nextcloud.tasks.preferences

import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper for managing app locale/language at runtime.
 * Uses AndroidX AppCompat's Per-App Language API which works on:
 * - Android 13+ (API 33+): Native Per-App Language Preferences
 * - Android 8-12 (API 26-32): AppCompat backport
 *
 * Note: AppCompatDelegate automatically persists the selected locale in
 * SharedPreferences and restores it on app restart. No manual initialization needed!
 */
@Singleton
class LocaleHelper
    @Inject
    constructor() {
        /**
         * Applies the given language to the app.
         * @param languageCode Language code (e.g., "en", "de") or null for system default
         *
         * AppCompatDelegate automatically persists this selection and applies it
         * across app restarts. With android:configChanges="locale" in the manifest,
         * the change happens smoothly without recreating the activity.
         */
        fun applyLanguage(languageCode: String?) {
            val localeList =
                if (languageCode.isNullOrBlank()) {
                    // System default
                    LocaleListCompat.getEmptyLocaleList()
                } else {
                    // Specific language
                    LocaleListCompat.forLanguageTags(languageCode)
                }

            AppCompatDelegate.setApplicationLocales(localeList)

            Timber.i(
                "Locale applied: %s (API %d)",
                languageCode ?: "system",
                Build.VERSION.SDK_INT,
            )
        }

        /**
         * Gets the currently active locale.
         */
        fun getCurrentLocale(): String? {
            val locales = AppCompatDelegate.getApplicationLocales()
            return if (locales.isEmpty) {
                null // System default
            } else {
                locales[0]?.toLanguageTag()
            }
        }
    }
