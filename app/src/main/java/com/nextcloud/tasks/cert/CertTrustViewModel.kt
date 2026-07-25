package com.nextcloud.tasks.cert

import androidx.lifecycle.ViewModel
import com.nextcloud.tasks.data.network.CertDecisionRegistry
import com.nextcloud.tasks.data.network.PendingCertDecision
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class CertTrustViewModel
    @Inject
    constructor(
        private val registry: CertDecisionRegistry,
    ) : ViewModel() {
        val pending: StateFlow<PendingCertDecision?> = registry.pending

        fun onDecision(
            fingerprint: String,
            trusted: Boolean,
        ) {
            registry.submitDecision(fingerprint, trusted)
        }
    }
