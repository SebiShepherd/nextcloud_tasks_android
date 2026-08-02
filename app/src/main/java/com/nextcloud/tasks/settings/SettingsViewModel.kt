package com.nextcloud.tasks.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nextcloud.tasks.data.AppPreferences
import com.nextcloud.tasks.data.network.CertTrustReset
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val certTrustReset: CertTrustReset,
        private val appPreferences: AppPreferences,
    ) : ViewModel() {
        val perListSortEnabled =
            appPreferences.perListSortEnabled
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

        fun setPerListSortEnabled(enabled: Boolean) {
            viewModelScope.launch { appPreferences.setPerListSortEnabled(enabled) }
        }

        fun resetTrustedCertificates() {
            // evictAll() closes sockets → must not run on the main thread.
            viewModelScope.launch(Dispatchers.IO) {
                certTrustReset.reset()
            }
        }
    }
