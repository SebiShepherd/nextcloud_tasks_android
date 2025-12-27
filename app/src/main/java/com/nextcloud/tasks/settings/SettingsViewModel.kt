package com.nextcloud.tasks.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nextcloud.tasks.preferences.Language
import com.nextcloud.tasks.preferences.LanguagePreferencesManager
import com.nextcloud.tasks.preferences.LocaleHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val languagePreferencesManager: LanguagePreferencesManager,
        private val localeHelper: LocaleHelper,
    ) : ViewModel() {
        private val _selectedLanguage = MutableStateFlow<Language>(Language.SYSTEM)
        val selectedLanguage: StateFlow<Language> = _selectedLanguage.asStateFlow()

        init {
            viewModelScope.launch {
                languagePreferencesManager.selectedLanguage.collect { code ->
                    _selectedLanguage.value = Language.fromCode(code)
                }
            }
        }

        fun setLanguage(language: Language) {
            viewModelScope.launch {
                val code = if (language == Language.SYSTEM) null else language.code
                languagePreferencesManager.setLanguage(code)
                // Apply locale immediately
                localeHelper.applyLanguage(code)
            }
        }
    }
