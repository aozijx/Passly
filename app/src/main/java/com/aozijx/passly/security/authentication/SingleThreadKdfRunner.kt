package com.aozijx.passly.security.authentication

import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class SingleThreadKdfRunner(
    private val executor: ExecutorService = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "passly-kdf").apply { isDaemon = true }
    }
) : KdfRunner {
    override suspend fun <T> execute(secret: SecretChars, operation: KdfOperation<T>): T {
        coroutineContext.ensureActive()
        val workerSecret = secret.copyForWorker()
        return suspendCancellableCoroutine { continuation ->
            val cancelled = AtomicBoolean(false)
            continuation.invokeOnCancellation { cancelled.set(true) }
            try {
                executor.execute {
                    var result: T? = null
                    var handedOff = false
                    try {
                        result = operation.run(workerSecret)
                        if (!cancelled.get()) {
                            continuation.resume(result) { _, cancelledResult, _ ->
                                (cancelledResult as? DiscardableResult)?.discard()
                            }
                            handedOff = true
                        }
                    } catch (failure: Throwable) {
                        if (!cancelled.get()) continuation.resumeWithException(failure)
                    } finally {
                        workerSecret.fill('\u0000')
                        if (!handedOff) (result as? DiscardableResult)?.discard()
                    }
                }
            } catch (failure: Throwable) {
                workerSecret.fill('\u0000')
                continuation.resumeWithException(failure)
            }
        }
    }

    override fun close() {
        executor.shutdown()
    }
}
