package com.nextcloud.tasks.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "language_preferences")

/**
 * Manages language preference storage using DataStore.
 * Supports both system default and manual language selection.
 */
@Singleton
class LanguagePreferencesManager
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private val languageKey = stringPreferencesKey("selected_language")

        /**
         * Flow of the currently selected language code.
         * Returns null if system default is selected.
         */
        val selectedLanguage: Flow<String?> =
            context.dataStore.data.map { preferences ->
                preferences[languageKey]
            }

        /**
         * Sets the selected language.
         * @param languageCode Language code (e.g., "en", "de") or null for system default
         */
        suspend fun setLanguage(languageCode: String?) {
            context.dataStore.edit { preferences ->
                if (languageCode == null) {
                    preferences.remove(languageKey)
                } else {
                    preferences[languageKey] = languageCode
                }
            }
        }

        /**
         * Clears the language preference, reverting to system default.
         */
        suspend fun clearLanguage() {
            setLanguage(null)
        }
    }

/**
 * Supported languages in the application.
 */
enum class Language(
    val code: String,
    val displayName: String,
) {
    SYSTEM(
        code = "",
        displayName = "System Default",
    ),
    ENGLISH(
        code = "en",
        displayName = "English",
    ),
    GERMAN(
        code = "de",
        displayName = "Deutsch",
    ),
    ;

    companion object {
        fun fromCode(code: String?): Language =
            when (code) {
                null, "" -> SYSTEM
                "en" -> ENGLISH
                "de" -> GERMAN
                else -> SYSTEM
            }

        fun all(): List<Language> = listOf(SYSTEM, ENGLISH, GERMAN)
    }
}
