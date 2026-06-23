package com.nextcloud.tasks.data.network

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tracks whether the app is currently in the foreground, exposed as a [StateFlow].
 *
 * cert4android only prompts the user to accept an untrusted certificate while the app is in the
 * foreground; for background syncs ([isInForeground] == false) it rejects unaccepted certificates
 * silently. The value defaults to `true` until [start] runs so an interactive first launch can show
 * the prompt even before the process-lifecycle observer has fired.
 */
@Singleton
class AppForegroundMonitor
    @Inject
    constructor() {
        private val _isInForeground = MutableStateFlow(true)
        val isInForeground: StateFlow<Boolean> = _isInForeground.asStateFlow()

        /**
         * Registers a process-lifecycle observer. Must be called on the main thread (e.g. from
         * Application.onCreate), because [ProcessLifecycleOwner] requires it.
         */
        fun start() {
            ProcessLifecycleOwner.get().lifecycle.addObserver(
                object : DefaultLifecycleObserver {
                    override fun onStart(owner: LifecycleOwner) {
                        _isInForeground.value = true
                    }

                    override fun onStop(owner: LifecycleOwner) {
                        _isInForeground.value = false
                    }
                },
            )
        }
    }
