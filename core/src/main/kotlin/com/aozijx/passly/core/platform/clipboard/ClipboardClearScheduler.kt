package com.aozijx.passly.core.platform.clipboard

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal class ClipboardClearScheduler(
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher,
) {
    private val lock = Any()
    private var pending: Job? = null

    fun replace(
        ownershipToken: String,
        delayMillis: Long?,
        publish: () -> Unit,
        clear: (String) -> Unit,
    ) {
        synchronized(lock) {
            publish()
            cancelPendingLocked()
            if (delayMillis != null) scheduleLocked(ownershipToken, delayMillis, clear)
        }
    }

    fun <T> withLock(operation: () -> T): T = synchronized(lock, operation)

    fun cancel() {
        synchronized(lock) {
            cancelPendingLocked()
        }
    }

    private fun scheduleLocked(
        ownershipToken: String,
        delayMillis: Long,
        clear: (String) -> Unit,
    ) {
        lateinit var scheduled: Job
        scheduled = scope.launch(dispatcher, start = CoroutineStart.LAZY) {
            delay(delayMillis)
            synchronized(lock) {
                if (pending === scheduled) {
                    clear(ownershipToken)
                    pending = null
                }
            }
        }
        pending = scheduled
        scheduled.start()
    }

    private fun cancelPendingLocked() {
        pending?.cancel()
        pending = null
    }
}
