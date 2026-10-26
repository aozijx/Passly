package com.aozijx.passly.data.repository.entry.command

import com.aozijx.passly.core.error.result.AppResult
import com.aozijx.passly.domain.access.port.SecureSessionAccessState
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Coalesces cleanup work and retains failed requests until storage is available again. */
internal class IconResourceCleanupQueue(
    private val scope: CoroutineScope,
    private val access: SecureSessionAccessState,
    private val reclaim: suspend (String) -> AppResult<IconResourceReclaimOutcome>,
) {
    private class Request
    private val pending = ConcurrentHashMap<String, Request>()
    private val mutex = Mutex()
    private val wakeups = Channel<Unit>(Channel.CONFLATED)

    init {
        scope.launch { access.authenticationState.collect { wakeups.trySend(Unit) } }
        scope.launch {
            for (ignored in wakeups) {
                // Database sealing may cancel a lease holder; keep the queue observer alive.
                scope.launch { drain() }.join()
            }
        }
    }

    fun enqueue(path: String) {
        if (path.isNotBlank()) pending[path] = Request()
    }

    fun request(path: String) {
        enqueue(path)
        wakeups.trySend(Unit)
    }

    suspend fun drain() = mutex.withLock {
        if (!access.hasFullSecureSessionAccess()) return@withLock
        for ((path, request) in pending.entries.map { it.key to it.value }) {
            if (!access.hasFullSecureSessionAccess()) return@withLock
            when (reclaim(path)) {
                is AppResult.Success -> pending.remove(path, request)
                is AppResult.Failure -> Unit
            }
        }
    }
}
