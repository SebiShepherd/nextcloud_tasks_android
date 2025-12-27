package com.nextcloud.tasks.preferences

import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper for managing app locale/language at runtime.
 * Uses AndroidX AppCompat's Per-App Language API which works on:
 * - Android 13+ (API 33+): Native Per-App Language Preferences
 * - Android 8-12 (API 26-32): AppCompat backport
 */
@Singleton
class LocaleHelper
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val languagePreferencesManager: LanguagePreferencesManager,
    ) {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

        /**
         * Initializes the locale based on saved preferences.
         * Should be called early in application startup.
         */
        fun initialize() {
            scope.launch {
                val savedLanguage = languagePreferencesManager.selectedLanguage.first()
                applyLanguage(savedLanguage)
            }
        }

        /**
         * Applies the given language to the app.
         * @param languageCode Language code (e.g., "en", "de") or null for system default
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
