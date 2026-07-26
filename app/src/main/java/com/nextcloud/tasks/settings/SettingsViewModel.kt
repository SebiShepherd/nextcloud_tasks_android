package com.nextcloud.tasks.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nextcloud.tasks.data.network.CertTrustReset
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val certTrustReset: CertTrustReset,
    ) : ViewModel() {
        fun resetTrustedCertificates() {
            // evictAll() closes sockets → must not run on the main thread.
            viewModelScope.launch(Dispatchers.IO) {
                certTrustReset.reset()
            }
        }
    }
