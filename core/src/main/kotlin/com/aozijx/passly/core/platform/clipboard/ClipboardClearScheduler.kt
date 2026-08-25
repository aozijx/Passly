package com.aozijx.passly.core.platform.clipboard

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class ClipboardClearScheduler(
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher,
) {
    private val lock = Any()
    private var pending: Job? = null

    fun schedule(
        ownershipToken: String,
        delayMillis: Long,
        clear: suspend (String) -> Unit,
    ) {
        synchronized(lock) {
            pending?.cancel()
            pending = scope.launch(dispatcher) {
                delay(delayMillis)
                clear(ownershipToken)
            }
        }
    }

    fun cancel() {
        synchronized(lock) {
            pending?.cancel()
            pending = null
        }
    }
}
