package com.aozijx.passly.app.settings

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Broadcasts explicit settings retries to long-lived process readers. */
@Singleton
class SettingsReadRetrySignal
    @Inject
    constructor() {
        private val requests = MutableStateFlow(0L)

        internal val version: StateFlow<Long> = requests

        fun request() {
            requests.value++
        }
    }
